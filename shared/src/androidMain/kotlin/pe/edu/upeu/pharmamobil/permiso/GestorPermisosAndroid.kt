package pe.edu.upeu.pharmamobil.permiso

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import pe.edu.upeu.pharmamobil.domain.permiso.decidirEstado

/**
 * Sesion 13: permisos en tiempo de ejecucion de Android.
 *
 * shouldShowRequestPermissionRationale responde false antes de la primera solicitud Y despues
 * de un rechazo definitivo. Para distinguirlos se guarda si el permiso ya se pidio alguna vez
 * (tabla del paso 5, en [decidirEstado]).
 */
class GestorPermisosAndroid(private val contexto: Context) : GestorPermisos {

    private val preferencias = contexto.getSharedPreferences("permisos", Context.MODE_PRIVATE)

    /** La Activity registra aqui su lanzador en onCreate y lo quita en onDestroy. */
    var puente: PuentePermisos? = null

    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        val nombre = nombreDeSistema(permiso) ?: return sinPermisoEnTiempoDeEjecucion(permiso)
        val concedido = ContextCompat.checkSelfPermission(contexto, nombre) == PackageManager.PERMISSION_GRANTED
        if (concedido) return EstadoPermiso.Concedido
        val actividad = puente?.actividad ?: return EstadoPermiso.Denegado
        return decidirEstado(
            concedido = false,
            yaSePidio = preferencias.getBoolean("pedido_$nombre", false),
            procedeExplicar = ActivityCompat.shouldShowRequestPermissionRationale(actividad, nombre)
        )
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        val nombre = nombreDeSistema(permiso) ?: return sinPermisoEnTiempoDeEjecucion(permiso)
        preferencias.edit().putBoolean("pedido_$nombre", true).apply()
        val concedido = puente?.pedir(nombre) ?: false
        return if (concedido) EstadoPermiso.Concedido else estado(permiso)
    }

    /** Lleva a los ajustes de notificaciones de la app (o a su ficha, para los demas permisos). */
    override fun abrirAjustes() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
        }
        contexto.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Antes de Android 13 las notificaciones no se piden: solo se pueden apagar en Ajustes. */
    private fun sinPermisoEnTiempoDeEjecucion(permiso: Permiso): EstadoPermiso =
        if (permiso == Permiso.NOTIFICACIONES && !NotificationManagerCompat.from(contexto).areNotificationsEnabled())
            EstadoPermiso.DenegadoParaSiempre
        else EstadoPermiso.Concedido

    private fun nombreDeSistema(permiso: Permiso): String? = when (permiso) {
        Permiso.CAMARA -> Manifest.permission.CAMERA
        Permiso.UBICACION -> Manifest.permission.ACCESS_FINE_LOCATION
        Permiso.NOTIFICACIONES ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
    }
}
