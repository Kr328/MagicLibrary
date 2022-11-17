rootProject.name = "MagicLibrary"

include(":library")
include(":hideapi")

pluginManagement {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://maven.kr328.app/releases")
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://maven.kr328.app/releases")
        mavenLocal()
    }
}
