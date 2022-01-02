import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
    }
}

subprojects {
    group = "com.github.kr328.magic"
    version = "1.2"

    repositories {
        google()
        mavenCentral()
    }

    apply(plugin = "com.android.library")

    extensions.configure<LibraryExtension> {
        compileSdk = 31

        defaultConfig {
            minSdk = 26
            targetSdk = 31
        }
    }

    if (name == "library") {
        apply(plugin = "maven-publish")

        afterEvaluate {
            extensions.configure<PublishingExtension> {
                repositories {
                    mavenLocal()

                    maven {
                        name = "kr328app"
                        url = uri("https://maven.kr328.app/releases")
                        credentials(PasswordCredentials::class.java)
                    }
                }
                publications {
                    create("main", MavenPublication::class.java) {
                        pom {
                            name.set("MagicLibrary")
                            description.set("MagicLibrary")
                            url.set("https://github.com/Kr328/MagicLibrary")
                            licenses {
                                license {
                                    name.set("MIT License")
                                }
                            }
                            developers {
                                developer {
                                    name.set("Kr328")
                                }
                            }
                            scm {
                                connection.set("scm:git:https://github.com/Kr328/MagicLibrary.git")
                                url.set("https://github.com/Kr328/MagicLibrary.git")
                            }
                        }

                        afterEvaluate {
                            from(components["release"])
                        }

                        val sourcesJar = tasks.register("sourcesJar", type = Jar::class) {
                            archiveClassifier.set("sources")

                            val extensions = project.extensions
                            from(extensions.getByType(BaseExtension::class.java).sourceSets["main"].java.srcDirs)
                        }

                        artifact(sourcesJar)

                        groupId = project.group.toString()
                        version = project.version.toString()
                        artifactId = project.name
                    }
                }
            }
        }
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}