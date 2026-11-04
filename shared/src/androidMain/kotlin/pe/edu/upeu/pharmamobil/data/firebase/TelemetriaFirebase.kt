package pe.edu.upeu.pharmamobil.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import pe.edu.upeu.pharmamobil.data.telemetria.TelemetriaConsola
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.port.sinDatosSensibles

/**
 * Sesion 13: la Telemetria del dominio con Firebase Crashlytics (SDK multiplataforma de
 * GitLive). Es la UNICA clase que importa el SDK: el resto de la app solo ve Telemetria.
 *
 * Si la compilacion no trae google-services.json (por ejemplo en GitHub Actions, donde no se
 * sube porque tiene la clave del proyecto), Firebase no se inicializa: entonces se escribe en
 * el registro del dispositivo en lugar de fallar.
 */
class TelemetriaFirebase(private val contexto: Context) : Telemetria {

    private val respaldo = TelemetriaConsola()

    private val disponible: Boolean
        get() = FirebaseApp.getApps(contexto).isNotEmpty()

    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        if (!disponible) return respaldo.registrarError(e, contexto)
        val crashlytics = Firebase.crashlytics
        contexto.sinDatosSensibles().forEach { (clave, valor) -> crashlytics.setCustomKey(clave, valor) }
        crashlytics.recordException(e)
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        if (!disponible) return respaldo.registrarEvento(nombre, datos)
        Firebase.crashlytics.log("$nombre ${datos.sinDatosSensibles()}")
    }
}
