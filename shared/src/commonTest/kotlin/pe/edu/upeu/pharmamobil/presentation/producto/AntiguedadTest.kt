package pe.edu.upeu.pharmamobil.presentation.producto

import kotlin.test.Test
import kotlin.test.assertEquals

/** Autonoma 11: «Actualizado hace...» en lenguaje natural, con un reloj fijo (sin esperar). */
class AntiguedadTest {

    private val ahora = 1_792_000_000_000L
    private fun hace(ms: Long) = textoDeAntiguedad(ahora - ms, ahora)
    private val minuto = 60_000L
    private val hora = 60 * minuto
    private val dia = 24 * hora

    @Test
    fun nuncaSincronizadoSeDiceExplicitamente() {
        assertEquals("Nunca sincronizado", textoDeAntiguedad(null, ahora))
    }

    @Test
    fun menosDeUnMinutoEsUnMomento() {
        assertEquals("Actualizado hace un momento", hace(30_000))
    }

    @Test
    fun minutosHorasYDiasConSingularYPlural() {
        assertEquals("Actualizado hace 1 minuto", hace(minuto))
        assertEquals("Actualizado hace 5 minutos", hace(5 * minuto + 10_000))
        assertEquals("Actualizado hace 1 hora", hace(hora))
        assertEquals("Actualizado hace 2 horas", hace(2 * hora + 30 * minuto))
        assertEquals("Actualizado hace 1 día", hace(26 * hora))
        assertEquals("Actualizado hace 3 días", hace(3 * dia + hora))
    }

    @Test
    fun unRelojAtrasadoNoProduceTiemposNegativos() {
        assertEquals("Actualizado hace un momento", textoDeAntiguedad(ahora + hora, ahora))
    }
}
