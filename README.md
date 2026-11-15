# Córdoba en Efemérides

App para Android que cada día muestra las efemérides de Córdoba (España) y envía un aviso por la mañana.
Nace del hashtag #CórdobaEnEfemérides.

## Cómo funciona

- **Las efemérides** están en [`data/efemerides.json`](data/efemerides.json). Es la única fuente de datos: la app las lleva dentro, así que funciona sin internet y sin servidor.
- **El aviso diario** salta hacia las 9:00. Android puede retrasarlo unos minutos para ahorrar batería.
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
- `verificada`: pon `true` cuando hayas comprobado la fecha en una fuente fiable. Mientras sea `false`, la app la muestra como «Pendiente de verificar».

## Compilar en tu ordenador (opcional)

Hace falta Java 17 y el SDK de Android:

```bash
./gradlew testDebugUnitTest assembleDebug
```

La app queda en `app/build/outputs/apk/debug/app-debug.apk`.
