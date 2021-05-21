import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
}

group = "me.zeroeightsix"
version = "1.0-SNAPSHOT"

val jdaVersion = "4.2.1_253"
val ktxVersion = "985db8173e"
val ktormVersion = "3.4.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    implementation("org.jetbrains:annotations:20.1.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    // Images
    implementation("com.sksamuel.scrimage:scrimage-core:4.0.18")
    implementation("com.sksamuel.scrimage:scrimage-filters:4.0.18")
    // Cache
    implementation("org.cache2k:cache2k-api:2.0.0.Final")
    implementation("org.cache2k:cache2k-core:2.0.0.Final")
    // JDA
//    implementation("net.dv8tion:JDA:${jdaVersion}")
    implementation("com.github.DV8FromTheWorld:JDA:feature~slash-commands-SNAPSHOT")
    implementation("com.github.minndevelopment:jda-ktx:${ktxVersion}")
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    implementation("org.apache.logging.log4j:log4j-core:2.11.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")
    // Database stuff
    implementation("org.ktorm:ktorm-core:$ktormVersion")
    implementation("org.ktorm:ktorm-support-mysql:$ktormVersion")
    implementation("mysql:mysql-connector-java:5.1.48")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

configurations.forEach {
    it.exclude("ch.qos.logback")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xallow-result-return-type" + "-Xopt-in=io.lettuce.core.ExperimentalLettuceCoroutinesApi"
    }
}