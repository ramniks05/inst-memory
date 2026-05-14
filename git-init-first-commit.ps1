# Run from the dolr-backend project root (same folder as pom.xml).
# In PowerShell:  Set-Location 'C:\Users\raim''s\Downloads\institutional-memory-backend\dolr-backend'
#                  .\git-init-first-commit.ps1
# Or use Git Bash:  cd "/c/Users/raim's/Downloads/institutional-memory-backend/dolr-backend"
#                  bash git-init-first-commit.sh

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Test-Path -LiteralPath '.git')) {
    git init
    Write-Host 'Initialized new git repository.'
} else {
    Write-Host 'Git repository already exists (.git present).'
}

$origin = 'https://github.com/ramniks05/inst-memory.git'
$hasOrigin = git remote 2>$null | Select-String -Pattern '^origin$' -Quiet
if ($hasOrigin) {
    git remote set-url origin $origin
    Write-Host 'Updated remote origin URL.'
} else {
    git remote add origin $origin
    Write-Host 'Added remote origin.'
}

git add -A
git status

git diff --cached --quiet 2>$null
if ($LASTEXITCODE -ne 0) {
	git commit -m "Initial commit: DoLR institutional memory backend (Spring Boot)"
	git branch -M main 2>$null
} else {
	Write-Host 'Nothing to commit (working tree clean or no changes).'
}

Write-Host @'

Next step (requires GitHub auth — PAT or gh CLI):
  git push -u origin main

If the empty repo rejects non-fast-forward, use:
  git push -u origin main --force-with-lease

'@
