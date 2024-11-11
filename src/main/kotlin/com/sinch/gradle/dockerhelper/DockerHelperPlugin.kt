package com.sinch.gradle.dockerhelper

import com.bmuschko.gradle.docker.DockerExtension
import com.bmuschko.gradle.docker.DockerRemoteApiPlugin
import com.bmuschko.gradle.docker.tasks.container.*
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.exception.NotModifiedException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input

const val DEFAULT_WAIT_SECONDS = 60

@Suppress("MemberVisibilityCanBePrivate")
open class DockerHelperPluginExtension {
    companion object {
        const val NAME = "dockerhelper"
    }

    var taskGroup: String = "docker"
    var taskPrefix: String = "docker"

    var registryAuthority: String? = null
    var registryUsername: String? = null
    var registryPassword: String? = null

    var imageName: String? = null
    var imagePath: String? = null
    var tag: String = "latest"

    var waitSeconds: Int = DEFAULT_WAIT_SECONDS

    var envVars: Map<String, String> = mapOf()

    var portBindings: List<String> = listOf()

    val imageId get() = registryAuthority + imagePath + "/" + imageName + ":" + tag

    fun containerName(project: Project) = project.name + "-" + imageName

    fun validate() {
        if (registryAuthority != null) {
            if (registryUsername == null) {
                throw IllegalArgumentException("had registryAuthority but registryUsername was not specified for $NAME extension")
            }
            if (registryPassword == null) {
                throw IllegalArgumentException("had registryAuthority but registryPassword was not specified for $NAME extension")
            }
        }

        if (imageName == null) {
            throw IllegalArgumentException("imageName was not specified for $NAME extension")
        }
        if (imagePath == null) {
            throw IllegalArgumentException("imagePath was not specified for $NAME extension")
        }
    }
}

open class DockerWaitForContainerHealthy : DockerExistingContainer() {
    @Input
    var waitSeconds: Int = DEFAULT_WAIT_SECONDS

    override fun runRemoteCommand() {
        for (i in 0 until waitSeconds) {
            val healthStatus =
                dockerClient
                    .inspectContainerCmd(containerId.get())
                    .exec()
                    .state.health
                    ?.status
            println(
                (
                    if (healthStatus == null) {
                        "- null healthStatus -"
                    } else if (healthStatus.isBlank()) {
                        "- blank healthStatus -"
                    } else {
                        healthStatus
                    }
                ) + " ($i s)",
            )
            if (healthStatus == "healthy") {
                return
            }
            Thread.sleep(1_000)
        }

        println("waited $waitSeconds seconds, but still not healthy; attempting to continue anyway...")
    }
}

inline fun <reified T : DockerExistingContainer> Project.createDockerExistingContainerTask(
    ext: DockerHelperPluginExtension,
    taskSuffix: String,
): T =
    tasks.create(
        "${ext.taskPrefix}$taskSuffix",
        T::class.java,
    ) {
        it.group = ext.taskGroup

        it.targetContainerId(ext.containerName(this))
    }

@Suppress("unused", "UNUSED_VARIABLE")
class DockerHelperPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(DockerRemoteApiPlugin::class.java)

        project.extensions.add(DockerHelperPluginExtension.NAME, DockerHelperPluginExtension::class.java)

        project.afterEvaluate {
            val ext = it.extensions.getByType(DockerHelperPluginExtension::class.java)
            ext.validate()

            val dockerExtension = it.extensions.getByType(DockerExtension::class.java)

            if (ext.registryAuthority != null) {
                dockerExtension.registryCredentials.apply {
                    url.set("https://" + ext.registryAuthority)
                    username.set(ext.registryUsername)
                    password.set(ext.registryPassword)
                }
            }

            val pullImageTask =
                project.tasks.create("${ext.taskPrefix}PullImage", DockerPullImage::class.java).apply {
                    group = ext.taskGroup

                    image.set(ext.imageId)
                }

            var createContainerTask: DockerCreateContainer? = null
            createContainerTask =
                project.tasks.create("${ext.taskPrefix}CreateContainer", DockerCreateContainer::class.java).apply {
                    group = ext.taskGroup
                    targetImageId(ext.imageId)

                    // dependsOn("${ext.taskPrefix}PullImage")
                    containerName.set(ext.containerName(project))
                    hostConfig.portBindings.addAll(ext.portBindings)
                    // hostConfig.autoRemove = true
                    ext.envVars.forEach { envVar ->
                        withEnvVar(envVar.key, envVar.value)
                    }

                    onError { throwable ->
                        when (throwable) {
                            is NotFoundException -> {
                                println("${ext.taskPrefix} image not found, pulling...")
                                pullImageTask.start()
                                createContainerTask!!.start() // recursive
                            }

                            else -> {
                                println("$name() failed: $this")
                                throw throwable
                            }
                        }
                    }
                }

            var startContainerTask: DockerStartContainer? = null
            startContainerTask =
                project.tasks.create("${ext.taskPrefix}StartContainer", DockerStartContainer::class.java).apply {
                    group = ext.taskGroup
                    targetContainerId(ext.containerName(project))

                    // dependsOn("${ext.taskPrefix}CreateContainer")

                    onError { throwable ->
                        when (throwable) {
                            is NotModifiedException -> {
                                println("${ext.taskPrefix} container already running, OK!")
                            }

                            is NotFoundException -> {
                                println("${ext.taskPrefix} container not found, creating...")
                                project.layout.buildDirectory
                                    .dir(".docker")
                                    .get()
                                    .asFile
                                    .mkdir()
                                // see https://github.com/bmuschko/gradle-docker-plugin/issues/1195
                                createContainerTask.start()
                                startContainerTask!!.start() // recursive
                            }

                            else -> {
                                println("$name() failed: $this")
                                throw throwable
                            }
                        }
                    }
                }

            val stopContainerTask = project.createDockerExistingContainerTask<DockerStopContainer>(ext, "StopContainer")

            val removeContainerTask =
                project.createDockerExistingContainerTask<DockerRemoveContainer>(ext, "RemoveContainer")

            val stopAndRemoveContainerTask =
                project.createDockerExistingContainerTask<DockerRemoveContainer>(ext, "StopAndRemoveContainer")
            stopAndRemoveContainerTask.dependsOn(stopContainerTask)

            val startAndWaitForContainer =
                project.createDockerExistingContainerTask<DockerWaitForContainerHealthy>(
                    ext,
                    "StartAndWaitForContainer",
                )
            startAndWaitForContainer.waitSeconds = ext.waitSeconds
            startAndWaitForContainer.dependsOn(startContainerTask)
        }
    }
}
