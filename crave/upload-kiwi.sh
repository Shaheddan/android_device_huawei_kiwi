#!/bin/bash
# Upload the newest kiwi ROM zip from this Crave workspace to a GitHub release.
# WHY: `crave pull` cannot reach Crave's transfer tunnel from Shade's network, while
# GitHub downloads work; so the build node uploads the zip itself.
# Start it from the devspace CLI (no rebuild, it only uploads what the last build made):
#   crave run --projectID 93 --no-patch -- "curl -sfL <raw url of this file> | GH_TOKEN=... bash"
# GH_TOKEN: fine-grained token, Contents read/write on $REPO only.
REPO=${REPO:-Shaheddan/android_device_huawei_kiwi}
BRANCH=${BRANCH:-lineage-21}
API=${API:-https://api.github.com}
UPLOADS=${UPLOADS:-https://uploads.github.com}
OUT=${OUT:-out/target/product/kiwi}

[ -n "${GH_TOKEN:-}" ] || { echo "!! GH_TOKEN is not set"; exit 1; }
ZIP=$(ls -t "$OUT"/lineage-21.0-*-UNOFFICIAL-kiwi.zip 2>/dev/null | head -1)
if [ -z "$ZIP" ]; then
  echo "!! no lineage-21.0 zip in $PWD/$OUT; what is there:"
  ls -la "$OUT" 2>&1 | tail -30
  exit 1
fi
NAME=$(basename "$ZIP")
TAG=${NAME%.zip}
ls -la "$ZIP"
# Checksum file with the bare name, so `sha256sum -c` works next to the downloaded zip.
SUM=/tmp/$NAME.sha256sum
( cd "$OUT" && sha256sum "$NAME" ) > "$SUM" && cat "$SUM"

AUTH=(-H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28")
json() { python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$1') or '')"; }

# Reuse the release if this tag already exists (re-running the upload is safe).
ID=$(curl -sS "${AUTH[@]}" "$API/repos/$REPO/releases/tags/$TAG" | json id)
if [ -z "$ID" ]; then
  BODY=$(python3 -c "import json,sys; print(json.dumps({'tag_name': sys.argv[1], 'target_commitish': sys.argv[2], 'name': sys.argv[1], 'prerelease': True, 'body': 'LineageOS 21 (Android 14) test build for kiwi, eng variant. Not for daily use.'}))" "$TAG" "$BRANCH")
  RESP=$(curl -sS "${AUTH[@]}" -X POST "$API/repos/$REPO/releases" -d "$BODY")
  ID=$(echo "$RESP" | json id)
  [ -n "$ID" ] || { echo "!! could not create release: $RESP"; exit 1; }
fi
echo "release id $ID"

for f in "$ZIP" "$SUM"; do
  echo "uploading $(basename "$f") ($(stat -c %s "$f") bytes) ..."
  # -T streams the file instead of loading ~1 GB into memory.
  RESP=$(curl -sS "${AUTH[@]}" -X POST -H "Content-Type: application/octet-stream" \
    -T "$f" "$UPLOADS/repos/$REPO/releases/$ID/assets?name=$(basename "$f")")
  URL=$(echo "$RESP" | json browser_download_url)
  [ -n "$URL" ] || { echo "!! upload failed: $RESP"; exit 1; }
  echo "DOWNLOAD: $URL"
done
