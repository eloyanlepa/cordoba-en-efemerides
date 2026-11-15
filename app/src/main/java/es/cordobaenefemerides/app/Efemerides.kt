package es.cordobaenefemerides.app

import android.content.Context
import org.json.JSONArray
import java.time.LocalDate

data class Efemeride(
    val fecha: String,      // "MM-DD"
    val anio: Int,
    val titulo: String,
    val texto: String,
    val fuente: String,
    val verificada: Boolean,
)

object Efemerides {
    @Volatile private var cache: List<Efemeride>? = null

    /** Todas las efemérides incluidas en la app (archivo data/efemerides.json). */
    fun todas(context: Context): List<Efemeride> =
        cache ?: cargar(context).also { cache = it }

    /** Las efemérides de un día concreto, de la más antigua a la más reciente. */
    fun delDia(lista: List<Efemeride>, dia: LocalDate): List<Efemeride> {
        val clave = "%02d-%02d".format(dia.monthValue, dia.dayOfMonth)
        return lista.filter { it.fecha == clave }.sortedBy { it.anio }
    }

    private fun cargar(context: Context): List<Efemeride> {
        val json = context.assets.open("efemerides.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        return List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Efemeride(
                fecha = o.getString("fecha"),
                anio = o.getInt("anio"),
                titulo = o.getString("titulo"),
                texto = o.getString("texto"),
                fuente = o.optString("fuente"),
                verificada = o.optBoolean("verificada", false),
            )
        }
    }
}
