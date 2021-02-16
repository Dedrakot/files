pluginManagement {
    plugins {
        kotlin("jvm") version "1.4.30"
        kotlin("plugin.serialization") version "1.4.30"
    }
}

rootProject.name = "files"
include("server", "client")