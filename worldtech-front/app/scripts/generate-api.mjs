import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { mkdtemp, readFile, readdir, mkdir, copyFile, rm } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { createClient } from '@hey-api/openapi-ts';

const source = new URL('../../backend/openapi/maintenance-api.json', new URL('../', import.meta.url));
const snapshot = new URL('../api-contracts/maintenance-api.json', import.meta.url);
let sourcePresent = true;
const raw = await readFile(source).catch((error) => {
  if (error.code !== 'ENOENT') throw error;
  sourcePresent = false; return readFile(snapshot);
});
const spec = JSON.parse(raw);
assert.equal(spec.openapi, '3.1.0');
// Never rewrite the source spec or invent operationIds: the generator derives method/path names.
const target = resolve('src/app/api/generated');
const temporary = await mkdtemp(join(process.env['LOCALAPPDATA'] ? join(process.env['LOCALAPPDATA'], 'Temp', 'opencode') : tmpdir(), 'worldtech-api-'));
try {
  await createClient({ input: spec, output: { path: temporary }, plugins: ['@hey-api/typescript'] });
  const files = (await readdir(temporary)).filter((name) => name.endsWith('.ts')).sort();
  assert(files.includes('types.gen.ts'));
  const provenance = JSON.stringify({ generator: '@hey-api/openapi-ts@0.97.0', source: '../backend/openapi/maintenance-api.json',
    sourceSha256: createHash('sha256').update(raw).digest('hex'), openapi: spec.openapi, apiVersion: spec.info.version }, null, 2) + '\n';
  if (process.argv.includes('--check')) {
    if (sourcePresent) assert.equal(await readFile(snapshot, 'utf8'), raw.toString('utf8'), 'Backend contract changed; review before regeneration');
    for (const name of files) assert.equal(await readFile(join(target, name), 'utf8'), await readFile(join(temporary, name), 'utf8'), `Regenerate API: ${name}`);
    assert.equal(await readFile(join(target, 'provenance.json'), 'utf8'), provenance, 'API source changed; review and regenerate');
  } else {
    await mkdir(new URL('../api-contracts/', import.meta.url), { recursive: true });
    if (sourcePresent) await copyFile(source, snapshot);
    await mkdir(target, { recursive: true });
    for (const name of files) await copyFile(join(temporary, name), join(target, name));
    const { writeFile } = await import('node:fs/promises');
    await writeFile(join(target, 'provenance.json'), provenance);
  }
  console.log(JSON.stringify({ mode: process.argv.includes('--check') ? 'verified' : 'generated', files, apiVersion: spec.info.version }));
} finally {
  // Only this invocation's newly created temporary output, never a user directory.
  await rm(temporary, { recursive: true, force: true });
}
