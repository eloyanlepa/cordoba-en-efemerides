"""Genera un vídeo vertical (1080x1920, ~30 s) para Instagram con una efeméride.

Uso:
    python3 herramientas/video.py videos/guiones/11-16-1830.json

El guion es un JSON con:
    fecha        "MM-DD" de la efeméride
    anio         año de la efeméride (negativo = antes de Cristo)
    diapositivas lista de frases cortas que aparecen en pantalla, una tras otra
    voz          (opcional) texto para leer en voz alta mientras se ve el vídeo
    pie          (opcional) texto para la publicación de Instagram

El vídeo se guarda en videos/salida/MM-DD-AAAA.mp4 (esa carpeta no se sube a GitHub).
Necesita Python con Pillow y ffmpeg.
"""
import json
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw, ImageFont

ANCHO, ALTO, FPS = 1080, 1920, 30
GRANATE, CREMA, DORADO, GRANATE_OSCURO = "#7A1F2B", "#F4E3C1", "#C9A24A", "#4A0F18"
SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
MESES = "enero febrero marzo abril mayo junio julio agosto septiembre octubre noviembre diciembre".split()

# Duraciones en segundos: portada, año, cierre. El resto se reparte entre las diapositivas.
PORTADA, ANIO, CIERRE, TOTAL = 4.0, 3.0, 4.0, 30.0


def fuente(ruta, tam):
    return ImageFont.truetype(ruta, tam)


def partir(texto, f, ancho_max, dibujo):
    """Divide el texto en líneas que caben en ancho_max."""
    lineas, actual = [], ""
    for palabra in texto.split():
        prueba = f"{actual} {palabra}".strip()
        if dibujo.textlength(prueba, font=f) <= ancho_max:
            actual = prueba
        else:
            lineas.append(actual)
            actual = palabra
    if actual:
        lineas.append(actual)
    return lineas


def lienzo():
    img = Image.new("RGB", (ANCHO, ALTO), GRANATE)
    d = ImageDraw.Draw(img)
    # Degradado suave hacia abajo
    for y in range(ALTO):
        t = y / ALTO
        r = int(0x7A + (0x4A - 0x7A) * t)
        g = int(0x1F + (0x0F - 0x1F) * t)
        b = int(0x2B + (0x18 - 0x2B) * t)
        d.line([(0, y), (ANCHO, y)], fill=(r, g, b))
    # Marco dorado
    d.rectangle([50, 50, ANCHO - 50, ALTO - 50], outline=DORADO, width=4)
    return img, d


def arco(d, cx, cy, escala):
    """Arco califal sencillo, el mismo motivo que el icono de la app."""
    s = escala
    d.rectangle([cx - 20 * s, cy, cx - 12 * s, cy + 24 * s], fill=CREMA)
    d.rectangle([cx + 12 * s, cy, cx + 20 * s, cy + 24 * s], fill=CREMA)
    d.pieslice([cx - 20 * s, cy - 20 * s, cx + 20 * s, cy + 20 * s], 180, 360, fill=CREMA)
    d.pieslice([cx - 12 * s, cy - 12 * s, cx + 12 * s, cy + 12 * s], 180, 360, fill=GRANATE)
    d.rectangle([cx - 12 * s, cy - 1, cx + 12 * s, cy + 24 * s], fill=GRANATE)
    d.rectangle([cx - 24 * s, cy + 25 * s, cx + 24 * s, cy + 27 * s], fill=DORADO)


def ajustada(d, texto, ruta, tam, ancho_max=ANCHO - 220):
    """La fuente más grande (hasta tam) con la que el texto cabe en una línea."""
    f = fuente(ruta, tam)
    while d.textlength(texto, font=f) > ancho_max and tam > 20:
        tam -= 4
        f = fuente(ruta, tam)
    return f


def centrado(d, y, texto, f, color):
    w = d.textlength(texto, font=f)
    d.text(((ANCHO - w) / 2, y), texto, font=f, fill=color)


def bloque(d, texto, f, color, y_centro, interlineado=1.3):
    lineas = partir(texto, f, ANCHO - 220, d)
    alto_linea = f.size * interlineado
    y = y_centro - alto_linea * len(lineas) / 2
    for l in lineas:
        centrado(d, y, l, f, color)
        y += alto_linea


