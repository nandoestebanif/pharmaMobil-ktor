package pe.edu.upeu.pharmamobil

import pe.edu.upeu.pharmamobil.presentation.components.LogoPharmaMobil
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.presentation.permiso.TarjetaDeAvisos

import pe.edu.upeu.pharmamobil.domain.model.Permiso
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.navigation.PilaDeNavegacion
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.platform.AlPulsarAtras
import pe.edu.upeu.pharmamobil.presentation.acerca.AcercaDeScreen
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteScreen
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.IndicadorCarga
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoScreen
import pe.edu.upeu.pharmamobil.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobil.presentation.producto.FormularioProductoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.sesion.LoginScreen
import pe.edu.upeu.pharmamobil.presentation.sesion.SesionViewModel
import pe.edu.upeu.pharmamobil.theme.PharmaMobilTheme

/** Modulos del menu lateral. */
private data class Destino(
    val screen: Screen,
    val titulo: String,
    val icono: ImageVector,
    /** Sesion 10: permiso que exige el modulo; null si lo ve cualquier usuario. */
    val permiso: String? = null
)

private val DESTINOS = listOf(
    Destino(Screen.Inicio, "Inicio", Icons.Default.Home),
    Destino(Screen.Productos, "Productos", Icons.Default.Medication, Permiso.CATALOGO_LEER),
    Destino(Screen.Clientes, "Clientes", Icons.Default.Person, Permiso.CLIENTES_LEER),
    Destino(Screen.Pedidos, "Pedidos", Icons.Default.ShoppingCart, Permiso.VENTAS_LEER),
    Destino(Screen.AcercaDe, "Acerca de", Icons.Default.Info)
)

/** Permiso que exige cada pantalla (las de productos heredan el del modulo). */
private fun Screen.permisoRequerido(): String? =
    DESTINOS.firstOrNull { it.screen == moduloDe(this) }?.permiso

private fun moduloDe(pantalla: Screen): Screen = when (pantalla) {
    is Screen.DetalleProducto, is Screen.FormularioProducto -> Screen.Productos
    else -> pantalla
}

private const val DURACION_TRANSICION_MS = 220

/**
 * Sesion 10: sin sesion se muestra el login; con sesion, la app. El 401 en
 * cualquier pantalla devuelve al login (lo decide SesionViewModel).
 *
 * @param mostrarAccionesSinPermiso solo para provocar un 403 a proposito
 * (evidencia de la practica): muestra las acciones aunque el rol no las tenga.
 * @param pantallaInicial pantalla con la que abre la app. Por defecto Inicio;
 * se puede cambiar al arrancar: `-pantalla productos` en iOS y el extra
 * «pantalla» del Intent en Android (lo usa el workflow de GitHub Actions para
 * capturar evidencias en el simulador y en el emulador).
 */
@Composable
fun App(
    pantallaInicial: Screen = Screen.Inicio,
    mostrarAccionesSinPermiso: Boolean = false
) = KoinContext {

    val sesionViewModel: SesionViewModel = koinViewModel()
    val estadoSesion by sesionViewModel.uiState.collectAsStateWithLifecycle()

    var darkTheme by rememberSaveable {
        mutableStateOf(false)
    }

    PharmaMobilTheme(darkTheme = darkTheme) {

        val sesion = estadoSesion.sesion

        when {
            estadoSesion.verificando ->
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        IndicadorCarga(texto = "Comprobando tu sesión…")
                    }
                }

            sesion == null ->
                LoginScreen(
                    estado = estadoSesion,
                    onUsuarioChange = sesionViewModel::onUsuarioChange,
                    onClaveChange = sesionViewModel::onClaveChange,
                    onIngresar = sesionViewModel::iniciarSesion,
                    onDescartarMensajes = sesionViewModel::descartarMensajes
                )

            // Cada sesion arranca con su propia pila y sus propios ViewModel.
            else -> key(sesion.token) {
                AppConSesion(
                    sesion = sesion,
                    pantallaInicial = pantallaInicial,
                    mostrarAccionesSinPermiso = mostrarAccionesSinPermiso,
                    darkTheme = darkTheme,
                    onCambiarTema = { darkTheme = it },
                    onCerrarSesion = sesionViewModel::cerrarSesion
                )
            }
        }
    }
}


