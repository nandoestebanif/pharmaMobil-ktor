package pe.edu.upeu.pharmamobil.presentation.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.IconoEnRecuadro
import pe.edu.upeu.pharmamobil.presentation.components.MensajeExito
import pe.edu.upeu.pharmamobil.presentation.components.Tarjeta
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField

@Composable
fun ClienteScreen(
    viewModel: ClienteViewModel,
    modifier: Modifier = Modifier
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        FormularioClienteCard(
            formulario = uiState.formulario,
            registrando = uiState.registrando,
            onNombreChange = viewModel::onNombreChange,
            onCorreoChange = viewModel::onCorreoChange,
            onTelefonoChange = viewModel::onTelefonoChange,
            onRegistrar = viewModel::registrar
        )

        uiState.mensajeExito?.let {
            MensajeExito(it)
        }

        EncabezadoCartera(uiState.fase)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            when (val fase = uiState.fase) {

                ClienteUiState.Fase.Cargando ->
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        CircularProgressIndicator()

                        Text(
                            text = "Cargando clientes…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                ClienteUiState.Fase.SinClientes ->
                    EstadoVacio(
                        icono = Icons.Default.Group,
                        titulo = "Todavía no hay clientes",
                        descripcion = "Registra el primero con el formulario de arriba.",
                        modifier = Modifier.align(Alignment.Center)
                    )

                is ClienteUiState.Fase.ConClientes ->
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = fase.clientes,
                            key = { it.id }
                        ) { cliente ->
                            ClienteItem(cliente)
                        }
                    }

                is ClienteUiState.Fase.Error ->
                    EstadoVacio(
                        icono = Icons.Default.CloudOff,
                        titulo = "No pudimos cargar los clientes",
                        descripcion = fase.mensaje,
                        colorIcono = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                        accion = {
                            FilledTonalButton(onClick = viewModel::cargarClientes) {
                                Text("Reintentar")
                            }
                        }
                    )
            }
        }
    }
}


@Composable
private fun FormularioClienteCard(
    formulario: FormularioCliente,
    registrando: Boolean,
    onNombreChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onRegistrar: () -> Unit
) {

    Tarjeta(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Registrar cliente",
                style = MaterialTheme.typography.titleMedium
            )

            ValidatedTextField(
                value = formulario.nombre,
                onValueChange = onNombreChange,
                label = "Nombre",
                error = formulario.nombreError,
                leadingIcon = Icons.Default.Person,
                modifier = Modifier.fillMaxWidth()
            )

            ValidatedTextField(
                value = formulario.correo,
                onValueChange = onCorreoChange,
                label = "Correo",
                error = formulario.correoError,
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )

            ValidatedTextField(
                value = formulario.telefono,
                onValueChange = onTelefonoChange,
                label = "Teléfono",
                error = formulario.telefonoError,
                leadingIcon = Icons.Default.Phone,
                ayuda = "Opcional, entre 6 y 9 dígitos",
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onRegistrar,
                enabled = !registrando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (registrando) "Registrando…" else "Registrar")
            }
        }
    }
}


@Composable
private fun EncabezadoCartera(
    fase: ClienteUiState.Fase
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "Cartera",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        if (fase is ClienteUiState.Fase.ConClientes) {

            val cantidad = fase.clientes.size

            Text(
                text = if (cantidad == 1) "1 cliente" else "$cantidad clientes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ClienteItem(
    cliente: ClienteUi
) {

    Tarjeta(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            IconoEnRecuadro(
                icono = Icons.Default.Person,
                fondo = MaterialTheme.colorScheme.secondaryContainer,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = cliente.nombre,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "${cliente.correo}  ·  ${cliente.telefono}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
