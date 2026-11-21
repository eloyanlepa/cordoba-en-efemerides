// Guarda la app en el móvil para que funcione sin conexión.
// Las efemérides se piden primero a internet (para estar al día) y, si no hay conexión, se usa la copia guardada.
const VERSION = "efemerides-v1";
const ARCHIVOS = ["./", "index.html", "estilo.css", "app.js", "logica.js", "tarjeta.js",
  "manifest.webmanifest", "efemerides.json", "iconos/icono-180.png", "iconos/icono-192.png", "iconos/icono-512.png"];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(VERSION).then((c) => c.addAll(ARCHIVOS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(caches.keys()
    .then((claves) => Promise.all(claves.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener("fetch", (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== "GET" || url.origin !== location.origin) return;
  // Primero internet, y si falla, la copia guardada.
  e.respondWith(
    fetch(e.request)
      .then((r) => {
        if (r.ok) { const copia = r.clone(); caches.open(VERSION).then((c) => c.put(e.request, copia)); }
        return r;
      })
      .catch(() => caches.match(e.request, { ignoreSearch: true }).then((r) => r || caches.match("index.html")))
  );
});
