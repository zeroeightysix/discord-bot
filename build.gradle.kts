import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
}

group = "me.zeroeightsix"
version = "1.0-SNAPSHOT"

val jdaVersion = "4.3.0_277"
val ktxVersion = "985db8173e"
val ktormVersion = "3.4.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    implementation("org.jetbrains:annotations:22.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    // Images
    implementation("com.sksamuel.scrimage:scrimage-core:4.0.22")
    implementation("com.sksamuel.scrimage:scrimage-filters:4.0.22")
    implementation("com.github.zh79325:open-gif:1.0.4")
    // Cache
    implementation("org.cache2k:cache2k-api:2.2.0.Final")
    implementation("org.cache2k:cache2k-core:2.2.0.Final")
    // JDA
    implementation("net.dv8tion:JDA:${jdaVersion}")
    implementation("com.github.minndevelopment:jda-ktx:${ktxVersion}")
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
    // Database stuff
    implementation("org.ktorm:ktorm-core:$ktormVersion")
    implementation("org.ktorm:ktorm-support-mysql:$ktormVersion")
    implementation("mysql:mysql-connector-java:8.0.25")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
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