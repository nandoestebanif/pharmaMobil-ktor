package pe.edu.upeu.pharmamobil.data.telemetria

import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.port.sinDatosSensibles
import pe.edu.upeu.pharmamobil.platform.escribirRegistro

/**
 * Sesion 13: telemetria que solo escribe en el registro del dispositivo. La usa iOS (sin el
 * SDK nativo de Firebase enlazado en Xcode) y Android cuando no hay google-services.json.
 */
class TelemetriaConsola : Telemetria {
    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        escribirRegistro("Telemetria error ${e::class.simpleName}: ${e.message} ${contexto.sinDatosSensibles()}")
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        escribirRegistro("Telemetria evento $nombre ${datos.sinDatosSensibles()}")
    }
}
