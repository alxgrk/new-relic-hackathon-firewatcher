import de.undercouch.gradle.tasks.download.Download

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.4.21"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    id("de.undercouch.download") version "3.4.3"
    id("com.heroku.sdk.heroku-gradle") version "2.0.0"
}

group = "de.alxgrk"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.cio.EngineMain"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-metrics:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.github.microutils:kotlin-logging:1.12.0")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.google.guava:guava:30.1-jre")
    implementation("org.apache.lucene:lucene-core:8.8.0")

    implementation("nl.martijndwars:web-push:5.1.1")
    implementation("org.bouncycastle:bcprov-jdk15on:1.68")

    implementation("com.newrelic.agent.java:newrelic-java:6.4.1")
    implementation("io.micrometer:micrometer-core:1.6.4")
    implementation("io.micrometer:micrometer-registry-new-relic:1.6.4")
    implementation("io.ktor:ktor-metrics-micrometer:$ktor_version")

    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

tasks.register<Download>("downloadNewrelic") {
    mkdir("newrelic")
    src("https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip")
    dest(file("newrelic"))
}

tasks.register<Copy>("unzipNewrelic") {
    from(zipTree(file("newrelic/newrelic-java.zip")))
    into(rootDir)
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }
}

heroku {
    appName = "firewatcher-backend"

    includes = listOf(
        "build/libs/$appName-$version-all.jar",
        "newrelic/newrelic.jar",
        "newrelic/newrelic.yml",
        "newrelic/newrelic-api.jar"
    )
    isIncludeBuildDir = false
    processTypes =
        mapOf("web" to "java -javaagent:/app/newrelic/newrelic.jar -jar build/libs/$appName-$version-all.jar")
}
