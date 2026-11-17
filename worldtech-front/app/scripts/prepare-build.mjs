import { copyFile } from 'node:fs/promises';

// Keep the compiler-generated runtime license notices in the published static directory.
await copyFile(
  new URL('../dist/worldtech-web/3rdpartylicenses.txt', import.meta.url),
  new URL('../dist/worldtech-web/browser/3rdpartylicenses.txt', import.meta.url),
);
