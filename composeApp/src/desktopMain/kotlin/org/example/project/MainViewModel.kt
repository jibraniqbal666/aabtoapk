package org.example.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import downloadBundleTool
import io.ktor.client.*
import isBundleToolExists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively

class MainViewModel : ViewModel() {
    private val client = HttpClient()

    private val _uiState =
        MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (isBundleToolExists().not()) {
                _uiState.update { state -> state.copy(isLoading = true) }
                downloadBundleTool(client)
                _uiState.update { state -> state.copy(isLoading = false) }
            }
        }
    }

    fun fromAABToAPK() {
        viewModelScope.launch(Dispatchers.IO) {
            setIsLoading(true)

            val src = _uiState.value.file.shellEscape()
            val dst = src.split(".aab")[0] + ".apk"
            val home = System.getProperty("user.home")
            val workingDir = Path("$home/.aabtoapk")

            try {
                convertToAPK(src, workingDir) { _uiState.update { state -> state.copy(output = state.output + it) } }
                unzipAPKs(workingDir) { _uiState.update { state -> state.copy(output = state.output + it) } }
                moveAPKToSrc(dst, workingDir) { _uiState.update { state -> state.copy(output = state.output + it) } }
            } catch (e: Exception) {
                setIsLoading(false)
                _uiState.update { state -> state.copy(error = e.message) }
            }


            setIsLoading(false)
        }
    }

    fun setFile(file: String) {
        _uiState.update { state -> state.copy(file = file, output = null, error = null, outputFile = null) }
    }

    fun setFileChooserOpen(isOpen: Boolean) {
        _uiState.update { state -> state.copy(isFileChooserOpen = isOpen) }
    }

    private fun setIsLoading(isLoading: Boolean) {
        _uiState.update { state -> state.copy(isLoading = isLoading) }
    }

    fun openContainingFolder(path: String?) {
        val file = File(path)
        val absoluteFilePath = file.absolutePath;
        val folder = File(absoluteFilePath)
        openFolder(folder);
    }

    private fun openFolder(folder: File) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browseFileDirectory(folder);
        }
    }

    private fun convertToAPK(src: String, workingDir: Path, output: (text: String) -> Unit) {
        val tempDst = "$workingDir/app.apks"
        val tempDstPath = Path(tempDst)
        tempDstPath.deleteIfExists()

        var runCommand = "java -jar $workingDir/bundletool.jar build-apks"
        runCommand += " --bundle=${src} --output=${tempDst} --mode=universal"
        //        run_command += " --ks=#{key_store}"
        //        run_command += " --ks-pass=pass:#{key_store_password}"
        //        run_command += " --ks-key-alias=#{key_store_alias}"
        //        run_command += " --key-pass=pass:#{key_store_alias_password}"

        runCommand.runCommand(output)
    }

    @OptIn(ExperimentalPathApi::class)
    private fun unzipAPKs(workingDir: Path, output: (text: String) -> Unit) {
        val tempDstDir = Path("$workingDir/temp_dst_folder")
        tempDstDir.deleteRecursively()

        "unzip $workingDir/app.apks -d $workingDir/temp_dst_folder".runCommand(output)
    }

    private fun moveAPKToSrc(dst: String, workingDir: Path, output: (text: String) -> Unit) {
        val sourcePath = "$workingDir/temp_dst_folder/universal.apk"
        File(sourcePath).let { sourceFile ->
            sourceFile.copyTo(File(dst))
            sourceFile.delete()
        }
        output("\n Move the file from $sourcePath to $dst")
        _uiState.update { state -> state.copy(outputFile = dst) }
    }
}

data class MainUiState(
    val file: String = "",
    val isFileChooserOpen: Boolean = false,
    val isLoading: Boolean = false,
    val outputFile: String? = null,
    val output: String? = null,
    val error: String? = null
)
