package pe.edu.upeu.pharmamobil.platform

import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import pe.edu.upeu.pharmamobil.domain.port.Notificador

/**
 * Sesion 13: en iOS el permiso de notificaciones se pide con UNUserNotificationCenter y
 * necesita su clave en Info.plist. Sin una Mac para compilar y probar en el simulador, aqui
 * queda una version minima que no pide nada (la tarjeta muestra «Activar avisos» sin efecto).
 */
class GestorPermisosIos : GestorPermisos {
    override suspend fun estado(permiso: Permiso): EstadoPermiso = EstadoPermiso.Denegado
    override suspend fun solicitar(permiso: Permiso): EstadoPermiso = EstadoPermiso.Denegado
    override fun abrirAjustes() = Unit
}

class NotificadorIos : Notificador {
    override fun avisarEnviosFallidos(cantidad: Long) = escribirRegistro("Avisos: $cantidad envios fallidos")
    override fun avisoDePrueba() = escribirRegistro("Avisos: aviso de prueba")
}
