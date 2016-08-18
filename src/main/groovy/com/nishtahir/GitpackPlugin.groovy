package com.nishtahir

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.BuildException

/**
 *
 */
class GitpackPlugin implements Plugin<Project> {

    /**
     *
     */
    static final CONFIGURATION_GIT = "git"

    /**
     *
     */
    static final CONFIGURATION_COMPILE = "compile"

    /**
     * Build phase in java plugin
     */
    static final TASK_COMPILE_JAVA = "compileJava"

    /**
     *
     */
    static final TASK_FETCH = "fetch"

    @Override
    void apply(Project project) {
        project.configurations.create CONFIGURATION_GIT
        project.extensions.create CONFIGURATION_GIT, GitpackPluginExtension
        project.repositories.add(project.repositories.mavenLocal())

        project.afterEvaluate {
            if (isJavaProject(project)) {
                def fetchTask = project.tasks.create(TASK_FETCH, FetchDependenciesTask.class)
                project.tasks.getByName("compileJava").dependsOn(fetchTask)
            } else {
                throw new BuildException("The project isn't a java project.", null)
            }
        }

    }

    static def isJavaProject(project) {
        project.plugins.hasPlugin('java')
    }
}
