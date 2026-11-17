"""Private local directories, including effective Windows ACL checks (no secrets logged)."""
import base64
import json
import os
from pathlib import Path
import stat
import subprocess


def default_root() -> Path:
    if os.name == 'nt':
        return Path(os.environ['LOCALAPPDATA']) / 'maintenance-api/dev'
    state = Path(os.environ.get('XDG_STATE_HOME', str(Path.home() / '.local/state')))
    return state / 'maintenance-api/dev'


def reject_link(path: Path) -> None:
    if path.is_symlink() or (path.exists() and getattr(path.lstat(), 'st_file_attributes', 0) & 0x400):
        raise RuntimeError('A private path must not be a symlink or junction.')


def _windows_acl(paths: list[Path]) -> None:
    script = r'''
$ErrorActionPreference = 'Stop'
$sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
$allowed = @($sid.Value, 'S-1-3-4', 'S-1-5-18', 'S-1-5-32-544')
$paths = ConvertFrom-Json $env:MAINT_PRIVATE_PATHS
foreach ($path in $paths) {
    $actual = Get-Acl -LiteralPath $path
    if ($actual.GetOwner([System.Security.Principal.SecurityIdentifier]).Value -ne $sid.Value) {
        throw 'Unexpected private-path owner.'
    }
    foreach ($rule in $actual.GetAccessRules($true, $true, [System.Security.Principal.SecurityIdentifier])) {
        if ($rule.AccessControlType -eq 'Allow' -and $allowed -notcontains $rule.IdentityReference.Value) {
            throw 'Private path grants another principal access.'
        }
    }
}
'''
    env = os.environ.copy()
    env['MAINT_PRIVATE_PATHS'] = json.dumps([str(p.resolve()) for p in paths])
    encoded = base64.b64encode(script.encode('utf-16-le')).decode('ascii')
    result = subprocess.run(['powershell.exe', '-NoProfile', '-NonInteractive', '-EncodedCommand', encoded],
                            env=env, capture_output=True, text=True)
    if result.returncode:
        # Do not echo commands, environment, file contents or foreign ACL details.
        raise RuntimeError('Private Windows ACL verification failed; no secrets may be generated here.')


def private_directory(path: Path) -> None:
    reject_link(path)
    created = not path.exists()
    if created:
        path.mkdir(mode=0o700, parents=True, exist_ok=False)
    if os.name == 'nt':
        # Python >= 3.11.10 applies a private DACL for mkdir(0700) on Windows.
        # OWNER RIGHTS is valid only with the current owner verified above.
        # Older interpreters fail this effective ACL check before any secret is written.
        _windows_acl([path])
    else:
        if created:
            path.chmod(0o700)
        if stat.S_IMODE(path.stat().st_mode) != 0o700 or path.stat().st_uid != os.getuid():
            raise RuntimeError('Private directory must belong to this user and have effective mode 0700.')


def verify_private_files(paths: list[Path]) -> None:
    for path in paths:
        reject_link(path)
        if not path.is_file():
            raise RuntimeError('Unexpected non-file in private configuration.')
    if os.name == 'nt' and paths:
        _windows_acl(paths)
