package pe.edu.upeu.pharmamobil.domain.port

/**
 * Sesion 13: avisos del sistema (notificaciones). Android necesita un Context y el permiso
 * POST_NOTIFICATIONS; por eso el dominio solo declara que se puede avisar.
 */
interface Notificador {
    /** Algunos cambios hechos en el dispositivo no se pudieron enviar a PharmaSoft. */
    fun avisarEnviosFallidos(cantidad: Long)

    /** Aviso de prueba para comprobar que el permiso funciona. */
    fun avisoDePrueba()
}
