import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
}

group = "me.ridan"
version = "1.0-SNAPSHOT"

val jdaVersion = "4.2.1_253"
val ktxVersion = "1a45395155"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
//    implementation("net.dv8tion:JDA:${jdaVersion}")
    implementation("com.github.DV8FromTheWorld:JDA:feature~slash-commands-SNAPSHOT")
    implementation("com.github.minndevelopment:jda-ktx:${ktxVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.32")
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    implementation("org.apache.logging.log4j:log4j-core:2.11.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")
    // Database stuff
    implementation("org.jetbrains.exposed:exposed-core:0.30.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.30.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.30.1")
    implementation("mysql:mysql-connector-java:5.1.48")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "13"
        freeCompilerArgs = freeCompilerArgs + "-Xallow-result-return-type"
    }
}