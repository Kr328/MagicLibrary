plugins {
    id("com.android.library")
    `maven-publish`
}

android {
    namespace = "com.github.kr328.magic"

    publishing {
        multipleVariants("all") {
            withSourcesJar()
            includeBuildTypeValues("debug", "release")
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create(project.name, MavenPublication::class) {
                artifactId = project.name

                from(components["all"])
            }
        }
    }
}

dependencies {
    compileOnly(project(":hideapi"))
}
