// Versión web de Córdoba en Efemérides: Hoy, Calendario, Buscar, Favoritas y Juego.
import {
  MESES, anioTexto, buscar, delDia, idDe, mesDia, nombreDia, nuevaPregunta, ordenar,
  quitarFecha, temas as listaTemas, textoParaCompartir,
} from "./logica.js";
import { tarjeta } from "./tarjeta.js";

const contenido = document.getElementById("contenido");
let todas = [];

// ───────── Preferencias guardadas en el navegador ─────────
function leer(clave, porDefecto) {
  try { const v = localStorage.getItem(clave); return v === null ? porDefecto : JSON.parse(v); }
  catch { return porDefecto; }
}
function guardar(clave, valor) {
  try { localStorage.setItem(clave, JSON.stringify(valor)); } catch { /* sin almacenamiento */ }
}
let favoritas = new Set(leer("favoritas", []));
let escala = leer("escala", 1);
document.documentElement.style.setProperty("--escala", escala);

// ───────── Estado ─────────
const hoy = new Date();
let dia = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
let mesCalendario = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
let consulta = "", temaElegido = null;
const juego = { pregunta: null, respuesta: null, aciertos: 0, jugadas: 0, racha: 0, record: leer("record", 0) };

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const mismoDia = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();

function avisar(texto) {
  const a = document.createElement("div");
  a.className = "aviso-flotante"; a.textContent = texto; a.setAttribute("role", "status");
  document.body.append(a);
  setTimeout(() => a.remove(), 2600);
}

// ───────── Tarjetas ─────────
function htmlTarjeta(e) {
  const id = idDe(e), fav = favoritas.has(id);
  return `<article class="tarjeta">
    <div class="cabecera-tarjeta">
      <p class="anio">${esc(anioTexto(e))}</p>
      <button class="estrella ${fav ? "activa" : ""}" data-fav="${esc(id)}"
        aria-label="${fav ? "Quitar de favoritas" : "Añadir a favoritas"}">${fav ? "★" : "☆"}</button>
    </div>
    ${e.temas?.length ? `<p class="temas">${esc(e.temas.join(" · "))}</p>` : ""}
    ${e.titulo ? `<p><strong>${esc(e.titulo)}</strong></p>` : ""}
    <p class="texto">${esc(e.texto)}</p>
    ${e.extra ? `<p class="extra">${esc(e.extra)}</p>` : ""}
    <div class="acciones">
      <button class="boton-texto" data-compartir="${esc(id)}">Compartir</button>
      ${e.video ? `<a class="boton-texto" href="${esc(e.video)}" target="_blank" rel="noopener">▶ Ver vídeo</a>` : ""}
      ${e.fuente ? `<a class="boton-texto" href="${esc(e.fuente)}" target="_blank" rel="noopener">Ver fuente</a>` : ""}
      ${e.verificada ? "" : `<span class="etiqueta">Pendiente de verificar</span>`}
    </div>
  </article>`;
}

function htmlFila(e) {
  const id = idDe(e), fav = favoritas.has(id), { mes, dia: d } = mesDia(e);
  return `<div class="fila" role="button" tabindex="0" data-abrir="${e.fecha}">
    <div class="info">
      <div class="fecha">${d} ${MESES[mes - 1].slice(0, 3)}. ${esc(anioTexto(e))}</div>
      <div class="resumen-texto">${esc(e.titulo || quitarFecha(e.texto))}</div>
    </div>
    <button class="estrella ${fav ? "activa" : ""}" data-fav="${esc(id)}"
      aria-label="${fav ? "Quitar de favoritas" : "Añadir a favoritas"}">${fav ? "★" : "☆"}</button>
  </div>`;
}

