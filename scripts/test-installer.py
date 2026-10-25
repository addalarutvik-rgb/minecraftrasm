"""Exercise the packaged installer against disposable Minecraft instances."""
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

repo = Path(__file__).resolve().parents[1]
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java'
installer = repo / 'build/libs/rasm-installer.jar'
payload = repo / 'build/libs/rasm-client-0.1.0.jar'

def run(instance, success=True):
    result = subprocess.run([java, '-jar', str(installer), '--install', str(instance)], capture_output=True, text=True)
    assert (result.returncode == 0) == success, result.stderr

with tempfile.TemporaryDirectory(prefix='rasm-installer-test-') as temp:
    instance = Path(temp)
    run(instance, False)  # Never creates an arbitrary instance/mods directory.
    mods = instance / 'mods'
    mods.mkdir()
    unrelated = mods / 'other.jar'
    with zipfile.ZipFile(unrelated, 'w') as jar:
        jar.writestr('fabric.mod.json', '{"id":"other"}')
    untouched = unrelated.read_bytes()
    run(instance)
    installed = mods / payload.name
    assert installed.read_bytes() == payload.read_bytes()
    renamed = mods / 'renamed-old-rasm.jar'
    installed.rename(renamed)
    run(instance)
    assert installed.read_bytes() == payload.read_bytes()
    assert not renamed.exists()
    backups = list((instance / 'rasm-backups').glob('*/renamed-old-rasm.jar'))
    assert len(backups) == 1 and backups[0].read_bytes() == payload.read_bytes()
    assert unrelated.read_bytes() == untouched
    installed.write_bytes(b'unrelated file with conflicting name')
    run(instance, False)
    assert installed.read_bytes() == b'unrelated file with conflicting name'
    assert unrelated.read_bytes() == untouched
    assert not list(mods.glob('.rasm-install-*'))
print('Installer integration checks passed: install, update, backup, preservation, invalid folder, filename conflict.')
