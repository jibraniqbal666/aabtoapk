package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel = MainViewModel()
        Main(viewModel)
    }
}

@Composable
private fun Main(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    if (state.isFileChooserOpen) {
        FileDialog(

            onCloseRequest = {
                viewModel.setFileChooserOpen(false)
                viewModel.setFile(it ?: "")
            }
        )
    }
    Column(Modifier.size(400.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(100.dp).clickable {
            viewModel.setFileChooserOpen(true)
        }) {
            if (state.file.isEmpty()) {
                Text("Drop AAB here")
            } else {
                Text(state.file)
            }
        }
        Button(onClick = { viewModel.fromAABToAPK() }) {
            Text("Convert")
        }
        AnimatedVisibility(state.isLoading) {
            LinearProgressIndicator()
        }
    }
}

@Composable
private fun FileDialog(
    parent: Frame? = null,
    onCloseRequest: (result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", LOAD) {
            override fun setVisible(value: Boolean) {
                this.filenameFilter = FilenameFilter { dir, name ->
                    name.contains(".aab")
                }
                super.setVisible(value)
                if (value) {
                    onCloseRequest(directory + file)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

