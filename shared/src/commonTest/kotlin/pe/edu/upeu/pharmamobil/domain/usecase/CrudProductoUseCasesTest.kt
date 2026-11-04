package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Casos de uso que la sesion 8 agrega al CRUD: obtener, actualizar y eliminar. */
class CrudProductoUseCasesTest {

    private val paracetamol = Producto(id = 1L, nombre = "Paracetamol", precio = 12.5, stock = 5, categoriaId = 2)

    private fun repositorioConUno() = FakeProductoRepository(mutableListOf(paracetamol))

    @Test
    fun obtenerDevuelveElProductoPorSuId() = runTest {

        val resultado = ObtenerProductoUseCase(repositorioConUno()).invoke(1L)

        assertEquals(paracetamol, resultado.getOrThrow())
    }

    @Test
    fun obtenerUnIdInexistenteDevuelveFailureNoEncontrado() = runTest {

        val fallo = ObtenerProductoUseCase(repositorioConUno()).invoke(99L).exceptionOrNull()

        assertEquals(ErrorApi.NoEncontrado, assertIs<ErrorApiException>(fallo).error)
    }

    @Test
    fun actualizarCambiaLosDatosYConservaIdYCategoria() = runTest {

        val repositorio = repositorioConUno()

        val actualizado = ActualizarProductoUseCase(repositorio)
            .invoke(paracetamol, " Paracetamol 500mg ", "15.00", "20")
            .getOrThrow()

        assertEquals(Producto(1L, "Paracetamol 500mg", 15.0, 20, categoriaId = 2), actualizado)
        assertEquals(actualizado, repositorio.enServidor.single())
    }

    @Test
    fun actualizarConDatosInvalidosNoLlegaAlRepositorio() = runTest {

        val repositorio = repositorioConUno()

        val fallo = ActualizarProductoUseCase(repositorio)
            .invoke(paracetamol, "", "abc", "-1")
            .exceptionOrNull()

        val errores = assertIs<ProductoInvalidoException>(fallo).errores
        assertEquals("El nombre es obligatorio", errores.nombre)
        assertEquals("El precio debe ser un número válido", errores.precio)
        assertEquals("El stock no puede ser negativo", errores.stock)
        assertEquals(paracetamol, repositorio.enServidor.single())
    }

    @Test
    fun reactivarVuelveAActivarConservandoSusDatos() = runTest {

        val dadoDeBaja = paracetamol.copy(activo = false)
        val repositorio = FakeProductoRepository(mutableListOf(dadoDeBaja))

        val reactivado = ReactivarProductoUseCase(repositorio).invoke(1L).getOrThrow()

        assertEquals(paracetamol, reactivado)
        assertTrue(repositorio.enServidor.single().activo)
    }

    @Test
    fun eliminarQuitaElProductoDelInventario() = runTest {

        val repositorio = repositorioConUno()

        val resultado = EliminarProductoUseCase(repositorio).invoke(1L)

        assertTrue(resultado.isSuccess)
        assertTrue(repositorio.enServidor.isEmpty())
    }

    @Test
    fun eliminarDevuelveFailureCuandoElServidorFalla() = runTest {

        val repositorio = repositorioConUno().apply {
            fallaAlEliminar = ErrorApiException(ErrorApi.Servidor)
        }

        val fallo = EliminarProductoUseCase(repositorio).invoke(1L).exceptionOrNull()

        assertEquals(ErrorApi.Servidor, assertIs<ErrorApiException>(fallo).error)
        assertEquals(1, repositorio.enServidor.size)
    }
}
