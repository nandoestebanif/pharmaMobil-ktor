package pe.edu.upeu.pharmamobil

import androidx.compose.ui.window.ComposeUIViewController
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.navigation.pantallaPorNombre
import platform.Foundation.NSProcessInfo
import kotlin.experimental.ExperimentalNativeApi

fun MainViewController() = ComposeUIViewController {
    App(
        pantallaInicial = pantallaDesdeArgumentos(),
        mostrarAccionesSinPermiso = mostrarAccionesSinPermiso()
    )
}

/**
 * Sesion 10: `-acciones todas` muestra acciones que el rol no tiene, para
 * provocar un 403 a proposito. Solo en una compilacion de depuracion.
 */
@OptIn(ExperimentalNativeApi::class)
private fun mostrarAccionesSinPermiso(): Boolean {
    if (!Platform.isDebugBinary) return false
    val argumentos = NSProcessInfo.processInfo.arguments.map { it.toString() }
    val indice = argumentos.indexOf("-acciones")
    return indice >= 0 && argumentos.getOrNull(indice + 1) == "todas"
}

/**
 * Lee el argumento de arranque `-pantalla <nombre>` (ej. `xcrun simctl launch ... -pantalla productos`).
 * Los nombres son los mismos con que se guarda la pila (inicio, productos, acerca...).
 * Sin argumento, la app abre en Inicio como siempre.
 */
private fun pantallaDesdeArgumentos(): Screen {
    val argumentos = NSProcessInfo.processInfo.arguments.map { it.toString() }
    val indice = argumentos.indexOf("-pantalla")
    if (indice < 0) return Screen.Inicio
    return pantallaPorNombre(argumentos.getOrNull(indice + 1))
}
