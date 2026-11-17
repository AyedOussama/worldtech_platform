#!/usr/bin/env python3
"""Static pack + implemented infrastructure checks; not a substitute for Maven/Docker tests."""
import ast
import base64
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import tomllib
import xml.etree.ElementTree as ET
import yaml  # PyYAML requis par cet outil uniquement.
from public_manifest import verify as verify_manifest

ROOT = Path(__file__).resolve().parents[1]

class UniqueKeyLoader(yaml.SafeLoader):
    pass

def construct_mapping(loader, node, deep=False):
    keys = [loader.construct_object(key, deep=deep) for key, _ in node.value]
    if len(set(keys)) != len(keys):
        raise ValueError('Clé YAML dupliquée')
    return yaml.SafeLoader.construct_mapping(loader, node, deep=deep)

UniqueKeyLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, construct_mapping)

def read_yaml(path):
    return yaml.load(path.read_text(encoding='utf-8'), Loader=UniqueKeyLoader)

def load_script(name):
    spec = importlib.util.spec_from_file_location(name.replace('-', '_'), ROOT / 'scripts' / name)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def main():
    for path in (ROOT / 'scripts').rglob('*.py'):
        ast.parse(path.read_text(encoding='utf-8'), filename=str(path))
    pom = ET.parse(ROOT / 'references/pom.xml.reference')
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    deps = {n.text for n in pom.findall('.//m:dependencies/m:dependency/m:artifactId', ns)}
    assert 'spring-boot-starter-security-oauth2-client' in deps
    assert 'spring-boot-starter-security-oauth2-resource-server' not in deps
    assert 'bcprov-jdk18on' in deps
    actual_pom = ET.parse(ROOT / 'pom.xml')
    actual_deps = {n.text for n in actual_pom.findall('.//m:dependencies/m:dependency/m:artifactId', ns)}
    assert {'spring-boot-starter-data-jpa', 'spring-boot-starter-flyway',
            'flyway-database-postgresql', 'postgresql', 'testcontainers-junit-jupiter',
            'spring-boot-starter-session-jdbc', 'spring-boot-starter-mail',
            'spring-boot-starter-security-oauth2-client', 'bcprov-jdk18on', 'java-otp'} <= actual_deps
    assert not {'h2', 'spring-boot-starter-security-oauth2-resource-server'} & actual_deps
    compose = read_yaml(ROOT / 'infra/compose.dev.yaml')
    assert set(compose['services']) == {'db', 'mailpit', 'garage'}
    assert set(compose['secrets']) == {'postgres_password', 'migrator_password', 'runtime_password'}
    assert compose['services']['db']['platform'] == 'linux/amd64'
    assert 'db_data:/var/lib/postgresql' in compose['services']['db']['volumes']
    for service in compose['services'].values():
        for mapping in service.get('ports', []):
            assert mapping.startswith('127.0.0.1:'), mapping
    assert all(value['file'].startswith('${MAINT_LOCAL_DIR:') for value in compose['secrets'].values())
    actual = read_yaml(ROOT / 'src/main/resources/application.yaml')
    assert actual['spring']['datasource']['username'] == 'maint_app'
    assert actual['spring']['jpa']['hibernate']['ddl-auto'] == 'validate'
    assert actual['spring']['jpa']['open-in-view'] is False
    assert actual['spring']['flyway']['enabled'] is False
    assert actual['spring']['session']['jdbc']['initialize-schema'] == 'never'
    assert actual['spring']['session']['timeout'] == '30m'
    assert actual['app']['identity']['google']['enabled'] is False
    assert actual['app']['identity']['crypto']['keys']['v1'] == '${mfa_encryption_key}'
    assert actual['spring']['jackson']['deserialization']['fail-on-unknown-properties'] is True
    assert actual['spring']['jackson']['read']['strict-duplicate-detection'] is True
    assert actual['app']['mail']['enabled'] is False
    contract = json.loads((ROOT / 'openapi/maintenance-api.json').read_text(encoding='utf-8'))
    assert contract['openapi'] == '3.1.0'
    def references(value):
        if isinstance(value, dict):
            if '$ref' in value:
                assert value['$ref'].startswith('#/'), 'Référence distante inattendue'
                resolved = contract
                for part in value['$ref'][2:].split('/'):
                    resolved = resolved[part.replace('~1', '/').replace('~0', '~')]
            for child in value.values():
                references(child)
        elif isinstance(value, list):
            for child in value:
                references(child)
    references(contract)
    for path, operations in contract['paths'].items():
        for method, operation in operations.items():
            assert 'security' in operation, (path, method)
            if path.startswith('/api/web/') and method in {'post', 'put', 'patch', 'delete'}:
                assert all('Csrf' in item for item in operation['security']), path
    assert contract['components']['securitySchemes']['MobileAccess']['bearerFormat'] == 'opaque-256-bit'
    development = read_yaml(ROOT / 'src/main/resources/application-dev.yaml')
    assert development['spring']['flyway']['user'] == 'maint_migrator'
    assert development['spring']['flyway']['enabled'] is True
    production = read_yaml(ROOT / 'src/main/resources/application-prod.yaml')
    assert production['spring']['flyway']['enabled'] is False
    assert 'migrator_password' not in (ROOT / 'src/main/resources/application-prod.yaml').read_text()
    assert 'USER 10001:10001' in (ROOT / 'Dockerfile').read_text()
    assert (ROOT / '.dockerignore').read_text().splitlines()[1] == '**'
    workflow = read_yaml(ROOT / '.github/workflows/ci.yml')
    assert workflow['jobs']['verify']['runs-on'] == 'ubuntu-24.04'
    assert 'windows-unit' in workflow['jobs']
    for path in (ROOT / 'references').glob('*.yaml.reference'):
        read_yaml(path)
    config = read_yaml(ROOT / 'references/application.yaml.reference')
    assert 'security' not in config['spring'], 'Google doit rester dans son profil optionnel'
    assert config['app']['auth']['google']['enabled'] is False
    assert config['app']['auth']['password']['argon2']['memory-kib'] >= 19456
    assert config['app']['auth']['mfa']['admin-required'] is True
    google = read_yaml(ROOT / 'references/application-google.yaml.reference')
    assert google['app']['auth']['google']['enabled'] is True
    assert 'google' in google['spring']['security']['oauth2']['client']['registration']
    assert len(list((ROOT / 'prompts').glob('[0-9][0-9]_*.md'))) == 15
    for number in range(15):
        assert len(list((ROOT / 'prompts').glob(f'{number:02d}_*.md'))) == 1
    for path in (ROOT / 'prompts').glob('*.md'):
        text = path.read_text(encoding='utf-8')
        assert all(s in text for s in ('Révision 1.1', 'AUTHENTIFICATION.md', 'SECURITE.md'))
    for script in (ROOT / 'infra').rglob('*.sh'):
        subprocess.run([os.environ.get('MAINT_BASH', 'bash'), '-n', script.as_posix()], check=True)
    init = load_script('init-dev.py')
    with tempfile.TemporaryDirectory(prefix='maintenance-pack-check-') as directory:
        root = Path(directory) / 'private'
        init.initialize(root)
        before = {p.relative_to(root): p.read_bytes() for p in (root / '.local').rglob('*') if p.is_file()}
        init.initialize(root)
        after = {p.relative_to(root): p.read_bytes() for p in (root / '.local').rglob('*') if p.is_file()}
        assert before == after, 'Secrets remplacés au second lancement'
        secret_files = list((root / '.local/secrets').iterdir())
        assert len(secret_files) == len(set(p.read_text() for p in secret_files)) == 9
        mfa = (root / '.local/secrets/mfa_encryption_key').read_text().strip()
        assert len(base64.urlsafe_b64decode(mfa + '=' * (-len(mfa) % 4))) == 32
        if os.name == 'posix':
            assert ((root / '.local').stat().st_mode & 0o777) == 0o700
            assert ((root / '.local/secrets').stat().st_mode & 0o777) == 0o700
        garage = tomllib.loads((root / '.local/garage.toml').read_text())
        assert garage['data_dir'].startswith('/var/lib/')
        assert garage['replication_factor'] == 1
        assert re.fullmatch('[0-9a-f]{64}', garage['rpc_secret'])
    lock = load_script('lock-images.py')
    assert len(lock.candidates(ROOT / 'infra/source-images.env')) == 4
    assert len(lock.read_lock(ROOT / 'infra/images.lock.env')) == 4
    with tempfile.TemporaryDirectory(prefix='maintenance-invalid-image-') as directory:
        path = Path(directory) / 'images.env'
        original = (ROOT / 'infra/source-images.env').read_text()
        path.write_text(original.replace('18-3.6', 'latest'))
        try:
            lock.candidates(path)
        except ValueError:
            pass
        else:
            raise AssertionError('Un tag latest a été accepté')
    public_files = load_script('public_manifest.py').files()
    for path in public_files.values():
        if not path.is_file():
            continue
        if path.suffix in {'.md', '.py', '.sh', '.env', '.yaml', '.reference'}:
            text = path.read_text(encoding='utf-8')
            if path.name not in {'CHANGELOG.md', 'UPGRADE.md', 'validate-pack.py'}:
                assert 'keycloak' not in text.lower(), str(path)
            if path.suffix == '.md':
                assert sum(line.startswith('```') for line in text.splitlines()) % 2 == 0, path
    assert not (ROOT / '.local').exists(), 'Secrets locaux présents dans le pack'
    count = verify_manifest()
    print(f'OK statique : Python/XML/YAML/TOML/Bash, profils et privilèges configurés ; 4 digests ; 9 secrets stables/MFA 32 octets ; 15 prompts ; manifeste public {count} fichiers.')
    print('Contrat OpenAPI/références locales et profils identité contrôlés statiquement. Ce validateur ne lance pas Docker/SQL/Maven/Spring/scans ; voir les preuves séparées. Google réel/métier/pentest non démontrés.')

if __name__ == '__main__':
    main()
