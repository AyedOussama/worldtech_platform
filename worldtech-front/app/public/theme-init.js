// WorldTech: only a non-sensitive theme preference is read before the first paint.
(function () {
  var root = document.documentElement;
  var mode = 'system';
  try {
    var saved = window.localStorage.getItem('wt-theme');
    if (saved === 'light' || saved === 'dark' || saved === 'system') mode = saved;
  } catch (_) { /* Storage may be unavailable. */ }
  var dark = mode === 'dark';
  try {
    if (mode === 'system') dark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  } catch (_) { /* A light fallback remains usable. */ }
  root.classList.toggle('wt-dark', dark);
  root.style.colorScheme = dark ? 'dark' : 'light';
  root.dataset.wtTheme = mode;
})();
