package pe.edu.upeu.pharmamobil.permiso

import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.port.Telemetria

/**
 * Sesion 13: dobles de las piezas del mundo exterior. Es la ventaja de declararlas como
 * interfaz: en commonTest no hace falta Activity, Context ni Firebase.
 */
class GestorPermisosFalso(
    var actual: EstadoPermiso = EstadoPermiso.Denegado,
    /** Lo que «responde» el dialogo del sistema cuando se pide. */
    var alPedir: EstadoPermiso = EstadoPermiso.Concedido
) : GestorPermisos {
    var solicitudes = 0
    var ajustesAbiertos = 0
    override suspend fun estado(permiso: Permiso) = actual
    override suspend fun solicitar(permiso: Permiso): EstadoPermiso { solicitudes++; actual = alPedir; return actual }
    override fun abrirAjustes() { ajustesAbiertos++ }
}

class NotificadorFalso : Notificador {
    val fallidosAvisados = mutableListOf<Long>()
    var pruebas = 0
    override fun avisarEnviosFallidos(cantidad: Long) { fallidosAvisados += cantidad }
    override fun avisoDePrueba() { pruebas++ }
}

class TelemetriaFalsa : Telemetria {
    val errores = mutableListOf<Throwable>()
    val eventos = mutableListOf<Pair<String, Map<String, String>>>()
    override fun registrarError(e: Throwable, contexto: Map<String, String>) { errores += e }
    override fun registrarEvento(nombre: String, datos: Map<String, String>) { eventos += nombre to datos }
}
