// Lógica de la versión web de Córdoba en Efemérides.
// Es la misma que la de la app de Android (Efemerides.kt y Juego.kt), para que ambas se comporten igual.

export const MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
  "agosto", "septiembre", "octubre", "noviembre", "diciembre"];

const FECHA_INICIAL =
  /^\s*(en la noche del\s+)?\d{1,2}\s*(de\s+)?[a-záéíóú]+\s+(d+el?\s+|d+e\s+)?\d{1,4}\s*(a\.\s*C\.)?\s*,?\s*/i;

/** Minúsculas y sin tildes, para buscar sin preocuparse de acentos. */
export function normalizar(s) {
  return s.toLowerCase().normalize("NFD").replace(/\p{Mn}+/gu, "");
}

/** Quita la fecha del comienzo: «16 de noviembre de 1830, fallece…» → «Fallece…». */
export function quitarFecha(t) {
  const sin = t.replace(FECHA_INICIAL, "");
  return sin.charAt(0).toUpperCase() + sin.slice(1);
}

export function anioTexto(e) {
  return e.anio < 0 ? `${-e.anio} a. C.` : String(e.anio);
}

export function mesDia(e) {
  const [m, d] = e.fecha.split("-").map(Number);
  return { mes: m, dia: d };
}

/** «16 de noviembre» */
export function nombreDia(mes, dia) {
  return `${dia} de ${MESES[mes - 1]}`;
}

/** Identificador para guardar favoritas (no cambia aunque se añadan temas o fuentes). */
export function idDe(e) {
  let h = 0;
  for (const c of e.texto.slice(0, 60)) h = (h * 31 + c.codePointAt(0)) | 0;
  return `${e.fecha}|${e.anio}|${h}`;
}

export function esBisiesto(anio) {
  return (anio % 4 === 0 && anio % 100 !== 0) || anio % 400 === 0;
}

/**
 * Efemérides de un día, de la más antigua a la más reciente.
 * En los años no bisiestos, las del 29 de febrero se muestran el 28.
 */
export function delDia(lista, anio, mes, dia) {
  const claves = new Set([`${String(mes).padStart(2, "0")}-${String(dia).padStart(2, "0")}`]);
  if (mes === 2 && dia === 28 && !esBisiesto(anio)) claves.add("02-29");
  return lista.filter((e) => claves.has(e.fecha)).sort((a, b) => a.anio - b.anio);
}

function porFecha(a, b) {
  return a.fecha.localeCompare(b.fecha) || a.anio - b.anio;
}

/** Efemérides que contienen todas las palabras de la consulta y, si se indica, el tema. */
export function buscar(lista, consulta, tema = null) {
  const palabras = normalizar(consulta).split(/\s+/).filter(Boolean);
  return lista
    .filter((e) => (!tema || (e.temas || []).includes(tema)) &&
      palabras.every((p) => normalizar(`${e.titulo || ""} ${e.texto} ${e.extra || ""}`).includes(p)))
    .sort(porFecha);
}

/** Temas que aparecen, del más frecuente al menos. */
export function temas(lista) {
  const cuenta = new Map();
  for (const e of lista) for (const t of e.temas || []) cuenta.set(t, (cuenta.get(t) || 0) + 1);
  return [...cuenta.entries()].sort((a, b) => b[1] - a[1]).map(([t]) => t);
}

export function ordenar(lista) {
  return [...lista].sort(porFecha);
}

/** Texto para compartir en redes sociales. */
export function textoParaCompartir(e) {
  let t = (e.titulo ? `${e.titulo}\n\n` : "") + e.texto;
  if (e.video) t += `\n\n▶ ${e.video}`;
  return `${t}\n\n#CórdobaEnEfemérides`;
}

// ───────────── Juego ─────────────

export function candidatas(lista) {
  return lista.filter((e) => e.verificada && e.anio > 0 && quitarFecha(e.texto).length > 40);
}

/** Cambia el año que se pregunta por «…» si aparece en el texto, para no dar la respuesta. */
export function ocultarAnio(texto, anio) {
  return texto.replace(new RegExp(`\\b${anio}\\b`, "g"), "…");
}

/** Pregunta «¿En qué año ocurrió?» con cuatro opciones. `azar` devuelve números entre 0 y 1. */
export function nuevaPregunta(lista, azar = Math.random, anioMaximo = new Date().getFullYear()) {
  const posibles = candidatas(lista);
  const e = posibles[Math.floor(azar() * posibles.length)];
  const opciones = new Set([e.anio]);
  while (opciones.size < 4) {
    const distancia = (4 + Math.floor(azar() * 86)) * (azar() < 0.5 ? 1 : -1);
    const propuesta = e.anio + distancia;
    if (propuesta >= 1 && propuesta <= anioMaximo) opciones.add(propuesta);
  }
  const lista4 = [...opciones];
  for (let i = lista4.length - 1; i > 0; i--) {
    const j = Math.floor(azar() * (i + 1));
    [lista4[i], lista4[j]] = [lista4[j], lista4[i]];
  }
  return { efemeride: e, enunciado: ocultarAnio(quitarFecha(e.texto), e.anio), opciones: lista4, correcta: e.anio };
}
