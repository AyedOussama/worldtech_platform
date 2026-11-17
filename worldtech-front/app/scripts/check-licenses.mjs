import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';

// Reviewed open-source licenses, all allowing commercial use without paid seats.
// MPL and attribution obligations still apply; "free" does not mean "without conditions".
const allowed = new Set([
  'MIT', 'ISC', 'Apache-2.0', 'BSD-2-Clause', 'BSD-3-Clause', '0BSD',
  'CC-BY-4.0', 'BlueOak-1.0.0', 'MPL-2.0', 'OFL-1.1', '(Apache-2.0 AND BSD-3-Clause)',
]);
const lock = JSON.parse(readFileSync(new URL('../package-lock.json', import.meta.url), 'utf8'));
const reviewed = [
  ['@lucide/angular', '1.71.0', 'b495047bd93a9b06913511076f504daba17d5bbeb3e0650f3bb53a4220329c57'],
  ['@fontsource/ibm-plex-sans', '5.3.0', 'd0283623ef57e722fd0eb688a8041589670c608ab780cd3612d06ba6f153d3fd'],
  ['@axe-core/playwright', '4.14.0', '812e9d96e900a093ae4d1d3f22c5f82f568a0a0461c3007a99d00573d41c5461'],
  ['@hey-api/openapi-ts', '0.97.0', '7354457f471fda3d2ef7ae7b7dc758c98a57054875ccc1e211b950646b03bf10', 'LICENSE.md'],
];
for (const [name, version, sha256, file = 'LICENSE'] of reviewed) {
  assert.equal(lock.packages[`node_modules/${name}`]?.version, version, `New version needs a license review: ${name}`);
  const license = readFileSync(new URL(`../node_modules/${name}/${file}`, import.meta.url));
  assert.equal(createHash('sha256').update(license).digest('hex'), sha256, `License changed: ${name}`);
}
const counts = {};
for (const [path, info] of Object.entries(lock.packages)) {
  if (!path) continue;
  assert(!/node_modules\/(primeng|primeicons|@primeuix\/|@primeui\/|@primeicons\/)/.test(path), `Excluded supplier: ${path}`);
  assert(allowed.has(info.license), `License requires review: ${path}: ${info.license ?? 'missing'}`);
  counts[info.license] = (counts[info.license] ?? 0) + 1;
}
console.log(JSON.stringify({ scope: 'locked_package_license_metadata', primeUiPackages: 0, licenses: counts }, null, 2));
