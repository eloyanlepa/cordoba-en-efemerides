package es.cordobaenefemerides.app

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale.forLanguageTag("es-ES")
private val formatoDia = DateTimeFormatter.ofPattern("d 'de' MMMM", ES)
private val formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", ES)
private val formatoCorto = DateTimeFormatter.ofPattern("d MMM", ES)

/** Fecha corta de una efeméride: «16 nov. 1830». */
fun fechaCorta(e: Efemeride): String =
    "${LocalDate.of(2024, e.mes, e.dia).format(formatoCorto)} ${e.anioTexto}"

/** Día (en el año actual) en que cae una efeméride; el 29 de febrero se lleva al 28 en años no bisiestos. */
fun diaDe(e: Efemeride): LocalDate {
    val anio = LocalDate.now().year
    return if (e.mes == 2 && e.dia == 29 && !java.time.Year.isLeap(anio.toLong())) LocalDate.of(anio, 2, 28)
    else LocalDate.of(anio, e.mes, e.dia)
}

// ───────────────────────── Hoy ─────────────────────────

@Composable
fun PantallaDia(
    todas: List<Efemeride>,
    dia: LocalDate,
    favoritas: Set<String>,
    onCambiarDia: (LocalDate) -> Unit,
    onFavorita: (Efemeride) -> Unit,
) {
    val hoy = LocalDate.now()
    val delDia = remember(dia, todas) { Efemerides.delDia(todas, dia) }
    val contexto = LocalContext.current
    var horaAviso by remember { mutableStateOf(AvisoDiario.horaElegida(contexto)) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { onCambiarDia(dia.minusDays(1)) }) { Text("‹ Anterior") }
            Text(dia.format(formatoDia), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { onCambiarDia(dia.plusDays(1)) }) { Text("Siguiente ›") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (dia != hoy) {
                TextButton(onClick = { onCambiarDia(hoy) }) { Text("Volver a hoy") }
            }
            TextButton(onClick = {
                val (h, m) = horaAviso
                TimePickerDialog(contexto, { _, nuevaH, nuevaM ->
                    AvisoDiario.cambiarHora(contexto, nuevaH, nuevaM)
                    horaAviso = nuevaH to nuevaM
                }, h, m, true).show()
            }) {
                Text("🔔 Aviso a las %02d:%02d".format(horaAviso.first, horaAviso.second))
            }
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
                items(delDia) { TarjetaEfemeride(it, it.id in favoritas, onFavorita) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaEfemeride(e: Efemeride, favorita: Boolean, onFavorita: (Efemeride) -> Unit) {
    val enlaces = LocalUriHandler.current
    val contexto = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.anioTexto,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                BotonFavorita(favorita) { onFavorita(e) }
            }
            if (e.temas.isNotEmpty()) {
                Text(
                    e.temas.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (e.titulo.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(e.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(e.texto, style = MaterialTheme.typography.bodyMedium)
            if (e.extra.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    e.extra,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(verticalArrangement = Arrangement.Center) {
                TextButton(onClick = { Compartir.compartir(contexto, e) }) { Text("Compartir") }
                if (e.video.isNotBlank()) {
                    TextButton(onClick = { enlaces.openUri(e.video) }) { Text("▶ Ver vídeo") }
                }
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

@Composable
fun BotonFavorita(favorita: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = if (favorita) "Quitar de favoritas" else "Añadir a favoritas"
        },
    ) {
        Text(
            if (favorita) "★" else "☆",
            style = MaterialTheme.typography.headlineSmall,
            color = if (favorita) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        )
    }
}

/** Fila compacta para listas (búsqueda y favoritas): al tocarla se abre ese día. */
@Composable
fun FilaEfemeride(e: Efemeride, favorita: Boolean, onAbrir: () -> Unit, onFavorita: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onAbrir),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    fechaCorta(e),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    e.titulo.ifBlank { Texto.quitarFecha(e.texto) },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BotonFavorita(favorita, onFavorita)
        }
    }
}

// ───────────────────────── Calendario ─────────────────────────

@Composable
fun PantallaCalendario(todas: List<Efemeride>, onAbrirDia: (LocalDate) -> Unit) {
    var mesTexto by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val mes = YearMonth.parse(mesTexto)
    val hoy = LocalDate.now()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { mesTexto = mes.minusMonths(1).toString() }) { Text("‹") }
            Text(
                mes.format(formatoMes).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { mesTexto = mes.plusMonths(1).toString() }) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val hueco = mes.atDay(1).dayOfWeek.value - 1   // lunes = 0
        val celdas = hueco + mes.lengthOfMonth()
        val semanas = (celdas + 6) / 7
        for (s in 0 until semanas) {
            Row(Modifier.fillMaxWidth()) {
                for (d in 0 until 7) {
                    val n = s * 7 + d - hueco + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (n in 1..mes.lengthOfMonth()) {
                            val fecha = mes.atDay(n)
                            val lista = Efemerides.delDia(todas, fecha)
                            CeldaDia(n, lista.size, lista.any { it.video.isNotBlank() }, fecha == hoy) {
                                onAbrirDia(fecha)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Los días en color tienen efemérides (el número pequeño indica cuántas). ▶ = tiene vídeo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CeldaDia(n: Int, cuantas: Int, video: Boolean, esHoy: Boolean, onClick: () -> Unit) {
    val fondo = when {
        esHoy -> MaterialTheme.colorScheme.primary
        cuantas > 0 -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val texto = when {
        esHoy -> MaterialTheme.colorScheme.onPrimary
        cuantas > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(fondo).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$n", color = texto, fontWeight = if (cuantas > 0) FontWeight.Bold else FontWeight.Normal)
            if (cuantas > 0) {
                Text(
                    (if (video) "▶ " else "") + "$cuantas",
                    color = texto,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

// ───────────────────────── Buscar ─────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaBuscar(
    todas: List<Efemeride>,
    favoritas: Set<String>,
    onAbrirDia: (LocalDate) -> Unit,
    onFavorita: (Efemeride) -> Unit,
    consultaInicial: String = "",
) {
    var consulta by rememberSaveable { mutableStateOf(consultaInicial) }
    var tema by rememberSaveable { mutableStateOf<String?>(null) }
    val temas = remember(todas) { Efemerides.temas(todas) }
    val resultados = remember(consulta, tema, todas) {
        if (consulta.isBlank() && tema == null) emptyList() else Efemerides.buscar(todas, consulta, tema)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OutlinedTextField(
                value = consulta,
                onValueChange = { consulta = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Góngora, Mezquita, San Lorenzo…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
        }
        item {
            Text("Temas", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                temas.forEach { t ->
                    FilterChip(
                        selected = tema == t,
                        onClick = { tema = if (tema == t) null else t },
                        label = { Text(t) },
                    )
                }
            }
        }
        item {
            Text(
                when {
                    consulta.isBlank() && tema == null -> "Escribe algo o elige un tema para buscar entre ${todas.size} efemérides."
                    resultados.isEmpty() -> "No hay efemérides que coincidan."
                    resultados.size == 1 -> "1 efeméride"
                    else -> "${resultados.size} efemérides"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(resultados, key = { it.id }) { e ->
            FilaEfemeride(e, e.id in favoritas, onAbrir = { onAbrirDia(diaDe(e)) }, onFavorita = { onFavorita(e) })
        }
    }
}

// ───────────────────────── Favoritas ─────────────────────────

@Composable
fun PantallaFavoritas(
    todas: List<Efemeride>,
    favoritas: Set<String>,
    onAbrirDia: (LocalDate) -> Unit,
    onFavorita: (Efemeride) -> Unit,
) {
    val lista = remember(todas, favoritas) {
        todas.filter { it.id in favoritas }.sortedWith(compareBy({ it.fecha }, { it.anio }))
    }
    if (lista.isEmpty()) {
        Text(
            "Aún no tienes favoritas.\n\nToca la estrella ☆ de cualquier efeméride para guardarla aquí.",
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(lista, key = { it.id }) { e ->
            FilaEfemeride(e, true, onAbrir = { onAbrirDia(diaDe(e)) }, onFavorita = { onFavorita(e) })
        }
    }
}

// ───────────────────────── Juego ─────────────────────────

@Composable
fun PantallaJuego(todas: List<Efemeride>) {
    val contexto = LocalContext.current
    var pregunta by remember(todas) { mutableStateOf(Juego.nuevaPregunta(todas)) }
    var respuesta by remember { mutableStateOf<Int?>(null) }
    var aciertos by rememberSaveable { mutableIntStateOf(0) }
    var jugadas by rememberSaveable { mutableIntStateOf(0) }
    var racha by rememberSaveable { mutableIntStateOf(0) }
    var record by remember { mutableIntStateOf(Ajustes.recordJuego(contexto)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Aciertos: $aciertos de $jugadas", style = MaterialTheme.typography.labelLarge)
            Text("Racha: $racha · Récord: $record", style = MaterialTheme.typography.labelLarge)
        }
        Text(
            "¿En qué año ocurrió?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(Modifier.fillMaxWidth()) {
            Text(pregunta.enunciado, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
        pregunta.opciones.chunked(2).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                fila.forEach { opcion ->
                    val elegida = respuesta
                    val colores = when {
                        elegida == null -> ButtonDefaults.buttonColors()
                        opcion == pregunta.correcta -> ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                            disabledContainerColor = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                            disabledContentColor = androidx.compose.ui.graphics.Color.White,
                        )
                        opcion == elegida -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            disabledContainerColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.onError,
                        )
                        else -> ButtonDefaults.buttonColors()
                    }
                    Button(
                        onClick = {
                            respuesta = opcion
                            jugadas++
                            if (opcion == pregunta.correcta) {
                                aciertos++; racha++
                                Ajustes.guardarRecordJuego(contexto, racha)
                                record = Ajustes.recordJuego(contexto)
                            } else {
                                racha = 0
                            }
                        },
                        enabled = respuesta == null,
                        colors = colores,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Text("$opcion", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        val elegida = respuesta
        if (elegida != null) {
            val e = pregunta.efemeride
            Text(
                (if (elegida == pregunta.correcta) "¡Correcto! 🎉 " else "¡Casi! ") +
                    "Fue el ${LocalDate.of(2024, e.mes, e.dia).format(formatoDia)} de ${e.anioTexto}.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { pregunta = Juego.nuevaPregunta(todas); respuesta = null }) {
                    Text("Siguiente pregunta")
                }
                OutlinedButton(onClick = {
                    Compartir.compartirTexto(
                        contexto,
                        "He acertado $aciertos de $jugadas preguntas sobre la historia de Córdoba " +
                            "(mi récord: $record seguidas). ¿Te atreves? #CórdobaEnEfemérides",
                    )
                }) { Text("Compartir") }
            }
        }
        Spacer(Modifier.width(1.dp))
    }
}
