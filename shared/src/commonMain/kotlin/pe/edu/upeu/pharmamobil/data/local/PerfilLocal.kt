package pe.edu.upeu.pharmamobil.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Sesion

/**
 * Sesion 11: datos de la ultima sesion validada por el servidor (nombre, permisos y
 * roles). Sin ellos, con PharmaSoft apagado la app no podria entrar: hasta la sesion 10
 * los pedia siempre a GET auth/me. El token no se guarda aqui: sigue en AlmacenSeguro.
 */
class PerfilLocal(base: PharmaDb) {

    private val consultas = base.perfilQueries

    suspend fun guardar(sesion: Sesion) = withContext(Dispatchers.IO) {
        consultas.transaction {
            consultas.borrar()
            consultas.guardar(
                usuario = sesion.usuario,
                nombre_completo = sesion.nombreCompleto,
                permisos = sesion.permisos.joinToString(SEPARADOR),
                roles = sesion.roles.joinToString(SEPARADOR)
            )
        }
    }

    /** La sesion guardada con el token vigente del almacen, o null si no hay perfil. */
    suspend fun leer(token: String): Sesion? = withContext(Dispatchers.IO) {
        consultas.obtener().executeAsOneOrNull()?.let { fila ->
            Sesion(
                token = token,
                usuario = fila.usuario,
                nombreCompleto = fila.nombre_completo,
                permisos = fila.permisos.split(SEPARADOR).filter { it.isNotBlank() }.toSet(),
                roles = fila.roles.split(SEPARADOR).filter { it.isNotBlank() }
            )
        }
    }

    suspend fun borrar() {
        withContext(Dispatchers.IO) { consultas.borrar() }
    }

    private companion object {
        const val SEPARADOR = ","
    }
}
