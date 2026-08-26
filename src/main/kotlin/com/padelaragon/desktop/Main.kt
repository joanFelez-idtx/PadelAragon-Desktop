package com.padelaragon.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.padelaragon.desktop.data.local.AppDatabase
import com.padelaragon.desktop.data.network.HtmlFetcher
import com.padelaragon.desktop.di.MultiLeagueContainer
import com.padelaragon.desktop.ui.navigation.AppNavHost
import com.padelaragon.desktop.ui.theme.PadelAragonTheme
import java.io.File

/** Per-user application data directory, e.g. ~/.padelaragon on Linux/macOS or %USERPROFILE%\.padelaragon on Windows. */
private val appDataDir: File
    get() = File(System.getProperty("user.home"), ".padelaragon")

fun main() {
    // Prewarm HTTPS connection during startup (saves TLS handshake on first real request).
    HtmlFetcher.prewarmConnection("https://padelfederacion.es/pAGINAS/ARAPADEL/Ligas_Calendario.asp")

    val cacheDir = File(appDataDir, "cache")
    val database = AppDatabase.getInstance(appDataDir)
    val multiLeagueContainer = MultiLeagueContainer(database, cacheDir)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PadelAragon",
            state = rememberWindowState(size = DpSize(1100.dp, 800.dp))
        ) {
            PadelAragonTheme {
                AppNavHost(multiLeagueContainer)
            }
        }
    }
}
