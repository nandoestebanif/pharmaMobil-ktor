package pe.edu.upeu.pharmamobil.navigation

sealed class Screen {

    data object Inicio : Screen()

    data object Productos : Screen()

    data object Clientes : Screen()

    data object Pedidos : Screen()

    /** Datos del dispositivo y formato regional (actividad autonoma 9). */
    data object AcercaDe : Screen()

    /** Detalle de un producto: se llega tocando una fila del inventario. */
    data class DetalleProducto(val productoId: Long) : Screen()

    /** Formulario de producto. Con [productoId] edita; sin el, registra uno nuevo. */
    data class FormularioProducto(val productoId: Long? = null) : Screen()
}

/**
 * Pantalla con que abre la app segun el nombre recibido al arrancar
 * (`-pantalla productos` en iOS, extra «pantalla» del Intent en Android).
 * Un nombre desconocido o ausente abre Inicio.
 */
fun pantallaPorNombre(nombre: String?): Screen =
    nombre?.lowercase()?.let(PilaDeNavegacion::desdeTexto) ?: Screen.Inicio
