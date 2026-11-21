// Pruebas de la lógica de la versión web. Se ejecutan con:  node --test web/
import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  quitarFecha, buscar, temas, delDia, nuevaPregunta, idDe, anioTexto, textoParaCompartir,
} from "./logica.js";

const e = (fecha, anio, texto, extra = {}) => ({ fecha, anio, texto, verificada: true, temas: [], ...extra });

test("quita la fecha inicial", () => {
  assert.equal(quitarFecha("16 de noviembre de 1830, fallece en Londres."), "Fallece en Londres.");
  assert.equal(quitarFecha("2 de julio de 942, muere en Córdoba."), "Muere en Córdoba.");
  assert.equal(quitarFecha("17 de marzo del 45 a.C., batalla de Munda."), "Batalla de Munda.");
  assert.equal(quitarFecha("10 de junio de 1245 fallece D. Lope."), "Fallece D. Lope.");
  assert.equal(quitarFecha("En la noche del 24 de diciembre de 1235, Álvaro salta."), "Álvaro salta.");
  assert.equal(quitarFecha("25 de marzo de 1867, nace el escultor."), "Nace el escultor.");
});

test("busca sin tildes ni mayúsculas y por tema", () => {
  const lista = [
    e("05-23", 1627, "Muere Luis de Góngora en Córdoba.", { temas: ["Arte y cultura"] }),
    e("07-13", 1857, "Se localizan los restos de GÓNGORA.", { temas: ["Religión"] }),
    e("01-01", 1919, "Manifiesto andalucista."),
  ];
  assert.equal(buscar(lista, "gongora").length, 2);
  assert.equal(buscar(lista, "gongora cordoba").length, 1);
  assert.equal(buscar(lista, "", "Religión").length, 1);
  assert.deepEqual(temas(lista).sort(), ["Arte y cultura", "Religión"]);
});

test("el 29 de febrero sale el 28 en años no bisiestos", () => {
  const lista = [e("02-28", 1900, "Del 28."), e("02-29", 1964, "Del 29.")];
  assert.deepEqual(delDia(lista, 2026, 2, 28).map((x) => x.anio), [1900, 1964]);
  assert.deepEqual(delDia(lista, 2028, 2, 28).map((x) => x.anio), [1900]);
  assert.deepEqual(delDia(lista, 2028, 2, 29).map((x) => x.anio), [1964]);
});

test("pregunta con cuatro opciones distintas, la correcta y sin el año en el texto", () => {
  const lista = [e("04-29", 1953, "29 de abril de 1953, se inaugura el puente de San Rafael, que en 1953 fue el más ancho de España.")];
  for (let semilla = 1; semilla <= 50; semilla++) {
    let s = semilla;
    const azar = () => ((s = (s * 16807) % 2147483647) / 2147483647);
    const p = nuevaPregunta(lista, azar, 2026);
    assert.equal(new Set(p.opciones).size, 4);
    assert.ok(p.opciones.includes(1953));
    assert.ok(!p.enunciado.includes("1953"));
    assert.ok(p.enunciado.startsWith("Se inaugura"));
  }
});

test("formatos de año, identificador y texto para compartir", () => {
  assert.equal(anioTexto({ anio: -45 }), "45 a. C.");
  const a = e("01-01", 1919, "Manifiesto andalucista.");
  assert.equal(idDe(a), idDe({ ...a, temas: ["Política y gobierno"] }));
  assert.equal(textoParaCompartir({ ...a, video: "https://x/y" }),
    "Manifiesto andalucista.\n\n▶ https://x/y\n\n#CórdobaEnEfemérides");
});

test("el archivo de efemérides tiene todos los días del año", () => {
  const datos = JSON.parse(readFileSync(new URL("../data/efemerides.json", import.meta.url), "utf8"));
  const dias = new Set(datos.map((x) => x.fecha));
  assert.equal(dias.size, 366);
  for (const x of datos) assert.match(x.fecha, /^\d{2}-\d{2}$/);
});
