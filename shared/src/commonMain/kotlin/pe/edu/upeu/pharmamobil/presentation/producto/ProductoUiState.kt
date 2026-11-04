package pe.edu.upeu.pharmamobil.presentation.producto

/**
 * Estado unico e inmutable de la pantalla de productos.
 *
 * Separa dos cosas que no deben confundirse:
 *  - [fase]: el estado de la pantalla completa (la consulta del inventario).
 *  - [operacion]: el estado de la accion del usuario (crear, actualizar, eliminar).
 * Asi, al guardar o eliminar, el listado sigue visible y no vuelve a Cargando.
 *
 * La fase describe el inventario ACTIVO; los dados de baja van aparte en
 * [dadosDeBaja] para poder reactivarlos.
 */
data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val formulario: FormularioProducto = FormularioProducto(),
    val operacion: Operacion = Operacion.Inactiva,
    val mensajeExito: String? = null,
    /** Productos dados de baja (eliminados): se pueden reactivar. */
    val dadosDeBaja: List<ProductoUi> = emptyList(),
    /** true cuando el usuario mira la lista de dados de baja en vez del inventario activo. */
    val viendoBajas: Boolean = false,
    /** Sesion 11: true mientras se sube lo pendiente y se baja la lista del servidor. */
    val sincronizando: Boolean = false,
    /** Sesion 11: aviso cuando la ultima sincronizacion fallo (p. ej. sin conexion). */
    val avisoRed: String? = null,
    /** Autonoma 11: instante (ms) de la ultima sincronizacion correcta; null = nunca. */
    val ultimaSincronizacion: Long? = null
) {

    /** Sesion 11: productos creados sin red que aun no llegan al servidor. */
    val pendientes: Int
        get() = todos.count { it.pendiente && !it.fallido }

    /** Sesion 13: pendientes que ya no se reintentan solos (el servidor los rechazo o se agotaron). */
    val fallidas: Int
        get() = todos.count { it.fallido }

    private val todos: List<ProductoUi>
        get() = (fase as? Fase.ConProductos)?.productos.orEmpty() + dadosDeBaja

    /** true mientras hay una operacion contra el servidor: los botones se deshabilitan. */
    val operando: Boolean
        get() = operacion is Operacion.EnCurso

    /** Fases excluyentes del inventario: solo una puede estar activa. */
    sealed interface Fase {

        data object Cargando : Fase

        data object SinProductos : Fase

        /**
         * Autonoma 11: la base local esta vacia y no hay red para llenarla (primera apertura
         * sin conexion). No es un error del usuario ni de la app: se explica y se espera la red.
         */
        data object SinDatosSinConexion : Fase

        data class ConProductos(val productos: List<ProductoUi>) : Fase

        data class Error(val mensaje: String) : Fase
    }

    /** Estado de la accion en curso; convive con cualquier fase. */
    sealed interface Operacion {

        data object Inactiva : Operacion

        /** [productoId] identifica la fila afectada (null al crear). */
        data class EnCurso(val tipo: Tipo, val productoId: Long? = null) : Operacion

        /** La operacion fallo: se avisa sin perder la lista. */
        data class Fallida(val mensaje: String) : Operacion

        enum class Tipo { Crear, Actualizar, Eliminar, Reactivar }
    }
}

data class FormularioProducto(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null,
    /** Id del producto que se esta editando; null cuando el formulario crea uno nuevo. */
    val editandoId: Long? = null
) {

    val enEdicion: Boolean
        get() = editandoId != null
}
