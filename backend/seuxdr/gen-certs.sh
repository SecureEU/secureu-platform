#!/bin/bash

# Make certs directories if they don't exist
mkdir -p manager/certs
mkdir -p manager_front/certs

# === CA Setup ===
if [ ! -f manager/certs/server-ca.key ]; then
    echo "Generating CA private key..."
    openssl genrsa -out manager/certs/server-ca.key 2048
fi

if [ ! -f manager/certs/server-ca.crt ]; then
    echo "Generating CA certificate..."
    openssl req -new -x509 -nodes -days 1000 \
        -key manager/certs/server-ca.key \
        -out manager/certs/server-ca.crt \
        -subj "/C=CY/O=Clone Systems/OU=CS/CN=testServerCA"
fi

# === Copy CA to agent ===
if [ ! -f agent/certs/server-ca.crt ]; then
    mkdir -p agent/certs
    cp manager/certs/server-ca.crt agent/certs
fi

# === Server TLS Cert ===
if [ ! -f manager/certs/server.key ]; then
    echo "Generating server key and CSR..."
    openssl req -newkey rsa:2048 -nodes \
        -keyout manager/certs/server.key \
        -out manager/certs/server.req \
        -subj "/C=CY/O=Clone Systems/OU=CS/CN=testServerTLS"
fi

if [ ! -f manager/certs/server.crt ]; then
    echo "Signing server certificate..."
    openssl x509 -req -in manager/certs/server.req -days 398 \
        -CA manager/certs/server-ca.crt \
        -CAkey manager/certs/server-ca.key \
        -set_serial 01 \
        -out manager/certs/server.crt \
        -extfile localhost.ext
fi

# === Frontend TLS Cert ===
if [ ! -f manager_front/certs/frontend.key ]; then
    echo "Generating frontend key and CSR..."
    openssl req -newkey rsa:2048 -nodes \
        -keyout manager_front/certs/frontend.key \
        -out manager_front/certs/frontend.req \
        -subj "/C=CY/O=Clone Systems/OU=CS/CN=frontend.local"
fi

if [ ! -f manager_front/certs/frontend.crt ]; then
    echo "Signing frontend certificate..."
    openssl x509 -req -in manager_front/certs/frontend.req -days 398 \
        -CA manager/certs/server-ca.crt \
        -CAkey manager/certs/server-ca.key \
        -set_serial 02 \
        -out manager_front/certs/frontend.crt \
        -extfile localhost.ext
fi

# === Encryption Key ===
if [ ! -f manager/certs/encryption_key.pem ]; then
    echo "Generating encryption key..."
    openssl genrsa -out manager/certs/encryption_key.pem 2048
    openssl rsa -in manager/certs/encryption_key.pem -pubout -out manager/certs/encryption_pubkey.pem
fi

# === RS256 JWT Key Pair ===
if [ ! -f manager/certs/jwt_private.key ]; then
    echo "Generating JWT RS256 private key..."
    openssl genrsa -out manager/certs/jwt_private.key 2048
fi

if [ ! -f manager/certs/jwt_public.key ]; then
    echo "Generating JWT RS256 public key..."
    openssl rsa -in manager/certs/jwt_private.key -pubout -out manager/certs/jwt_public.key
fi

# === Cleanup ===
rm -f manager/certs/server.req
rm -f manager_front/certs/frontend.req

echo "Certificate setup completed."
