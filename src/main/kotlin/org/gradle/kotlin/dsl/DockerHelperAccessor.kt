package org.gradle.kotlin.dsl

import com.sinch.gradle.dockerhelper.DockerHelperPluginExtension
import org.gradle.api.Project

val Project.dockerhelper: DockerHelperPluginExtension
    get() = this.extensions.getByType(DockerHelperPluginExtension::class.java)
