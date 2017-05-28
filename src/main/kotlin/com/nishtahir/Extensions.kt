package com.nishtahir

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.TaskContainer

inline operator fun Project.invoke(function: Project.() -> Unit): Project {
    function(); return this
}

inline operator fun ConfigurationContainer.invoke(function: ConfigurationContainer.() -> Unit): ConfigurationContainer {
    function(); return this
}

inline operator fun RepositoryHandler.invoke(function: RepositoryHandler.() -> Unit): RepositoryHandler {
    function(); return this
}

inline operator fun ExtensionContainer.invoke(function: ExtensionContainer.() -> Unit): ExtensionContainer {
    function(); return this
}


inline operator fun TaskContainer.invoke(function: TaskContainer.() -> Unit): TaskContainer {
    function(); return this
}


