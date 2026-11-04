package pe.edu.upeu.pharmamobil.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pe.edu.upeu.pharmamobil.domain.port.Notificador

/**
 * Sesion 13: notificaciones del sistema. Sin el permiso POST_NOTIFICATIONS (Android 13+)
 * no hace nada: el permiso lo pide la tarjeta «Avisos» de la portada, nunca este codigo.
 */
class NotificadorAndroid(private val contexto: Context) : Notificador {

    override fun avisarEnviosFallidos(cantidad: Long) = mostrar(
        ID_FALLIDOS,
        "Cambios sin enviar a PharmaSoft",
        if (cantidad == 1L) "1 producto no se pudo enviar. Ábrelo en Productos para revisarlo."
        else "$cantidad productos no se pudieron enviar. Ábrelos en Productos para revisarlos."
    )

    override fun avisoDePrueba() = mostrar(
        ID_PRUEBA, "Avisos de PharmaMobil", "Así verás cuando un cambio no se pueda enviar."
    )

    @SuppressLint("MissingPermission") // se comprueba en puedeNotificar()
    private fun mostrar(id: Int, titulo: String, texto: String) {
        if (!puedeNotificar()) return
        crearCanal()
        val abrirApp = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName)?.let {
            PendingIntent.getActivity(contexto, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val aviso = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(contexto).notify(id, aviso)
    }

    private fun puedeNotificar(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else NotificationManagerCompat.from(contexto).areNotificationsEnabled()

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, "Sincronización", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Cambios hechos sin conexión que no se pudieron enviar" }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private companion object {
        const val CANAL = "sincronizacion"
        const val ID_FALLIDOS = 13
        const val ID_PRUEBA = 14
    }
}
