import org.gradle.kotlin.dsl.provideDelegate

plugins {
    java
}

group = "org.dedrakot"
version = "0.1-SNAPSHOT"

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("https://kotlin.bintray.com/kotlinx") }
    maven { url = uri("https://kotlin.bintray.com/kotlin-js-wrappers") }
}

val junitVersion:String by parent!!
val kotlinVersion:String by parent!!
val logbackVersion:String by parent!!


dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    implementation(kotlin("stdlib-jdk8", kotlinVersion))

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
    useJUnitPlatform()
}
