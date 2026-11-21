package es.cordobaenefemerides.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

val ColoresClaros = lightColorScheme(
    primary = Color(0xFF7A1F2B), onPrimary = Color.White,
    primaryContainer = Color(0xFFF4E3C1), onPrimaryContainer = Color(0xFF3B0D14),
    secondary = Color(0xFF8A6D1E),
    secondaryContainer = Color(0xFFF1E2B8), onSecondaryContainer = Color(0xFF2B2000),
)
val ColoresOscuros = darkColorScheme(
    primary = Color(0xFFFFB3B8), onPrimary = Color(0xFF55101C),
    primaryContainer = Color(0xFF5E1520), onPrimaryContainer = Color(0xFFF4E3C1),
    secondary = Color(0xFFE2C46F),
    secondaryContainer = Color(0xFF4F3F12), onSecondaryContainer = Color(0xFFF4E3C1),
)

enum class Pestana(val nombre: String, val icono: ImageVector) {
    HOY("Hoy", Icons.Filled.Home),
    CALENDARIO("Calendario", Icons.Filled.DateRange),
    BUSCAR("Buscar", Icons.Filled.Search),
    FAVORITAS("Favoritas", Icons.Filled.Star),
    JUEGO("Juego", Icons.Filled.Face),
}

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
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) ColoresOscuros else ColoresClaros) {
                App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    pestanaInicial: Pestana = Pestana.HOY,
    diaInicial: LocalDate = LocalDate.now(),
    consultaInicial: String = "",
) {
    val contexto = LocalContext.current
    var todas by remember { mutableStateOf(Efemerides.todas(contexto)) }
    var favoritas by remember { mutableStateOf(Ajustes.favoritas(contexto)) }
    var escala by remember { mutableStateOf(Ajustes.escalaLetra(contexto)) }
    var pestana by rememberSaveable { mutableStateOf(pestanaInicial) }
    var diaTexto by rememberSaveable { mutableStateOf(diaInicial.toString()) }
    var eligiendoLetra by remember { mutableStateOf(false) }

    // Al abrir la app se descarga la última versión de las efemérides desde GitHub.
    LaunchedEffect(Unit) {
        val cambio = withContext(Dispatchers.IO) { Efemerides.actualizar(contexto) }
        if (cambio) {
            todas = Efemerides.todas(contexto)
            WidgetEfemeride.actualizarTodos(contexto)
        }
    }

    val abrirDia: (LocalDate) -> Unit = { diaTexto = it.toString(); pestana = Pestana.HOY }
    val cambiarFavorita: (Efemeride) -> Unit = { favoritas = Ajustes.cambiarFavorita(contexto, it.id) }

    val densidad = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(densidad.density, densidad.fontScale * escala)) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Córdoba en Efemérides") },
                    actions = {
                        TextButton(onClick = { eligiendoLetra = true }) {
                            Text("Aa", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
            bottomBar = {
                NavigationBar {
                    Pestana.entries.forEach { p ->
                        NavigationBarItem(
                            selected = pestana == p,
                            onClick = { pestana = p },
                            icon = { Icon(p.icono, contentDescription = null) },
                            label = { Text(p.nombre, maxLines = 1) },
                        )
                    }
                }
            },
        ) { relleno ->
            Box(Modifier.padding(relleno)) {
                when (pestana) {
                    Pestana.HOY -> PantallaDia(
                        todas, LocalDate.parse(diaTexto), favoritas,
                        onCambiarDia = { diaTexto = it.toString() }, onFavorita = cambiarFavorita,
                    )
                    Pestana.CALENDARIO -> PantallaCalendario(todas, onAbrirDia = abrirDia)
                    Pestana.BUSCAR -> PantallaBuscar(todas, favoritas, onAbrirDia = abrirDia, onFavorita = cambiarFavorita, consultaInicial = consultaInicial)
                    Pestana.FAVORITAS -> PantallaFavoritas(todas, favoritas, onAbrirDia = abrirDia, onFavorita = cambiarFavorita)
                    Pestana.JUEGO -> PantallaJuego(todas)
                }
            }
        }

        if (eligiendoLetra) {
            DialogoLetra(
                actual = escala,
                onElegir = { escala = it; Ajustes.cambiarEscalaLetra(contexto, it); eligiendoLetra = false },
                onCerrar = { eligiendoLetra = false },
            )
        }
    }
}

@Composable
fun DialogoLetra(actual: Float, onElegir: (Float) -> Unit, onCerrar: () -> Unit) {
    val opciones = listOf("Pequeña" to 0.9f, "Normal" to 1f, "Grande" to 1.2f, "Muy grande" to 1.4f)
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Tamaño de letra") },
        text = {
            androidx.compose.foundation.layout.Column {
                opciones.forEach { (nombre, valor) ->
                    TextButton(onClick = { onElegir(valor) }) {
                        Text((if (valor == actual) "● " else "○ ") + nombre)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}
