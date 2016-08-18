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
        println "fetching"
        def gitConf = project.configurations.getByName(GitpackPlugin.GIT_CONFIGURATION)
        def compileConf = project.configurations.getByName("compile")

        gitConf.getDependencies().each { dependency ->
            String path = getGitUriFromDependency(dependency)
            File dir = new File(project.buildDir, dependency.name)
            dir.mkdirs()

            Git git = openOrCreate(dir)

            //Check for pom
            if (new File(dir, "pom.xml").exists()) {

                //Build using maven
                Shell.executeOnShell("mvn install -DskipTests", dir)

                //find maven artifact
                File artifact = getArtifactFromTarget(new File(dir, "target"))
                if (artifact == null) {
                    throw new BuildException("Could not find artifact.", null)
                }

                //Install into repo using dep name
                /**
                 * mvn install:install-file -Dfile=target/ALang-1.0-SNAPSHOT.jar -DgroupId=com.github.nishtahir -DartifactId=ALang -Dversion=-SNAPSHOT
                 */
                Shell.executeOnShell("mvn install:install-file -Dfile=${artifact.absolutePath} -DgroupId=${dependency.group}" +
                        " -DartifactId=${dependency.name} -Dversion=${dependency.version} -Dpackaging=jar", artifact.parentFile)

                //Add dependency to compile classpath

            } else if (new File("${dir.absolutePath}/build.gradle").exists()) {

            }

            compileConf.getDependencies().add(dependency)
        }
    }

    static Git openOrCreate(File gitDirectory) throws IOException, GitAPIException {
        Git git;
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        repositoryBuilder.addCeilingDirectory(gitDirectory);
        repositoryBuilder.findGitDir(gitDirectory);
        if (repositoryBuilder.getGitDir() == null) {
            git = Git.init().setDirectory(gitDirectory.getParentFile()).call();
        } else {
            git = new Git(repositoryBuilder.build());
        }
        return git;
    }

    static File getArtifactFromTarget(File file) {
        //Need a better way to find the artifact
        File art = null;
        file.eachFileMatch(FileType.FILES, ~/.*.jar/) {
            art = it
        }
        return art
    }


    static String getGitUriFromDependency(Dependency dependency) {
        def values = dependency.group.split("\\.")
        return "https://${values[1]}.${values[0]}/${values[2]}/${dependency.name}.git"
    }
}
