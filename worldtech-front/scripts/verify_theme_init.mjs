#!/usr/bin/env node
// Behavioral checks for the reference initializer only, not Angular.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const source = readFileSync(fileURLToPath(new URL("../templates/theme/theme-init.js", import.meta.url)), "utf8");
const cases = [
  { name: "explicit dark", saved: "dark", systemDark: false, dark: true, mode: "dark" },
  { name: "explicit light", saved: "light", systemDark: true, dark: false, mode: "light" },
  { name: "system dark", saved: "system", systemDark: true, dark: true, mode: "system" },
  { name: "system light", saved: "system", systemDark: false, dark: false, mode: "system" },
  { name: "first visit", saved: null, systemDark: true, dark: true, mode: "system" },
  { name: "invalid saved preference", saved: "invalid", systemDark: true, dark: true, mode: "system" },
  { name: "storage unavailable", storageThrows: true, systemDark: true, dark: true, mode: "system" },
  { name: "media API unavailable", saved: null, noMedia: true, dark: false, mode: "system" },
  { name: "media API throws", saved: "system", mediaThrows: true, dark: false, mode: "system" }
];
for (const test of cases) {
  const classState = new Map();
  const root = { classList: { toggle: (name, state) => classState.set(name, state) }, style: {}, dataset: {} };
  const browser = {
    localStorage: { getItem(key) {
      assert.equal(key, "wt-theme");
      if (test.storageThrows) throw new Error("Storage blocked");
      return test.saved;
    }}
  };
  if (!test.noMedia) browser.matchMedia = query => {
    assert.equal(query, "(prefers-color-scheme: dark)");
    if (test.mediaThrows) throw new Error("Media blocked");
    return { matches: Boolean(test.systemDark) };
  };
  vm.runInNewContext(source, { document: { documentElement: root }, window: browser }, { timeout: 1000 });
  assert.equal(classState.get("wt-dark"), test.dark, test.name);
  assert.equal(root.style.colorScheme, test.dark ? "dark" : "light", test.name);
  assert.equal(root.dataset.wtTheme, test.mode, test.name);
}
console.log(JSON.stringify({ ok: true, scope: "reference_theme_initializer_only", cases: cases.length }));

