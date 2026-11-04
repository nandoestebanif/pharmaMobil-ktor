package pe.edu.upeu.pharmamobil.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Pila de pantallas de la app. La ultima es la que se ve; la flecha de la
 * barra superior y el boton Atras del sistema quitan la de arriba.
 *
 * Es estado observable de Compose (mutableStateListOf): al cambiar la pila,
 * la interfaz se vuelve a dibujar sola.
 */
class PilaDeNavegacion(inicial: List<Screen> = listOf(Screen.Inicio)) {

    private val pantallas = mutableStateListOf<Screen>().apply {
        addAll(inicial.ifEmpty { listOf(Screen.Inicio) })
    }

    /** Pantalla visible. */
    val actual: Screen
        get() = pantallas.last()

    /** true si hay una pantalla debajo a la que volver. */
    val puedeVolver: Boolean
        get() = pantallas.size > 1

    /** Cantidad de pantallas apiladas: sirve para saber si se avanza o se retrocede. */
    val profundidad: Int
        get() = pantallas.size

    /** Abre [pantalla] encima de la actual. Ir a la que ya se ve no la duplica. */
    fun ir(pantalla: Screen) {
        if (pantalla != actual) pantallas.add(pantalla)
    }

    /** Quita la pantalla de arriba. Devuelve false si ya se estaba en la raiz. */
    fun volver(): Boolean {
        if (!puedeVolver) return false
        pantallas.removeAt(pantallas.lastIndex)
        return true
    }

    /** Retrocede hasta [pantalla]. Si no esta en la pila, no hace nada. */
    fun volverA(pantalla: Screen) {
        val indice = pantallas.lastIndexOf(pantalla)
        if (indice < 0) return
        while (pantallas.lastIndex > indice) pantallas.removeAt(pantallas.lastIndex)
    }

    /**
     * Cambia de modulo desde el menu lateral: la pila queda en Inicio y,
     * encima, el modulo elegido. Asi la flecha siempre regresa a Inicio.
     */
    fun irAModulo(modulo: Screen) {
        pantallas.clear()
        pantallas.add(Screen.Inicio)
        if (modulo != Screen.Inicio) pantallas.add(modulo)
    }

    internal fun comoLista(): List<Screen> = pantallas.toList()

    companion object {

        /** La pila sobrevive a la rotacion: se guarda como una lista de textos. */
        val Saver: Saver<PilaDeNavegacion, Any> = listSaver(
            save = { pila -> pila.comoLista().map(::aTexto) },
            restore = { textos -> PilaDeNavegacion(textos.mapNotNull(::desdeTexto)) }
        )

        /** Pila con la que arranca la app si se pide abrir directo en un modulo. */
        fun desde(pantallaInicial: Screen): PilaDeNavegacion =
            PilaDeNavegacion().apply { irAModulo(pantallaInicial) }

        internal fun aTexto(pantalla: Screen): String = when (pantalla) {
            Screen.Inicio -> "inicio"
            Screen.Productos -> "productos"
            Screen.Clientes -> "clientes"
            Screen.Pedidos -> "pedidos"
            Screen.AcercaDe -> "acerca"
            is Screen.DetalleProducto -> "detalle:${pantalla.productoId}"
            is Screen.FormularioProducto -> "formulario:${pantalla.productoId ?: ""}"
        }

        internal fun desdeTexto(texto: String): Screen? {
            val clave = texto.substringBefore(':')
            val valor = texto.substringAfter(':', missingDelimiterValue = "")
            return when (clave) {
                "inicio" -> Screen.Inicio
                "productos" -> Screen.Productos
                "clientes" -> Screen.Clientes
                "pedidos" -> Screen.Pedidos
                "acerca" -> Screen.AcercaDe
                "detalle" -> valor.toLongOrNull()?.let { Screen.DetalleProducto(it) }
                "formulario" -> Screen.FormularioProducto(valor.toLongOrNull())
                else -> null
            }
        }
    }
}
