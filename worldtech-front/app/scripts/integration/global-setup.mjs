import { spawn, spawnSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { mkdtemp, readFile } from 'node:fs/promises';
import { join, resolve, delimiter } from 'node:path';
import { createServer } from 'node:net';
import { createInterface } from 'node:readline';
import { tmpdir } from 'node:os';

export default async function setup() {
  await new Promise((accept, reject) => {
    const probe = createServer(); probe.once('error', reject); probe.listen(18083, '127.0.0.1', () => probe.close(accept));
  });
  const backend = resolve('../../backend');
  const temporary = await mkdtemp(join(process.env['LOCALAPPDATA'] ? join(process.env['LOCALAPPDATA'], 'Temp', 'opencode') : tmpdir(), 'worldtech-spring-'));
  const classpathFile = join(temporary, 'classpath.txt');
  const windows = process.platform === 'win32';
  const maven = windows ? spawnSync('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command', `& '.\\mvnw.cmd' -q test-compile dependency:build-classpath '-Dmdep.includeScope=test' '-Dmdep.outputFile=${classpathFile}'`], {
    cwd: backend, encoding: 'utf8', timeout: 240_000, env: { ...process.env, MAVEN_OPTS: '-Xmx512m' },
  }) : spawnSync('./mvnw', ['-q', 'test-compile', 'dependency:build-classpath', '-Dmdep.includeScope=test', `-Dmdep.outputFile=${classpathFile}`], { cwd: backend, encoding: 'utf8', timeout: 240_000 });
  if (maven.status !== 0) throw new Error(`Backend test compilation/classpath failed (${maven.error?.code ?? maven.status}). ` + (maven.stderr ?? maven.stdout ?? '').slice(-1500));
  const classpath = [resolve(backend, 'target/test-classes'), resolve(backend, 'target/classes'), (await readFile(classpathFile, 'utf8')).trim()].join(delimiter);
  const compiled = spawnSync('javac', ['-cp', classpath, '-d', temporary, resolve('scripts/integration/IdentityHarness.java')], { encoding: 'utf8', timeout: 60_000 });
  if (compiled.status !== 0) throw new Error('Frontend integration harness compilation failed: ' + compiled.stderr);
  const secret = () => randomBytes(24).toString('base64url') + ' test-only passphrase';
  const credentials = { clientEmail: 'client-fe03@example.test', clientPassword: secret(), adminEmail: 'admin-fe03@example.test', adminPassword: secret(),
    resetToken: randomBytes(32).toString('base64url'), verificationToken: randomBytes(32).toString('base64url') };
  process.env['WT_TEST_CLIENT_PASSWORD'] = credentials.clientPassword;
  process.env['WT_TEST_ADMIN_PASSWORD'] = credentials.adminPassword;
  process.env['WT_TEST_RESET_TOKEN'] = credentials.resetToken;
  process.env['WT_TEST_VERIFICATION_TOKEN'] = credentials.verificationToken;
  const child = spawn('java', ['-Xmx512m', '-cp', temporary + delimiter + classpath, 'IdentityHarness'], { cwd: backend, stdio: ['pipe', 'pipe', 'pipe'] });
  const lines = createInterface({ input: child.stdout });
  const ready = new Promise((accept, reject) => {
    const timeout = setTimeout(() => reject(new Error('Isolated Spring fixture startup timeout')), 180_000);
    lines.on('line', (line) => { if (line === 'WT_IDENTITY_READY') { clearTimeout(timeout); accept(); } });
    child.once('exit', (code) => { clearTimeout(timeout); reject(new Error(`Spring fixture exited before ready (${code}); no integration success claimed`)); });
    child.once('error', reject);
  });
  child.stderr.on('data', () => undefined); // No JDBC passwords, CSRF or response bodies in test logs.
  child.stdin.write(JSON.stringify(credentials) + '\n');
  try { await ready; } catch (error) { child.stdin.end(); throw error; }
  return async () => {
    child.stdin.end();
    await new Promise((accept) => { if (child.exitCode !== null) return accept(); child.once('exit', accept); });
    lines.close(); delete process.env['WT_TEST_CLIENT_PASSWORD']; delete process.env['WT_TEST_ADMIN_PASSWORD'];
    delete process.env['WT_TEST_RESET_TOKEN']; delete process.env['WT_TEST_VERIFICATION_TOKEN'];
  };
}
