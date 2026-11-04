package pe.edu.upeu.pharmamobil.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Esta prueba vive en commonTest y se ejecuta contra el `actual` de cada
 * plataforma. Por eso no compara con un texto fijo: los separadores y los
 * espacios pueden diferir entre Android e iOS, y esa diferencia es justo lo
 * que el expect/actual deja en manos de cada sistema.
 */
class FormatoTest {

    @Test
    fun muestraElSimboloDelSolYDosDecimales() {

        val texto = formatearSoles(12.5)

        assertTrue(texto.contains("S/"), "Falta el símbolo del sol en \"$texto\"")
        assertTrue(texto.contains("12.50"), "Faltan los dos decimales en \"$texto\"")
    }

    @Test
    fun redondeaAlCentimo() {

        assertTrue(formatearSoles(3.456).contains("3.46"))
    }

    @Test
    fun conservaTodasLasCifrasEnMontosGrandes() {

        // El separador de miles depende de la plataforma: se comparan solo los digitos.
        val cifras = formatearSoles(1234.5).filter { it.isDigit() }

        assertEquals("123450", cifras)
    }
}
