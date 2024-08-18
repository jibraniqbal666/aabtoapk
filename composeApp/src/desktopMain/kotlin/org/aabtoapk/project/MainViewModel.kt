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
                convertToAPK(
                    src,
                    workingDir,
                    _uiState.value.keyStore
                ) { _uiState.update { state -> state.copy(output = state.output + it) } }
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

    fun setFileChooserOpen(fileType: FileType) {
        _uiState.update { state -> state.copy(isFileChooserOpen = true, fileType = fileType) }
    }

    fun setFileChooserClose() {
        _uiState.update { state -> state.copy(isFileChooserOpen = false) }
    }

    fun resetKeystore() {
        _uiState.update { state -> state.copy(keyStore = null, error = null) }
    }

    fun setKeystore(path: String) {
        _uiState.update { state ->
            state.copy(
                keyStore = state.keyStore?.copy(path = path) ?: KeyStore(
                    path = path
                ),
                error = null
            )
        }
    }

    fun setKeystorePassword(text: String) {
        _uiState.update { state ->
            state.copy(
                keyStore = state.keyStore?.copy(keyStorePassword = text) ?: KeyStore(
                    keyStorePassword = text
                ),
                error = null
            )
        }
    }

    fun setAlias(text: String) {
        _uiState.update { state ->
            state.copy(
                keyStore = state.keyStore?.copy(alias = text) ?: KeyStore(alias = text),
                error = null
            )
        }
    }

    fun setAliasPassword(text: String) {
        _uiState.update { state ->
            state.copy(
                keyStore = state.keyStore?.copy(aliasPassword = text) ?: KeyStore(
                    aliasPassword = text
                ),
                error = null
            )
        }
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

    private fun convertToAPK(src: String, workingDir: Path, keyStore: KeyStore?, output: (text: String) -> Unit) {
        val tempDst = "$workingDir/app.apks"
        val tempDstPath = Path(tempDst)
        tempDstPath.deleteIfExists()

        var runCommand = "java -jar $workingDir/bundletool.jar build-apks"
        runCommand += " --bundle=${src} --output=${tempDst} --mode=universal"
        if (keyStore != null && keyStore.valid()) {
            runCommand += " --ks=${keyStore.path}"
            runCommand += " --ks-pass=pass:${keyStore.keyStorePassword}"
            runCommand += " --ks-key-alias=${keyStore.alias}"
            runCommand += " --key-pass=pass:${keyStore.aliasPassword}"
        }
        if (keyStore != null && keyStore.valid().not()) {
            throw Exception(keyStore.showInvalidMessage())
        }

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
    val fileType: FileType = FileType.AAB,
    val isLoading: Boolean = false,
    val keyStore: KeyStore? = null,
    val outputFile: String? = null,
    val output: String? = null,
    val error: String? = null
)
