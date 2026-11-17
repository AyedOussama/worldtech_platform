#!/usr/bin/env python3
"""Verify this documentation/starter pack, not the future Angular application."""
from __future__ import annotations
import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = [
    "README.md", "DEMARRER_ICI.md", "AGENTS.md",
    "Cahier_frontend_Angular_WorldTech_v1.md", "ROADMAP.md",
    "SUIVI_DES_LOTS.md", "THIRD_PARTY_NOTICES.md",
    "docs/ARCHITECTURE.md", "docs/DESIGN_SYSTEM.md", "docs/UX_ECRANS.md",
    "docs/AUTHENTIFICATION.md", "docs/API_CONTRATS.md",
    "docs/ALIGNEMENT_BACKEND.md", "docs/SECURITE.md",
    "docs/PERFORMANCE.md", "docs/TESTS_RECETTE.md",
    "docs/DEPENDANCES.md", "docs/DEPLOIEMENT.md",
    "vendor/unslop/LICENSE", "vendor/unslop/README.md",
    "vendor/unslop/skills/unslop/SKILL.md",
    "templates/design/tokens.json", "templates/design/worldtech.tokens.css",
    "templates/theme/theme-init.js", "templates/config/runtime-config.example.json",
]
HEADINGS = [
    "## Objectif", "## Prérequis", "## Lecture obligatoire",
    "## Travail demandé", "## Vérifications et critères d'acceptation",
    "## Livrables", "## Limite du lot", "## Compte rendu final",
]
LINK = re.compile(r"!?\[[^\]]*\]\(([^)\n]+)\)")

def all_files():
    return sorted(p for p in ROOT.rglob("*") if p.is_file()
                  and "__pycache__" not in p.parts and not p.name.endswith(".pyc"))

def luminance(hex_color):
    values = [int(hex_color[i:i+2], 16) / 255 for i in (1, 3, 5)]
    linear = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4
              for v in values]
    return sum(v * w for v, w in zip(linear, (0.2126, 0.7152, 0.0722)))

def contrast(a, b):
    bright, dark = sorted((luminance(a), luminance(b)), reverse=True)
    return (bright + 0.05) / (dark + 0.05)

