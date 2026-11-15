"""Convierte la hoja de cálculo «Efemérides de Córdoba» (exportada como CSV) al formato de la app.

Uso:  python3 herramientas/importar_hoja.py hoja.csv data/efemerides.json

Columnas esperadas: Fecha (dd/mm), Efeméride (texto que empieza por la fecha), Extra (opcional).
Las efemérides que ya están en el JSON y no vienen de la hoja se conservan.
"""
import csv
import json
import re
import sys

MESES = "enero febrero marzo abril mayo junio julio agosto septiembre octubre noviembre diciembre".split()
PATRON = re.compile(
    r"(\d{1,2})\s*(?:de\s+)?(" + "|".join(MESES) + r")\s+(?:d+el?\s+|d+e\s+)?(\d{1,4})\s*(a\.\s*C\.)?",
    re.IGNORECASE,
)


def convertir(fila):
    fecha_a, texto = fila[0].strip(), fila[1].strip()
    extra = fila[2].strip() if len(fila) > 2 else ""
    dia, mes = (int(x) for x in fecha_a.split("/"))
    m = PATRON.search(texto)
    if not m:
        raise ValueError(f"No encuentro el año en: {texto[:80]}")
    anio = int(m.group(3)) * (-1 if m.group(4) else 1)
    dia_texto, mes_texto = int(m.group(1)), MESES.index(m.group(2).lower()) + 1
    coincide = (dia_texto, mes_texto) == (dia, mes)
    e = {
        "fecha": f"{mes:02d}-{dia:02d}",
        "anio": anio,
        "texto": texto,
        "fuente": "",
        "origen": "hoja",
        # Son efemérides recopiladas por el autor; solo se marcan para revisar
        # si la fecha de la columna A no coincide con la del texto.
        "verificada": coincide,
    }
    if extra:
        e["extra"] = extra
    return e, coincide


def main(csv_path, json_path):
    filas = list(csv.reader(open(csv_path, encoding="utf-8")))[1:]
    nuevas, avisos = [], []
    for f in filas:
        if len(f) < 2 or not f[1].strip():
            continue
        e, coincide = convertir(f)
        if not coincide:
            avisos.append(f"{f[0]}: la fecha del texto no coincide → {f[1][:70]}")
        nuevas.append(e)

    actuales = json.load(open(json_path, encoding="utf-8"))
    conservadas = [e for e in actuales if e.get("origen") != "hoja"]
    claves_hoja = {(e["fecha"], e["anio"]) for e in nuevas}
    repetidas = [e for e in conservadas if (e["fecha"], e["anio"]) in claves_hoja]
    conservadas = [e for e in conservadas if (e["fecha"], e["anio"]) not in claves_hoja]

    todas = sorted(conservadas + nuevas, key=lambda e: (e["fecha"], e["anio"]))
    with open(json_path, "w", encoding="utf-8") as out:
        json.dump(todas, out, ensure_ascii=False, indent=2)
        out.write("\n")

    print(f"{len(nuevas)} efemérides de la hoja, {len(conservadas)} anteriores conservadas, total {len(todas)}")
    for e in repetidas:
        print(f"Sustituida por la versión de la hoja: {e['fecha']} {e['anio']} {e.get('titulo', '')}")
    for a in avisos:
        print("REVISAR", a)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
