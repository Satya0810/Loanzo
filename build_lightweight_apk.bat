@echo off
title Loanzo - Build Lightweight APK
echo ========================================================
echo   Building Clean, Lightweight Loanzo APK (28.7 MB)
echo ========================================================
echo.
call gradlew.bat clean assembleDebug
echo.
echo ========================================================
echo   BUILD COMPLETED!
echo   Clean APK: app\build\outputs\apk\debug\app-debug.apk
echo ========================================================
pause
