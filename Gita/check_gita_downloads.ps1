Param(
    [Parameter(Mandatory=$false)] [string] $Path,
    [Parameter(Mandatory=$false)] [int] $ExpectedCount = 700,
    [Parameter(Mandatory=$false)] [switch] $ResetNotification
)

if (-not $Path -or $Path.Trim().Length -eq 0) {
    $Path = (Get-Location).Path
}

$resolvedPath = Resolve-Path -LiteralPath $Path -ErrorAction SilentlyContinue
if (-not $resolvedPath) {
    Write-Error "Path not found: $Path"
    exit 1
}
$resolvedPath = $resolvedPath.Path

$flagPath = Join-Path $resolvedPath ".all_verses_downloaded.flag"

if ($ResetNotification) {
    if (Test-Path $flagPath) { Remove-Item -LiteralPath $flagPath -Force -ErrorAction SilentlyContinue }
}

# Count all files recursively under the target path
$fileCount = (Get-ChildItem -LiteralPath $resolvedPath -File -Recurse -ErrorAction SilentlyContinue).Count

Write-Output "Scan path: $resolvedPath"
Write-Output "Found files: $fileCount"
Write-Output "Expected verses: $ExpectedCount"

function Show-CompletionNotification {
    param([string] $Title, [string] $Body)

    $burntToastAvailable = Get-Module -ListAvailable -Name BurntToast -ErrorAction SilentlyContinue
    if ($burntToastAvailable) {
        Import-Module BurntToast -ErrorAction SilentlyContinue | Out-Null
        try {
            New-BurntToastNotification -Text $Title, $Body | Out-Null
            return
        } catch {
            # fall through to message box
        }
    }

    # Fallback to a simple message box if Toast is unavailable
    Add-Type -AssemblyName System.Windows.Forms -ErrorAction SilentlyContinue
    [System.Windows.Forms.MessageBox]::Show($Body, $Title, [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
}

if ($fileCount -ge $ExpectedCount) {
    if (-not (Test-Path -LiteralPath $flagPath)) {
        Show-CompletionNotification -Title "All verses downloaded" -Body "You can read offline now."
        # create a flag so we only notify once
        New-Item -ItemType File -Path $flagPath -Force | Out-Null
        Write-Output "Notification sent. Flag created at: $flagPath"
    } else {
        Write-Output "All verses already downloaded. Notification previously sent (flag exists)."
    }
} else {
    $remaining = $ExpectedCount - $fileCount
    if ($remaining -lt 0) { $remaining = 0 }
    Write-Output "Not complete. Remaining (approx): $remaining"
}

