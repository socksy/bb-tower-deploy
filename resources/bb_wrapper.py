#!/usr/bin/env python3
import os
import subprocess
import sys
import platform
import glob

def get_default_task():
    """Find the first .bb file in the directory"""
    bb_files = glob.glob('*.bb')
    if bb_files:
        return os.path.splitext(bb_files[0])[0]
    return 'main'

def main():
    # Get the babashka task from environment variable or use smart default
    bb_task = os.environ.get('bb_task')
    if not bb_task:
        bb_task = get_default_task()
        print(f'No bb_task specified, using: {bb_task}')

    # Determine babashka binary based on architecture
    arch = platform.machine().lower()
    if arch in ['x86_64', 'amd64']:
        bb_binary = 'bin/bb-linux-amd64'
    elif arch in ['aarch64', 'arm64']:
        bb_binary = 'bin/bb-linux-aarch64'
    else:
        raise RuntimeError(f'Unsupported architecture: {arch}')

    # Run the babashka task
    try:
        result = subprocess.run([bb_binary, bb_task], check=True)
        sys.exit(result.returncode)
    except subprocess.CalledProcessError as e:
        print(f'Error running babashka task: {e}')
        sys.exit(e.returncode)

if __name__ == '__main__':
    main()