@echo off
title LiveEasy - Install FFmpeg
setlocal EnableDelayedExpansion

set "ROOT=%~dp0"
set "FFMPEG_DIR=%ROOT%tools\ffmpeg"
set "FFMPEG_EXE=%FFMPEG_DIR%\bin\ffmpeg.exe"
set "FFPROBE_EXE=%FFMPEG_DIR%\bin\ffprobe.exe"
set "ZIP=%TEMP%\easylive-ffmpeg.zip"
set "URL=https://github.com/GyanD/codexffmpeg/releases/download/7.1/ffmpeg-7.1-essentials_build.zip"
set "SILENT=%~1"
set "EXIT_CODE=0"

if exist "%FFMPEG_EXE%" if exist "%FFPROBE_EXE%" (
    echo [OK] FFmpeg already exists:
    echo %FFMPEG_DIR%\bin
    "%FFMPEG_EXE%" -version 2>nul | findstr /i "ffmpeg version"
    goto :END
)

echo.
echo Downloading FFmpeg to:
echo %FFMPEG_DIR%
echo.
echo URL: %URL%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; try { Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP%' -UseBasicParsing; exit 0 } catch { Write-Host ('Download failed: ' + $_.Exception.Message); exit 1 }"
if errorlevel 1 goto :FAIL

echo Extracting...
if exist "%ROOT%tools\ffmpeg-dl" rmdir /s /q "%ROOT%tools\ffmpeg-dl"
mkdir "%ROOT%tools\ffmpeg-dl" 2>nul

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Expand-Archive -Path '%ZIP%' -DestinationPath '%ROOT%tools\ffmpeg-dl' -Force"
if errorlevel 1 goto :FAIL

if exist "%FFMPEG_DIR%" rmdir /s /q "%FFMPEG_DIR%"

for /d %%D in ("%ROOT%tools\ffmpeg-dl\ffmpeg-*") do (
    move "%%~D" "%FFMPEG_DIR%" >nul
)

del /f /q "%ZIP%" 2>nul
rmdir /s /q "%ROOT%tools\ffmpeg-dl" 2>nul

if not exist "%FFMPEG_EXE%" (
    echo [ERROR] ffmpeg.exe not found after install.
    goto :FAIL
)

echo.
echo [OK] FFmpeg installed:
"%FFMPEG_EXE%" -version 2>nul | findstr /i "ffmpeg version"
"%FFPROBE_EXE%" -version 2>nul | findstr /i "ffprobe version"
echo.
echo Path: %FFMPEG_DIR%\bin
echo Next: run start-web.bat or start-admin.bat
goto :END

:FAIL
set "EXIT_CODE=1"
echo.
echo [ERROR] FFmpeg install failed. Check network and try again.

:END
if /i not "%SILENT%"=="silent" (
    echo.
    echo Press any key to close...
    pause >nul
)
exit /b %EXIT_CODE%
