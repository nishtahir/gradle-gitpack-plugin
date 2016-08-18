package com.nishtahir

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.BuildException

/**
 *
 */
class GitpackPlugin implements Plugin<Project> {

    static final GIT_CONFIGURATION = "git"

    static final FETCH_TASK = "fetch"

    @Override
    void apply(Project project) {
        project.configurations.create GIT_CONFIGURATION
        project.extensions.create GIT_CONFIGURATION, GitpackPluginExtension

        project.repositories.add(project.repositories.mavenLocal())

        def fetchTask = project.tasks.create(FETCH_TASK, FetchDependenciesTask.class)

        project.afterEvaluate {
            if (isJavaProject(project)) {
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
