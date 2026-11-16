package es.cordobaenefemerides.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Widget de la pantalla de inicio con la efeméride del día. */
class WidgetEfemeride : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { pintar(context, manager, it) }
    }

    companion object {
        private val formatoDia = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES"))

        /** Vuelve a dibujar todos los widgets (al cambiar de día o al descargar efemérides nuevas). */
        fun actualizarTodos(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WidgetEfemeride::class.java))
            ids.forEach { pintar(context, manager, it) }
        }

        private fun pintar(context: Context, manager: AppWidgetManager, id: Int) {
            val hoy = LocalDate.now()
            val delDia = Efemerides.delDia(Efemerides.todas(context), hoy)
            val vista = RemoteViews(context.packageName, R.layout.widget_efemeride)
            vista.setTextViewText(R.id.widget_fecha, "TAL DÍA COMO HOY · ${hoy.format(formatoDia).uppercase()}")
            if (delDia.isEmpty()) {
                vista.setTextViewText(R.id.widget_anio, "")
                vista.setTextViewText(R.id.widget_texto, "Hoy no hay efeméride guardada. ¡Toca para ver otros días!")
            } else {
                val e = delDia.first()
                val mas = if (delDia.size > 1) "  ·  +${delDia.size - 1} más" else ""
                vista.setTextViewText(R.id.widget_anio, e.anioTexto + mas)
                vista.setTextViewText(R.id.widget_texto, e.titulo.ifBlank { Texto.quitarFecha(e.texto) })
            }
            val abrir = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            vista.setOnClickPendingIntent(R.id.widget_raiz, abrir)
            manager.updateAppWidget(id, vista)
        }
    }
}
