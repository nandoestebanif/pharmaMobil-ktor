package pe.edu.upeu.pharmamobil.domain.permiso

/** Sesion 13: permisos que la app sabe pedir. Hoy se usa NOTIFICACIONES. */
enum class Permiso { CAMARA, UBICACION, NOTIFICACIONES }

sealed interface EstadoPermiso {
    data object Concedido : EstadoPermiso
    /** Se puede volver a pedir (nunca se pidio, o el usuario dijo que no una vez). */
    data object Denegado : EstadoPermiso
    /** El sistema ya no muestra el dialogo: solo queda ir a Ajustes. */
    data object DenegadoParaSiempre : EstadoPermiso
}

/**
 * Se declara como interfaz porque la implementacion de Android necesita la Activity para
 * lanzar el dialogo del sistema, y el codigo comun no puede nombrarla.
 */
interface GestorPermisos {
    suspend fun estado(permiso: Permiso): EstadoPermiso
    suspend fun solicitar(permiso: Permiso): EstadoPermiso
    fun abrirAjustes()
}

/**
 * La tabla del paso 5 de la guia como funcion pura (se prueba en commonTest).
 * Android responde «no procede explicar» en dos casos distintos: antes de la primera
 * solicitud y despues de un rechazo definitivo. Solo recordando si ya se pidio se distinguen.
 */
fun decidirEstado(concedido: Boolean, yaSePidio: Boolean, procedeExplicar: Boolean): EstadoPermiso = when {
    concedido -> EstadoPermiso.Concedido
    !yaSePidio -> EstadoPermiso.Denegado
    procedeExplicar -> EstadoPermiso.Denegado
    else -> EstadoPermiso.DenegadoParaSiempre
}
