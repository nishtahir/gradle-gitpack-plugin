package com.nishtahir

/**
 * Utility class to help with shell execution
 * This hopefully makes things less of a pain to work with
 *
 * Thanks to Joerg Mueller
 * http://www.joergm.com/2010/09/executing-shell-commands-in-groovy/
 */
class Shell {

    /**
     * Simply executes a command on the shell. The default working directory is user.dir
     * @param command
     * @param workingDir
     * @return
     */
    static int executeOnShell(String command, File workingDir = new File(System.getProperty('user.dir'))) {
        println command
        def process = new ProcessBuilder(addShellPrefix(command))
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
        process.inputStream.eachLine { println it }
        process.waitFor();
        return process.exitValue()
    }

    /**
     *
     * @param command
     * @return
     */
    static private def addShellPrefix(String command) {
        def commandArray = new String[3]
        commandArray[0] = "sh"
        commandArray[1] = "-c"
        commandArray[2] = command
        return commandArray
    }
}
