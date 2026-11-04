package pe.edu.upeu.pharmamobil.domain.model

/** Por que no se pudo iniciar o mantener la sesion. */
sealed interface ErrorSesion {

    /** 401 en el login: usuario o contrasena incorrectos. */
    data object CredencialesInvalidas : ErrorSesion

    /** 401 con un token vencido: hay que volver a iniciar sesion. */
    data object SesionExpirada : ErrorSesion

    /** 401 con un token que el servidor no acepta (alterado, de otro servidor...). */
    data object SesionInvalida : ErrorSesion

    /** 401 de un usuario que el administrador deshabilito con la sesion abierta. */
    data object UsuarioDeshabilitado : ErrorSesion

    /** 403: la sesion sigue, pero el rol no tiene ese permiso. */
    data object SinPermiso : ErrorSesion

    /** No se llego al servidor. */
    data object SinConexion : ErrorSesion
}

/** Transporta un [ErrorSesion] por el canal de fallo de Result. */
class ErrorSesionException(val error: ErrorSesion) : Exception(error.toString())
