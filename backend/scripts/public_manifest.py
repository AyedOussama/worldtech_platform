"""Public source inventory only; never package generated artifacts or private configuration."""
import argparse
import hashlib
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
DIRECTORIES = {'.github', '.mvn', 'docs', 'infra', 'openapi', 'prompts', 'references', 'scripts', 'src'}
ROOT_FILES = {
    '.dockerignore', '.gitattributes', '.gitignore', 'AGENTS.md', 'AUTHENTIFICATION.md',
    'Cahier_backend_SpringBoot_v1.md', 'CHANGELOG.md', 'Dockerfile', 'mvnw', 'mvnw.cmd',
    'pom.xml', 'README.md', 'README-dev.md', 'SECURITE.md', 'UPGRADE.md',
    'VALIDATION.md', 'VERIFICATIONS.txt',
}
EXCLUDED_PARTS = {'.git', '.local', '.venv', '__pycache__', 'target', 'node_modules'}


def files() -> dict[str, Path]:
    result = {}
    for name in sorted(ROOT_FILES):
        path = ROOT / name
        if not path.is_file() or path.is_symlink():
            raise ValueError(f'Missing or linked public file: {name}')
        result[name] = path
    for directory in sorted(DIRECTORIES):
        for path in sorted((ROOT / directory).rglob('*')):
            relative = path.relative_to(ROOT)
            if EXCLUDED_PARTS.intersection(relative.parts) or not path.is_file():
                continue
            if path.is_symlink() or not path.resolve().is_relative_to(ROOT.resolve()):
                raise ValueError('Linked public source is not accepted.')
            # Only the two deliberately public image lists are allowed as .env files.
            if path.suffix == '.env' and relative.as_posix() not in {'infra/source-images.env', 'infra/images.lock.env'}:
                raise ValueError('Unexpected environment file in public sources.')
            if path.name.endswith('.trivy.json') or path.suffix in {'.log', '.jar', '.zip', '.pem', '.key'}:
                raise ValueError('Private/generated artifact in public sources.')
            result[relative.as_posix()] = path
    return dict(sorted(result.items()))


def verify() -> int:
    expected = files()
    entries = {}
    for line in (ROOT / 'MANIFEST.sha256').read_text(encoding='utf-8').splitlines():
        digest, separator, name = line.partition('  ')
        if not separator or not re.fullmatch(r'[0-9a-f]{64}', digest) or name in entries or name not in expected:
            raise ValueError('Invalid, duplicated or out-of-scope public manifest entry.')
        entries[name] = digest
    if entries.keys() != expected.keys():
        raise ValueError('Public source inventory changed; review and regenerate the manifest explicitly.')
    for name, path in expected.items():
        if hashlib.sha256(path.read_bytes()).hexdigest() != entries[name]:
            raise ValueError(f'Public manifest checksum mismatch: {name}')
    return len(entries)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true', help='After review, explicitly update the public source inventory.')
    args = parser.parse_args()
    if args.write:
        content = ''.join(f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {name}\n' for name, path in files().items())
        (ROOT / 'MANIFEST.sha256').write_text(content, encoding='utf-8', newline='\n')
    print(f'Public manifest verified: {verify()} files; no build artifacts or private configuration included.')
