package pe.edu.upeu.pharmamobil.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PilaDeNavegacionTest {

    @Test
    fun arrancaEnInicioYSinPantallaALaQueVolver() {

        val pila = PilaDeNavegacion()

        assertEquals(Screen.Inicio, pila.actual)
        assertFalse(pila.puedeVolver)
        assertFalse(pila.volver())
    }

    @Test
    fun volverRegresaALaPantallaAnterior() {

        val pila = PilaDeNavegacion()

        pila.ir(Screen.Productos)
        pila.ir(Screen.DetalleProducto(3L))

        assertEquals(Screen.DetalleProducto(3L), pila.actual)
        assertTrue(pila.volver())
        assertEquals(Screen.Productos, pila.actual)
        assertTrue(pila.volver())
        assertEquals(Screen.Inicio, pila.actual)
    }

    @Test
    fun irALaPantallaQueYaSeVeNoLaDuplica() {

        val pila = PilaDeNavegacion()

        pila.ir(Screen.Productos)
        pila.ir(Screen.Productos)

        assertEquals(2, pila.profundidad)
    }

    @Test
    fun trasGuardarSeRegresaAlInventarioSaltandoElDetalle() {

        val pila = PilaDeNavegacion()
        pila.ir(Screen.Productos)
        pila.ir(Screen.DetalleProducto(3L))
        pila.ir(Screen.FormularioProducto(3L))

        pila.volverA(Screen.Productos)

        assertEquals(Screen.Productos, pila.actual)
        assertEquals(2, pila.profundidad)
    }

    @Test
    fun volverAUnaPantallaQueNoEstaEnLaPilaNoCambiaNada() {

        val pila = PilaDeNavegacion()
        pila.ir(Screen.Clientes)

        pila.volverA(Screen.Productos)

        assertEquals(Screen.Clientes, pila.actual)
    }

    @Test
    fun elMenuLateralDejaElModuloElegidoSobreInicio() {

        val pila = PilaDeNavegacion()
        pila.ir(Screen.Productos)
        pila.ir(Screen.DetalleProducto(3L))

        pila.irAModulo(Screen.Clientes)

        assertEquals(Screen.Clientes, pila.actual)
        assertTrue(pila.volver())
        assertEquals(Screen.Inicio, pila.actual)
        assertFalse(pila.puedeVolver)
    }

    @Test
    fun abrirDirectoEnUnModuloTambienPermiteVolverAInicio() {

        val pila = PilaDeNavegacion.desde(Screen.Productos)

        assertEquals(Screen.Productos, pila.actual)
        assertTrue(pila.puedeVolver)

        assertEquals(Screen.Inicio, PilaDeNavegacion.desde(Screen.Inicio).actual)
    }

    @Test
    fun cadaPantallaSeGuardaComoTextoYSeRecuperaIgual() {

        val pantallas = listOf(
            Screen.Inicio,
            Screen.Productos,
            Screen.Clientes,
            Screen.Pedidos,
            Screen.DetalleProducto(42L),
            Screen.FormularioProducto(),
            Screen.FormularioProducto(7L)
        )

        pantallas.forEach { pantalla ->
            assertEquals(pantalla, PilaDeNavegacion.desdeTexto(PilaDeNavegacion.aTexto(pantalla)))
        }
    }

    @Test
    fun unTextoDesconocidoSeDescartaAlRestaurar() {

        assertEquals(null, PilaDeNavegacion.desdeTexto("otra-cosa"))
        assertEquals(null, PilaDeNavegacion.desdeTexto("detalle:no-es-numero"))
    }

    @Test
    fun abreLaPantallaPedidaAlArrancar() {

        assertEquals(Screen.AcercaDe, pantallaPorNombre("acerca"))
        assertEquals(Screen.Productos, pantallaPorNombre("PRODUCTOS"))
        assertEquals(Screen.Inicio, pantallaPorNombre("no-existe"))
        assertEquals(Screen.Inicio, pantallaPorNombre(null))
    }

    @Test
    fun acercaDeSeGuardaYSeRestauraComoTexto() {

        val texto = PilaDeNavegacion.aTexto(Screen.AcercaDe)

        assertEquals(Screen.AcercaDe, PilaDeNavegacion.desdeTexto(texto))
    }
}