// ───────── Pantallas ─────────
function pantallaHoy() {
  const lista = delDia(todas, dia.getFullYear(), dia.getMonth() + 1, dia.getDate());
  contenido.innerHTML = `
    <div class="navegacion-dia">
      <button class="boton-texto flecha" data-mover="-1" aria-label="Día anterior">‹</button>
      <h2>${nombreDia(dia.getMonth() + 1, dia.getDate())}</h2>
      <button class="boton-texto flecha" data-mover="1" aria-label="Día siguiente">›</button>
    </div>
    <div class="fila-centrada">
      ${mismoDia(dia, hoy) ? "" : `<button class="boton-texto" data-hoy>Volver a hoy</button>`}
    </div>
    ${lista.length ? lista.map(htmlTarjeta).join("") : `<p class="vacio">Todavía no hay efemérides guardadas para este día.</p>`}`;
}

function pantallaCalendario() {
  const anio = mesCalendario.getFullYear(), mes = mesCalendario.getMonth();
  const diasMes = new Date(anio, mes + 1, 0).getDate();
  const hueco = (new Date(anio, mes, 1).getDay() + 6) % 7; // lunes = 0
  let celdas = ["L", "M", "X", "J", "V", "S", "D"].map((d) => `<div class="cabecera">${d}</div>`).join("");
  celdas += "<div></div>".repeat(hueco);
  for (let d = 1; d <= diasMes; d++) {
    const lista = delDia(todas, anio, mes + 1, d);
    const video = lista.some((e) => e.video);
    const esHoy = mismoDia(new Date(anio, mes, d), hoy);
    celdas += `<button class="celda ${lista.length ? "con" : ""} ${esHoy ? "hoy" : ""}" data-dia="${anio}-${mes + 1}-${d}"
      aria-label="${d} de ${MESES[mes]}: ${lista.length} efemérides">${d}
      ${lista.length ? `<small>${video ? "▶ " : ""}${lista.length}</small>` : ""}</button>`;
  }
  const titulo = `${MESES[mes][0].toUpperCase()}${MESES[mes].slice(1)} ${anio}`;
  contenido.innerHTML = `
    <div class="mes">
      <button class="boton-texto flecha" data-mes="-1" aria-label="Mes anterior">‹</button>
      <h2>${titulo}</h2>
      <button class="boton-texto flecha" data-mes="1" aria-label="Mes siguiente">›</button>
    </div>
    <div class="rejilla">${celdas}</div>
    <p class="leyenda">Los días en color tienen efemérides (el número pequeño indica cuántas). ▶ = tiene vídeo.</p>`;
}

function pantallaBuscar() {
  contenido.innerHTML = `
    <input class="campo-buscar" type="search" placeholder="Góngora, Mezquita, San Lorenzo…"
      aria-label="Buscar efemérides" value="${esc(consulta)}">
    <p class="resumen" style="margin-bottom:0">Temas</p>
    <div class="chips">${listaTemas(todas).map((t) =>
      `<button class="chip ${t === temaElegido ? "activo" : ""}" data-tema="${esc(t)}">${esc(t)}</button>`).join("")}</div>
    <div id="resultados"></div>`;
  const campo = contenido.querySelector("input");
  campo.addEventListener("input", () => { consulta = campo.value; resultados(); });
  resultados();
}

function resultados() {
  const caja = document.getElementById("resultados");
  if (!consulta.trim() && !temaElegido) {
    caja.innerHTML = `<p class="resumen">Escribe algo o elige un tema para buscar entre ${todas.length} efemérides.</p>`;
    return;
  }
  const lista = buscar(todas, consulta, temaElegido);
  const n = lista.length;
  caja.innerHTML = `<p class="resumen">${n === 0 ? "No hay efemérides que coincidan." : n === 1 ? "1 efeméride" : `${n} efemérides`}</p>
    ${lista.map(htmlFila).join("")}`;
}

function pantallaFavoritas() {
  const lista = ordenar(todas.filter((e) => favoritas.has(idDe(e))));
  contenido.innerHTML = lista.length ? lista.map(htmlFila).join("")
    : `<p class="vacio">Aún no tienes favoritas.<br><br>Toca la estrella ☆ de cualquier efeméride para guardarla aquí.</p>`;
}

