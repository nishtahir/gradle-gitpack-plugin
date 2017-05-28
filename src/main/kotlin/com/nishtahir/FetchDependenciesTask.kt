package com.nishtahir

import org.apache.maven.cli.MavenCli
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnector
import java.io.File

open class FetchDependenciesTask : DefaultTask() {

    val localMavenRepository by lazy { "${project.buildDir}/maven" }

    @TaskAction
    fun fetchDependencies() {
        val gitConfiguration = project.configurations.getByName(CONFIGURATION_GIT)
        val compileConfiguration = project.configurations.getByName(CONFIGURATION_COMPILE)

        gitConfiguration.dependencies.forEach { dependency ->
            val dependentProjectRoot = File(project.buildDir, dependency.name).apply { mkdirs() }
            openOrCloneRepository(dependentProjectRoot, getGitUriFromDependency(dependency))
            val artifact = if (isMavenProject(dependentProjectRoot)) {
                buildMavenProject(dependentProjectRoot)
                getArtifactFromTarget(File(dependentProjectRoot, "target"))
            } else if (isGradleProject(dependentProjectRoot)) {
                buildGradleProject(dependentProjectRoot)
                getArtifactFromTarget(File(dependentProjectRoot, "build/libs"))
            } else {
                throw BuildException("Not a valid project maven or gradle project", null)
            }
            installArtifactToRepository(artifact, dependentProjectRoot, dependency)
            compileConfiguration.dependencies.add(dependency)
        }
    }

    fun buildGradleProject(projectRoot: File) {
        val connection = GradleConnector.newConnector()
                .forProjectDirectory(projectRoot)
                .connect()
        try {
            connection.newBuild().apply {
                forTasks("assemble")
                setStandardOutput(System.out)
                setStandardError(System.err)
            }.run()
        } catch (exception: Exception) {
            throw BuildException("Failed to build the project", exception)
        } finally {
            connection.close()
        }
    }

    fun buildMavenProject(dependentProjectRoot: File) {
        val mavenCli = MavenCli()
        System.setProperty("maven.multiModuleProjectDirectory", dependentProjectRoot.absolutePath)
        val params = listOf("-Dmaven.test.skip=true", "install")

        prettyPrintCommand("maven", params)

        if (0 != mavenCli.doMain(params.toTypedArray(), dependentProjectRoot.absolutePath, System.out, System.err)) {
            throw BuildException("Failed to build and install maven project", null)
        }
    }

    fun installArtifactToRepository(artifact: File, dependentProjectRoot: File, dependency: Dependency) {
        val mavenCli = MavenCli()
        val params = listOf("install:install-file", "-Dfile=${artifact.absolutePath}",
                "-DgroupId=${dependency.group}",
                "-DartifactId=${dependency.name}",
                "-Dversion=${dependency.version}",
                "-Dpackaging=jar",
                "-D-DlocalRepositoryPath=$localMavenRepository")

        prettyPrintCommand("maven", params)

        if (0 != mavenCli.doMain(params.toTypedArray(), dependentProjectRoot.absolutePath, System.out, System.err)) {
            throw BuildException("Failed to install artifact into local repository", null)
        }
    }

    fun openOrCloneRepository(directory: File, pathToRepository: String) {
        try {
            val repositoryBuilder = FileRepositoryBuilder().apply {
                addCeilingDirectory(directory)
                findGitDir(directory)
            }
            val git = if (repositoryBuilder.gitDir == null) {
                directory.delete()
                Git.cloneRepository().setURI(pathToRepository).setDirectory(directory).call()
            } else {
                Git(repositoryBuilder.build()).apply {
                    pull().call()
                }
            }
            git.close()
        } catch (exception: Exception) {
            throw BuildException("Failed to clone or access the repository", exception)
        }
    }

    fun getArtifactFromTarget(buildDirectory: File): File {
        val artifact = buildDirectory.listFiles().find {
            it.extension == "jar"
        }
        return requireNotNull(artifact) { "Could not find artifact." }
    }

    fun getGitUriFromDependency(dependency: Dependency): String {
        val values = dependency.group.split("\\.")
        return "https://${values[1]}.${values[0]}/${values[2]}/${dependency.name}.git"
    }

    fun isMavenProject(projectRoot: File): Boolean {
        return File(projectRoot, "pom.xml").exists()
    }

    fun isGradleProject(projectRoot: File): Boolean {
        return File(projectRoot, "build.gradle").exists()
    }

    fun prettyPrintCommand(executor: String, params: List<String>) {
        println("executing $executor task" + params.joinToString(" "))
    }
}
