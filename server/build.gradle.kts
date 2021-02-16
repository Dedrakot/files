plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("app-conventions")
}

val ktorVersion: String by parent!!
val kodeinVersion: String by parent!!
val serializationVersion: String by parent!!
val javaVersion: String by parent!!

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

compileKotlin.kotlinOptions {
    jvmTarget = javaVersion
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")

    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    implementation("org.kodein.di:kodein-di:$kodeinVersion")

    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
}