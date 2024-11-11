// dotenv plugin
plugins {
    kotlin("jvm") version libs.versions.kotlin

    alias(libs.plugins.plugin.publish)
}

assert(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17))

group = "com.sinch.gradle"
version = "0.1.0"

repositories {
    mavenCentral()

    gradlePluginPortal()
}

dependencies {
    implementation(libs.gradle.docker.plugin)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

val gitRepoUrl = "https://github.com/sinch/gradle-docker-helper"

@Suppress("UnstableApiUsage")
gradlePlugin {
    website = gitRepoUrl
    vcsUrl = "$gitRepoUrl.git"

    plugins {
        create("dockerHelperPlugin") {
            id = "$group.${rootProject.name}"
            displayName = "Gradle Docker helper"
            description =
                "A plugin simplifying Docker task configuration."
            tags = listOf("docker")
            implementationClass = "$id.DockerHelperPlugin"
        }
    }
}