function pantallaJuego() {
  if (!juego.pregunta) juego.pregunta = nuevaPregunta(todas);
  const p = juego.pregunta, r = juego.respuesta;
  const { mes, dia: d } = mesDia(p.efemeride);
  contenido.innerHTML = `
    <div class="marcador"><span>Aciertos: ${juego.aciertos} de ${juego.jugadas}</span>
      <span>Racha: ${juego.racha} · Récord: ${juego.record}</span></div>
    <h2 class="pregunta-titulo">¿En qué año ocurrió?</h2>
    <div class="tarjeta"><p class="texto">${esc(p.enunciado)}</p></div>
    <div class="opciones">${p.opciones.map((o) => {
      const clase = r === null ? "" : o === p.correcta ? "correcta" : o === r ? "fallada" : "";
      return `<button class="opcion ${clase}" data-opcion="${o}" ${r === null ? "" : "disabled"}>${o}</button>`;
    }).join("")}</div>
    ${r === null ? "" : `
      <p><strong>${r === p.correcta ? "¡Correcto! 🎉" : "¡Casi!"} Fue el ${nombreDia(mes, d)} de ${esc(anioTexto(p.efemeride))}.</strong></p>
      <div class="fila-centrada" style="justify-content:flex-start;gap:12px">
        <button class="boton" data-siguiente>Siguiente pregunta</button>
        <button class="boton secundario" data-compartir-juego>Compartir</button>
      </div>`}`;
}

const pantallas = { hoy: pantallaHoy, calendario: pantallaCalendario, buscar: pantallaBuscar, favoritas: pantallaFavoritas, juego: pantallaJuego };

function pestanaActual() {
  const p = location.hash.slice(1);
  return p in pantallas ? p : "hoy";
}

function pintar() {
  const p = pestanaActual();
  document.querySelectorAll(".pestanas a").forEach((a) => {
    const activa = a.dataset.pestana === p;
    a.classList.toggle("activa", activa);
    if (activa) a.setAttribute("aria-current", "page"); else a.removeAttribute("aria-current");
  });
  pantallas[p]();
}

// ───────── Compartir ─────────
async function compartir(e) {
  const texto = textoParaCompartir(e);
  try {
    const blob = await new Promise((ok) => tarjeta(e).toBlob(ok, "image/png"));
    const archivo = new File([blob], "efemeride.png", { type: "image/png" });
    if (navigator.canShare?.({ files: [archivo] })) {
      await navigator.share({ files: [archivo], text: texto });
      return;
    }
    if (navigator.share) { await navigator.share({ text: texto }); return; }
    // Ordenador sin menú de compartir: se descarga la imagen y se copia el texto.
    const enlace = document.createElement("a");
    enlace.href = URL.createObjectURL(blob); enlace.download = "efemeride.png"; enlace.click();
    await navigator.clipboard?.writeText(texto);
    avisar("Imagen descargada y texto copiado");
  } catch (error) {
    if (error?.name !== "AbortError") avisar("No se ha podido compartir");
  }
}

async function compartirTexto(texto) {
  try {
    if (navigator.share) await navigator.share({ text: texto });
    else { await navigator.clipboard?.writeText(texto); avisar("Texto copiado"); }
  } catch { /* cancelado */ }
}

