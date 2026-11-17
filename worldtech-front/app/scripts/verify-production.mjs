import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import { join } from 'node:path';

const root = new URL('../dist/worldtech-web/browser/', import.meta.url);
const entries = await readdir(root, { recursive: true });
const scripts = entries.filter((path) => path.endsWith('.js'));
assert(scripts.length > 0, 'Production build missing');
for (const path of scripts) {
  const content = await readFile(new URL(path, root), 'utf8');
  assert(!content.includes('WT_DEV_CATALOGUE_ONLY'), `Development example leaked into ${path}`);
  assert(!content.includes('Composants de développement'), `Development page leaked into ${path}`);
  assert(!content.includes('WT_DEV_TICKETS_ONLY'), `Reference tickets leaked into ${path}`);
  assert(!content.includes('DEMO-2048'), `Synthetic ticket leaked into ${path}`);
  assert(!content.includes('Atelier Boréal'), `Synthetic account leaked into ${path}`);
}
assert(!entries.some((path) => path.endsWith('.map')), 'Production source maps unexpectedly published');
const config = JSON.parse(await readFile(new URL('runtime-config.json', root), 'utf8'));
assert.equal(config.apiBaseUrl, '/api/web/v1');
assert.equal(config.googleLoginEnabled, false);
assert.equal(config.telemetryEnabled, false);
assert(!Object.hasOwn(config, 'demoEnabled'));
assert((await readFile(new URL('3rdpartylicenses.txt', root), 'utf8')).length > 0, 'Runtime notices missing');
for (const path of ['licenses/ibm-plex-sans/LICENSE', 'licenses/lucide-angular/LICENSE']) {
  assert((await readFile(new URL(path, root), 'utf8')).length > 0, `License missing: ${path}`);
}
console.log(JSON.stringify({ productionScriptsChecked: scripts.length, devCatalogueIncluded: false,
  apiBaseUrl: config.apiBaseUrl, build: join('dist', 'worldtech-web', 'browser') }));
