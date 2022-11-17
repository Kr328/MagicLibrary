import com.android.build.gradle.BaseExtension

plugins {
    id("com.android.library") version "7.3.0" apply false
}

subprojects {
    group = "com.github.kr328.magic"
    version = "1.4"

    plugins.withId("com.android.base") {
        extensions.configure<BaseExtension> {
            compileSdkVersion(33)

            defaultConfig {
                minSdk = 26
                targetSdk = 33
            }
        }
    }

    plugins.withId("maven-publish") {
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
                withType<MavenPublication> {
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

                    groupId = project.group.toString()
                    version = project.version.toString()
                }
            }
        }
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}