package es.cordobaenefemerides.app

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.time.LocalDate

data class Efemeride(
    val fecha: String,      // "MM-DD"
    val anio: Int,          // negativo = antes de Cristo
    val titulo: String,     // puede estar vacío
    val texto: String,
    val fuente: String,
    val verificada: Boolean,
    val extra: String = "",
    val video: String = "",   // enlace al vídeo de Instagram, si lo hay
    val temas: List<String> = emptyList(),
) {
    /** El año tal como se muestra: «1236» o «45 a. C.». */
    val anioTexto: String get() = if (anio < 0) "${-anio} a. C." else anio.toString()

    /** Frase corta para el aviso: el título, o el comienzo del texto si no hay título. */
    val resumen: String get() = titulo.ifBlank { texto }

    val mes: Int get() = fecha.substring(0, 2).toInt()
    val dia: Int get() = fecha.substring(3, 5).toInt()

    /** Identificador para guardar favoritas. */
    val id: String get() = "$fecha|$anio|${texto.take(60).hashCode()}"

    /** Texto para compartir en redes sociales. */
    val textoParaCompartir: String get() = buildString {
        if (titulo.isNotBlank()) append(titulo).append("\n\n")
        append(texto)
        if (video.isNotBlank()) append("\n\n▶ ").append(video)
        append("\n\n#CórdobaEnEfemérides")
    }
}

object Texto {
    private val MARCAS = Regex("\\p{Mn}+")
    private val FECHA_INICIAL = Regex(
        "^\\s*(en la noche del\\s+)?\\d{1,2}\\s*(de\\s+)?[a-záéíóú]+\\s+(d+el?\\s+|d+e\\s+)?\\d{1,4}\\s*(a\\.\\s*C\\.)?\\s*,?\\s*",
        RegexOption.IGNORE_CASE,
    )

    /** Minúsculas y sin tildes, para buscar sin preocuparse de acentos. */
    fun normalizar(s: String): String =
        MARCAS.replace(Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD), "")

    /** Quita la fecha del comienzo: «16 de noviembre de 1830, fallece…» → «Fallece…». */
    fun quitarFecha(t: String): String =
        t.replaceFirst(FECHA_INICIAL, "").replaceFirstChar { it.uppercase() }
}

object Efemerides {
    /** Archivo de efemérides publicado en GitHub; la app lo descarga para estar al día sin reinstalarse. */
    const val URL_DATOS =
        "https://raw.githubusercontent.com/eloyanlepa/cordoba-en-efemerides/main/data/efemerides.json"
    private const val DESCARGADO = "efemerides-descargadas.json"

    @Volatile private var cache: List<Efemeride>? = null

    /** Todas las efemérides: las descargadas si son más recientes que la app, o las incluidas en ella. */
    fun todas(context: Context): List<Efemeride> =
        cache ?: cargar(context).also { cache = it }

    /**
     * Las efemérides de un día concreto, de la más antigua a la más reciente.
     * En los años no bisiestos, las del 29 de febrero se muestran el 28.
     */
    fun delDia(lista: List<Efemeride>, dia: LocalDate): List<Efemeride> {
        val claves = mutableSetOf("%02d-%02d".format(dia.monthValue, dia.dayOfMonth))
        if (dia.monthValue == 2 && dia.dayOfMonth == 28 && !dia.isLeapYear) claves.add("02-29")
        return lista.filter { it.fecha in claves }.sortedBy { it.anio }
    }

    /**
     * Busca efemérides que contengan todas las palabras de [consulta] (sin importar tildes ni
     * mayúsculas) y, si se indica, que tengan el [tema]. Ordenadas por fecha del año.
     */
    fun buscar(lista: List<Efemeride>, consulta: String, tema: String? = null): List<Efemeride> {
        val palabras = Texto.normalizar(consulta).split(Regex("\\s+")).filter { it.isNotBlank() }
        return lista.filter { e ->
            (tema == null || tema in e.temas) &&
                Texto.normalizar(e.titulo + " " + e.texto + " " + e.extra).let { t -> palabras.all { it in t } }
        }.sortedWith(compareBy({ it.fecha }, { it.anio }))
    }

    /** Temas que aparecen, del más frecuente al menos. */
    fun temas(lista: List<Efemeride>): List<String> =
        lista.flatMap { it.temas }.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.map { it.key }

    fun parsear(json: String): List<Efemeride> {
        val array = JSONArray(json)
        return List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val temas = o.optJSONArray("temas")
            Efemeride(
                fecha = o.getString("fecha"),
                anio = o.getInt("anio"),
                titulo = o.optString("titulo"),
                texto = o.getString("texto"),
                fuente = o.optString("fuente"),
                verificada = o.optBoolean("verificada", false),
                extra = o.optString("extra"),
                video = o.optString("video"),
                temas = if (temas == null) emptyList() else List(temas.length()) { temas.getString(it) },
            )
        }
    }

    private fun cargar(context: Context): List<Efemeride> {
        val descargado = File(context.filesDir, DESCARGADO)
        val instalada = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        }.getOrDefault(0L)
        // Si la app se ha actualizado después de la última descarga, sus datos son más nuevos.
        if (descargado.exists() && descargado.lastModified() > instalada) {
            runCatching { return parsear(descargado.readText()) }
        }
        return parsear(context.assets.open("efemerides.json").bufferedReader().use { it.readText() })
    }

    /**
     * Descarga la última versión desde GitHub. Devuelve true si ha cambiado algo.
     * Hace una conexión a internet: hay que llamarla fuera del hilo principal.
     * Si no hay conexión o el archivo no es válido, no cambia nada.
     */
    fun actualizar(context: Context): Boolean {
        val json = runCatching { descargar(URL_DATOS) }.getOrNull() ?: return false
        val nuevas = runCatching { parsear(json) }.getOrNull() ?: return false
        if (nuevas.size < 50) return false  // protección ante un archivo roto o vacío
        val actuales = todas(context)
        val destino = File(context.filesDir, DESCARGADO)
        val temporal = File(context.filesDir, "$DESCARGADO.tmp")
        temporal.writeText(json)
        temporal.renameTo(destino)
        cache = nuevas
        return nuevas != actuales
    }

    private fun descargar(direccion: String): String {
        val conexion = URL(direccion).openConnection() as HttpURLConnection
        conexion.connectTimeout = 10_000
        conexion.readTimeout = 15_000
        try {
            check(conexion.responseCode == 200) { "Respuesta ${conexion.responseCode}" }
            return conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }
}

object Ajustes {
    private fun prefs(c: Context) = c.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    fun favoritas(c: Context): Set<String> =
        prefs(c).getStringSet("favoritas", emptySet())!!.toSet()

    /** Añade o quita una favorita y devuelve el conjunto actualizado. */
    fun cambiarFavorita(c: Context, id: String): Set<String> {
        val s = favoritas(c).toMutableSet()
        if (!s.add(id)) s.remove(id)
        prefs(c).edit().putStringSet("favoritas", s).apply()
        return s
    }

    fun escalaLetra(c: Context): Float = prefs(c).getFloat("escala_letra", 1f)

    fun cambiarEscalaLetra(c: Context, escala: Float) =
        prefs(c).edit().putFloat("escala_letra", escala).apply()

    fun recordJuego(c: Context): Int = prefs(c).getInt("record_juego", 0)

    fun guardarRecordJuego(c: Context, racha: Int) {
        if (racha > recordJuego(c)) prefs(c).edit().putInt("record_juego", racha).apply()
    }
}
