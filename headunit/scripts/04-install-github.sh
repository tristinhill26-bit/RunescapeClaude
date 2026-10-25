#!/usr/bin/env bash
# Step 6: install an app on the unit from its OFFICIAL GitHub release. CHANGES THE UNIT, reversible (uninstall).
# Usage: ./04-install-github.sh <owner/repo> <asset-regex>
#   e.g. ./04-install-github.sh jellyfin/jellyfin-android 'libre.*\.apk$'
# - Downloads the latest release asset to this phone/laptop.
# - Verifies its SHA-256 against the digest GitHub publishes for that asset; refuses if missing or different.
# - Asks for YES, installs over adb, and writes an undo script (uninstall) for whatever got installed.
source "$(dirname "$0")/common.sh"
need_adb; ensure_connected
command -v jq >/dev/null 2>&1   || die "Needs jq on this phone/laptop. In Termux: pkg install -y jq"
command -v curl >/dev/null 2>&1 || die "Needs curl. In Termux: pkg install -y curl"
[[ -n "$(latest_backup)" ]] || die "No finished backup found. Run 02-backup.sh first."

repo="${1:-}"; pattern="${2:-}"
[[ $repo == */* && -n $pattern ]] || die "Usage: $0 <owner/repo> <asset-regex>"

json="$(curl -fsSL "https://api.github.com/repos/$repo/releases/latest")" || die "Couldn't reach GitHub for $repo."
tag="$(jq -r '.tag_name' <<<"$json")"
mapfile -t names < <(jq -r --arg re "$pattern" '.assets[] | select(.name | test($re)) | .name' <<<"$json")
if (( ${#names[@]} != 1 )); then
  echo "Release $tag of $repo has these files:"; jq -r '.assets[].name' <<<"$json" | sed 's/^/  /'
  die "Expected exactly one file matching '$pattern', found ${#names[@]}. Send Claude this list."
fi
name="${names[0]}"
url="$(jq -r --arg n "$name" '.assets[] | select(.name==$n) | .browser_download_url' <<<"$json")"
digest="$(jq -r --arg n "$name" '.assets[] | select(.name==$n) | .digest // empty' <<<"$json")"
[[ $digest == sha256:* ]] || die "GitHub publishes no SHA-256 digest for $name, so it can't be verified. Not installing."
want="${digest#sha256:}"

dl="$HU_DIR/downloads/${repo//\//_}/$tag"; mkdir -p "$dl"
echo "Downloading $name ($tag) from github.com/$repo ..."
curl -fL --progress-bar -o "$dl/$name" "$url" || die "Download failed."
got="$(sha256 "$dl/$name" | cut -d' ' -f1)"
[[ $got == "$want" ]] || { rm -f "$dl/$name"; die "SHA-256 MISMATCH. Expected $want, got $got. File deleted, nothing installed."; }

echo
echo "Repository: github.com/$repo   Release: $tag"
echo "File:       $name ($(du -h "$dl/$name" | cut -f1))"
echo "SHA-256:    $got  (matches GitHub)"
echo "Source:     $url"
read -r -p "Type YES to install it on the stereo: " ok
[[ $ok == YES ]] || die "Not installed. Nothing changed."

run="$HU_DIR/changes/$(date +%Y%m%d-%H%M%S)-install"; mkdir -p "$run"
ro_shell 'pm list packages' | tr -d '\r' | sed 's/^package://' | sort >"$run/before_all.txt"
printf '%s  CHANGE adb install -r %s\n' "$(date '+%F %T')" "$name" >>"$LOG_FILE"
res="$(adb -s "$(serial)" install -r "$dl/$name" 2>&1 | tr -d '\r')"
echo "$res" | tail -2
[[ $res == *Success* ]] || die "Install failed (see above). Nothing to undo."
ro_shell 'pm list packages' | tr -d '\r' | sed 's/^package://' | sort >"$run/after_all.txt"
new="$(comm -13 "$run/before_all.txt" "$run/after_all.txt")"

cat >"$run/undo.sh" <<EOF
#!/usr/bin/env bash
# Undo for $(basename "$run"): uninstall what this run added.
source "$HU_DIR/scripts/common.sh"
need_adb; ensure_connected
EOF
if [[ -n $new ]]; then
  for p in $new; do echo "rw_shell 'pm uninstall $p'" >>"$run/undo.sh"; done
  echo "Installed new app: $new"
else
  echo "# App was already installed; this run only updated it. Nothing to remove." >>"$run/undo.sh"
  echo "Updated an app that was already installed."
fi
echo 'echo "Undo complete."' >>"$run/undo.sh"; chmod +x "$run/undo.sh"
{ echo "repo=$repo"; echo "tag=$tag"; echo "file=$name"; echo "sha256=$got"; echo "packages=$new"; } >"$run/INSTALLED.txt"
echo "Undo (uninstall):  bash $run/undo.sh"
