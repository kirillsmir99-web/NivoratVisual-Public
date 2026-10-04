@echo off
setlocal
if "%~1"=="" (
    echo Usage: switch_edition.bat [pro^|free]
    echo   pro  - NivoratVisual (32 wing skins, 3D rotating preview, full modules)
    echo   free - NivoratFreeVisual (1 purple wing skin, watermark without Free)
    exit /b 1
)
python tools\install_edition.py %1
