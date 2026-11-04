package pe.edu.upeu.pharmamobil.domain.model

/** Lo que escribe el usuario en la pantalla de inicio de sesion. */
data class Credenciales(val usuario: String, val clave: String)

/**
 * Sesion activa. El dominio no sabe que el token es un JWT ni como viaja:
 * solo lo guarda para que la capa de datos lo use.
 */
data class Sesion(
    val token: String,
    val usuario: String,
    val nombreCompleto: String,
    val permisos: Set<String>,
    val roles: List<String> = emptyList()
) {
    fun puede(permiso: String): Boolean = permiso in permisos
}

/**
 * Permisos que emite PharmaSoft (enum Permiso del backend). Se usan para
 * decidir que acciones ofrece la interfaz; quien autoriza es el servidor.
 */
object Permiso {
    const val CATALOGO_LEER = "CATALOGO_LEER"
    const val CATALOGO_ESCRIBIR = "CATALOGO_ESCRIBIR"
    const val CLIENTES_LEER = "CLIENTES_LEER"
    const val CLIENTES_ESCRIBIR = "CLIENTES_ESCRIBIR"
    const val VENTAS_LEER = "VENTAS_LEER"
    const val VENTAS_REGISTRAR = "VENTAS_REGISTRAR"
    const val REPORTES_VER = "REPORTES_VER"
    const val USUARIOS_ADMINISTRAR = "USUARIOS_ADMINISTRAR"
}
