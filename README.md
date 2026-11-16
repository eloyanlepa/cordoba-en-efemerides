# Córdoba en Efemérides

App para Android que cada día muestra las efemérides de Córdoba (España) y envía un aviso por la mañana.
Nace del hashtag #CórdobaEnEfemérides.

## Cómo funciona

- **Las efemérides** están en [`data/efemerides.json`](data/efemerides.json). Es la única fuente de datos: la app las lleva dentro, así que funciona sin internet y sin servidor.
- **El aviso diario** salta hacia las 9:00. Android puede retrasarlo unos minutos para ahorrar batería.
- **La hora del aviso** se cambia en la propia app, con el botón 🔔.
- **El botón Compartir** abre el menú de Android para enviar la efeméride a WhatsApp, X, Facebook, Instagram, etc., con el hashtag #CórdobaEnEfemérides.
- **GitHub fabrica la app** automáticamente con cada cambio (pestaña *Actions*). El archivo para instalar (`.apk`) aparece en *Artifacts*.

## Añadir una efeméride

Cada efeméride tiene este aspecto:

```json
{
  "fecha": "07-04",
  "anio": 1917,
  "titulo": "Nace Manolete",
  "texto": "Breve explicación de lo que ocurrió.",
  "fuente": "https://enlace-a-la-fuente",
  "verificada": true
}
```

- `fecha`: mes y día, en formato `MM-DD`.
- `anio`: el año. Para fechas antes de Cristo, en negativo (por ejemplo `-45`).
- `titulo` es opcional; si no hay, el aviso muestra el comienzo del texto.
- `video` es opcional: el enlace al vídeo publicado en Instagram.
- `verificada`: pon `true` cuando hayas comprobado la fecha en una fuente fiable. Mientras sea `false`, la app la muestra como «Pendiente de verificar».

## Vídeos para Instagram

Cada vídeo parte de un guion en `videos/guiones/` (por ejemplo [`11-16-1830.json`](videos/guiones/11-16-1830.json)) con las frases que salen en pantalla, el texto para leer en voz alta y el pie de la publicación. El vídeo se genera así:

```bash
python3 herramientas/video.py videos/guiones/11-16-1830.json
```

Sale en `videos/salida/` (vertical, 1080×1920, 30 segundos, sin música para poder añadirla en Instagram).

Si el guion incluye `"final": "videos/recursos/cierre.mp4"`, el vídeo termina con ese cierre (el logo animado de Córdoba en Efemérides, con su sonido) en lugar de la diapositiva final.

Cuando el vídeo esté publicado, su enlace se añade a la efeméride con el campo `"video"` y la app muestra el botón «▶ Ver vídeo».

## Importar la hoja de cálculo

Las efemérides de la hoja «Efemérides de Córdoba» se importan así (exportando antes la hoja como CSV):

```bash
python3 herramientas/importar_hoja.py hoja.csv data/efemerides.json
```

Se puede repetir cuando la hoja cambie: sustituye las efemérides que venían de la hoja y conserva las demás.

## Compilar en tu ordenador (opcional)

Hace falta Java 17 y el SDK de Android:

```bash
./gradlew testDebugUnitTest assembleDebug
```

La app queda en `app/build/outputs/apk/debug/app-debug.apk`.
