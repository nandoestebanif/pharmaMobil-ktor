package pe.edu.upeu.pharmamobil.domain.platform

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Actividad autonoma 11: de donde sale «ahora». Es una interfaz para que las pruebas usen
 * un reloj fijo y comprueben «hace 2 horas» sin esperar dos horas.
 */
fun interface Reloj {
    /** Milisegundos desde 1970-01-01 (UTC). */
    fun ahoraMs(): Long
}

/** El reloj del telefono. */
@OptIn(ExperimentalTime::class)
object RelojDelSistema : Reloj {
    override fun ahoraMs(): Long = Clock.System.now().toEpochMilliseconds()
}
