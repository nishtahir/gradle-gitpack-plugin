package com.nishtahir

import org.apache.tools.ant.BuildException
import org.gradle.api.Plugin
import org.gradle.api.Project

val CONFIGURATION_GIT = "git"
val CONFIGURATION_COMPILE = "compile"
val TASK_COMPILE_JAVA = "compileJava"
val TASK_FETCH = "fetch"

class GitpackPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project {
            configurations {
                create(CONFIGURATION_GIT)
            }

            repositories {
                maven { repo -> repo.setUrl("${project.buildDir}/maven") }
            }

            extensions {
                create(CONFIGURATION_GIT, GitpackPluginExtension::class.java)
            }

            tasks {
                val fetchTask = create(TASK_FETCH, FetchDependenciesTask::class.java)
                if (hasCompatibleJvmPlugin()) {
                    getByName(TASK_COMPILE_JAVA).dependsOn(fetchTask)
                } else {
                    throw BuildException("The project isn't a compatible JVM project.")
                }
            }
        }
    }

}

fun Project.isJavaProject() = plugins.hasPlugin("java")
fun Project.isGroovyProject() = plugins.hasPlugin("groovy")
fun Project.isKotlinProject() = plugins.hasPlugin("kotlin")

fun Project.hasCompatibleJvmPlugin(): Boolean {
    return isJavaProject() || isGroovyProject() || isKotlinProject()
}

open class GitpackPluginExtension


