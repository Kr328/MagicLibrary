plugins {
    id("com.android.library")
    `maven-publish`
}

android {
    namespace = "com.github.kr328.magic"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

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

    compileOnly("androidx.annotation:annotation:1.7.0")
}
