import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

fun String.shellEscape(): String {
    val specialChars = arrayOf(
        ' ',
        '(',
        ')',
        '[',
        ']',
        '{',
        '}',
        '&',
        '|',
        ';',
        '<',
        '>',
        '*',
        '?',
        '\\',
        '$',
        '`',
        '"',
        '\'',
        '!',
        '#'
    )
    val escapedPath = StringBuilder()

    for (char in this) {
        if (char in specialChars) {
            escapedPath.append("\\")
        }
        escapedPath.append(char)
    }

    return escapedPath.toString()
}


fun String.runCommand(output: (text: String) -> Unit = {}) {
    output("\n " + this)
    val command = arrayOf("/bin/bash", "-c", this)
    val process = Runtime.getRuntime().exec(command)
    // Create threads to handle standard output and error streams
    val outputThread = Thread {
        val outputReader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String? = outputReader.readLine()
        while (line != null) {
            println(line)
            output("\n " + line)
            line = outputReader.readLine()
        }
    }

    val errorThread = Thread {
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        var line: String? = errorReader.readLine()
        while (line != null) {
            System.err.println(line)
            output("\n " + line)
            line = errorReader.readLine()
        }
    }

    // Start the threads
    outputThread.start()
    errorThread.start()

    println(process.waitFor(5, TimeUnit.MINUTES))

    // Wait for the process to complete
    val exitCode = process.waitFor()

    // Ensure the output threads finish
    outputThread.join()
    errorThread.join()

    if (exitCode != 0) {
        throw Exception("The command $this, has failed with exitCode $exitCode")
    }
}