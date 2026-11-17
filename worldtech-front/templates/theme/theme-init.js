/* WorldTech v1: initial preference only. Angular owns changes after bootstrap. */
(function initWorldTechTheme() {
  var root = document.documentElement;
  var mode = "system";
  try {
    var saved = window.localStorage.getItem("wt-theme");
    if (saved === "light" || saved === "dark" || saved === "system") {
      mode = saved;
    }
  } catch (_) {
    /* Storage may be unavailable; system remains the default. */
  }
  var systemDark = false;
  if (mode === "system") {
    try {
      systemDark =
        typeof window.matchMedia === "function" &&
        window.matchMedia("(prefers-color-scheme: dark)").matches;
    } catch (_) {
      systemDark = false;
    }
  }
  var dark = mode === "dark" || (mode === "system" && systemDark);
  root.classList.toggle("wt-dark", Boolean(dark));
  root.style.colorScheme = dark ? "dark" : "light";
  root.dataset.wtTheme = mode;
})();

