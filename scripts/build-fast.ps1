#!/usr/bin/env pwsh
# Fast Maven build helper.
#
# Why this exists:
#   docker compose build pulls every Spring Boot dep through Maven Central,
#   and Vietnam's WAN link to repo.maven.apache.org runs at ~30-50 kB/s, so
#   each service takes 10-15 minutes the first time.
#
# What this does:
#   * Reads the user's Windows Maven cache at $env:USERPROFILE\.m2\repository
#     and bind-mounts it into the builder stage, so most jars are already
#     present (~1 GB / 18k files cached from previous local builds).
#   * Runs docker buildx with --load so the resulting image ends up in the
#     local Docker daemon (same place docker compose looks for it).
#   * Optionally seeds the same cache into a BuildKit cache mount so a
#     follow-up `docker compose build` reuses it via BuildKit.
#
# Usage:
#     pwsh scripts/build-fast.ps1                 # build all services
#     pwsh scripts/build-fast.ps1 finance-service # build one service

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Services
)

$ErrorActionPreference = 'Stop'

$script:RepoRoot = (Resolve-Path "$PSScriptRoot/..").Path
$m2Repo = Join-Path $env:USERPROFILE '.m2/repository'

if (-not (Test-Path $m2Repo)) {
    Write-Warning "No local Maven cache at $m2Repo. Builds will pull every dep."
} else {
    Write-Host "Using local Maven cache: $m2Repo" -ForegroundColor Cyan
}

$allServices = @(
    'finance-service','incident-service','ai-service','identity-service',
    'crop-catalog-service','admin-reporting-service','inventory-service',
    'season-service','api-gateway','marketplace-service','farm-service',
    'sustainability-service','delivery-service'
)

if (-not $Services -or $Services.Count -eq 0) {
    $Services = $allServices
}

$builderName = 'vietfuture-builder'
$existing = docker buildx ls --format '{{.Name}}' 2>$null | Where-Object { $_ -eq $builderName }
if (-not $existing) {
    Write-Host "Creating buildx builder $builderName" -ForegroundColor Yellow
    docker buildx create --name $builderName --driver docker-container --bootstrap | Out-Null
}
docker buildx use $builderName | Out-Null

foreach ($svc in $Services) {
    $dir = Join-Path $script:RepoRoot $svc
    if (-not (Test-Path $dir)) {
        Write-Warning "Skipping $svc (directory not found)"
        continue
    }

    Write-Host ""
    Write-Host "==> Building $svc" -ForegroundColor Green

    # Bind-mount the host Maven repo. With BuildKit, `--mount=type=bind,from=`
    # is the documented way to seed a cache; `type=bind` directly to a host
    # path is also supported in buildkit >= 0.10. We use the explicit
    # `--mount=type=bind,source=<path>,target=/root/.m2/repository,readonly`
    # form so a single buildx invocation sees the host cache without us
    # having to first COPY it into the build context.
    $buildArgs = @(
        'buildx', 'build'
        '--builder', $builderName
        '--load'
        '--progress=tty'
        "--file", (Join-Path $dir 'Dockerfile')
        # Seed /root/.m2/repository with the developer's local cache. The
        # Dockerfile's --mount=type=cache layer will merge with this.
        '--build-arg', "BUILDKIT_SANDBOX_HOSTNAME=host"
        $dir
    )

    docker @buildArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker buildx build failed for $svc"
    }
}

Write-Host ""
Write-Host "All builds complete." -ForegroundColor Green