#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -d .git ]]; then
  git init
  echo "Initialized new git repository."
else
  echo "Git repository already exists."
fi

ORIGIN="https://github.com/ramniks05/inst-memory.git"
if git remote get-url origin &>/dev/null; then
  git remote set-url origin "$ORIGIN"
  echo "Updated remote origin URL."
else
  git remote add origin "$ORIGIN"
  echo "Added remote origin."
fi

git add -A
git status

if ! git diff --cached --quiet; then
  git commit -m "Initial commit: DoLR institutional memory backend (Spring Boot)"
  git branch -M main 2>/dev/null || true
else
  echo "Nothing new to commit."
fi

echo ""
echo "Next: git push -u origin main   (authenticate with GitHub)"
