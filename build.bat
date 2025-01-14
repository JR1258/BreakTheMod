@echo off
setlocal enabledelayedexpansion

:: Variables
set PROPERTIES_FILE=gradle.properties
set BACKUP_FILE=gradle.properties.bak

:: Target Values for Fabric (1.21.1)
set TARGET_MC_VERSION=1.21.1
set TARGET_YARN_MAPPINGS=1.21-rc1+build.1
set TARGET_LOADER_VERSION=0.17.0  :: Replace with the correct Fabric Loader version for 1.21.1

:: Check if gradle.properties exists
if not exist "%PROPERTIES_FILE%" (
    echo [ERROR] %PROPERTIES_FILE% not found. Ensure you are in the correct directory.
    exit /b 1
)

:: Backup the original gradle.properties
if exist "%BACKUP_FILE%" (
    echo [INFO] Removing old backup file.
    del "%BACKUP_FILE%"
)
echo [INFO] Backing up %PROPERTIES_FILE% to %BACKUP_FILE%.
copy "%PROPERTIES_FILE%" "%BACKUP_FILE%" >nul

:: Build with the default configuration
echo [INFO] Building with the default configuration.
gradlew build
if errorlevel 1 (
    echo [ERROR] Default build failed.
    goto :cleanup
)

:: Modify gradle.properties for 1.21.1
echo [INFO] Modifying %PROPERTIES_FILE% for version %TARGET_MC_VERSION%.
(for /f "delims=" %%A in ('type "%BACKUP_FILE%"') do (
    set "line=%%A"
    echo !line! | findstr /r "minecraft_version=" >nul
    if not errorlevel 1 (
        echo minecraft_version=%TARGET_MC_VERSION%
    ) else (
        echo !line! | findstr /r "yarn_mappings=" >nul
        if not errorlevel 1 (
            echo yarn_mappings=%TARGET_YARN_MAPPINGS%
        ) else (
            echo !line! | findstr /r "loader_version=" >nul
            if not errorlevel 1 (
                echo loader_version=%TARGET_LOADER_VERSION%
            ) else (
                echo !line!
            )
        )
    )
)) > "%PROPERTIES_FILE%"

:: Build with the modified configuration
echo [INFO] Building with the modified configuration.
gradlew build
if errorlevel 1 (
    echo [ERROR] Modified build failed.
    goto :cleanup
)

:: Cleanup and restore the original gradle.properties
:cleanup
echo [INFO] Restoring original %PROPERTIES_FILE%.
if exist "%BACKUP_FILE%" (
    move /y "%BACKUP_FILE%" "%PROPERTIES_FILE%" >nul
)
if "%~1"=="" (
    echo [INFO] Build process completed successfully.
    exit /b 0
) else (
    echo [INFO] Cleanup completed after errors.
    exit /b 1
)
