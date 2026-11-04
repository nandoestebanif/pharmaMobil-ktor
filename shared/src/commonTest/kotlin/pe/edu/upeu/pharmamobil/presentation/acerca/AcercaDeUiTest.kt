package pe.edu.upeu.pharmamobil.presentation.acerca

import pe.edu.upeu.pharmamobil.platform.InfoDispositivo
import pe.edu.upeu.pharmamobil.platform.formatearSoles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AcercaDeUiTest {

    @Test
    fun reconoceElEspacioDeNoSeparacion() {
        assertEquals(
            "U+00A0 (espacio de no separación)",
            describirSeparador("S/ 24.50")
        )
    }

    @Test
    fun reconoceUnEspacioNormal() {
        assertEquals("U+0020 (espacio)", describirSeparador("S/ 24.50"))
    }

    @Test
    fun avisaCuandoElSimboloVaPegado() {
        assertEquals("Ninguno: el símbolo va pegado al monto", describirSeparador("S/24.50"))
    }

    @Test
    fun noFallaConUnTextoSinCifras() {
        assertEquals("No se reconoce el formato", describirSeparador("S/"))
    }

    @Test
    fun laPantallaUsaElFormatoDeLaPlataforma() {

        val datos = InfoDispositivo().aUi()

        assertEquals(formatearSoles(MONTO_DE_MUESTRA), datos.precioDeMuestra)
        assertTrue(datos.precioDeMuestra.startsWith("S/"))
        assertTrue(datos.sistema.isNotBlank())
    }
}
