package command

import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Command.OptionType
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.util.*
import javax.xml.parsers.SAXParserFactory
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KParameter.Kind.EXTENSION_RECEIVER
import kotlin.reflect.KParameter.Kind.INSTANCE
import kotlin.reflect.KParameter.Kind.VALUE
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

class XMLCommandLoader private constructor() : DefaultHandler() {

    val commandCount: Int
        get() = commandStack.size

    private val commandStack = Stack<XMLCommand>().also {
        it.push(XMLCommand("", ""))
    }

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        fun missing(elementName: String, attributeName: String) =
            SAXException("$elementName element must have '$attributeName' attribute")

        when (qName) {
            "command" -> {
                val name = attributes.getValue("name") ?: throw missing("command", "name")
                val comment = attributes.getValue("comment") ?: throw missing("arg", "comment")
                val command = XMLCommand(name, comment)

                commandStack.push(command)
            }
            "arg" -> {
                val name = attributes.getValue("name") ?: throw missing("arg", "name")
                val type = attributes.getValue("type") ?: throw missing("arg", "type")
                val comment = attributes.getValue("comment") ?: throw missing("arg", "comment")
                val required = attributes.getValue("required").toBoolean()
                val command = commandStack.peek()

                command.arguments += XMLArg(name, OptionType.valueOf(type.toUpperCase()), comment, required)
            }
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        when (qName) {
            "command" -> {
                val finishedCommand = commandStack.pop()
                commandStack.peek().subCommands += finishedCommand
            }
        }
    }

    inline fun <reified T : CommandOptions> createExecutor(packageName: String): CommandExecutor<T, CommandFailure> =
        createExecutor(packageName, T::class)

    fun <T : CommandOptions> createExecutor(
        packageName: String,
        contextClazz: KClass<T>
    ): CommandExecutor<T, CommandFailure> {
        val commandMap = HashMap<String, CommandExecutor<T, CommandFailure>>()

        commandStack.peek().subCommands.forEach { command ->
            val name = command.name

            // The class names to try
            val toTry = arrayOf(
                "$packageName.${name.capitalize()}Command",
                "$packageName.${name.capitalize()}"
            )

            // Try to find the class, otherwise throw a (hopefully helpful) error
            val clazz = toTry.asSequence().map {
                try {
                    Class.forName(it)
                } catch (e: ClassNotFoundException) {
                    null
                }
            }.filterNotNull().firstOrNull()?.kotlin
                ?: throw RuntimeException(
                    "Could not find implementation class for command '$name'. " +
                            "Tried ${toTry.joinToString(" and ") { "'$it'" }}."
                )

            // Get the instance, fail if clazz is not an `object`
            val instance = clazz.objectInstance
                ?: throw RuntimeException("${clazz.qualifiedName} is not an object declaration")

            val executor = command.createExecutor(instance, clazz, contextClazz)
            commandMap[command.name] = executor
        }

        return object : CommandExecutor<T, CommandFailure> {
            override suspend fun execute(context: T): Result<CommandFailure> {
                return (commandMap[context.name] ?: return Result.failure(CommandFailure.DOES_NOT_EXIST))
                    .execute(context)
            }
        }
    }

    fun buildCommands(): List<CommandUpdateAction.CommandData> = this.commandStack.peek().subCommands.map {
        it.createCommandData()
    }

    companion object {
        fun load(stream: InputStream): XMLCommandLoader {
            val loader = XMLCommandLoader()
            SAXParserFactory.newInstance().newSAXParser().parse(stream, loader)

            return loader
        }
    }

    private data class XMLCommand(
        val name: String,
        val comment: String,
        val arguments: MutableList<XMLArg> = mutableListOf(),
        val subCommands: MutableList<XMLCommand> = mutableListOf()
    ) {
        private val logger = KotlinLogging.logger {}

        fun <T : CommandOptions> createExecutor(
            instance: Any,
            clazz: KClass<*>,
            contextClazz: KClass<T>
        ): CommandExecutor<T, CommandFailure> {
            val executeFun = clazz.members.find { it.name == "execute" }
                ?: throw RuntimeException("${clazz.qualifiedName} has no `execute` function")
            if (!executeFun.isSuspend) throw RuntimeException("'$executeFun' must be suspend")

            val valueParameters = executeFun.valueParameters
            val specialParams = executeFun.parameters.minus(valueParameters)

            val gottenSignature by lazy {
                valueParameters.joinToString(", ") { it.type.jvmErasure.qualifiedName ?: "UNKNOWN" }
            }
            val wantedSignature by lazy {
                (listOf(contextClazz.qualifiedName!!) + arguments.map { "${it.name}: {it.type.name.toLowerCase().capitalize()}" })
                    .joinToString(", ")
            }

            // First off, let's figure out what to do with the special parameters
            val specials = specialParams.map {
                it to when (it.kind) {
                    INSTANCE -> { _: T -> instance }
                    EXTENSION_RECEIVER -> TODO()
                    VALUE -> unreachable()
                }
            }

            if (valueParameters.size <= arguments.size) {
                logger.warn { "'$executeFun' has too little arguments, but we can still proceed. Current signature: ($gottenSignature), wanted: ($wantedSignature)." }
            }

            // Now, we match every value parameter to the command argument.
            val values = valueParameters.mapIndexed { idx, par ->
                // The first parameter should always be the context
                par to if (idx == 0) {
                    if (par.type.isSupertypeOf(contextClazz.starProjectedType)) {
                        { ctx: T -> ctx }
                    } else throw RuntimeException("'$executeFun': first argument must be of type '${contextClazz.qualifiedName}'")
                } else {
                    val arg = arguments.getOrElse(idx - 1) {
                        throw RuntimeException("'$executeFun' has more parameters than the command! Current signature: ($gottenSignature), wanted: ($wantedSignature).")
                    }

                    // If `arg` is optional, the parameter must be nullable!
                    if (!arg.required && !par.type.isMarkedNullable) throw RuntimeException("$par is linked to the optional argument `${arg.name}`, thus, it must be nullable, but it is not.");

                    { ctx: T -> ctx.getOption(arg.name, par.type.jvmErasure) }
                }
            }

            val execMap = (specials + values).toMap()

            return object : CommandExecutor<T, CommandFailure> {
                override suspend fun execute(context: T): Result<CommandFailure> {
                    executeFun.callSuspendBy(execMap.mapValues { (_, exec) -> exec(context) })

                    return Result.success()
                }
            }
        }

        fun createCommandData(): CommandUpdateAction.CommandData {
            val data = CommandUpdateAction.CommandData(name, comment)
            arguments.forEach {
                data.addOption(
                    CommandUpdateAction.OptionData(it.type, it.name, it.comment)
                        .setRequired(it.required)
                )
            }
            subCommands.forEach {
                val subData = CommandUpdateAction.SubcommandData(it.name, it.comment)
                it.arguments.forEach { arg ->
                    subData.addOption(
                        CommandUpdateAction.OptionData(arg.type, arg.name, arg.comment)
                            .setRequired(arg.required)
                    )
                }
                data.addSubcommand(subData)
            }
            return data
        }
    }

    private data class XMLArg(val name: String, val type: OptionType, val comment: String, val required: Boolean)

}

@Suppress("NOTHING_TO_INLINE")
private inline fun unreachable(): Nothing = throw NotImplementedError()
