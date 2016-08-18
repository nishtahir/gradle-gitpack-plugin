package com.nishtahir


class Shell {
    static def executeOnShell(String command) {
        return executeOnShell(command, new File(System.getProperty('user.dir')))
    }

    static def executeOnShell(String command, File workingDir) {
        println command
        def process = new ProcessBuilder(addShellPrefix(command))
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
        process.inputStream.eachLine { println it }
        process.waitFor();
        return process.exitValue()
    }

    static private def addShellPrefix(String command) {
        def commandArray = new String[3]
        commandArray[0] = "sh"
        commandArray[1] = "-c"
        commandArray[2] = command
        return commandArray
    }
}
