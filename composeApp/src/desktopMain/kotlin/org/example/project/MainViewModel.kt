package org.example.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

class MainViewModel : ViewModel() {
    private val client = HttpClient()

    private val _uiState = MutableStateFlow(MainUiState(file = "", isFileChooserOpen = false, isLoading = false))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (isBundleToolExists().not()) {
                _uiState.update { state -> state.copy(isLoading = true) }
                downloadBundleTool()
                _uiState.update { state -> state.copy(isLoading = false) }
            }
        }
    }

    private suspend fun downloadBundleTool() {
        val home = System.getProperty("user.home")
        val path = Path("$home/.aabtoapk")
        val outputPath = Path("$home/.aabtoapk/bundletool.jar")
        if (path.exists().not()) {
            path.createDirectory()
        }
        if (outputPath.exists().not()) {
            outputPath.createFile()
        }
        val result =
            client.get("https://github.com/google/bundletool/releases/download/1.17.1/bundletool-all-1.17.1.jar") {
                onDownload { bytesSentTotal, contentLength ->
                    println("Downloaded $bytesSentTotal of $contentLength")
                }
            }.bodyAsChannel()
        result.copyAndClose(outputPath.toFile().writeChannel())
    }

    private fun isBundleToolExists(): Boolean {
        val home = System.getProperty("user.home")
        val path = Path("$home/.aabtoapk/bundletool.jar")
        return path.exists()
    }

    private fun String.runCommand3(): String {
        val process = ProcessBuilder("/bin/bash", "-c", this).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String? = ""
        val builder = StringBuilder()
        while (reader.readLine().also { line = it } != null) {
            builder.append(line).append(System.lineSeparator())
        }
        // remove the extra new line added in the end while reading from the stream
        return builder.toString().trim()
    }

    private fun String.runCommand2(): String {
        Runtime.getRuntime().exec(this).waitFor()
        return "bla"
//        val processBuilder = ProcessBuilder()
//        processBuilder.command("bash", "-c", this)
//    
//        return try {
//            val process = processBuilder.start()
//            val reader = BufferedReader(InputStreamReader(process.inputStream))
//            val result = StringBuilder()
//            var line: String? = reader.readLine()
//            while (line != null) {
//                result.append(line).append("\n")
//                line = reader.readLine()
//            }
//            process.waitFor()
//            result.toString()
//        } catch (e: Exception) {
//            e.printStackTrace()
//            "Error: ${e.message}"
//        }
    }

    private fun String.runCommand(workingDir: File) {
        println(this)
        val command = arrayOf("/bin/bash", "-c", this)
        val process = Runtime.getRuntime().exec(command)
        // Create threads to handle standard output and error streams
        val outputThread = Thread {
            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String? = outputReader.readLine()
            while (line != null) {
                println(line)
                line = outputReader.readLine()
            }
        }

        val errorThread = Thread {
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            var line: String? = errorReader.readLine()
            while (line != null) {
                System.err.println(line)
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

        println("Process exited with code: $exitCode")
    }

    @OptIn(ExperimentalPathApi::class)
    fun fromAABToAPK() {
        viewModelScope.launch(Dispatchers.IO) {
            setIsLoading(true)
            val src = _uiState.value.file
            val dst = src.split(".aab")[0] + ".apk"
            val home = System.getProperty("user.home")
            val workingDir = Path("$home/.aabtoapk")

            val tempDst: String = "$workingDir/app.apks"
            val tempDstPath = Path(tempDst)
            tempDstPath.deleteIfExists()

            var runCommand = "java -jar $workingDir/bundletool.jar build-apks"
            runCommand += " --bundle=${src} --output=${tempDst} --mode=universal"
            //        run_command += " --ks=#{key_store}"
            //        run_command += " --ks-pass=pass:#{key_store_password}"
            //        run_command += " --ks-key-alias=#{key_store_alias}"
            //        run_command += " --key-pass=pass:#{key_store_alias_password}"
            //    

            runCommand.runCommand(workingDir.toFile())

            val tempDstDir = Path("$workingDir/temp_dst_folder")
            tempDstDir.deleteRecursively()

            "unzip $workingDir/app.apks -d $workingDir/temp_dst_folder".runCommand(workingDir.toFile())

            File("$workingDir/temp_dst_folder/universal.apk").let { sourceFile ->
                sourceFile.copyTo(File(dst))
                sourceFile.delete()
            }


            setIsLoading(false)
        }
    }

    fun setFile(file: String) {
        _uiState.update { state -> state.copy(file = file) }
    }

    fun setFileChooserOpen(isOpen: Boolean) {
        _uiState.update { state -> state.copy(isFileChooserOpen = isOpen) }
    }

    private fun setIsLoading(isLoading: Boolean) {
        _uiState.update { state -> state.copy(isLoading = isLoading) }
    }
}

data class MainUiState(val file: String, val isFileChooserOpen: Boolean, val isLoading: Boolean) 
