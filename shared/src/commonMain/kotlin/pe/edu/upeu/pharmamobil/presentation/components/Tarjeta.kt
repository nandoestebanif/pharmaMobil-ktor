package pe.edu.upeu.pharmamobil.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tarjeta de la app: fondo claro, borde fino y esquinas amplias. Se usa en
 * todas las pantallas para que el contenido se vea igual en cualquiera.
 * Con [onClick] la tarjeta completa se puede tocar.
 */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    habilitada: Boolean = true,
    contenido: @Composable ColumnScope.() -> Unit
) {

    val esquema = MaterialTheme.colorScheme
    val temaOscuro = esquema.background.luminance() < 0.5f

    val forma = RoundedCornerShape(20.dp)
    val fondo = if (temaOscuro) esquema.surfaceContainerHigh else esquema.surfaceContainerLowest
    val colores = CardDefaults.cardColors(
        containerColor = fondo,
        // Una tarjeta deshabilitada no se atenua: solo deja de responder al toque.
        disabledContainerColor = fondo,
        disabledContentColor = esquema.onSurface
    )
    val borde = BorderStroke(1.dp, esquema.outlineVariant.copy(alpha = 0.6f))

    if (onClick != null) {
        Card(
            onClick = onClick,
            enabled = habilitada,
            modifier = modifier,
            shape = forma,
            colors = colores,
            border = borde,
            content = contenido
        )
    } else {
        Card(
            modifier = modifier,
            shape = forma,
            colors = colores,
            border = borde,
            content = contenido
        )
    }
}

/** Icono dentro de un recuadro de color: identifica cada fila o modulo. */
@Composable
fun IconoEnRecuadro(
    icono: ImageVector,
    modifier: Modifier = Modifier,
    fondo: Color = MaterialTheme.colorScheme.primaryContainer,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    lado: Dp = 44.dp
) {

    Surface(
        modifier = modifier.size(lado),
        shape = RoundedCornerShape(lado * 0.32f),
        color = fondo,
        contentColor = color
    ) {

        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                modifier = Modifier.size(lado * 0.5f)
            )
        }
    }
}

/** Etiqueta corta de estado (por ejemplo "Reponer" o "Activo"). */
@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
    fondo: Color = MaterialTheme.colorScheme.secondaryContainer,
    color: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = fondo,
        contentColor = color
    ) {

        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