def anio_texto(anio):
    return f"{-anio} a. C." if anio < 0 else str(anio)


def portada(fecha):
    img, d = lienzo()
    mes, dia = (int(x) for x in fecha.split("-"))
    arco(d, ANCHO / 2, 560, 6)
    centrado(d, 860, "TAL DÍA COMO HOY", fuente(SANS, 54), DORADO)
    texto_fecha = f"{dia} de {MESES[mes - 1]}"
    centrado(d, 960, texto_fecha, ajustada(d, texto_fecha, SERIF, 110), CREMA)
    centrado(d, 1120, "en Córdoba", fuente(SERIF, 70), CREMA)
    return img


def diapositiva_anio(anio):
    img, d = lienzo()
    centrado(d, 700, "EN EL AÑO", fuente(SANS, 54), DORADO)
    f = ajustada(d, anio_texto(anio), SERIF, 260)
    centrado(d, 800, anio_texto(anio), f, CREMA)
    return img


def diapositiva_texto(texto):
    img, d = lienzo()
    bloque(d, texto, fuente(SERIF, 78), CREMA, ALTO / 2)
    d.rectangle([ANCHO / 2 - 80, 1480, ANCHO / 2 + 80, 1486], fill=DORADO)
    return img


def cierre():
    img, d = lienzo()
    arco(d, ANCHO / 2, 700, 5)
    centrado(d, 980, "#CórdobaEnEfemérides", ajustada(d, "#CórdobaEnEfemérides", SERIF, 76), CREMA)
    centrado(d, 1100, "Cada día, un trozo de nuestra historia", fuente(SANS, 44), DORADO)
    return img


def clip(img, segundos, ruta_png, ruta_mp4):
    img.save(ruta_png)
    marcos = int(segundos * FPS)
    filtro = (
        f"scale={ANCHO * 2}:-1,"
        f"zoompan=z='1+0.04*on/{marcos}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d={marcos}:s={ANCHO}x{ALTO}:fps={FPS},"
        f"fade=t=in:st=0:d=0.4,fade=t=out:st={segundos - 0.4}:d=0.4,format=yuv420p"
    )
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-loop", "1", "-i", ruta_png, "-t", str(segundos),
         "-vf", filtro, "-r", str(FPS), "-c:v", "libx264", "-preset", "medium", "-crf", "20", ruta_mp4],
        check=True,
    )


def main(ruta_guion):
    g = json.load(open(ruta_guion, encoding="utf-8"))
    diapos = g["diapositivas"]
    resto = (TOTAL - PORTADA - ANIO - CIERRE) / len(diapos)
    escenas = [(portada(g["fecha"]), PORTADA), (diapositiva_anio(g["anio"]), ANIO)]
    escenas += [(diapositiva_texto(t), resto) for t in diapos]
    escenas.append((cierre(), CIERRE))

    raiz = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    salida_dir = os.path.join(raiz, "videos", "salida")
    os.makedirs(salida_dir, exist_ok=True)
    nombre = f"{g['fecha']}-{g['anio']}"
    salida = os.path.join(salida_dir, nombre + ".mp4")

    with tempfile.TemporaryDirectory() as tmp:
        lista = []
        for i, (img, seg) in enumerate(escenas):
            mp4 = os.path.join(tmp, f"{i:02d}.mp4")
            clip(img, seg, os.path.join(tmp, f"{i:02d}.png"), mp4)
            lista.append(f"file '{mp4}'")
        portada(g["fecha"]).save(os.path.join(salida_dir, nombre + "-portada.jpg"), quality=92)
        lista_txt = os.path.join(tmp, "lista.txt")
        open(lista_txt, "w").write("\n".join(lista))
        # Se añade una pista de audio en silencio: Instagram permite poner música encima.
        subprocess.run(
            ["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", lista_txt,
             "-f", "lavfi", "-i", "anullsrc=r=44100:cl=stereo", "-shortest",
             "-c:v", "copy", "-c:a", "aac", "-movflags", "+faststart", salida],
            check=True,
        )
    print(salida)


if __name__ == "__main__":
    main(sys.argv[1])
