$srcBase = "\\wsl.localhost\Ubuntu\home\kali\iot\XiaoMi\BE10000Pro\ubi_output\root.ubi\rootfs\usr\lib\lua"
$outBase = "G:\HackerOne\Xiaomi\lua"
$jar = "E:\Workspace\Trae\Relua\target\relua-1.0.0.jar"
$deps = "E:\Workspace\Trae\Relua\target\dependency\*"
$cp = "$jar;$deps"
$jvmArgs = @("-Xmx2g", "-Xss4m")

# Get file list from WSL
$wslFiles = wsl.exe -d Ubuntu -- bash -c "find /home/kali/iot/XiaoMi/BE10000Pro/ubi_output/root.ubi/rootfs/usr/lib/lua -name '*.lua'" 2>&1

$total = 0
$success = 0
$errors = @()

foreach ($wslPath in $wslFiles) {
    if ([string]::IsNullOrWhiteSpace($wslPath)) { continue }
    $wslPath = $wslPath.Trim()

    # Convert WSL path to Windows path
    $winPath = wsl.exe -d Ubuntu -- bash -c "wslpath -w '$wslPath'" 2>&1
    $winPath = $winPath.Trim()

    # Compute relative path
    $linuxBase = "/home/kali/iot/XiaoMi/BE10000Pro/ubi_output/root.ubi/rootfs/usr/lib/lua"
    $relPath = $wslPath.Substring($linuxBase.Length).TrimStart('/')

    # Compute output path
    $outPath = Join-Path $outBase $relPath
    $outDir = Split-Path $outPath -Parent

    # Create output directory
    if (!(Test-Path $outDir)) {
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
    }

    $total++

    # Run decompiler
    $runArgs = @("-cp", $cp, "com.github.relua.Relua", "-o", $outPath, $winPath)
    $result = & java $jvmArgs $runArgs 2>&1

    if ($LASTEXITCODE -eq 0) {
        $success++
    } else {
        $errorMsg = ($result | Out-String).Trim()
        $errors += [PSCustomObject]@{
            File = $relPath
            ExitCode = $LASTEXITCODE
            Error = $errorMsg
        }
    }
}

Write-Host ""
Write-Host "=============================="
Write-Host "Total: $total | Success: $success | Failed: $($errors.Count)"
Write-Host "=============================="

if ($errors.Count -gt 0) {
    Write-Host ""
    Write-Host "Failed files:"
    foreach ($e in $errors) {
        Write-Host "  [$($e.ExitCode)] $($e.File)"
        if ($e.Error) {
            $firstLine = ($e.Error -split "`n")[0]
            Write-Host "       $firstLine"
        }
    }
}
