package pe.edu.upeu.pharmamobil.domain.port

/**
 * Sesion 13: lo que la app necesita de un servicio de telemetria, sin nombrar a ninguno.
 * La implementacion con Firebase Crashlytics vive en la capa de datos de Android; si mañana
 * se cambia de proveedor, cambia esa clase y nada mas.
 *
 * Nunca se registran datos personales ni el token: esto termina en un servicio de terceros.
 */
interface Telemetria {
    fun registrarError(e: Throwable, contexto: Map<String, String> = emptyMap())
    fun registrarEvento(nombre: String, datos: Map<String, String> = emptyMap())
}

/** Claves que nunca deben salir del dispositivo, aunque alguien las pase por error. */
private val SENSIBLES = Regex("token|password|clave|contrase|authorization|usuario|email", RegexOption.IGNORE_CASE)

/** Quita del contexto cualquier dato sensible antes de enviarlo al proveedor. */
fun Map<String, String>.sinDatosSensibles(): Map<String, String> = filterKeys { !SENSIBLES.containsMatchIn(it) }
