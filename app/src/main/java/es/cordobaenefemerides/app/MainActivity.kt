package es.cordobaenefemerides.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoDia = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES"))

private val claro = lightColorScheme(
    primary = Color(0xFF7A1F2B), onPrimary = Color.White,
    primaryContainer = Color(0xFFF4E3C1), onPrimaryContainer = Color(0xFF3B0D14),
    secondary = Color(0xFF8A6D1E),
)
private val oscuro = darkColorScheme(
    primary = Color(0xFFFFB3B8), onPrimary = Color(0xFF55101C),
    primaryContainer = Color(0xFF5E1520), onPrimaryContainer = Color(0xFFF4E3C1),
    secondary = Color(0xFFE2C46F),
)

class MainActivity : ComponentActivity() {
    private val pedirPermiso =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AvisoDiario.crearCanal(this)
        AvisoDiario.programar(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val todas = Efemerides.todas(this)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) oscuro else claro) {
                Pantalla(todas)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pantalla(todas: List<Efemeride>) {
    val hoy = LocalDate.now()
    var diaTexto by rememberSaveable { mutableStateOf(hoy.toString()) }
    val dia = LocalDate.parse(diaTexto)
    val delDia = remember(dia, todas) { Efemerides.delDia(todas, dia) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Córdoba en Efemérides") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { diaTexto = dia.minusDays(1).toString() }) { Text("‹ Anterior") }
                Text(
                    dia.format(formatoDia),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = { diaTexto = dia.plusDays(1).toString() }) { Text("Siguiente ›") }
            }
            if (dia != hoy) {
                TextButton(
                    onClick = { diaTexto = hoy.toString() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Volver a hoy") }
            }

            if (delDia.isEmpty()) {
                Text(
                    "Todavía no hay efemérides guardadas para este día.",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(delDia) { TarjetaEfemeride(it) }
                }
            }
        }
    }
}

@Composable
fun TarjetaEfemeride(e: Efemeride) {
    val enlaces = LocalUriHandler.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                e.anio.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(e.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(e.texto, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (e.fuente.isNotBlank()) {
                    TextButton(onClick = { enlaces.openUri(e.fuente) }) { Text("Ver fuente") }
                }
                if (!e.verificada) {
                    AssistChip(onClick = { }, label = { Text("Pendiente de verificar") })
                }
            }
        }
    }
}
