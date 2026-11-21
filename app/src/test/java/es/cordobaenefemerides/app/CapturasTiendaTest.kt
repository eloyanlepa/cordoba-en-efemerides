package es.cordobaenefemerides.app

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
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

/** Capturas para la ficha de Google Play (proporción 9:16), guardadas en app/build/capturas-tienda. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h731dp-xxhdpi")
class CapturasTiendaTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val carpeta = File("build/capturas-tienda").apply { mkdirs() }
    private val contexto get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun captura(nombre: String, contenido: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { MaterialTheme(colorScheme = ColoresClaros) { contenido() } }
        compose.waitForIdle()
        val vista = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888)
        vista.draw(Canvas(bmp))
        File(carpeta, "$nombre.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun t1Hoy() = captura("1-hoy") { App(diaInicial = LocalDate.of(2026, 8, 22)) }

    @Test fun t2Calendario() = captura("2-calendario") {
        App(pestanaInicial = Pestana.CALENDARIO)
    }

    @Test fun t3Buscar() = captura("3-buscar") { App(pestanaInicial = Pestana.BUSCAR, consultaInicial = "mezquita") }

    @Test fun t4Favoritas() {
        val todas = Efemerides.todas(contexto)
        listOf("07-04" to 1917, "05-23" to 1627, "04-29" to 1953, "11-15" to 1923).forEach { (f, a) ->
            Ajustes.cambiarFavorita(contexto, todas.first { it.fecha == f && it.anio == a }.id)
        }
        captura("4-favoritas") { App(pestanaInicial = Pestana.FAVORITAS) }
    }

    @Test fun t5Juego() = captura("5-juego") { App(pestanaInicial = Pestana.JUEGO) }
}
