package pe.edu.upeu.pharmamobil.platform

import android.os.Build

/**
 * Android: los datos vienen de android.os.Build, que el sistema llena al
 * arrancar. Son campos estaticos: no hace falta un Context.
 */
actual class InfoDispositivo actual constructor() {

    actual val sistema: String = "Android"

    // RELEASE es la version visible («16»); SDK_INT es el nivel de API que usa el codigo.
    actual val version: String = "${Build.VERSION.RELEASE ?: "?"} (API ${Build.VERSION.SDK_INT})"

    actual val modelo: String = listOfNotNull(Build.MANUFACTURER, Build.MODEL)
        .joinToString(" ")
        .ifBlank { "Desconocido" }
}
