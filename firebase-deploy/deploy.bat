@echo off
setlocal
cd /d "%~dp0"
set "PROJ=%~dp0.."
set "APK_SRC=%PROJ%\app\build\outputs\apk\debug\app-debug.apk"

echo.
echo === [1/4] Checking tools ===
where node >nul 2>nul
if errorlevel 1 (
  echo Node.js not found. Install it from https://nodejs.org then run this file again.
  pause
  exit /b 1
)
where firebase >nul 2>nul
if errorlevel 1 (
  echo Installing firebase-tools...
  call npm install -g firebase-tools
)

echo.
echo === [2/4] Looking for the APK ===
if exist "%~dp0public\office-attendance.apk" goto apk_ready
if exist "%APK_SRC%" goto have_apk
echo APK not found. Trying to build it...
if exist "%PROJ%\gradlew.bat" (
  pushd "%PROJ%"
  call gradlew.bat assembleDebug
  popd
  goto check_apk
)
where gradle >nul 2>nul
if not errorlevel 1 (
  pushd "%PROJ%"
  call gradle assembleDebug
  popd
  goto check_apk
)
:check_apk
if exist "%APK_SRC%" goto have_apk
echo.
echo Could not build the APK automatically.
echo Open the project in Android Studio and choose  Build ^> Build APK(s)
echo Then run this file again.
pause
exit /b 1

:have_apk
copy /Y "%APK_SRC%" "%~dp0public\office-attendance.apk" >nul
echo APK copied to public\office-attendance.apk
:apk_ready

echo.
echo === [3/4] Firebase project ===
if exist ".firebaserc" goto have_rc
set /p PID=Enter your Firebase Project ID: 
>.firebaserc echo {"projects":{"default":"%PID%"}}
:have_rc
call firebase projects:list >nul 2>nul
if errorlevel 1 call firebase login

echo.
echo === [4/4] Deploying ===
call firebase deploy --only hosting
if errorlevel 1 (
  echo Deploy failed. Check the message above.
  pause
  exit /b 1
)

echo.
echo ================================================
echo Done. Look for "Hosting URL" in the output above.
echo Share that link. Direct APK link = HostingURL/office-attendance.apk
echo ================================================
pause
