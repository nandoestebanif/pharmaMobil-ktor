package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.sync.ResumenSync

/**
 * Sesion 11: reemplaza a ListarProductosUseCase. La pantalla ya no pide la lista:
 * se suscribe una vez y recibe cada cambio de la base local.
 */
class ObservarProductosUseCase(
    private val productoRepository: ProductoRepository
) {
    operator fun invoke(): Flow<List<Producto>> = productoRepository.observar()
}

/** Actividad autonoma 11: hora de la ultima sincronizacion correcta (null = nunca). */
class ObservarUltimaSincronizacionUseCase(
    private val productoRepository: ProductoRepository
) {
    operator fun invoke(): Flow<Long?> = productoRepository.ultimaSincronizacion()
}

/**
 * Sube lo pendiente y trae la lista del servidor. Sin red devuelve failure (SinConexion).
 * Sesion 13: si en esta sincronizacion alguna fila quedo fallida, avisa con una notificacion
 * (el Notificador de Android solo la muestra si el usuario concedio el permiso).
 */
class SincronizarProductosUseCase(
    private val productoRepository: ProductoRepository,
    private val notificador: Notificador? = null
) {
    suspend operator fun invoke(): Result<ResumenSync> = resultadoDe {
        productoRepository.sincronizar().also { avisar(it) }
    }

    /** «Reintentar»: las fallidas vuelven a la cola y se sincroniza de inmediato. */
    suspend fun reintentarFallidas(): Result<ResumenSync> = resultadoDe {
        productoRepository.reintentarFallidas().also { avisar(it) }
    }

    private fun avisar(r: ResumenSync) {
        if (r.nuevasFallidas > 0) notificador?.avisarEnviosFallidos(r.fallidas)
    }
}
