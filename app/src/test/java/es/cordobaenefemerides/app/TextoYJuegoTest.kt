package es.cordobaenefemerides.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TextoYJuegoTest {
    private fun e(fecha: String, anio: Int, texto: String, temas: List<String> = emptyList()) =
        Efemeride(fecha, anio, "", texto, "", true, temas = temas)

    @Test fun quitaLaFechaInicial() {
        assertEquals("Fallece en Londres.", Texto.quitarFecha("16 de noviembre de 1830, fallece en Londres."))
        assertEquals("Muere en Córdoba.", Texto.quitarFecha("2 de julio de 942, muere en Córdoba."))
        assertEquals("Batalla de Munda.", Texto.quitarFecha("17 de marzo del 45 a.C., batalla de Munda."))
        assertEquals("Fallece D. Lope.", Texto.quitarFecha("10 de junio de 1245 fallece D. Lope."))
        assertEquals("Álvaro Colodro salta.", Texto.quitarFecha("En la noche del 24 de diciembre de 1235, Álvaro Colodro salta."))
        assertEquals("Nace el escultor.", Texto.quitarFecha("25 de marzo de 1867, nace el escultor."))
    }

    @Test fun buscaSinTildesNiMayusculas() {
        val lista = listOf(
            e("05-23", 1627, "Muere Luis de Góngora en Córdoba.", listOf("Arte y cultura")),
            e("07-13", 1857, "Se localizan los restos de GÓNGORA.", listOf("Religión")),
            e("01-01", 1919, "Manifiesto andalucista."),
        )
        assertEquals(2, Efemerides.buscar(lista, "gongora").size)
        assertEquals(1, Efemerides.buscar(lista, "gongora cordoba").size)
        assertEquals(1, Efemerides.buscar(lista, "", "Religión").size)
        assertEquals(listOf("Arte y cultura", "Religión"), Efemerides.temas(lista).sorted())
    }

    @Test fun preguntaConCuatroOpcionesDistintasYLaCorrecta() {
        val lista = listOf(e("04-29", 1953, "29 de abril de 1953, se inaugura el puente de San Rafael, que en 1953 fue el más ancho de España."))
        repeat(50) { i ->
            val p = Juego.nuevaPregunta(lista, Random(i))
            assertEquals(4, p.opciones.toSet().size)
            assertTrue(1953 in p.opciones)
            assertFalse("1953" in p.enunciado)
            assertTrue(p.enunciado.startsWith("Se inaugura"))
        }
    }

    @Test fun elIdNoDependeDelOrden() {
        val a = e("01-01", 1919, "Manifiesto andalucista.")
        assertEquals(a.id, a.copy(temas = listOf("Política y gobierno")).id)
    }
}

class VeintinueveDeFebreroTest {
    private val lista = listOf(
        Efemeride("02-28", 1900, "", "Del 28.", "", true),
        Efemeride("02-29", 1964, "", "Del 29.", "", true),
    )

    @Test fun enAnioNoBisiestoSaleEl28() {
        assertEquals(listOf(1900, 1964), Efemerides.delDia(lista, java.time.LocalDate.of(2026, 2, 28)).map { it.anio })
    }

    @Test fun enAnioBisiestoSaleSuDia() {
        assertEquals(listOf(1900), Efemerides.delDia(lista, java.time.LocalDate.of(2028, 2, 28)).map { it.anio })
        assertEquals(listOf(1964), Efemerides.delDia(lista, java.time.LocalDate.of(2028, 2, 29)).map { it.anio })
    }
}
