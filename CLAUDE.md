# Notas para trabajar en este proyecto

- **Cordobapedia** está ahora en **https://cordobapedia.datta.capital/wiki/Portada** (la antigua dirección `cordobapedia.wikanda.es` ya no funciona). Usar siempre la nueva.
  - Páginas en bruto: `https://cordobapedia.datta.capital/wiki/index.php?title=TÍTULO&action=raw`
  - API: `https://cordobapedia.datta.capital/wiki/api.php` (la búsqueda de texto puede no devolver resultados; `list=allpages&apprefix=` sí funciona).
- Las efemérides están en `data/efemerides.json`. Cada texto empieza por la fecha («7 de diciembre de 1958, …») y no debe ser muy largo (unos 240 caracteres de media; las más largas rondan 400).
- No inventar datos: lo que se añada a una efeméride debe salir de una fuente (Wikipedia, Cordobapedia, prensa) o de otras efemérides de la colección.
