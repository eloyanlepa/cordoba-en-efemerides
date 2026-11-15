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
    /** Hora aproximada del aviso diario (Android puede retrasarlo unos minutos para ahorrar batería). */
    const val HORA = 9
    private const val CANAL = "efemeride-del-dia"
    private const val TRABAJO = "aviso-diario"

    /** Tiempo que falta hasta la próxima vez que sean las [hora]:00. */
    fun retrasoHasta(ahora: LocalDateTime, hora: Int): Duration {
        var siguiente = ahora.toLocalDate().atTime(hora, 0)
        if (!siguiente.isAfter(ahora)) siguiente = siguiente.plusDays(1)
        return Duration.between(ahora, siguiente)
    }

    fun programar(context: Context) {
        val retraso = retrasoHasta(LocalDateTime.now(), HORA)
        val peticion = PeriodicWorkRequestBuilder<AvisoWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(retraso.toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, peticion)
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
                    setContentTitle("Tal día como hoy, en ${primera.anio}")
                    setContentText(primera.titulo)
                    setStyle(NotificationCompat.BigTextStyle().bigText("${primera.titulo}. ${primera.texto}"))
                } else {
                    setContentTitle("${delDia.size} efemérides hoy en Córdoba")
                    setContentText("${primera.anio}: ${primera.titulo}")
                    setStyle(NotificationCompat.InboxStyle().also { s ->
                        delDia.forEach { s.addLine("${it.anio}: ${it.titulo}") }
                    })
                }
            }
            .build()
        NotificationManagerCompat.from(context).notify(1, aviso)
    }
}

class AvisoWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val hoy = Efemerides.delDia(Efemerides.todas(applicationContext), LocalDate.now())
        AvisoDiario.mostrar(applicationContext, hoy)
        return Result.success()
    }
}
