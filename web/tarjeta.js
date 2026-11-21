// Dibuja la tarjeta para compartir (1080×1350), igual que Compartir.kt en la app de Android.
import { anioTexto, mesDia, nombreDia, quitarFecha } from "./logica.js";

const ANCHO = 1080, ALTO = 1350;
const GRANATE = "#7A1F2B", GRANATE_OSCURO = "#4A0F18", CREMA = "#F4E3C1", DORADO = "#C9A24A";
const SERIF = 'Georgia, "Times New Roman", serif';
const SANS = '-apple-system, "Segoe UI", Roboto, sans-serif';

function arco(c, cx, cy, s) {
  c.fillStyle = CREMA;
  c.beginPath();
  c.moveTo(cx - 20 * s, cy + 24 * s);
  c.lineTo(cx - 20 * s, cy);
  c.arc(cx, cy, 20 * s, Math.PI, 0);
  c.lineTo(cx + 20 * s, cy + 24 * s);
  c.lineTo(cx + 12 * s, cy + 24 * s);
  c.lineTo(cx + 12 * s, cy);
  c.arc(cx, cy, 12 * s, 0, Math.PI, true);
  c.lineTo(cx - 12 * s, cy + 24 * s);
  c.closePath();
  c.fill();
  c.fillStyle = DORADO;
  c.fillRect(cx - 24 * s, cy + 25 * s, 48 * s, 2 * s);
}

function centrado(c, texto, y, tam, familia, color, peso = "bold", maxAncho = ANCHO - 120) {
  c.fillStyle = color;
  c.textAlign = "center";
  do {
    c.font = `${peso} ${tam}px ${familia}`;
    tam -= 2;
  } while (c.measureText(texto).width > maxAncho && tam > 20);
  c.fillText(texto, ANCHO / 2, y);
}

function lineas(c, texto, ancho) {
  const res = [];
  let actual = "";
  for (const palabra of texto.split(/\s+/)) {
    const prueba = actual ? `${actual} ${palabra}` : palabra;
    if (c.measureText(prueba).width <= ancho || !actual) actual = prueba;
    else { res.push(actual); actual = palabra; }
  }
  if (actual) res.push(actual);
  return res;
}

/** Devuelve un <canvas> con la tarjeta de la efeméride. */
export function tarjeta(e) {
  const lienzo = document.createElement("canvas");
  lienzo.width = ANCHO; lienzo.height = ALTO;
  const c = lienzo.getContext("2d");
  const g = c.createLinearGradient(0, 0, 0, ALTO);
  g.addColorStop(0, GRANATE); g.addColorStop(1, GRANATE_OSCURO);
  c.fillStyle = g; c.fillRect(0, 0, ANCHO, ALTO);
  c.strokeStyle = DORADO; c.lineWidth = 4; c.strokeRect(40, 40, ANCHO - 80, ALTO - 80);

  arco(c, ANCHO / 2, 150, 3.2);
  const { mes, dia } = mesDia(e);
  centrado(c, "TAL DÍA COMO HOY", 300, 38, SANS, DORADO, "normal");
  centrado(c, `${nombreDia(mes, dia)} de ${anioTexto(e)}`, 380, 64, SERIF, CREMA, "bold", ANCHO - 160);

  const cuerpo = (e.titulo ? `${e.titulo}. ` : "") + quitarFecha(e.texto);
  const superior = 450, inferior = ALTO - 190, anchoTexto = ANCHO - 200;
  let tam = 50, filas, alto;
  do {
    c.font = `bold ${tam}px ${SERIF}`;
    filas = lineas(c, cuerpo, anchoTexto);
    alto = filas.length * tam * 1.18;
    tam -= 2;
  } while (alto > inferior - superior && tam > 22);
  tam += 2;
  c.fillStyle = CREMA; c.textAlign = "center"; c.textBaseline = "top";
  let y = superior + (inferior - superior - alto) / 2;
  for (const f of filas) { c.fillText(f, ANCHO / 2, y); y += tam * 1.18; }
  c.textBaseline = "alphabetic";

  c.fillStyle = DORADO; c.fillRect(ANCHO / 2 - 70, ALTO - 165, 140, 5);
  centrado(c, "#CórdobaEnEfemérides", ALTO - 95, 48, SERIF, CREMA);
  return lienzo;
}