def verify(skip_manifest=False):
    errors, checks, ratios = [], [], []
    def require(condition, message):
        if not condition:
            errors.append(message)

    for name in REQUIRED:
        p = ROOT / name
        require(p.is_file() and p.stat().st_size > 0, "Missing/empty: " + name)
    checks.append("Required files")

    prompts = sorted((ROOT / "prompts").glob("[0-9][0-9]_*.md"))
    require([p.name[:2] for p in prompts] == [f"{i:02d}" for i in range(15)],
            "Expected exactly one numbered prompt 00..14")
    for p in prompts:
        text = p.read_text(encoding="utf-8")
        for h in HEADINGS:
            require(h in text, f"{p.name}: missing section {h}")
        require(len(text) >= 2500, p.name + ": prompt unexpectedly short")
    checks.append("15 complete sequential prompts")

    md_count, local_links = 0, 0
    for p in all_files():
        if p.suffix != ".md" or "vendor" in p.relative_to(ROOT).parts:
            continue
        md_count += 1
        text = p.read_text(encoding="utf-8")
        require(text.count("~~~") % 2 == 0, str(p.relative_to(ROOT)) + ": unclosed fence")
        for raw in LINK.findall(text):
            target = raw.strip().strip("<>").split(" ", 1)[0]
            if re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:", target) or target.startswith("#"):
                continue
            target = unquote(target.split("#", 1)[0])
            if not target:
                continue
            local_links += 1
            require((p.parent / target).exists(),
                    f"{p.relative_to(ROOT)}: broken link {target}")
    checks.append(f"UTF-8 Markdown and {local_links} local links across {md_count} files")

    for p in all_files():
        if p.suffix == ".json":
            try:
                json.loads(p.read_text(encoding="utf-8"))
            except Exception as exc:
                errors.append(f"Invalid JSON {p.relative_to(ROOT)}: {exc}")
    checks.append("JSON syntax")

    config = json.loads((ROOT / "templates/config/runtime-config.example.json").read_text())
    require(set(config) == {"appName", "apiBaseUrl", "googleLoginEnabled",
                            "telemetryEnabled", "buildVersion"}, "Unexpected runtime keys")
    require(config["appName"] == "WorldTech", "Wrong product name")
    require(re.fullmatch(r"/api/web/v[1-9][0-9]*", config["apiBaseUrl"]) is not None,
            "Runtime example must use web API relative path")
    require(type(config["googleLoginEnabled"]) is bool and
            type(config["telemetryEnabled"]) is bool, "Flags must be booleans")
    checks.append("Public configuration example structure")

    tokens = json.loads((ROOT / "templates/design/tokens.json").read_text())
    css = (ROOT / "templates/design/worldtech.tokens.css").read_text()
    for mode, palette in tokens["theme"].items():
        for key, value in palette.items():
            require(re.fullmatch(r"#[0-9A-Fa-f]{6}", value) is not None,
                    f"Invalid color {mode}/{key}")
            css_key = re.sub(r"[A-Z]", lambda m: "-" + m.group(0).lower(), key)
            require(f"--wt-{css_key}: {value};" in css, f"CSS/token mismatch {mode}/{key}")
        for background in ("background", "surface", "selected"):
            for foreground in ("text", "secondary", "muted", "error", "success", "warning"):
                value = contrast(palette[foreground], palette[background])
                ratios.append({"theme": mode, "foreground": foreground,
                               "background": background, "ratio": round(value, 3), "minimum": 4.5})
                require(value >= 4.5, f"Text contrast {mode}/{foreground}/{background}: {value:.3f}")
            value = contrast(palette["controlBorder"], palette[background])
            ratios.append({"theme": mode, "foreground": "controlBorder",
                           "background": background, "ratio": round(value, 3), "minimum": 3.0})
            require(value >= 3.0, f"Control contrast {mode}/{background}: {value:.3f}")
        value = contrast(palette["onAccent"], palette["accent"])
        ratios.append({"theme": mode, "foreground": "onAccent",
                       "background": "accent", "ratio": round(value, 3), "minimum": 4.5})
        require(value >= 4.5, f"Accent contrast {mode}: {value:.3f}")
    checks.append(f"{len(ratios)} token contrast pairs and CSS parity")

    tracked = json.loads((ROOT / "vendor/UNSLOP_PROVENANCE.json").read_text())
    require(tracked["commit"] == "97daa3eb365670ea3f87ed85356cbfcee18105a5",
            "Unexpected unslop provenance")
    for name, expected in tracked["files"].items():
        p = ROOT / "vendor/unslop" / name
        require(p.is_file(), "Missing vendor source: " + name)
        if p.is_file():
            require(hashlib.sha256(p.read_bytes()).hexdigest() == expected,
                    "Modified vendor source: " + name)
    checks.append(f"Unchanged unslop sources ({len(tracked['files'])} tracked files)")

    names = [p.relative_to(ROOT).as_posix() for p in all_files()]
    for p in ROOT.rglob("*"):
        require(not p.is_symlink(), "Unexpected symlink: " + str(p.relative_to(ROOT)))
        require(p.name not in {".git", "node_modules"}, "Unexpected directory: " + p.name)
    checks.append("No symlinks, .git history or node_modules")

    if not skip_manifest:
        manifest_path = ROOT / "manifest.sha256.json"
        require(manifest_path.is_file(), "Manifest absent")
        if manifest_path.is_file():
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            expected = manifest["files"]
            actual = set(names) - {"manifest.sha256.json"}
            require(set(expected) == actual, "Manifest inventory differs from pack")
            for name, entry in expected.items():
                p = ROOT / name
                require(p.is_file(), "Manifest file absent: " + name)
                if p.is_file():
                    data = p.read_bytes()
                    require(len(data) == entry["bytes"], "Size differs: " + name)
                    require(hashlib.sha256(data).hexdigest() == entry["sha256"],
                            "SHA256 differs: " + name)
        checks.append("SHA256 manifest and exact inventory")
    else:
        checks.append("SHA256 manifest skipped explicitly for pack construction")
    return {"ok": not errors, "scope": "documentation_and_reference_templates_only",
            "checks": checks, "errors": errors, "file_count": len(names),
            "prompt_count": len(prompts), "contrast_pairs": ratios,
            "angular_build_executed": False, "spring_boot_integration_executed": False}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--skip-manifest", action="store_true",
                        help="Pack construction only; normal verification requires the manifest")
    args = parser.parse_args()
    result = verify(skip_manifest=args.skip_manifest)
    if args.json:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print("PASS" if result["ok"] else "FAIL")
        for item in result["checks"]:
            print(" - " + item)
        for error in result["errors"]:
            print(" ERROR: " + error)
        print("Scope: pack files and reference templates; no Angular build/API integration.")
    return 0 if result["ok"] else 1

if __name__ == "__main__":
    sys.exit(main())

