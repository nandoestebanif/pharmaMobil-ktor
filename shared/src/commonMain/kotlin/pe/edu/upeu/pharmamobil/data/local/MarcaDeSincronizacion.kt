package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import pe.edu.upeu.pharmamobil.db.PharmaDb

/** Actividad autonoma 11: guarda y emite el instante de la ultima sincronizacion correcta. */
class MarcaDeSincronizacion(base: PharmaDb) {

    private val consultas = base.sincronizacionQueries

    /** null mientras nunca se haya sincronizado. Emite de nuevo con cada sincronizacion. */
    fun observar(): Flow<Long?> = consultas.ultima().asFlow().mapToOneOrNull(Dispatchers.Default)

    suspend fun registrar(instanteMs: Long) {
        withContext(Dispatchers.IO) { consultas.registrar(instanteMs) }
    }
}
