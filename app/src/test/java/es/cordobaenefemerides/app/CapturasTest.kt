package es.cordobaenefemerides.app

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Dibuja las pantallas principales y guarda capturas en app/build/capturas para revisarlas. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class CapturasTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val carpeta = File("build/capturas").apply { mkdirs() }
    private val todas by lazy { Efemerides.todas(ApplicationProvider.getApplicationContext()) }

    private fun guardar(nombre: String, bmp: Bitmap) =
        File(carpeta, "$nombre.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

    private fun captura(nombre: String, contenido: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { MaterialTheme(colorScheme = ColoresClaros) { contenido() } }
        compose.waitForIdle()
        val vista = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888)
        vista.draw(Canvas(bmp))
        guardar(nombre, bmp)
    }

    @Test fun app() = captura("app") { App() }

    @Test fun dia() = captura("dia") {
        PantallaDia(todas, LocalDate.of(2026, 7, 28), emptySet(), {}, {})
    }

    @Test fun calendario() = captura("calendario") { PantallaCalendario(todas) {} }

    @Test fun buscar() = captura("buscar") {
        PantallaBuscar(todas, setOf(todas.first { it.fecha == "05-23" }.id), {}, {})
    }

    @Test fun favoritasConUna() = captura("favoritas-una") {
        PantallaFavoritas(todas, setOf(todas.first { it.fecha == "11-16" }.id), {}, {})
    }

    @Test fun widget() {
        val c = ApplicationProvider.getApplicationContext<android.content.Context>()
        val v = android.view.LayoutInflater.from(c).inflate(R.layout.widget_efemeride, null)
        v.findViewById<android.widget.TextView>(R.id.widget_fecha).text = "TAL DÍA COMO HOY · 16 DE NOVIEMBRE"
        v.findViewById<android.widget.TextView>(R.id.widget_anio).text = "1830"
        v.findViewById<android.widget.TextView>(R.id.widget_texto).text =
            Texto.quitarFecha(todas.first { it.fecha == "11-16" }.texto)
        val ancho = (320 * c.resources.displayMetrics.density).toInt()
        val alto = (150 * c.resources.displayMetrics.density).toInt()
        v.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(ancho, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(alto, android.view.View.MeasureSpec.EXACTLY),
        )
        v.layout(0, 0, ancho, alto)
        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        guardar("widget", bmp)
    }

    @Test fun favoritasVacia() = captura("favoritas") { PantallaFavoritas(todas, emptySet(), {}, {}) }

    @Test fun juego() = captura("juego") { PantallaJuego(todas) }

    @Test fun tarjetaParaCompartir() {
        val e = todas.first { it.fecha == "11-16" }
        guardar("tarjeta-corta", Compartir.tarjeta(e))
        guardar("tarjeta-larga", Compartir.tarjeta(todas.maxBy { it.texto.length }))
    }
}
