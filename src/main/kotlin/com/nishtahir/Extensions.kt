package com.nishtahir

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.TaskContainer

internal inline operator fun Project.invoke(function: Project.() -> Unit): Project {
    function(); return this
}

internal inline operator fun ConfigurationContainer.invoke(function: ConfigurationContainer.() -> Unit): ConfigurationContainer {
    function(); return this
}

internal inline operator fun RepositoryHandler.invoke(function: RepositoryHandler.() -> Unit): RepositoryHandler {
    function(); return this
}

internal inline operator fun ExtensionContainer.invoke(function: ExtensionContainer.() -> Unit): ExtensionContainer {
    function(); return this
}


internal inline operator fun TaskContainer.invoke(function: TaskContainer.() -> Unit): TaskContainer {
    function(); return this
}

fun Dependency.toGitRepoUri(): String {
    val values = group.split(".")
    return "https://${values[1]}.${values[0]}/${values[2]}/$name.git"
}
