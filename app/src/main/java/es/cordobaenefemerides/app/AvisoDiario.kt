package es.cordobaenefemerides.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object AvisoDiario {
    /** Hora del aviso si el usuario no ha elegido otra. Android puede retrasarlo unos minutos para ahorrar batería. */
    const val HORA_POR_DEFECTO = 9
    private const val CANAL = "efemeride-del-dia"
    private const val TRABAJO = "aviso-diario"
    private const val PREFS = "ajustes"

    /** Tiempo que falta hasta la próxima vez que sean las [hora]:[minuto]. */
    fun retrasoHasta(ahora: LocalDateTime, hora: Int, minuto: Int = 0): Duration {
        var siguiente = ahora.toLocalDate().atTime(hora, minuto)
        if (!siguiente.isAfter(ahora)) siguiente = siguiente.plusDays(1)
        return Duration.between(ahora, siguiente)
    }

    /** Hora y minuto elegidos para el aviso. */
    fun horaElegida(context: Context): Pair<Int, Int> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getInt("hora", HORA_POR_DEFECTO) to p.getInt("minuto", 0)
    }

    /** Guarda una nueva hora y vuelve a programar el aviso para esa hora. */
    fun cambiarHora(context: Context, hora: Int, minuto: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("hora", hora).putInt("minuto", minuto).apply()
        programar(context, reemplazar = true)
    }

    fun programar(context: Context, reemplazar: Boolean = false) {
        val (hora, minuto) = horaElegida(context)
        val retraso = retrasoHasta(LocalDateTime.now(), hora, minuto)
        val peticion = PeriodicWorkRequestBuilder<AvisoWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(retraso.toMinutes(), TimeUnit.MINUTES)
            .build()
        val politica = if (reemplazar) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            else ExistingPeriodicWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(TRABAJO, politica, peticion)
    }

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL, context.getString(R.string.canal_aviso), NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun mostrar(context: Context, delDia: List<Efemeride>) {
        if (delDia.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val abrirApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val primera = delDia.first()
        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .apply {
                if (delDia.size == 1) {
                    setContentTitle("Tal día como hoy, en ${primera.anioTexto}")
                    setContentText(primera.resumen)
                    val completo = if (primera.titulo.isBlank()) primera.texto
                        else "${primera.titulo}. ${primera.texto}"
                    setStyle(NotificationCompat.BigTextStyle().bigText(completo))
                } else {
                    setContentTitle("${delDia.size} efemérides hoy en Córdoba")
                    setContentText("${primera.anioTexto}: ${primera.resumen}")
                    setStyle(NotificationCompat.InboxStyle().also { s ->
                        delDia.forEach { s.addLine("${it.anioTexto}: ${it.resumen}") }
                    })
                }
            }
            .build()
        NotificationManagerCompat.from(context).notify(1, aviso)
    }
}

class AvisoWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // Antes del aviso se intenta descargar la última versión de las efemérides (si no hay
        // conexión no pasa nada: se usan las que ya tiene la app).
        Efemerides.actualizar(applicationContext)
        val hoy = Efemerides.delDia(Efemerides.todas(applicationContext), LocalDate.now())
        AvisoDiario.mostrar(applicationContext, hoy)
        WidgetEfemeride.actualizarTodos(applicationContext)
        return Result.success()
    }
}
