import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

@Composable
@Preview
fun App() {
    MaterialTheme(colors = MaterialTheme.colors.copy(surface = BackgroundColor)) {
        val viewModel = MainViewModel()
        Main(viewModel)
    }
}

@Composable
private fun Main(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    if (state.isFileChooserOpen) {
        FileDialog(
            fileType = state.fileType,
            onCloseRequest = {
                viewModel.setFileChooserClose()
                when (state.fileType) {
                    FileType.AAB -> viewModel.setFile(it ?: "")
                    FileType.KEYSTORE -> viewModel.setKeystore(it ?: "")
                }
            }
        )
    }
    MainComponent(
        state = state,
        openFileChooser = { viewModel.setFileChooserOpen(it) },
        convert = { viewModel.fromAABToAPK() },
        showInFolder = { viewModel.openContainingFolder(state.outputFile) },
        setKeystorePassword = { viewModel.setKeystorePassword(it) },
        setAlias = { viewModel.setAlias(it) },
        setAliasPassword = { viewModel.setAliasPassword(it) },
        resetKeyStore = { viewModel.resetKeystore() }
    )
}

@Composable
private fun MainComponent(
    state: MainUiState,
    openFileChooser: (fileType: FileType) -> Unit,
    convert: () -> Unit,
    showInFolder: () -> Unit,
    setKeystorePassword: (text: String) -> Unit,
    setAlias: (text: String) -> Unit,
    setAliasPassword: (text: String) -> Unit,
    resetKeyStore: () -> Unit
) {
    Scaffold(Modifier.fillMaxWidth().fillMaxHeight().background(BackgroundColor)) {
        Column(
            Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
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
                    .clickable { openFileChooser(FileType.AAB) }
                    .padding(16.dp)
            ) {
                if (state.file.isEmpty()) {
                    Text(
                        "Click here to browse your AAB",
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
            Column(
                Modifier
                    .widthIn(600.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                var isDebug by remember { mutableStateOf(true) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "debug.keystore",
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.subtitle1,
                        color = PrimaryTextColor
                    )
                    RadioButton(
                        selected = isDebug,
                        onClick = {
                            isDebug = true
                            resetKeyStore()
                        }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "release.keystore",
                        modifier = Modifier.padding(end = 2.dp),
                        style = MaterialTheme.typography.subtitle1,
                        color = PrimaryTextColor
                    )
                    RadioButton(
                        selected = isDebug.not(),
                        onClick = { isDebug = false }
                    )
                }
                if (isDebug.not()) {
                    Column(
                        Modifier
                            .widthIn(600.dp)
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .dashedBorder(
                                SolidColor(SecondaryTextColor),
                                shape = RoundedCornerShape(12.dp),
                                gapLength = 0.dp
                            ).padding(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp),
                            ) {
                                Text(
                                    text = state.keyStore?.path ?: "",
                                    style = MaterialTheme.typography.subtitle2,
                                    color = PrimaryTextColor
                                )
                                Button(
                                    modifier = Modifier.padding(16.dp),
                                    onClick = { openFileChooser(FileType.KEYSTORE) },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryColor),
                                    enabled = state.error == null
                                ) {
                                    val text = "Browse Keystore"
                                    Text(text, color = Color.White)
                                }
                            }
                            TextField(
                                value = state.keyStore?.keyStorePassword ?: "",
                                placeholder = { Text("Keystore password") },
                                modifier = Modifier.padding(8.dp),
                                onValueChange = setKeystorePassword
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextField(
                                value = state.keyStore?.alias ?: "",
                                placeholder = { Text("Keystore alias") },
                                modifier = Modifier.padding(8.dp),
                                onValueChange = setAlias
                            )
                            TextField(
                                value = state.keyStore?.aliasPassword ?: "",
                                placeholder = { Text("Keystore alias password") },
                                modifier = Modifier.padding(8.dp),
                                onValueChange = setAliasPassword
                            )
                        }
                    }
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
                        .dashedBorder(
                            SolidColor(SecondaryTextColor),
                            shape = RoundedCornerShape(12.dp),
                            gapLength = 0.dp
                        )
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
    fileType: FileType,
    onCloseRequest: (result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", LOAD) {
            override fun setVisible(value: Boolean) {
                this.filenameFilter = FilenameFilter { dir, name ->
                    name.contains(fileType.pattern)
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
        showInFolder = {},
        setKeystorePassword = {},
        setAlias = {},
        setAliasPassword = {},
        resetKeyStore = {}
    )
}

