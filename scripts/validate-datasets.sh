#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────
# validate-datasets.sh — CI check for wordlist datasets & manifest
#
# Validates:
#   1. manifest.json is valid JSON with all required fields
#   2. Each manifest entry has a matching {code}.json wordlist
#   3. Each wordlist is valid JSON: { "N": ["word", ...], ... }
#   4. Minimum word counts per word length (4, 5, 6)
#   5. No duplicate codes in manifest
#   6. No orphan wordlist files (warning)
#   7. Language codes are valid (lowercase, 2–3 chars)
#
# Exit codes:
#   0 — all checks passed
#   1 — one or more validation errors
# ──────────────────────────────────────────────────────────────────
set -euo pipefail

WORDLISTS_DIR="core/ai/src/main/resources/wordlists"
MANIFEST="$WORDLISTS_DIR/manifest.json"
MIN_WORDS_PER_LENGTH=10
REQUIRED_LENGTHS=(4 5 6)
ERRORS=0

error() {
  echo "❌ ERROR: $1"
  ERRORS=$((ERRORS + 1))
}

warn() {
  echo "⚠️  WARNING: $1"
}

info() {
  echo "✅ $1"
}

# ── Check manifest exists ────────────────────────────────────────
if [[ ! -f "$MANIFEST" ]]; then
  error "manifest.json not found at $MANIFEST"
  exit 1
fi

# ── Check manifest is valid JSON ─────────────────────────────────
if ! python3 -c "import json, sys; json.load(open(sys.argv[1]))" "$MANIFEST" 2>/dev/null; then
  error "manifest.json is not valid JSON"
  exit 1
fi
info "manifest.json is valid JSON"

# ── Parse manifest ───────────────────────────────────────────────
MANIFEST_VALIDATION=$(python3 <<'PYEOF'
import json, sys, os, re

manifest_path = sys.argv[1] if len(sys.argv) > 1 else "core/ai/src/main/resources/wordlists/manifest.json"
wordlists_dir = os.path.dirname(manifest_path)
min_words = int(os.environ.get("MIN_WORDS_PER_LENGTH", 10))
required_lengths = [4, 5, 6]

errors = []
warnings = []

with open(manifest_path) as f:
    manifest = json.load(f)

if not isinstance(manifest, list):
    errors.append("manifest.json must be a JSON array")
    for e in errors:
        print(f"ERROR:{e}")
    sys.exit(0)

required_fields = {"code", "displayName", "flag", "promptName"}
seen_codes = set()
manifest_codes = set()

for i, entry in enumerate(manifest):
    if not isinstance(entry, dict):
        errors.append(f"Entry {i} is not a JSON object")
        continue

    # Check required fields
    missing = required_fields - set(entry.keys())
    if missing:
        errors.append(f"Entry {i}: missing required fields: {', '.join(sorted(missing))}")
        continue

    code = entry["code"]
    manifest_codes.add(code)

    # Validate code format (lowercase, 2-3 chars)
    if not re.match(r'^[a-z]{2,3}$', code):
        errors.append(f"Entry '{code}': code must be 2-3 lowercase letters (ISO 639-1)")

    # Check duplicates
    if code in seen_codes:
        errors.append(f"Duplicate code '{code}' in manifest")
    seen_codes.add(code)

    # Check non-empty string fields
    for field in ["displayName", "flag", "promptName"]:
        val = entry.get(field, "")
        if not isinstance(val, str) or not val.strip():
            errors.append(f"Entry '{code}': '{field}' must be a non-empty string")

    # Check matching wordlist file
    wordlist_path = os.path.join(wordlists_dir, f"{code}.json")
    if not os.path.isfile(wordlist_path):
        errors.append(f"Entry '{code}': no matching wordlist file '{code}.json'")
        continue

    # Validate wordlist format
    try:
        with open(wordlist_path) as wf:
            wordlist = json.load(wf)
    except json.JSONDecodeError as e:
        errors.append(f"Wordlist '{code}.json' is not valid JSON: {e}")
        continue

    if not isinstance(wordlist, dict):
        errors.append(f"Wordlist '{code}.json' must be a JSON object (length -> words[])")
        continue

    for key, words in wordlist.items():
        if not key.isdigit():
            errors.append(f"Wordlist '{code}.json': key '{key}' must be a numeric string")
            continue
        if not isinstance(words, list):
            errors.append(f"Wordlist '{code}.json': value for key '{key}' must be an array")
            continue
        for w in words:
            if not isinstance(w, str):
                errors.append(f"Wordlist '{code}.json': all words in key '{key}' must be strings")
                break

    # Check minimum word counts for required lengths
    for length in required_lengths:
        words = wordlist.get(str(length), [])
        if len(words) < min_words:
            errors.append(
                f"Wordlist '{code}.json': length {length} has {len(words)} words "
                f"(minimum {min_words})"
            )

# Check for orphan wordlist files (no manifest entry)
for filename in sorted(os.listdir(wordlists_dir)):
    if filename == "manifest.json":
        continue
    if filename.endswith(".json"):
        orphan_code = filename[:-5]
        if orphan_code not in manifest_codes:
            warnings.append(f"Orphan wordlist '{filename}' has no manifest entry")

for w in warnings:
    print(f"WARNING:{w}")
for e in errors:
    print(f"ERROR:{e}")
if not errors:
    print("OK:All dataset validations passed")
PYEOF
)

# ── Process Python output ────────────────────────────────────────
while IFS= read -r line; do
  case "$line" in
    ERROR:*)
      error "${line#ERROR:}"
      ;;
    WARNING:*)
      warn "${line#WARNING:}"
      ;;
    OK:*)
      info "${line#OK:}"
      ;;
  esac
done <<< "$MANIFEST_VALIDATION"

# ── Final result ─────────────────────────────────────────────────
echo ""
if [[ $ERRORS -gt 0 ]]; then
  echo "💥 Validation failed with $ERRORS error(s)"
  exit 1
else
  echo "🎉 All dataset validations passed!"
  exit 0
fi
