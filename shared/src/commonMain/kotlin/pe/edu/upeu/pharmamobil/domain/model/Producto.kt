package pe.edu.upeu.pharmamobil.domain.model

import pe.edu.upeu.pharmamobil.domain.sync.PoliticaDeReintento

data class Producto(
    val id:Long,
    val nombre:String,
    val precio:Double,
    val stock: Int,
    // Sesion 8: categoria que le asigna el backend. Se conserva al editar;
    // null en un producto nuevo (el repositorio usa la categoria por defecto).
    val categoriaId: Long? = null,
    // false = dado de baja. PharmaSoft no borra productos: los desactiva para
    // que sigan en ventas y reportes. Un producto dado de baja se puede reactivar.
    val activo: Boolean = true,
    // Sesion 11: true mientras el producto solo existe en el dispositivo (se creo sin red)
    // y todavia no llega a PharmaSoft. Su id es negativo hasta que se sincroniza.
    val pendiente: Boolean = false,
    // Sesion 13: estado de envio de un pendiente. intentos llega a MAX_INTENTOS cuando el
    // envio fallo de forma permanente (o se agotaron los reintentos): la fila queda fallida.
    val intentos: Int = 0,
    val ultimoError: String? = null
) {


    init {
        require(nombre.isNotBlank()) {
            "El nombre del producto no puede estar vacio"
        }
        require(precio > 0 && precio.isFinite()) {
            "El precio debe ser un numero mayor que cero"
        }
        require(stock >= 0) {
            "El stock no puede ser negativo"
        }
    }

    /**
     * Un producto necesita reposicion cuando su stock cae por debajo del
     * minimo que la botica mantiene en gondola.
     */
    val requiereReposicion: Boolean
        get() = stock < STOCK_MINIMO

    /** Sesion 13: pendiente que ya no se reintenta solo; espera que el usuario lo revise. */
    val envioFallido: Boolean
        get() = pendiente && intentos >= PoliticaDeReintento.MAX_INTENTOS

    companion object {
        const val STOCK_MINIMO = 10
    }
}
