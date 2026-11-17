#!/usr/bin/env python3
"""Résout les tags candidats par Docker ; échoue si un digest manque."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def candidates(path: Path) -> dict[str, str]:
    result = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        key, separator, value = line.partition('=')
        if not separator or not re.fullmatch(r'[A-Z_]+', key):
            raise ValueError('Entrée image invalide')
        if key in result or not re.fullmatch(r'[a-zA-Z0-9./:_-]+', value):
            raise ValueError('Image candidate invalide ou dupliquée')
        tag = value.rsplit(':', 1)[-1].lower()
        if ':' not in value or any(word in tag for word in ('latest', 'snapshot', 'rc', 'alpha', 'beta')):
            raise ValueError('Un tag stable explicite est requis')
        result[key] = value
    expected = {'DB_IMAGE', 'MAILPIT_IMAGE', 'GARAGE_IMAGE', 'JAVA_RUNTIME_IMAGE'}
    if set(result) != expected:
        raise ValueError('La liste des images attendues ne correspond pas')
    return result


def resolve(key: str, ref: str) -> str:
    command = ['docker', 'pull']
    if key == 'DB_IMAGE':
        command += ['--platform', 'linux/amd64']
    subprocess.run(command + [ref], check=True)
    raw = subprocess.check_output(
        ['docker', 'image', 'inspect', '--format', '{{json .RepoDigests}}', ref], text=True)
    digests = json.loads(raw) or []
    repo = ref.rsplit(':', 1)[0]
    matches = [d for d in digests
               if re.fullmatch(re.escape(repo) + r'@sha256:[0-9a-f]{64}', d)]
    if not matches:
        raise RuntimeError(f'Aucun digest vérifiable pour {key}')
    return sorted(matches)[0]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--refresh', action='store_true', help='Explicitly resolve and replace an existing public lock.')
    args = parser.parse_args()
    output = ROOT / 'infra/images.lock.env'
    if output.exists() and not args.refresh:
        read_lock(output)
        print('Lock existant conservé ; registre non revalidé. Utiliser --refresh pour une mise à jour explicite.')
        return
    if not shutil.which('docker'):
        raise SystemExit('Docker absent : aucun lock généré. Installer/configurer Docker puis relancer.')
    images = candidates(ROOT / 'infra/source-images.env')
    resolved = {key: resolve(key, ref) for key, ref in images.items()}
    temp = output.with_suffix('.env.tmp')
    temp.write_text('# Digests réellement résolus. Revoir/scanner avant utilisation.\n' +
                    ''.join(f'{key}={value}\n' for key, value in resolved.items()), encoding='utf-8', newline='\n')
    os.replace(temp, output)
    print('Lock généré. Vérifier les patches réels et les vulnérabilités avant validation.')


def read_lock(path: Path) -> dict[str, str]:
    values = {}
    expected = candidates(ROOT / 'infra/source-images.env')
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        key, separator, value = line.partition('=')
        if not separator or key not in expected or key in values:
            raise ValueError('Invalid or duplicate image lock entry.')
        repo = expected[key].rsplit(':', 1)[0]
        if not re.fullmatch(re.escape(repo) + r'@sha256:[0-9a-f]{64}', value):
            raise ValueError('Expected a real digest for the configured image repository.')
        values[key] = value
    if values.keys() != expected.keys():
        raise ValueError('Incomplete image lock.')
    return values


if __name__ == '__main__':
    main()
