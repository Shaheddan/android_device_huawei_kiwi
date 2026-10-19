#!/bin/bash
# Upload the newest kiwi LineageOS 20.0 zip from this Crave workspace to a GitHub release.
# WHY: `crave pull` cannot reach Crave's transfer tunnel from Shade's network, and Crave
# clears out/ after a job; so the build job uploads the zip itself (build-kiwi.sh calls this
# when GH_TOKEN is set).
# GH_TOKEN: fine-grained token, Contents read/write on $REPO.
REPO=${REPO:-Shaheddan/android_device_huawei_kiwi}
BRANCH=${BRANCH:-lineage-20}
LOSVER=${LOSVER:-20.0}
VARIANT=${VARIANT:-userdebug}
API=${API:-https://api.github.com}
UPLOADS=${UPLOADS:-https://uploads.github.com}
OUT=${OUT:-out/target/product/kiwi}

[ -n "${GH_TOKEN:-}" ] || { echo "!! GH_TOKEN is not set"; exit 1; }
ZIP=$(ls -t "$OUT"/lineage-"$LOSVER"-*-UNOFFICIAL-kiwi.zip 2>/dev/null | head -1)
if [ -z "$ZIP" ]; then
  echo "!! no lineage-$LOSVER zip in $PWD/$OUT; what is there:"
  ls -la "$OUT" 2>&1 | tail -30
  exit 1
fi
NAME=$(basename "$ZIP")
TAG=${NAME%.zip}
ls -la "$ZIP"
# GitHub refuses release assets of 2 GiB or more; say so plainly instead of a raw API error.
[ "$(stat -c %s "$ZIP")" -lt 2147483648 ] || { echo "!! $NAME is 2 GiB or larger; GitHub releases cannot take it"; exit 1; }
# Checksum file with the bare name, so `sha256sum -c` works next to the downloaded zip.
SUM=/tmp/$NAME.sha256sum
( cd "$OUT" && sha256sum "$NAME" ) > "$SUM" && cat "$SUM"

AUTH=(-H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28")
json() { python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$1') or '')"; }

# Reuse the release if this tag already exists (a second build on the same day).
ID=$(curl -sS "${AUTH[@]}" "$API/repos/$REPO/releases/tags/$TAG" | json id)
if [ -z "$ID" ]; then
  BODY=$(python3 -c "import json,sys; print(json.dumps({'tag_name': sys.argv[1], 'target_commitish': sys.argv[2], 'name': sys.argv[1], 'prerelease': True, 'body': 'LineageOS ' + sys.argv[3] + ' test build for kiwi (' + sys.argv[4] + '), built on crave.io from branch ' + sys.argv[2] + '.'}))" "$TAG" "$BRANCH" "$LOSVER" "$VARIANT")
  RESP=$(curl -sS "${AUTH[@]}" -X POST "$API/repos/$REPO/releases" -d "$BODY")
  ID=$(echo "$RESP" | json id)
  [ -n "$ID" ] || { echo "!! could not create release: $RESP"; exit 1; }
fi
echo "release id $ID"

for f in "$ZIP" "$SUM"; do
  n=$(basename "$f")
  # GitHub rejects an asset name that already exists in the release, so a rebuild on the
  # same day replaces the old asset instead of failing.
  OLD=$(curl -sS "${AUTH[@]}" "$API/repos/$REPO/releases/$ID/assets?per_page=100" \
    | python3 -c "import sys,json; n=sys.argv[1]; d=json.load(sys.stdin); print(next((str(a['id']) for a in d if a.get('name') == n), '') if isinstance(d, list) else '')" "$n")
  if [ -n "$OLD" ]; then
    echo "replacing the earlier $n (asset $OLD)"
    curl -sS "${AUTH[@]}" -X DELETE "$API/repos/$REPO/releases/assets/$OLD" || { echo "!! could not delete asset $OLD"; exit 1; }
  fi
  echo "uploading $n ($(stat -c %s "$f") bytes) ..."
  # -T streams the file instead of loading ~1 GB into memory.
  RESP=$(curl -sS "${AUTH[@]}" -X POST -H "Content-Type: application/octet-stream" \
    -T "$f" "$UPLOADS/repos/$REPO/releases/$ID/assets?name=$n")
  URL=$(echo "$RESP" | json browser_download_url)
  [ -n "$URL" ] || { echo "!! upload failed: $RESP"; exit 1; }
  echo "DOWNLOAD: $URL"
done
