"""Propone temas para cada efeméride a partir de palabras clave (se puede revisar a mano después).

Uso:  python3 herramientas/temas.py data/efemerides.json
Solo rellena «temas» en las efemérides que todavía no tienen; las ya revisadas no se tocan.
"""
import json
import re
import sys
import unicodedata

# «Roma» se asigna por fecha (antes del año 711) y «Al-Ándalus» también entre 711 y 1236.
TEMAS = {
    "Al-Ándalus": r"\bcalifa|\bemir\b|emirato|abderraman|almanzor|\bomeya|averroes|maimonides|\bibn\b|medina azahara|hammudi|alfaqui|ulema|\bzoco\b|mozarab|andalusi|boabdil|\bmoriscos?\b|mudejares",
    "Religión": r"iglesia|convento|cofradia|hermandad|obispo|\bmartir|\bvirgen\b|ermita|parroquia|monjas|frailes|\bbeat[oa]|\bbula\b|procesion|santuario|capuchin|arcediano|santos martires|\bsanto\b|seminario|jesuita|sagrado corazon|dominico|carmelita|clarisa|rogativa|reliquias|canonigo|cabildo catedralicio|cardenal|prelado|inquisicion|custodia",
    "Política y gobierno": r"alcalde|ayuntamiento|diputad|gobernador|republica|elecciones|constitucion|ministro|\bjunta\b|politic|\bcortes\b|decreto|estatuto de autonomia|referendum|corregidor|concejo|\brey\b|\breina\b|reyes catolicos|felipe|carlos i|alfonso x|isabel ii|fernando|sancho iv|enrique iv|privilegio|titulo de|socialista|cantones|sexenio",
    "Guerras y conflictos": r"gran capitan|batalla|tropas|\bguerra\b|guerra civil|franceses|frances\b|fusilad|asedio|ejercito|militar|sublevacion|subleva|bombard|atentado|\beta\b|ajusticiad|ahorcad|reconquista|conquista|revolucion|division azul|annual|cautiverio|estado de guerra|milicia|levantamiento|frente popular",
    "Arte y cultura": r"pintor|escultor|poeta|music|escritor|teatro|museo|\bcine\b|cinema|literatura|artista|imaginero|compositor|pianista|filosofo|periodist|academia|fotograf|poetisa|orfebre|platero|humanista|cronista|historiador|opera|galeria de arte|biblioteca|equipo 57|culteranismo|romantic|diario de cordoba|radio cordoba|emisora|equipo|arquitecto",
    "Monumentos y urbanismo": r"puente|plaza\b|plaza de|\bcalle\b|murallon|muralla|\bpuerta\b|\btorre\b|jardines|triunfo|estacion|mercado|palacio|fuente|cementerio|paseo|avenida|pgou|ordenacion urbana|alcazar|mezquita|catedral|glorieta|estatua|monumento|reloj|campanas|casa de las|embalse|pantano",
    "Toros": r"torer|\btoro\b|plaza de toros|novillero|toreo|manolete|lagartijo|machaquito|\bcoso\b|corridas",
    "Ciencia, salud y educación": r"medic|hospital|cirujano|colegio|instituto|escuela|universidad|facultad|veterinari|epidemia|\bpeste\b|trasplante|maestra|catedra|matematic|asilo|enseñanza|penicilina|doctora|teologia|conservatorio|expositos|manicomio|dentista",
    "Sucesos": r"incendi|\barde\b|riada|tormenta|\brayo\b|accidente|ciclon|crecida|inunda|explosion|derrumb|desploma|se hunde|lluvias|incedio",
    "Economía y vida cotidiana": r"\bferia\b|fabrica|comerci|\bbar\b|restaurante|\btren\b|ferroviari|via ferrea|luz electrica|telefon|caja de ahorros|monte de piedad|impuesto|consumos|futbol|cordoba cf|deportivo|hotel|lonja|cooperativa|galerias preciados|centro comercial|plateros|bodegas|abastos|autobus|autogiro|aerodromo|minas|mineros|huelga|cabalgata|batalla de las flores|patios|navegable",
}


def normalizar(t):
    t = unicodedata.normalize("NFD", t.lower())
    return "".join(c for c in t if unicodedata.category(c) != "Mn")


def proponer(e):
    t = normalizar(e.get("titulo", "") + " " + e["texto"])
    puntos = []
    for tema, patron in TEMAS.items():
        n = len(re.findall(patron, t))
        if n:
            puntos.append((n, tema))
    if 711 <= e["anio"] <= 1236 and not any(t == "Al-Ándalus" for _, t in puntos):
        puntos.append((1, "Al-Ándalus"))
    puntos.sort(key=lambda p: -p[0])
    if e["anio"] < 711:
        puntos.insert(0, (99, "Roma"))
    temas = [tema for _, tema in puntos[:2]]
    return temas or ["Economía y vida cotidiana"]


def main(ruta):
    d = json.load(open(ruta, encoding="utf-8"))
    for e in d:
        if not e.get("temas"):
            e["temas"] = proponer(e)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")
    from collections import Counter
    c = Counter(t for e in d for t in e["temas"])
    for tema, n in c.most_common():
        print(f"{n:4d}  {tema}")


if __name__ == "__main__":
    main(sys.argv[1])
