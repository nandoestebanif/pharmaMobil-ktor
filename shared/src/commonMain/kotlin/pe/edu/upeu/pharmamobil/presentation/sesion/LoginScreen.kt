package pe.edu.upeu.pharmamobil.presentation.sesion

import pe.edu.upeu.pharmamobil.presentation.components.LogoPharmaMobil
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.presentation.components.IconoEnRecuadro
import pe.edu.upeu.pharmamobil.presentation.components.MensajeError
import pe.edu.upeu.pharmamobil.presentation.components.Tarjeta
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField

/**
 * Inicio de sesion contra PharmaSoft. Solo pinta el estado y avisa las
 * acciones: el token nunca llega a esta pantalla.
 */
@Composable
fun LoginScreen(
    estado: SesionUiState,
    onUsuarioChange: (String) -> Unit,
    onClaveChange: (String) -> Unit,
    onIngresar: () -> Unit,
    onDescartarMensajes: () -> Unit,
    modifier: Modifier = Modifier
) {

    val esquema = MaterialTheme.colorScheme
    val temaOscuro = esquema.background.luminance() < 0.5f
    val degradado = if (temaOscuro) {
        listOf(esquema.primaryContainer, esquema.background)
    } else {
        listOf(esquema.primary, esquema.tertiary.copy(alpha = 0.85f), esquema.background)
    }

    var claveVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(degradado))
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(Modifier.height(24.dp))

            LogoPharmaMobil(lado = 96.dp, sombra = 6.dp)

            Text(
                text = "PharmaMobil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Ingresa con tu usuario de PharmaSoft",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Tarjeta(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    estado.aviso?.let { aviso -> Aviso(aviso) }

                    estado.error?.let { error ->
                        MensajeError(texto = error, onCerrar = onDescartarMensajes)
                    }

                    ValidatedTextField(
                        value = estado.usuario,
                        onValueChange = onUsuarioChange,
                        label = "Usuario",
                        error = estado.usuarioError,
                        leadingIcon = Icons.Default.Person,
                        habilitado = !estado.cargando,
                        imeAction = ImeAction.Next
                    )

                    ValidatedTextField(
                        value = estado.clave,
                        onValueChange = onClaveChange,
                        label = "Contraseña",
                        error = estado.claveError,
                        leadingIcon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        habilitado = !estado.cargando,
                        visualTransformation = if (claveVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { claveVisible = !claveVisible }) {
                                Icon(
                                    imageVector = if (claveVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (claveVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = { onIngresar() })
                    )

                    Button(
                        onClick = onIngresar,
                        enabled = !estado.cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (estado.cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Ingresando…")
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ingresar")
                        }
                    }
                }
            }

            Text(
                text = "La sesión dura una hora. Tu token se guarda cifrado en este dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}


/** Aviso informativo (no es un error): sesion expirada o cerrada. */
@Composable
private fun Aviso(texto: String) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
