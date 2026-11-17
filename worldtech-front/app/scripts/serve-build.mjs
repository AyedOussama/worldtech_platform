// Local smoke-test server only, not a production deployment or backend substitute.
import { createServer } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { resolve, extname, sep } from 'node:path';

const root = resolve('dist/worldtech-web/browser');
const mime = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.ico': 'image/x-icon',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.webp': 'image/webp',
  '.woff2': 'font/woff2', '.txt': 'text/plain; charset=utf-8' };

createServer(async (request, response) => {
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    response.writeHead(405).end();
    return;
  }
  try {
    const pathname = decodeURIComponent(new URL(request.url ?? '/', 'http://127.0.0.1').pathname);
    if (pathname === '/api' || pathname.startsWith('/api/')) {
      response.writeHead(502, { 'Content-Type': 'application/problem+json', 'Cache-Control': 'no-store' });
      response.end(JSON.stringify({ status: 502, code: 'BACKEND_NOT_CONNECTED' }));
      return;
    }
    let file = resolve(root, '.' + pathname);
    if (file !== root && !file.startsWith(root + sep)) {
      response.writeHead(400).end();
      return;
    }
    try {
      if (!(await stat(file)).isFile()) file = resolve(root, 'index.html');
    } catch {
      if (extname(pathname)) {
        response.writeHead(404).end();
        return;
      }
      file = resolve(root, 'index.html');
    }
    const data = await readFile(file);
    const entry = file.endsWith('index.html') || file.endsWith('runtime-config.json') || file.endsWith('theme-init.js');
    response.writeHead(200, {
      'Content-Type': mime[extname(file)] ?? 'application/octet-stream',
      'Cache-Control': entry ? 'no-store' : 'public, max-age=31536000, immutable',
      'X-Content-Type-Options': 'nosniff',
      'Referrer-Policy': 'no-referrer',
    });
    response.end(request.method === 'HEAD' ? undefined : data);
  } catch {
    response.writeHead(400).end();
  }
}).listen(4173, '127.0.0.1', () => console.log('WorldTech smoke server: http://127.0.0.1:4173'));
