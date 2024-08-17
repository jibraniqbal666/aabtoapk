package org.example.project

import BackgroundColor
import CollapsableTextField
import PrimaryColor
import PrimaryTextColor
import SecondaryTextColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
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
    MainComponent(
        state = state,
        openFileChooser = { viewModel.setFileChooserOpen(true) },
        convert = { viewModel.fromAABToAPK() },
        showInFolder = { viewModel.openContainingFolder(state.outputFile) }
    )
}

@Composable
private fun MainComponent(
    state: MainUiState,
    openFileChooser: () -> Unit,
    convert: () -> Unit,
    showInFolder: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().wrapContentHeight().background(BackgroundColor).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                "ABB -> APK",
                modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
                style = MaterialTheme.typography.h6,
                color = PrimaryTextColor
            )
        }
        Box(
            Modifier
                .padding(16.dp)
                .height(100.dp)
                .widthIn(600.dp)
                .fillMaxWidth()
                .dashedBorder(SolidColor(SecondaryTextColor), shape = RoundedCornerShape(12.dp))
                .clickable { openFileChooser() }
                .padding(16.dp)
        ) {
            if (state.file.isEmpty()) {
                Text(
                    "Drop your AAB here or browse",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.body1,
                    color = SecondaryTextColor
                )
            } else {
                Text(
                    state.file,
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.body1,
                    color = PrimaryTextColor
                )
            }
        }
        AnimatedVisibility(state.output != null) {
            Box(
                Modifier
                    .widthIn(600.dp)
                    .fillMaxWidth()
            ) {
                CollapsableTextField(state.output ?: "")
            }
        }
        AnimatedVisibility(state.error != null) {
            Box(
                Modifier
                    .widthIn(600.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    state.error ?: "",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.caption,
                    color = Color.Red
                )
            }
        }
        AnimatedVisibility(state.isLoading) {
            Box(
                Modifier
                    .padding(16.dp)
                    .height(40.dp)
                    .widthIn(600.dp)
                    .fillMaxWidth()
                    .dashedBorder(SolidColor(SecondaryTextColor), shape = RoundedCornerShape(12.dp), gapLength = 0.dp)
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(Modifier.align(Alignment.Center), color = PrimaryColor)
            }
        }
        AnimatedVisibility(state.isLoading.not()) {
            Box(
                Modifier
                    .widthIn(600.dp)
                    .fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier.width(200.dp).height(75.dp).padding(16.dp).align(Alignment.Center),
                    onClick = if (state.outputFile == null) convert else showInFolder,
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryColor),
                    enabled = state.error == null
                ) {
                    val text = if (state.outputFile == null) "Convert" else "Show in folder"
                    Text(text, color = Color.White)
                }
            }
        }
    }
}

fun Modifier.dashedBorder(
    brush: Brush,
    shape: Shape,
    strokeWidth: Dp = 2.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
    cap: StrokeCap = StrokeCap.Round
) = this.drawWithContent {
    // Draw the content
    drawContent()
    val outline = shape.createOutline(size, layoutDirection, density = this)
    val dashedStroke = Stroke(
        cap = cap,
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx())
        )
    )
    // Draw the border
    drawOutline(
        outline = outline,
        style = dashedStroke,
        brush = brush
    )
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

@Composable
@Preview
private fun MainComponentPreview() {
    MainComponent(
        state = MainUiState(),
        openFileChooser = {},
        convert = {},
        showInFolder = {}
    )
}

