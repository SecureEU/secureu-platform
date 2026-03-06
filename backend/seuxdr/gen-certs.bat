@echo off
SETLOCAL

REM === Check for OpenSSL ===
where openssl >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo OpenSSL not found. Attempting to install via Scoop...

    REM Check if Scoop is installed
    where scoop >nul 2>&1
    IF %ERRORLEVEL% NEQ 0 (
        echo Scoop not found. Installing Scoop...

        REM Ensure PowerShell is available
        powershell -Command "Set-ExecutionPolicy RemoteSigned -Scope CurrentUser -Force"
        powershell -Command "Invoke-RestMethod -Uri 'https://get.scoop.sh' | Invoke-Expression"

        REM Add Scoop to PATH for this session
        SET "PATH=%USERPROFILE%\scoop\shims;%PATH%"
    )

    REM Install OpenSSL using Scoop
    scoop install openssl

    REM Refresh path
    SET "PATH=%USERPROFILE%\scoop\shims;%PATH%"

    REM Check again if openssl is available
    where openssl >nul 2>&1
    IF %ERRORLEVEL% NEQ 0 (
        echo OpenSSL installation failed or not in PATH.
        goto end
    )
)

echo OpenSSL is available. Continuing...

REM === Make certs directories if they don't exist ===
IF NOT EXIST manager\certs (
    mkdir manager\certs
)
IF NOT EXIST manager_front\certs (
    mkdir manager_front\certs
)

REM === CA Setup ===
IF NOT EXIST manager\certs\server-ca.key (
    echo Generating CA private key...
    openssl genrsa -out manager\certs\server-ca.key 2048
)

IF NOT EXIST manager\certs\server-ca.crt (
    echo Generating CA certificate...
    openssl req -new -x509 -nodes -days 1000 ^
        -key manager\certs\server-ca.key ^
        -out manager\certs\server-ca.crt ^
        -subj "/C=CY/O=Clone Systems/OU=CS/CN=testServerCA"
)

REM === Copy CA to agent ===
IF NOT EXIST agent\certs\server-ca.crt (
    IF NOT EXIST agent\certs (
        mkdir agent\certs
    )
    copy manager\certs\server-ca.crt agent\certs\
)

REM === Server TLS Cert ===
IF NOT EXIST manager\certs\server.key (
    echo Generating server key and CSR...
    openssl req -newkey rsa:2048 -nodes ^
        -keyout manager\certs\server.key ^
        -out manager\certs\server.req ^
        -subj "/C=CY/O=Clone Systems/OU=CS/CN=testServerTLS"
)

IF NOT EXIST manager\certs\server.crt (
    echo Signing server certificate...
    openssl x509 -req -in manager\certs\server.req -days 398 ^
        -CA manager\certs\server-ca.crt ^
        -CAkey manager\certs\server-ca.key ^
        -set_serial 01 ^
        -out manager\certs\server.crt ^
        -extfile localhost.ext
)

REM === Frontend TLS Cert ===
IF NOT EXIST manager_front\certs\frontend.key (
    echo Generating frontend key and CSR...
    openssl req -newkey rsa:2048 -nodes ^
        -keyout manager_front\certs\frontend.key ^
        -out manager_front\certs\frontend.req ^
        -subj "/C=CY/O=Clone Systems/OU=CS/CN=frontend.local"
)

IF NOT EXIST manager_front\certs\frontend.crt (
    echo Signing frontend certificate...
    openssl x509 -req -in manager_front\certs\frontend.req -days 398 ^
        -CA manager\certs\server-ca.crt ^
        -CAkey manager\certs\server-ca.key ^
        -set_serial 02 ^
        -out manager_front\certs\frontend.crt ^
        -extfile localhost.ext
)

REM === Encryption Key ===
IF NOT EXIST manager\certs\encryption_key.pem (
    echo Generating encryption key...
    openssl genrsa -out manager\certs\encryption_key.pem 2048
    openssl rsa -in manager\certs\encryption_key.pem -pubout -out manager\certs\encryption_pubkey.pem
)

# === RS256 JWT Key Pair ===
IF NOT EXIST manager/certs/jwt_private.key (
    echo Generating JWT keys...
    openssl genrsa -out manager/certs/jwt_private.key 2048
    openssl rsa -in manager/certs/jwt_private.key -pubout -out manager/certs/jwt_public.key
)


REM === Cleanup ===
del /Q manager\certs\server.req
del /Q manager_front\certs\frontend.req

echo Certificate setup completed.

:end
ENDLOCAL
pause
