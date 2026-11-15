package es.cordobaenefemerides.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class LogicaTest {
    private fun e(fecha: String, anio: Int) = Efemeride(fecha, anio, "t$anio", "", "", false)

    @Test fun filtraPorDiaYOrdenaPorAnio() {
        val lista = listOf(e("07-04", 1917), e("07-11", 1561), e("07-04", 1800))
        val res = Efemerides.delDia(lista, LocalDate.of(2026, 7, 4))
        assertEquals(listOf(1800, 1917), res.map { it.anio })
    }

    @Test fun avisoMasTardeHoy() {
        val r = AvisoDiario.retrasoHasta(LocalDateTime.of(2026, 1, 1, 7, 30), 9)
        assertEquals(Duration.ofMinutes(90), r)
    }

    @Test fun avisoPasaAMananaSiYaEsLaHora() {
        val r = AvisoDiario.retrasoHasta(LocalDateTime.of(2026, 1, 1, 9, 0), 9)
        assertEquals(Duration.ofHours(24), r)
    }
}

class FormatoTest {
    @Test fun anioAntesDeCristo() {
        val e = Efemeride("03-17", -45, "", "Batalla de Munda", "", true)
        assertEquals("45 a. C.", e.anioTexto)
        assertEquals("Batalla de Munda", e.resumen)
    }
}
