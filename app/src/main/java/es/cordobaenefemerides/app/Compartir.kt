package es.cordobaenefemerides.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Compartir {
    const val ANCHO = 1080
    const val ALTO = 1350   // formato 4:5, el que mejor se ve en Instagram, WhatsApp y X

    private const val GRANATE = 0xFF7A1F2B.toInt()
    private const val GRANATE_OSCURO = 0xFF4A0F18.toInt()
    private const val CREMA = 0xFFF4E3C1.toInt()
    private const val DORADO = 0xFFC9A24A.toInt()
    private val formatoDia = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES"))

    /** Abre el menú de compartir de Android con la tarjeta de la efeméride y su texto. */
    fun compartir(context: Context, e: Efemeride) {
        val carpeta = File(context.cacheDir, "compartir").apply { mkdirs() }
        val archivo = File(carpeta, "efemeride.png")
        archivo.outputStream().use { tarjeta(e).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, e.textoParaCompartir)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(envio, "Compartir efeméride"))
    }

    /** Comparte un texto sencillo (por ejemplo, el resultado del juego). */
    fun compartirTexto(context: Context, texto: String) {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        context.startActivity(Intent.createChooser(envio, "Compartir"))
    }

    /** Dibuja la tarjeta de la efeméride con los colores de la app. */
    fun tarjeta(e: Efemeride): Bitmap {
        val bmp = Bitmap.createBitmap(ANCHO, ALTO, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val fondo = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, ALTO.toFloat(), GRANATE, GRANATE_OSCURO, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, ANCHO.toFloat(), ALTO.toFloat(), fondo)
        val marco = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 4f; color = DORADO
        }
        c.drawRect(40f, 40f, ANCHO - 40f, ALTO - 40f, marco)

        arco(c, ANCHO / 2f, 150f, 3.2f)

        val serif = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val fecha = LocalDate.of(2024, e.mes, e.dia).format(formatoDia)
        centrado(c, "TAL DÍA COMO HOY", 300f, 38f, sans, DORADO)
        centrado(c, "$fecha de ${e.anioTexto}", 380f, 64f, serif, CREMA, maxAncho = ANCHO - 160f)

        // Texto principal: se reduce la letra hasta que quepa en el hueco disponible.
        val cuerpo = buildString {
            if (e.titulo.isNotBlank()) append(e.titulo).append(". ")
            append(Texto.quitarFecha(e.texto))
        }
        val superior = 450f
        val inferior = ALTO - 190f
        val anchoTexto = ANCHO - 200
        var tam = 50f
        var layout: StaticLayout
        do {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif; textSize = tam; color = CREMA }
            layout = StaticLayout.Builder.obtain(cuerpo, 0, cuerpo.length, p, anchoTexto)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.18f)
                .build()
            tam -= 2f
        } while (layout.height > inferior - superior && tam > 22f)
        c.save()
        c.translate(100f, superior + (inferior - superior - layout.height) / 2f)
        layout.draw(c)
        c.restore()

        val linea = Paint().apply { color = DORADO }
        c.drawRect(ANCHO / 2f - 70f, ALTO - 165f, ANCHO / 2f + 70f, ALTO - 160f, linea)
        centrado(c, "#CórdobaEnEfemérides", ALTO - 95f, 48f, serif, CREMA)
        return bmp
    }

    private fun centrado(
        c: Canvas, texto: String, y: Float, tam: Float, tipo: Typeface, color: Int,
        maxAncho: Float = ANCHO - 120f,
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tipo; textSize = tam; this.color = color; textAlign = Paint.Align.CENTER
        }
        while (p.measureText(texto) > maxAncho && p.textSize > 20f) p.textSize -= 2f
        c.drawText(texto, ANCHO / 2f, y, p)
    }

    /** El arco califal del icono de la app. */
    private fun arco(c: Canvas, cx: Float, cy: Float, s: Float) {
        val crema = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CREMA }
        val dorado = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DORADO }
        val p = Path().apply {
            moveTo(cx - 20 * s, cy + 24 * s)
            lineTo(cx - 20 * s, cy)
            arcTo(RectF(cx - 20 * s, cy - 20 * s, cx + 20 * s, cy + 20 * s), 180f, 180f)
            lineTo(cx + 20 * s, cy + 24 * s)
            lineTo(cx + 12 * s, cy + 24 * s)
            lineTo(cx + 12 * s, cy)
            arcTo(RectF(cx - 12 * s, cy - 12 * s, cx + 12 * s, cy + 12 * s), 0f, -180f)
            lineTo(cx - 12 * s, cy + 24 * s)
            close()
        }
        c.drawPath(p, crema)
        c.drawRect(cx - 24 * s, cy + 25 * s, cx + 24 * s, cy + 27 * s, dorado)
    }
}
