package pe.edu.upeu.pharmamobil.domain.sync

import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Sesion 13: lo que paso al enviar UNA fila pendiente, sin ningun codigo HTTP.
 * La traduccion desde ErrorApi vive en la capa de datos (EnvioRemoto).
 */
sealed interface ResultadoEnvio {

    /** El servidor lo guardo: la fila local pasa a tener el id real. */
    data class Exito(val delServidor: Producto) : ResultadoEnvio

    /**
     * Fallo pasajero (servidor sin responder a tiempo o con un 5xx): se suma un intento,
     * se espera un poco mas cada vez y se vuelve a probar en la siguiente sincronizacion.
     */
    data class Transitorio(val motivo: String) : ResultadoEnvio

    /**
     * Repetirlo dara siempre lo mismo (datos invalidos, nombre duplicado, sin permiso,
     * recurso inexistente): la fila queda fallida y visible para que el usuario decida.
     */
    data class Permanente(val motivo: String) : ResultadoEnvio

    /**
     * No tiene sentido seguir con ninguna fila: sin red, o la sesion vencio (401). Las filas
     * no pierden intentos, porque la peticion nunca llego a evaluarse. Cuarta rama respecto a
     * la guia: tratar el 401 como permanente marcaria como fallido un producto valido solo
     * porque el token vencio (actividad autonoma 11, escenario 5).
     */
    data class Detener(val causa: Throwable) : ResultadoEnvio
}

/** Lo que deja una sincronizacion completa. */
data class ResumenSync(
    val enviadas: Int,
    /** Filas que fallaron en ESTA sincronizacion (permanente o sin intentos restantes). */
    val nuevasFallidas: Int,
    /** Pendientes que todavia se reintentaran solas. */
    val pendientes: Long,
    /** Total de filas fallidas que esperan al usuario. */
    val fallidas: Long
)

/** Limite y espera de los reintentos: sin limite gasta bateria; sin espera castiga la red. */
object PoliticaDeReintento {
    const val MAX_INTENTOS = 5

    /** 1 s, 2 s, 4 s, 8 s, 16 s: la espera se duplica con cada intento y se topa en 16 s. */
    fun espera(intentos: Int): Long = 1_000L shl intentos.coerceIn(0, 4)
}