// ───────── Eventos ─────────
contenido.addEventListener("click", (ev) => {
  const b = ev.target.closest("[data-fav],[data-compartir],[data-mover],[data-hoy],[data-mes],[data-dia],[data-tema],[data-abrir],[data-opcion],[data-siguiente],[data-compartir-juego]");
  if (!b) return;
  const ds = b.dataset;
  if (ds.fav !== undefined) {
    ev.stopPropagation();
    if (favoritas.has(ds.fav)) favoritas.delete(ds.fav); else favoritas.add(ds.fav);
    guardar("favoritas", [...favoritas]);
    if (pestanaActual() === "buscar") resultados(); else pintar();
  } else if (ds.compartir !== undefined) {
    const e = todas.find((x) => idDe(x) === ds.compartir);
    if (e) compartir(e);
  } else if (ds.mover !== undefined) {
    dia = new Date(dia.getFullYear(), dia.getMonth(), dia.getDate() + Number(ds.mover)); pintar();
  } else if (ds.hoy !== undefined) {
    dia = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate()); pintar();
  } else if (ds.mes !== undefined) {
    mesCalendario = new Date(mesCalendario.getFullYear(), mesCalendario.getMonth() + Number(ds.mes), 1); pintar();
  } else if (ds.dia !== undefined) {
    const [a, m, d] = ds.dia.split("-").map(Number);
    dia = new Date(a, m - 1, d); location.hash = "hoy";
  } else if (ds.tema !== undefined) {
    temaElegido = temaElegido === ds.tema ? null : ds.tema;
    contenido.querySelectorAll(".chip").forEach((c) => c.classList.toggle("activo", c.dataset.tema === temaElegido));
    resultados();
  } else if (ds.abrir !== undefined) {
    abrirFecha(ds.abrir);
  } else if (ds.opcion !== undefined && juego.respuesta === null) {
    const o = Number(ds.opcion);
    juego.respuesta = o; juego.jugadas++;
    if (o === juego.pregunta.correcta) {
      juego.aciertos++; juego.racha++;
      if (juego.racha > juego.record) { juego.record = juego.racha; guardar("record", juego.record); }
    } else juego.racha = 0;
    pintar();
  } else if (ds.siguiente !== undefined) {
    juego.pregunta = nuevaPregunta(todas); juego.respuesta = null; pintar();
  } else if (ds.compartirJuego !== undefined) {
    compartirTexto(`He acertado ${juego.aciertos} de ${juego.jugadas} preguntas sobre la historia de Córdoba ` +
      `(mi récord: ${juego.record} seguidas). ¿Te atreves? ${location.origin}${location.pathname}#juego #CórdobaEnEfemérides`);
  }
});
contenido.addEventListener("keydown", (ev) => {
  if ((ev.key === "Enter" || ev.key === " ") && ev.target.matches("[data-abrir]")) { ev.preventDefault(); abrirFecha(ev.target.dataset.abrir); }
});

function abrirFecha(fecha) {
  const [m, d] = fecha.split("-").map(Number);
  const anio = hoy.getFullYear();
  // El 29 de febrero se abre el 28 en los años no bisiestos.
  const diaReal = m === 2 && d === 29 && new Date(anio, 1, 29).getMonth() !== 1 ? 28 : d;
  dia = new Date(anio, m - 1, diaReal);
  location.hash = "hoy";
}

window.addEventListener("hashchange", () => { pintar(); window.scrollTo(0, 0); });

// Tamaño de letra
const dialogo = document.getElementById("dialogo-letra");
document.getElementById("boton-letra").addEventListener("click", () => {
  dialogo.querySelectorAll(".opciones-letra button").forEach((b) => b.classList.toggle("activa", Number(b.value) === escala));
  dialogo.showModal();
});
dialogo.addEventListener("close", () => {
  if (!dialogo.returnValue) return;
  escala = Number(dialogo.returnValue);
  document.documentElement.style.setProperty("--escala", escala);
  guardar("escala", escala);
});

// Aviso para instalarla en iPhone (solo en Safari de iPhone/iPad y si aún no está instalada)
const esIOS = /iphone|ipad|ipod/i.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
const instalada = window.matchMedia("(display-mode: standalone)").matches || navigator.standalone;
if (esIOS && !instalada && !leer("aviso-cerrado", false)) {
  const aviso = document.getElementById("aviso-instalar");
  aviso.hidden = false;
  document.getElementById("cerrar-aviso").addEventListener("click", () => { aviso.hidden = true; guardar("aviso-cerrado", true); });
}

// ───────── Arranque ─────────
async function cargar() {
  try {
    const r = await fetch("efemerides.json", { cache: "no-cache" });
    if (!r.ok) throw new Error(r.status);
    todas = await r.json();
    pintar();
  } catch {
    contenido.innerHTML = `<p class="vacio">No se han podido cargar las efemérides. Comprueba la conexión y vuelve a intentarlo.</p>`;
  }
}
cargar();

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => navigator.serviceWorker.register("sw.js").catch(() => {}));
}
