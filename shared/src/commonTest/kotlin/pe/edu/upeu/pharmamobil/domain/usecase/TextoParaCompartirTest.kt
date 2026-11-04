package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles
import kotlin.test.Test
import kotlin.test.assertEquals

class TextoParaCompartirTest {

    @Test
    fun incluyeNombrePrecioFormateadoYStock() {

        val producto = Producto(id = 3L, nombre = "Naproxeno 550mg", precio = 7.8, stock = 6)

        assertEquals(
            "Naproxeno 550mg — ${formatearSoles(7.8)} · Stock: 6",
            producto.comoTextoParaCompartir()
        )
    }
}
