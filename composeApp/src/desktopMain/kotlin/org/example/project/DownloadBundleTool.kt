import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.exists

suspend fun downloadBundleTool(client: HttpClient) {
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

fun isBundleToolExists(): Boolean {
    val home = System.getProperty("user.home")
    val path = Path("$home/.aabtoapk/bundletool.jar")
    return path.exists()
}