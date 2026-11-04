package pe.edu.upeu.pharmamobil.permiso

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

/**
 * Sesion 13: une la corrutina que pide el permiso con el dialogo del sistema. La Activity lo
 * crea en onCreate (registerForActivityResult debe llamarse antes de que la pantalla arranque)
 * y la respuesta del dialogo completa el valor que la funcion suspend estaba esperando.
 */
class PuentePermisos(val actividad: ComponentActivity) {

    private var pendiente: CompletableDeferred<Boolean>? = null

    private val lanzador = actividad.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> pendiente?.complete(concedido) }

    suspend fun pedir(nombre: String): Boolean {
        val espera = CompletableDeferred<Boolean>()
        pendiente = espera
        lanzador.launch(nombre)
        return espera.await()
    }
}
