package es.cordobaenefemerides.app

import java.time.LocalDate
import kotlin.random.Random

/** Una pregunta del juego: «¿En qué año ocurrió esto?» con cuatro opciones. */
data class Pregunta(
    val efemeride: Efemeride,
    val enunciado: String,
    val opciones: List<Int>,
) {
    val correcta: Int get() = efemeride.anio
}

object Juego {
    /** Solo efemérides comprobadas, después de Cristo y con texto suficiente para preguntar. */
    fun candidatas(lista: List<Efemeride>): List<Efemeride> =
        lista.filter { it.verificada && it.anio > 0 && Texto.quitarFecha(it.texto).length > 40 }

    /** Cambia el año que se pregunta por «…» si aparece dentro del texto, para no dar la respuesta. */
    fun ocultarAnio(texto: String, anio: Int): String =
        texto.replace(Regex("\\b$anio\\b"), "…")

    fun nuevaPregunta(lista: List<Efemeride>, azar: Random = Random.Default): Pregunta {
        val e = candidatas(lista).random(azar)
        val enunciado = ocultarAnio(Texto.quitarFecha(e.texto), e.anio)
        val anioMaximo = LocalDate.now().year
        val opciones = mutableSetOf(e.anio)
        while (opciones.size < 4) {
            val distancia = azar.nextInt(4, 90) * (if (azar.nextBoolean()) 1 else -1)
            val propuesta = e.anio + distancia
            if (propuesta in 1..anioMaximo) opciones.add(propuesta)
        }
        return Pregunta(e, enunciado, opciones.shuffled(azar))
    }
}
