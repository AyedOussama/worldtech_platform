#!/usr/bin/env python3
"""Génère les secrets locaux ; aucune valeur secrète n'est imprimée."""
import argparse
import base64
import os
from pathlib import Path
import re
import secrets

from private_local import default_root, private_directory, reject_link, verify_private_files

SECRET_NAMES = {
    'postgres_password', 'migrator_password', 'runtime_password', 'mfa_encryption_key',
    'garage_rpc_secret', 'garage_admin_token', 'garage_metrics_token',
    'garage_access_key', 'garage_secret_key',
}


def create_once(path: Path, value: str) -> str:
    """Ne remplace pas une clé déjà utilisée par un volume existant."""
    reject_link(path)
    try:
        descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError:
        return path.read_text(encoding='utf-8').strip()
    with os.fdopen(descriptor, 'w', encoding='utf-8', newline='\n') as stream:
        stream.write(value + '\n')
    # POSIX: parent 0700, mounted files readable by non-root containers.
    # Windows: inherited private DACL, not a misleading chmod emulation.
    if os.name != 'nt':
        path.chmod(0o444)
    return value


def initialize(root: Path) -> None:
    if any(part.lower().startswith('onedrive') for part in root.resolve().parts):
        raise RuntimeError('Refusing secrets in a OneDrive path; use the private local default.')
    private_directory(root)
    local = root / '.local'
    secret_dir = local / 'secrets'
    private_directory(local)
    private_directory(secret_dir)
    verify_private_files([p for p in local.rglob('*') if p.is_file()])
    for name in ('postgres_password', 'migrator_password', 'runtime_password'):
        create_once(secret_dir / name, secrets.token_urlsafe(36))
    create_once(secret_dir / 'mfa_encryption_key', secrets.token_urlsafe(32))

    rpc = create_once(secret_dir / 'garage_rpc_secret', secrets.token_hex(32))
    admin = create_once(secret_dir / 'garage_admin_token', secrets.token_urlsafe(32))
    metrics = create_once(secret_dir / 'garage_metrics_token', secrets.token_urlsafe(32))
    key = create_once(secret_dir / 'garage_access_key', 'GK' + secrets.token_hex(16))
    secret_key = create_once(secret_dir / 'garage_secret_key', secrets.token_hex(32))

    config = f'''# Développement à un nœud ; aucun service web public activé.
metadata_dir = "/var/lib/garage/meta"
data_dir = "/var/lib/garage/data"
db_engine = "sqlite"
replication_factor = 1
rpc_bind_addr = "[::]:3901"
rpc_public_addr = "127.0.0.1:3901"
rpc_secret = "{rpc}"

[s3_api]
s3_region = "garage"
api_bind_addr = "[::]:3900"
root_domain = ".s3.garage.localhost"

[admin]
api_bind_addr = "[::]:3903"
admin_token = "{admin}"
metrics_token = "{metrics}"
'''
    env = (f'GARAGE_DEFAULT_ACCESS_KEY={key}\n'
           f'GARAGE_DEFAULT_SECRET_KEY={secret_key}\n'
           'GARAGE_DEFAULT_BUCKET=maintenance-private\n')
    for path, content in ((local / 'garage.toml', config), (local / 'garage.env', env)):
        if path.exists() and path.read_text(encoding='utf-8').rstrip('\n') != content.rstrip('\n'):
            raise RuntimeError(f'Configuration existante différente : {path.name}. Examiner sans écraser.')
        create_once(path, content.rstrip('\n'))
    verify(root)


def verify(root: Path) -> None:
    local = root / '.local'
    private_directory(root)
    private_directory(local)
    private_directory(local / 'secrets')
    files = list((local / 'secrets').iterdir())
    if {p.name for p in files} != SECRET_NAMES:
        raise RuntimeError('Expected exactly nine named local secrets.')
    verify_private_files(files + [local / 'garage.toml', local / 'garage.env'])
    values = {p.name: p.read_text(encoding='utf-8').strip() for p in files}
    if len(set(values.values())) != 9 or any(not re.fullmatch(r'[A-Za-z0-9_-]{32,}', v) for v in values.values()):
        raise RuntimeError('Invalid or duplicate local secret; existing values were not replaced.')
    try:
        key = values['mfa_encryption_key']
        if len(base64.urlsafe_b64decode(key + '=' * (-len(key) % 4))) != 32:
            raise ValueError()
    except (ValueError, TypeError):
        raise RuntimeError('The MFA encryption key must encode exactly 32 bytes.') from None


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=default_root())
    parser.add_argument('--print-local-dir', action='store_true', help='Print the non-secret local directory path only.')
    parser.add_argument('--verify', action='store_true', help='Verify existing values and permissions without replacing them.')
    args = parser.parse_args()
    if args.print_local_dir:
        print((args.root.resolve() / '.local').as_posix())
    else:
        (verify if args.verify else initialize)(args.root.resolve())
        print('Neuf secrets privés vérifiés, clé MFA de 32 octets. Aucune valeur affichée ou remplacée.')
