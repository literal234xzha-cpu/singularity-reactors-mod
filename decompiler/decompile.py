#!/usr/bin/env python3
"""Wrapper script to decompile APKs using JADX and apktool."""
import argparse
import subprocess
import sys
from pathlib import Path

TOOLS_DIR = Path(__file__).parent / "tools"
JADX = TOOLS_DIR / "bin" / "jadx"
APKTOOL = TOOLS_DIR / "apktool.jar"
OUTPUT_DIR = Path(__file__).parent / "output"


def run_jadx(apk_path: Path, out_dir: Path):
    cmd = [
        str(JADX),
        "--show-bad-code",
        "--deobf",
        "-d",
        str(out_dir / "jadx_decompiled"),
        "-v",
        str(apk_path),
    ]
    print(f"[+] Running JADX on {apk_path}")
    subprocess.run(cmd, check=True)


def run_apktool(apk_path: Path, out_dir: Path):
    cmd = [
        "java", "-jar", str(APKTOOL),
        "d", "-f",
        "-o", str(out_dir / "apktool_decompiled"),
        str(apk_path),
    ]
    print(f"[+] Running apktool on {apk_path}")
    subprocess.run(cmd, check=True)


def main():
    parser = argparse.ArgumentParser(
        description="APK Decompiler - uses JADX (Java source) and Apktool (resources)"
    )
    parser.add_argument("apk", type=Path, help="Path to the APK file")
    parser.add_argument("-o", "--output", type=Path, default=OUTPUT_DIR,
                        help="Output directory")
    parser.add_argument("--jadx-only", action="store_true", help="Only run JADX")
    parser.add_argument("--apktool-only", action="store_true", help="Only run apktool")
    args = parser.parse_args()

    if not args.apk.exists():
        print(f"[!] APK not found: {args.apk}", file=sys.stderr)
        sys.exit(1)

    out = args.output / args.apk.stem
    out.mkdir(parents=True, exist_ok=True)

    if not args.apktool_only:
        run_jadx(args.apk, out)
    if not args.jadx_only:
        run_apktool(args.apk, out)

    print(f"[+] Done. Output in {out}")


if __name__ == "__main__":
    main()
