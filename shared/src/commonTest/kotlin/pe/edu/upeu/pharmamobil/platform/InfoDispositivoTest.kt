package pe.edu.upeu.pharmamobil.platform

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Se ejecuta con el actual de la plataforma de la prueba: en la JVM, el de
 * Android (con android.os.Build de la biblioteca de pruebas); en iOS, UIDevice.
 */
class InfoDispositivoTest {

    @Test
    fun informaElSistemaDeLaPlataforma() {

        val info = InfoDispositivo()

        assertTrue(info.sistema in setOf("Android", "iOS", "iPadOS"), "sistema: ${info.sistema}")
    }

    @Test
    fun versionYModeloNoQuedanVacios() {

        val info = InfoDispositivo()

        assertTrue(info.version.isNotBlank())
        assertTrue(info.modelo.isNotBlank())
    }
}
