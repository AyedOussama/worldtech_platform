import base64
import hashlib
import importlib.util
import os
from pathlib import Path
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))


def load(name):
    spec = importlib.util.spec_from_file_location(name.replace('-', '_'), SCRIPTS / name)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


INIT = load('init-dev.py')
LOCK = load('lock-images.py')


class LocalSecretsTest(unittest.TestCase):
    def test_nine_private_secrets_survive_reinitialization(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / 'private'
            INIT.initialize(root)
            files = list((root / '.local/secrets').iterdir())
            self.assertEqual({p.name for p in files}, INIT.SECRET_NAMES)
            self.assertTrue(all(b'\r' not in p.read_bytes() for p in files), 'Secret files must use LF on Windows too.')
            # Hashes only: never include raw credentials in assertion failure output.
            before = {p.relative_to(root): hashlib.sha256(p.read_bytes()).hexdigest()
                      for p in (root / '.local').rglob('*') if p.is_file()}
            INIT.initialize(root)
            after = {p.relative_to(root): hashlib.sha256(p.read_bytes()).hexdigest()
                     for p in (root / '.local').rglob('*') if p.is_file()}
            self.assertEqual(before, after)
            INIT.verify(root)
            encoded = (root / '.local/secrets/mfa_encryption_key').read_text().strip()
            self.assertEqual(len(base64.urlsafe_b64decode(encoded + '=' * (-len(encoded) % 4))), 32)
            if os.name == 'posix':
                self.assertEqual((root / '.local').stat().st_mode & 0o777, 0o700)

    def test_synced_path_is_refused_before_creating_any_secret(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / 'OneDrive' / 'private'
            with self.assertRaisesRegex(RuntimeError, 'OneDrive'):
                INIT.initialize(root)
            self.assertFalse(root.exists())

    def test_existing_garage_configuration_is_never_silently_replaced(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / 'private'
            INIT.initialize(root)
            config = root / '.local/garage.toml'
            if os.name != 'nt':
                config.chmod(0o600)
            config.write_text('# synthetic conflicting configuration\n', encoding='utf-8')
            expected = hashlib.sha256(config.read_bytes()).digest()
            with self.assertRaisesRegex(RuntimeError, 'Configuration existante différente'):
                INIT.initialize(root)
            self.assertEqual(hashlib.sha256(config.read_bytes()).digest(), expected)


class ImageLockTest(unittest.TestCase):
    def test_committed_lock_has_all_expected_digest_repositories(self):
        values = LOCK.read_lock(SCRIPTS.parent / 'infra/images.lock.env')
        self.assertEqual(len(values), 4)

    def test_a_tag_is_not_accepted_in_place_of_a_digest(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'invalid.env'
            path.write_text('DB_IMAGE=postgis/postgis:18-3.6\n', encoding='utf-8')
            with self.assertRaises(ValueError):
                LOCK.read_lock(path)

    def test_latest_candidate_is_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'candidates.env'
            source = (SCRIPTS.parent / 'infra/source-images.env').read_text()
            path.write_text(source.replace('18-3.6', 'latest'), encoding='utf-8')
            with self.assertRaises(ValueError):
                LOCK.candidates(path)


if __name__ == '__main__':
    unittest.main()