@Composable
private fun AppConSesion(
    sesion: Sesion,
    pantallaInicial: Screen,
    mostrarAccionesSinPermiso: Boolean,
    darkTheme: Boolean,
    onCambiarTema: (Boolean) -> Unit,
    onCerrarSesion: () -> Unit
) {

    // La interfaz solo ofrece lo que el rol puede hacer; quien autoriza es el servidor.
    val puede: (String) -> Boolean = { permiso -> mostrarAccionesSinPermiso || sesion.puede(permiso) }
    val puedeVer: (Screen) -> Boolean = { pantalla -> pantalla.permisoRequerido()?.let(puede) ?: true }
    val puedeEscribirCatalogo = puede(Permiso.CATALOGO_ESCRIBIR)

    // Los ViewModel se guardan en la Activity: con una clave por sesion, un
    // usuario nuevo no hereda la lista ni los avisos del anterior.
    val claveDeSesion = sesion.token.hashCode().toString()

    // Si la pantalla pedida al arrancar no corresponde al rol, se abre Inicio.
    val inicio = if (puedeVer(pantallaInicial)) pantallaInicial else Screen.Inicio

    // Pila de pantallas: la flecha de la barra y el boton Atras quitan la de arriba.
    val pila = rememberSaveable(saver = PilaDeNavegacion.Saver) {
        PilaDeNavegacion.desde(inicio)
    }

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    // El ViewModel de productos se pide solo dentro de su modulo: asi la
    // portada no dispara la carga del inventario.
    val productoViewModel: ProductoViewModel? =
        if (pila.actual.esDelModuloProductos()) koinViewModel(key = "productos-$claveDeSesion") else null

    fun volver() {
        val saliendoDe = pila.actual
        if (pila.volver() && saliendoDe is Screen.FormularioProducto) {
            // Se sale sin guardar: el formulario y sus avisos no deben quedar para despues.
            productoViewModel?.cancelarEdicion()
        }
    }

    // Boton Atras del sistema: cierra el menu o vuelve a la pantalla anterior.
    AlPulsarAtras(habilitado = drawerState.isOpen || pila.puedeVolver) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            volver()
        }
    }

    // Avanzar desliza hacia la izquierda; volver, hacia la derecha.
    var profundidadAnterior by remember { mutableIntStateOf(pila.profundidad) }
    val avanza = pila.profundidad >= profundidadAnterior
    SideEffect { profundidadAnterior = pila.profundidad }

    Box(modifier = Modifier.fillMaxSize()) {

        ModalNavigationDrawer(

            drawerState = drawerState,

            // Dentro de un modulo el borde izquierdo queda para el gesto de volver.
            gesturesEnabled = drawerState.isOpen || !pila.puedeVolver,

            drawerContent = {

                ModalDrawerSheet {

                    DrawerHeader(sesion)

                    HorizontalDivider()

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    DESTINOS.filter { destino -> destino.permiso?.let(puede) ?: true }.forEach { destino ->

                        NavigationDrawerItem(
                            label = {
                                Text(destino.titulo)
                            },
                            selected = pila.moduloActual() == destino.screen,
                            onClick = {

                                pila.irAModulo(destino.screen)

                                scope.launch {
                                    drawerState.close()
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destino.icono,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.padding(
                                NavigationDrawerItemDefaults.ItemPadding
                            )
                        )
                    }

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    HorizontalDivider()

                    ModoOscuro(
                        activo = darkTheme,
                        onCambiar = onCambiarTema
                    )

                    NavigationDrawerItem(
                        label = { Text("Cerrar sesión") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onCerrarSesion()
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        ) {

            Scaffold(

                topBar = {

                    TopAppBar(

                        title = {
                            Text(
                                text = tituloDe(pila.actual),
                                fontWeight = FontWeight.SemiBold
                            )
                        },

                        navigationIcon = {

                            if (pila.puedeVolver) {

                                IconButton(onClick = ::volver) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Volver"
                                    )
                                }

                            } else {

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            drawerState.open()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Abrir menú"
                                    )
                                }
                            }
                        },

                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

            ) { paddingValues ->

                AnimatedContent(
                    targetState = pila.actual,
                    transitionSpec = {
                        val sentido = if (avanza) 1 else -1
                        (slideInHorizontally(tween(DURACION_TRANSICION_MS)) { sentido * it / 5 } +
                            fadeIn(tween(DURACION_TRANSICION_MS))) togetherWith
                            (slideOutHorizontally(tween(DURACION_TRANSICION_MS)) { -sentido * it / 5 } +
                                fadeOut(tween(DURACION_TRANSICION_MS / 2)))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) { pantalla ->

                    Box(modifier = Modifier.fillMaxSize()) {

                        when (pantalla) {

                            Screen.Inicio ->
                                InicioScreen(
                                    onNavegar = { destino ->
                                        pila.ir(destino)
                                    },
                                    nombreUsuario = sesion.nombreCompleto,
                                    rol = sesion.roles.firstOrNull()?.let(::nombreDelRol),
                                    puedeVer = puedeVer,
                                    avisos = { TarjetaDeAvisos(koinViewModel()) }
                                )

                            Screen.Productos -> {
                                val viewModel: ProductoViewModel = koinViewModel(key = "productos-$claveDeSesion")
                                ProductoScreen(
                                    puedeEscribir = puedeEscribirCatalogo,
                                    viewModel = viewModel,
                                    onNuevo = {
                                        viewModel.prepararNuevo()
                                        pila.ir(Screen.FormularioProducto())
                                    },
                                    onVerDetalle = { id ->
                                        pila.ir(Screen.DetalleProducto(id))
                                    }
                                )
                            }

                            is Screen.DetalleProducto -> {
                                val viewModel: ProductoViewModel = koinViewModel(key = "productos-$claveDeSesion")
                                DetalleProductoScreen(
                                    productoId = pantalla.productoId,
                                    viewModel = koinViewModel(key = "detalle-$claveDeSesion"),
                                    puedeEscribir = puedeEscribirCatalogo,
                                    onEditar = {
                                        viewModel.editar(pantalla.productoId)
                                        pila.ir(Screen.FormularioProducto(pantalla.productoId))
                                    },
                                    onEliminar = {
                                        // El inventario muestra el progreso y el resultado.
                                        pila.volverA(Screen.Productos)
                                        viewModel.eliminar(pantalla.productoId)
                                    }
                                )
                            }

                            is Screen.FormularioProducto ->
                                FormularioProductoScreen(
                                    viewModel = koinViewModel(key = "productos-$claveDeSesion"),
                                    productoId = pantalla.productoId,
                                    activa = pantalla == pila.actual,
                                    onGuardado = {
                                        // Tras guardar se regresa al inventario, ya actualizado.
                                        pila.volverA(Screen.Productos)
                                    },
                                    onCerrar = ::volver
                                )

                            Screen.Clientes ->
                                ClienteScreen(
                                    viewModel = koinViewModel(key = "clientes-$claveDeSesion")
                                )

                            Screen.Pedidos ->
                                EstadoVacio(
                                    icono = Icons.Default.ShoppingCart,
                                    titulo = "Pedidos en construcción",
                                    descripcion = "Este módulo llega en una próxima sesión del curso.",
                                    modifier = Modifier.align(Alignment.Center)
                                )

                            Screen.AcercaDe -> {
                                // Sesion 13: el boton de diagnostico no sabe que detras esta Firebase.
                                val telemetria = koinInject<Telemetria>()
                                AcercaDeScreen(onProbarTelemetria = {
                                    telemetria.registrarError(
                                        IllegalStateException("Error de prueba de PharmaMobil (sesion 13)"),
                                        mapOf("pantalla" to "acerca", "operacion" to "prueba")
                                    )
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}


/** Las tres pantallas que comparten ProductoViewModel. */
private fun Screen.esDelModuloProductos(): Boolean =
    this is Screen.Productos || this is Screen.DetalleProducto || this is Screen.FormularioProducto

/** Modulo al que pertenece la pantalla visible: es el que se marca en el menu lateral. */
private fun PilaDeNavegacion.moduloActual(): Screen = when (val pantalla = actual) {
    is Screen.DetalleProducto, is Screen.FormularioProducto -> Screen.Productos
    else -> pantalla
}


@Composable
private fun DrawerHeader(sesion: Sesion) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        LogoPharmaMobil(lado = 52.dp, sombra = 2.dp)

        Column {

            Text(
                text = sesion.nombreCompleto,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "@${sesion.usuario} · ${sesion.roles.firstOrNull()?.let(::nombreDelRol) ?: "Sin rol"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ModoOscuro(
    activo: Boolean,
    onCambiar: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 28.dp,
                vertical = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Icon(
            imageVector = Icons.Default.DarkMode,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Modo oscuro",
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = activo,
            onCheckedChange = onCambiar
        )
    }
}


private fun tituloDe(
    screen: Screen
): String = when (screen) {
    Screen.Inicio -> "PharmaMobil"
    Screen.Productos -> "Productos"
    Screen.Clientes -> "Clientes"
    Screen.Pedidos -> "Pedidos"
    Screen.AcercaDe -> "Acerca de"
    is Screen.DetalleProducto -> "Detalle del producto"
    is Screen.FormularioProducto ->
        if (screen.productoId == null) "Nuevo producto" else "Editar producto"
}


/** Nombre legible de los roles de PharmaSoft. */
private fun nombreDelRol(rol: String): String = when (rol) {
    "ADMIN" -> "Administrador"
    "VENDEDOR" -> "Vendedor"
    "ALMACENERO" -> "Almacenero"
    else -> rol.lowercase().replaceFirstChar { it.uppercase() }
}
