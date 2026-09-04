# Run the labelled VCD benchmark from repo root.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
$VenvPy = Join-Path $Root "modules\voice-clone-defence\tools\.venv\Scripts\python.exe"
if (Test-Path $VenvPy) {
    & $VenvPy eval/benchmark.py @args
} else {
    python eval/benchmark.py @args
}
