plugins {
    kotlin("js") version "1.4.30"
}

group = "de.alxgrk"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-js-wrappers") }
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.js")
    }

    repositories {
        mavenCentral()
    }

    val implementation by configurations

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.9")
    }

    tasks.register<Copy>("copyDistributionToRoot") {
        group = "build"
        description = "Copies the distribution files to the root project distribution directory."

        from("$buildDir/distributions")
        into("${parent?.buildDir}/distributions")
    }

    tasks["build"].finalizedBy("copyDistributionToRoot")
}

tasks["run"].dependsOn(":serviceWorker:copyDevelopmentWebpackToClient")
