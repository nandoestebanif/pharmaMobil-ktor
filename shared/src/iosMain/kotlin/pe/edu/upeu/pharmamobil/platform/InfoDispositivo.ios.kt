package pe.edu.upeu.pharmamobil.platform

import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

/**
 * iOS: los datos vienen de UIKit (UIDevice.currentDevice). En el simulador,
 * UIDevice solo dice «iPhone»; el nombre del modelo simulado lo pone Xcode en
 * la variable de entorno SIMULATOR_DEVICE_NAME.
 */
actual class InfoDispositivo actual constructor() {

    private val dispositivo = UIDevice.currentDevice

    actual val sistema: String = dispositivo.systemName

    actual val version: String = dispositivo.systemVersion

    actual val modelo: String = modeloSimulado()
        ?.let { "$it (simulador)" }
        ?: dispositivo.model
}

private fun modeloSimulado(): String? =
    NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"]
        ?.toString()
        ?.takeIf { it.isNotBlank() }
