package com.nishtahir

import groovy.io.FileType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException

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
            installArtifactToRepository(artifact, dependency)
            compileConf.getDependencies().add(dependency)
        }
    }

    def buildGradleProject(File file) {
        use(Shell) {
            if (0 != "gradle assemble".executeOnShell(file)) {
                throw new BuildException("Failed to build the project", null)
            }
        }
    }

    /**
     *
     * @param file
     * @return
     */
    def buildMavenProject(File file) {
        use(Shell) {
            if (0 != "mvn install -DskipTests".executeOnShell(file)) {
                throw new BuildException("Failed to build the project", null)
            }
        }
    }

    /**
     * Installs the jar into the local maven repository.
     * @param artifact jar to install.
     * @param dependency
     * @return
     */
    def installArtifactToRepository(File artifact, Dependency dependency) {
        use(Shell) {
            if (0 != ("mvn install:install-file" +
                    " -Dfile=${artifact.absolutePath}" +
                    " -DgroupId=${dependency.group}" +
                    " -DartifactId=${dependency.name}" +
                    " -Dversion=${dependency.version}" +
                    " -Dpackaging=jar")
                    .executeOnShell(artifact.parentFile)
            ) {
                throw new BuildException("Failed to install artifact into repository", null)
            }
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
}
