// Shared data and helpers for the three launcher concepts.
// Banners in banners/ are crops of the real apps' leanback banners from the owner's TCL G08.
// Apps with `synth` are stand-ins (synthetic) for banners that were not captured.
const B = "banners/";
const FAVS = [
  { name: "TizenTube", img: B + "tizentube.jpg", glow: "#4fd2f2" },
  { name: "Jellyfin", img: B + "jellyfin.jpg", glow: "#7b5cff" },
  { name: "Netflix", img: B + "netflix.jpg", glow: "#e50914" },
  { name: "Prime Video", img: B + "prime.jpg", glow: "#1f8fff" },
];
const LIB = [
  { name: "Apple TV", img: B + "appletv.jpg", glow: "#b8b8c2" },
  { name: "Disney+", img: B + "disney.jpg", glow: "#18b6b9" },
  { name: "atresplayer", img: B + "atresplayer.jpg", glow: "#ff3a6b" },
  { name: "RTVE Play", img: B + "rtve.jpg", glow: "#ff6a00" },
  { name: "Twitch", synth: { bg: "#9146ff", fg: "#fff", text: "twitch" }, glow: "#9146ff" },
  { name: "TV Player", synth: { bg: "#1b1d22", fg: "#e9e9e9", text: "TV Player" }, glow: "#8a9099" },
  { name: "Google Play Store", img: B + "playstore.jpg", glow: "#34a853" },
  { name: "Downloader", synth: { bg: "#f08a24", fg: "#1a1206", text: "Downloader" }, glow: "#f08a24" },
  { name: "Galería", synth: { bg: "#23262c", fg: "#e9e9e9", text: "Galería" }, glow: "#8a9099" },
  { name: "Reproductor", synth: { bg: "#2a2d33", fg: "#e9e9e9", text: "Reproductor" }, glow: "#8a9099" },
  { name: "Quick Actions", synth: { bg: "#11161d", fg: "#7fb2ff", text: "Quick Actions" }, glow: "#4c8dff" },
  { name: "Ajustes", img: B + "settings.jpg", glow: "#8a9099" },
];
const INPUTS = [
  { name: "HDMI 1", icon: "hdmi" }, { name: "HDMI 2", icon: "hdmi" },
  { name: "HDMI 3", icon: "hdmi" }, { name: "HDMI 4", icon: "hdmi", note: "ARC" },
  { name: "TV en vivo", icon: "tv" },
];
const SSID = "surveillance-drone-02";

const svg = (body) => `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">${body}</svg>`;
const ICONS = {
  inputs: svg('<path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><path d="m10 17 5-5-5-5"/><path d="M15 12H3"/>'),
  wifi: svg('<path d="M12 20h.01"/><path d="M2 8.82a15 15 0 0 1 20 0"/><path d="M5 12.86a10 10 0 0 1 14 0"/><path d="M8.5 16.43a5 5 0 0 1 7 0"/>'),
  settings: svg('<path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/>'),
  hdmi: svg('<path d="M3 8h18v5l-3 3H6l-3-3z"/><path d="M7 11h10"/>'),
  tv: svg('<rect x="2" y="7" width="20" height="14" rx="2"/><path d="m17 2-5 5-5-5"/>'),
  home: svg('<path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/>'),
  grid: svg('<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>'),
};

// Artwork element for an app (real banner or synthetic stand-in)
function cardEl(app, synthSize = 40) {
  const c = document.createElement("div");
  c.className = "card";
  if (app.img) c.style.backgroundImage = `url("${app.img}")`;
  else {
    c.classList.add("synthetic");
    c.style.background = app.synth.bg;
    c.style.color = app.synth.fg;
    c.style.fontSize = synthSize + "px";
    c.textContent = app.synth.text;
  }
  return c;
}

// Mix a hex colour with black, keeping `k` of the colour
function shade(hex, k) {
  const n = parseInt(hex.slice(1), 16);
  return `rgb(${Math.round(((n >> 16) & 255) * k)}, ${Math.round(((n >> 8) & 255) * k)}, ${Math.round((n & 255) * k)})`;
}
function rgba(hex, a) {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${a})`;
}

function fitStage(stage, onFit) {
  const fit = () => {
    const k = Math.min(innerWidth / 1920, innerHeight / 1080);
    stage.style.transform = `translate(-50%, -50%) scale(${k})`;
    onFit && onFit();
  };
  addEventListener("resize", fit);
  fit();
}

function startClock() {
  const tick = () => {
    const d = new Date();
    document.querySelectorAll(".js-time").forEach(e => e.textContent = d.toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" }));
    document.querySelectorAll(".js-date").forEach(e => e.textContent = d.toLocaleDateString("es-ES", { weekday: "long", day: "numeric", month: "long" }));
  };
  tick(); setInterval(tick, 10000);
}

const PAGES = [["escenario.html", "Escenario"], ["panel.html", "Panel"], ["lista.html", "Lista"]];
function mountChrome(current) {
  const bar = document.createElement("nav");
  bar.className = "switcher";
  PAGES.forEach(([href, label], i) => {
    const a = document.createElement("a");
    a.href = href; a.innerHTML = `<kbd>${i + 1}</kbd>${label}`;
    if (href === current) a.className = "on";
    bar.append(a);
  });
  const hint = document.createElement("div");
  hint.className = "hint";
  hint.textContent = "Flechas para moverte · Enter para abrir · Esc para volver · 1-3 cambia de diseño";
  document.body.append(bar, hint);
  setTimeout(() => hint.classList.add("gone"), 6000);
  addEventListener("keydown", e => {
    const i = "123".indexOf(e.key);
    if (i >= 0 && e.key.length === 1) location.href = PAGES[i][0];
    else hint.classList.add("gone");
  });
}

const NAV_KEYS = ["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Enter", "Escape", "Backspace"];
