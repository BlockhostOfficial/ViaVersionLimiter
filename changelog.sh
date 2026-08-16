#!/usr/bin/env bash

set -euo pipefail

git fetch origin --tags --quiet

if latest_tag=$(git describe --tags --abbrev=0 2>/dev/null); then
    revision_range="$latest_tag..HEAD"
else
    revision_range="HEAD"
fi

git log "$revision_range" --no-merges --pretty=format:'%h %s'
