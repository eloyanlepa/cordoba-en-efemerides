# Ficha de Google Play · Córdoba en Efemérides

Todo lo necesario para rellenar Google Play Console. Los nombres de los apartados pueden variar un poco; entre paréntesis va el nombre en inglés.

## Datos básicos al crear la app (*Create app*)

| Campo | Qué poner |
|---|---|
| Nombre de la aplicación | `Córdoba en Efemérides` |
| Idioma predeterminado | Español (España) – es-ES |
| ¿Aplicación o juego? | **Aplicación** |
| ¿Gratuita o de pago? | **Gratuita** (no se puede cambiar después a de pago) |

## Ficha principal de Play Store (*Main store listing*)

**Nombre** (máx. 30): `Córdoba en Efemérides`

**Descripción breve** (máx. 80):

```
Cada día, un trozo de la historia de Córdoba: efemérides, calendario y juego.
```

**Descripción completa** (máx. 4000):

```
¿Qué pasó en Córdoba tal día como hoy?

Córdoba en Efemérides te cuenta cada día un trozo de la historia de la ciudad y su provincia: desde la Córdoba romana y el califato hasta el siglo XX. Más de 380 efemérides, una para cada día del año, recopiladas a partir de la prensa histórica cordobesa y otras fuentes, con su referencia.

QUÉ PUEDES HACER
• Recibir cada mañana un aviso con la efeméride del día, a la hora que elijas.
• Recorrer el año en un calendario y consultar cualquier día.
• Buscar por palabras («Góngora», «Mezquita», «San Lorenzo»…) o por temas: Al-Ándalus, Religión, Monumentos y urbanismo, Arte y cultura, Toros, Guerras y conflictos…
• Guardar tus favoritas.
• Jugar a «¿En qué año ocurrió?» y batir tu récord.
• Compartir cualquier efeméride como una imagen lista para Instagram, WhatsApp o X.
• Añadir un widget a la pantalla de inicio con la efeméride del día.
• Ajustar el tamaño de la letra.

SIEMPRE AL DÍA
Las efemérides nuevas y las correcciones llegan solas, sin actualizar la app. Y funciona sin conexión.

SIN PUBLICIDAD Y SIN RECOGER DATOS
No hay anuncios, ni cuentas, ni seguimiento. Tus favoritas y preferencias se quedan en tu móvil.

Nace del hashtag #CórdobaEnEfemérides. ¿Conoces una efeméride que falte? ¡Cuéntanosla!
```

**Gráficos:**
- Icono de la aplicación (512×512): `tienda/icono-512.png`
- Gráfico de funciones (*Feature graphic*, 1024×500): `tienda/grafico-destacado.png`
- Capturas de pantalla del teléfono (mín. 2): las cinco de `tienda/capturas/`

## Configuración de la tienda (*Store settings*)

| Campo | Qué poner |
|---|---|
| Categoría | **Libros y obras de consulta** (*Books & Reference*) |
| Etiquetas | Historia, Educación (las que ofrezca parecidas) |
| Correo de contacto | *El correo que elijas para la app* (es público) |
| Sitio web | `https://eloyanlepa.github.io/cordoba-en-efemerides/` |

## Contenido de la aplicación (*App content*)

**Política de privacidad**

```
https://eloyanlepa.github.io/cordoba-en-efemerides/privacidad.html
```

**Acceso a la aplicación** (*App access*): **Todas las funciones están disponibles sin restricciones de acceso** (no hay inicio de sesión).

**Anuncios** (*Ads*): **No**, mi aplicación no contiene anuncios.

**Clasificación del contenido** (*Content rating*):
- Categoría: **Referencia, noticias o educación**.
- Violencia, sexo, lenguaje, drogas, apuestas: **no** hay imágenes ni contenido de ese tipo. Si alguna pregunta habla de **referencias o descripciones de violencia**, contesta con sinceridad que **sí hay menciones textuales de hechos históricos** (batallas, bombardeos, fusilamientos, martirios), sin imágenes ni detalles explícitos.
- ¿Los usuarios pueden comunicarse o compartir contenido entre sí *dentro* de la app? **No** (compartir abre otras apps).
- ¿Compra de bienes digitales? **No**. ¿Ubicación? **No**.

**Público objetivo** (*Target audience*): marca **13-15, 16-17 y 18 o más**. No la dirijas a menores de 13 (eso activaría las normas de apps para familias).

**Aplicación de noticias** (*News apps*): **No**.

**Seguridad de los datos** (*Data safety*):
- ¿Recoge o comparte la app alguno de los tipos de datos de usuario requeridos? **No**.
- (Las favoritas y preferencias solo se guardan en el dispositivo; según las normas de Google eso **no** cuenta como «recogida».)

**Apps gubernamentales, financieras, de salud**: **No** en todas.

**Declaración de permisos**: solo usa **notificaciones** (aviso diario) e **internet** (descargar efemérides). No usa permisos especiales.

## Prueba cerrada (obligatoria para cuentas personales nuevas)

1. **Pruebas → Prueba cerrada** (*Testing → Closed testing*) → crear un *track*.
2. Subir la versión `.aab` que fabrica GitHub (artefacto «cordoba-en-efemerides-google-play»).
3. **Probadores**: crear una lista de correo con los **Gmail** de al menos 12 personas con Android.
4. Copiar el **enlace de participación** y enviárselo. Cada persona debe pulsar «Ser probador», instalarla desde Play y **no desinstalarla durante 14 días**.
5. Pasados los 14 días con 12 o más probadores, pedir **acceso a producción** (*Apply for production*).
