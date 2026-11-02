import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs';
import { dirname, join, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const output = resolve(root, '.web-dist');
const files = ['index.html', 'favicon.svg', 'css', 'js', 'audio', 'models'];
for (const file of files) {
  if (!existsSync(join(root, file))) throw new Error('Missing web input: ' + file);
}
if (output !== join(root, '.web-dist') || !output.startsWith(root + sep)) {
  throw new Error('Unexpected static output directory');
}
rmSync(output, { recursive: true, force: true });
mkdirSync(output, { recursive: true });
for (const file of files) cpSync(join(root, file), join(output, file), { recursive: true });
console.log('Static web bundle created at ' + output);
