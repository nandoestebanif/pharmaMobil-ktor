package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.sync.ResumenSync

/**
 * Contrato del inventario. Vive en el dominio; la implementacion offline-first
 * (ProductoRepositorioOffline) y la de memoria viven en la capa de datos.
 *
 * Sesion 11: el repositorio ya no entrega una «foto» de la lista pedida a mano.
 * La pantalla se suscribe a [observar] y la red solo mantiene la base local al dia.
 *
 * Si una operacion falla contra el servidor, la implementacion lanza
 * ErrorApiException: los casos de uso la convierten en Result.failure.
 */
interface ProductoRepository {

    /** La pantalla se suscribe una vez y se actualiza sola. Incluye los dados de baja. */
    fun observar(): Flow<List<Producto>>

    /**
     * Actividad autonoma 11: instante (ms) de la ultima sincronizacion correcta, o null si
     * nunca hubo una. Emite de nuevo con cada sincronizacion.
     */
    fun ultimaSincronizacion(): Flow<Long?>

    /**
     * Sube lo pendiente y luego trae la lista del servidor. En ese orden.
     * Sesion 13: devuelve cuantas filas se enviaron y cuantas quedaron fallidas.
     */
    suspend fun sincronizar(): ResumenSync

    /** Sesion 13: las filas fallidas vuelven a la cola (intentos en cero) y se sincroniza. */
    suspend fun reintentarFallidas(): ResumenSync

    /** Trae un producto por su id; falla con NoEncontrado si ya no existe. */
    suspend fun obtener(id: Long): Producto

    /** Guarda en local; intenta enviar si hay conexion. Sin red queda pendiente. */
    suspend fun registrar(producto: Producto): Producto

    /** Reemplaza los datos del producto con ese id y devuelve la version guardada. */
    suspend fun actualizar(producto: Producto): Producto

    /** Da de baja el producto con ese id (se reactiva actualizandolo con activo = true). */
    suspend fun eliminar(id: Long)
}
