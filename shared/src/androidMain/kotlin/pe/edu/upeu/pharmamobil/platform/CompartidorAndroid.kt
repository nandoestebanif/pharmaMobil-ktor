package pe.edu.upeu.pharmamobil.platform

import android.content.Context
import android.content.Intent
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor

/** Android: abre el selector del sistema con un Intent ACTION_SEND. */
class CompartidorAndroid(
    private val contexto: Context
) : Compartidor {

    override fun compartir(texto: String) {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        val selector = Intent.createChooser(envio, null).apply {
            // El Context que entrega Koin es el de la aplicacion, no el de una
            // Activity: sin esta bandera Android lanza una excepcion al abrirlo.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contexto.startActivity(selector)
    }
}
