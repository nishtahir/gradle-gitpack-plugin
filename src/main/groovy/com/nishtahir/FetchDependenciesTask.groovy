package com.nishtahir

import groovy.io.FileType
import org.apache.maven.cli.MavenCli
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection

/**
 *
 */
class FetchDependenciesTask extends DefaultTask {

    @TaskAction
    void fetchDependencies() {
        def gitConf = project.configurations.getByName(GitpackPlugin.CONFIGURATION_GIT)
        def compileConf = project.configurations.getByName(GitpackPlugin.CONFIGURATION_COMPILE)

        gitConf.getDependencies().each { dependency ->
            String path = getGitUriFromDependency(dependency)
            println "resolving from $path"
            File projectRoot = new File(project.buildDir, dependency.name)
            projectRoot.mkdirs()

            Git git = openOrCloneRepository(projectRoot, path)
            git.close()
            File artifact;
            if (isMavenProject(projectRoot)) {
                buildMavenProject(projectRoot)
                artifact = getArtifactFromTarget(new File(projectRoot, "target"))
            } else if (isGradleProject(projectRoot)) {
                buildGradleProject(projectRoot)
                artifact = getArtifactFromTarget(new File(projectRoot, "build/libs"))
            } else {
                throw new BuildException("Not a valid project maven or gradle project", null)
            }
            installArtifactToRepository(artifact, projectRoot, dependency)
            compileConf.getDependencies().add(dependency)
        }
    }

    /**
     * Run gradle build
     * @param file root directory of the project
     * @return
     */
    static def buildGradleProject(File file) {
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(file)
                .connect()
        try {
            BuildLauncher build = connection.newBuild()
            build.forTasks("assemble")
            build.standardOutput = System.out
            build.standardError = System.err
            build.run()
        } catch (Exception e) {
            throw new BuildException("Failed to build the project", e)
        } finally {
            connection.close()
        }
    }

    /**
     *
     * @param projectRoot
     * @return
     */
    static def buildMavenProject(File projectRoot) {
        MavenCli mavenCli = new MavenCli()
        System.setProperty("maven.multiModuleProjectDirectory", projectRoot.absolutePath)
        String[] params = ["-Dmaven.test.skip=true",
                           "install"].toArray()

        prettyPrintCommand "maven", params

        if (0 != mavenCli.doMain(params, projectRoot.absolutePath, System.out, System.err)) {
            throw new BuildException("Failed to build and install.", null)
        }
    }

    /**
     * Installs the jar into the local maven repository.
     * @param artifact jar to install.
     * @param dependency
     * @return
     */
    static def installArtifactToRepository(File artifact, File projectRoot, Dependency dependency) {
        MavenCli mavenCli = new MavenCli()
        String[] params = ["install:install-file", "-Dfile=${artifact.absolutePath}",
                           "-DgroupId=${dependency.group}",
                           "-DartifactId=${dependency.name}",
                           "-Dversion=${dependency.version}",
                           "-Dpackaging=jar"].toArray()

        prettyPrintCommand "maven", params

        if (0 != mavenCli.doMain(params, projectRoot.absolutePath, System.out, System.err)) {
            throw new BuildException("Failed to install artifact into repository", null)
        }
    }

    /**
     *
     * @param pathToRepository
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    static Git openOrCloneRepository(File directory, String pathToRepository) {

        try {
            Git git;
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            repositoryBuilder.addCeilingDirectory(directory);
            repositoryBuilder.findGitDir(directory);
            if (repositoryBuilder.getGitDir() == null) {
                directory.delete()
                git = Git.cloneRepository().setURI(pathToRepository).setDirectory(directory).call();
            } else {
                git = new Git(repositoryBuilder.build());
                git.pull().call()
            }
            return git;
        } catch (IOException | GitAPIException e) {
            throw new BuildException("Failed to clone or access the repository", e)
        }
    }

    /**
     *
     * @param file
     * @return
     */
    static File getArtifactFromTarget(File file) {
        //Need a better way to find the artifact
        File art = null;
        file.eachFileMatch(FileType.FILES, ~/.*.jar/) {
            art = it
        }
        if (art == null) {
            throw new BuildException("Couldn't find artifact", null)
        }
        return art
    }

    /**
     * Converts
     *
     * @param dependency
     * @return
     */
    static String getGitUriFromDependency(Dependency dependency) {
        def values = dependency.group.split("\\.")
        return "https://${values[1]}.${values[0]}/${values[2]}/${dependency.name}.git"
    }

    /**
     * Checks if pom.xml exists
     * @param projectRoot Root directory of the project
     * @return true if it's a maven project
     */
    static boolean isMavenProject(File projectRoot) {
        return new File(projectRoot, "pom.xml").exists()
    }

    /**
     * Checks if it's a gradle project by looking for the gradle build script
     * @param projectRoot Root directory of the project
     * @return true if it's a gradle project
     */
    static boolean isGradleProject(File projectRoot) {
        return new File(projectRoot, "build.gradle").exists()
    }

    static void prettyPrintCommand(String executor, String[] params) {
        println "executing $executor task" + params.join(" ")
    }
}
