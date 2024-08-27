import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.hydraulic.conveyor.control.SoftwareUpdateController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.inputStream

val updateController: SoftwareUpdateController? = SoftwareUpdateController.getInstance()
val canDoOnlineUpdates get() = updateController?.canTriggerUpdateCheckUI() == SoftwareUpdateController.Availability.AVAILABLE

@Composable
@Preview
fun CheckForUpdate() {
    val currentVersion by remember { mutableStateOf(updateController?.currentVersion?.version ?: "Unknown") }
    var remoteVersion by remember { mutableStateOf("Checking...") }
    var updateAvailable by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val remoteVersionObj: SoftwareUpdateController.Version? = updateController?.currentVersionFromRepository
                remoteVersion = remoteVersionObj?.version ?: "Unknown"
                updateAvailable = (remoteVersionObj?.compareTo(updateController.currentVersion) ?: 0) > 0
            } catch (e: Exception) {
                remoteVersion = "Error: ${e.message}"
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Current Version: $currentVersion")
        Text("Remote Version: $remoteVersion")
        Text("Update Available: ${if (updateAvailable) "Yes" else "No"}")
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (canDoOnlineUpdates) {
                    updateController!!.triggerUpdateCheckUI()
                }
            },
            enabled = updateAvailable && canDoOnlineUpdates
        ) {
            Text("Check for Updates")
        }
    }
}

fun main(args: Array<String>) {
    val version = System.getProperty("app.version") ?: "Development"
    application {
        // app.dir is set when packaged to point at our collected inputs.
        val appIcon = remember {
            System.getProperty("app.dir")
                ?.let { Paths.get(it, "icon.png") }
                ?.takeIf { it.exists() }
                ?.inputStream()
                ?.buffered()
                ?.use { BitmapPainter(loadImageBitmap(it)) }
        }

        Window(onCloseRequest = ::exitApplication, icon = appIcon, title = "AABtoAPK") {
            App()
        }
    }
}
