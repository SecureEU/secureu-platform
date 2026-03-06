'''
Initial Code : K.A.Draziotis (Nov.2024)
Licence : GPL v3
'''

import json
import sys
import socket
import ssl
from datetime import datetime, timezone
from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.asymmetric import rsa, ec
import certifi


def is_port_open(host, port, timeout=5):
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return 1
    except (socket.timeout, socket.error):
        return 0


def get_certificate_details(hostname, port):
    sock = None
    ssl_socket = None
    certificate_details = None

    context = ssl.create_default_context()
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE

    try:
        sock = socket.create_connection((hostname, port))
        ssl_socket = context.wrap_socket(sock, server_hostname=hostname)
        bin_cert = ssl_socket.getpeercert(True)
        cert = x509.load_der_x509_certificate(bin_cert, default_backend())
        key = cert.public_key()

        if isinstance(key, rsa.RSAPublicKey):
            key_type = 'RSA'
        elif isinstance(key, ec.EllipticCurvePublicKey):
            key_type = 'EC'
        else:
            key_type = 'Unknown'

        components = []
        subject = cert.subject
        for component in subject:
            components.append([component.oid._name, component.value])

        issuer = cert.issuer
        cn_attributes = issuer.get_attributes_for_oid(x509.NameOID.COMMON_NAME)
        issuers = ""
        for i, attribute in enumerate(cn_attributes):
            issuers += attribute.value
            if i != len(cn_attributes) - 1:
                issuers += ","


        expire_date = cert.not_valid_after_utc
        current_time = datetime.now(timezone.utc)

        if expire_date.tzinfo is None:
            expire_date = expire_date.replace(tzinfo=timezone.utc)

        expires_in = expire_date - current_time

        certificate_details = {
            "success": True,
            "key_type": key_type,
            "key_size": key.key_size,
            "serial_number": cert.serial_number,
            "subject": components,
            "issuer": issuers,
            "valid_until": expire_date,
            "expires_in": expires_in.days,
            "has_expired": expires_in.days < 0,
            "rsa_modulus_n": key.public_numbers().n if key_type == "RSA" else None,
            "rsa_modulus_e": key.public_numbers().e if key_type == "RSA" else None,
        }
    except Exception as e:
        certificate_details = {
            "success": False,
            "response": f"Error {str(e)}"
        }
    finally:
        if ssl_socket:
            ssl_socket.close()
        elif sock:
            sock.close()

    return certificate_details


def get_certificate(host, port):
    open_port = is_port_open(host, port, timeout=3)
    if open_port == 0:
        return {
            "success": False,
            "response": f"Cannot connect to {host}:{port}"
        }
    cert = get_certificate_details(host, port)
    return cert