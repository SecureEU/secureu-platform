from __future__ import annotations
import socket
import re
import subprocess
import xml.etree.ElementTree as ET


from cryptography import x509

OPENSSL_PROTOCOL_FLAG_RE = re.compile(
    r"^\s*-(?P<protocol>(?:ssl|tls)[1-9_]+\b)", re.MULTILINE
)


def is_port_open(host, port, timeout=5):
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return 1
    except (socket.timeout, socket.error):
        return 0


def get_openssl_version() -> str:
    cmd = ["openssl", "version"]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    return proc.stdout.strip()


def get_available_protocols() -> list[str]:
    cmd = ["openssl", "s_client", "--help"]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    help_str = proc.stderr.strip()
    return OPENSSL_PROTOCOL_FLAG_RE.findall(help_str)


def get_certificate(
        host: str, port: int, cipher: str, protocol_version: str
) -> x509.Certificate | None:

    if protocol_version == 'tls1_3':
        conn_cmd = [
            "openssl",
            "s_client",
            "-ciphersuites",
            cipher,
            f"-{protocol_version}",
            "-servername",
            host,
            "-connect",
            f"{host}:{port}",
        ]
    else:
        conn_cmd = [
            "openssl",
            "s_client",
            "-cipher",
            cipher,
            f"-{protocol_version}",
            "-servername",
            host,
            "-connect",
            f"{host}:{port}",
        ]

    conn_proc = subprocess.run(conn_cmd, stdin=subprocess.DEVNULL, capture_output=True)

    if conn_proc.returncode != 0:
        return None

    x509_cmd = ["openssl", "x509"]
    x509_proc = subprocess.run(x509_cmd, input=conn_proc.stdout, capture_output=True)

    if x509_proc.returncode != 0:
        return None

    return x509.load_pem_x509_certificate(x509_proc.stdout)


def get_supported_protocol_cipher_combinations(
        host: str, port: int
) -> tuple[dict[tuple[str, str], x509.Certificate], list[tuple[str, str]]]:
    supported_ciphers = {}
    unsupported_ciphers = []

    cmd = ["openssl", "ciphers", "ALL:eNULL"]
    proc = subprocess.run(cmd, capture_output=True, text=True)

    ciphers_str = proc.stdout.strip()
    ciphers = [cipher for cipher in ciphers_str.split(":")]

    available_protocols = get_available_protocols()
    for cipher in ciphers:
        for protocol_version in available_protocols:
            cert = get_certificate(host, port, cipher, protocol_version)
            if cert is not None:
                supported_ciphers[(protocol_version, cipher)] = cert
            else:
                unsupported_ciphers.append((protocol_version, cipher))
    return supported_ciphers, unsupported_ciphers



def get_protocols(host, port):
    open_port = is_port_open(host, port, timeout=3)
    if open_port == 0:
        return {
            "success": False,
            "response": f"Cannot connect to {host}:{port}",
        }
    try:
        supported, unsupported = get_supported_protocol_cipher_combinations(host, port)

        protocols = {}

        sslv3 = False
        tls1 = False
        tls1_1 = False

        weak_cipher_indicators = ["RC4", "EXPORT", "DES", "NULL", "MD5", "ECB", "3DES"]
        weak_ciphers = []

        for item in supported:
            if any(indicator in item[1].upper() for indicator in weak_cipher_indicators):
                weak_ciphers.append(item[1])
            if item[0] not in protocols:
                if "sslv3" == item[0]:
                    sslv3 = True
                if "tls1" == item[0]:
                    tls1 = True
                if "tls1_1" == item[0]:
                    tls1_1 = True
                protocols[item[0]] = [item[1]]
            else:
                protocols[item[0]].append(item[1])

        return {
            "success": True,
            "protocols": protocols,
            "weak_ciphers": weak_ciphers,
            "sslv3": sslv3,
            "tls1": tls1,
            "tls1_1": tls1_1,
        }
    except Exception as e:
        return {
            "success": False,
            "response": str(e)
        }

def run_nmap_ssl_enum(host, port='443'):
    try:
        result = subprocess.run(
            ["nmap", "--script", "ssl-enum-ciphers", "-p", str(port), host, "-oX", "-"],
            capture_output=True,
            text=True,
            timeout=30
        )
        if result.returncode != 0:
            return f"Error running nmap: {result.stderr}"

        xml_output = result.stdout
        return xml_output
    except subprocess.TimeoutExpired:
        return "Nmap scan timed out."

def extract_tls_info(xml_tree_data):
    root = ET.fromstring(xml_tree_data)
    tls_versions = {}
    for script in root.findall('.//script[@id="ssl-enum-ciphers"]'):
        for table in script.findall('table'):
            tls_version = table.get('key')
            if tls_version and tls_version.startswith("TLS"):
                ciphers = []
                ciphers_table = table.find('./table[@key="ciphers"]')
                if ciphers_table is not None:
                    for cipher_entry in ciphers_table.findall('table'):
                        cipher = {
                            'name': None,
                            'strength': None,
                        }
                        for elem in cipher_entry.findall('elem'):
                            key = elem.get('key')
                            if key in cipher:
                                cipher[key] = elem.text
                        ciphers.append(cipher)
                tls_versions[tls_version] = ciphers
    return tls_versions


def get_protocols_nmap(host, port):
    open_port = is_port_open(host, port, timeout=3)
    if open_port == 0:
        return {
            "success": False,
            "response": f"Cannot connect to {host}:{port}",
        }
    try:
        xml_data = run_nmap_ssl_enum(host, port)
        protocols = extract_tls_info(xml_data)

        return {
            "success": True,
            "protocols": protocols,
            "weak_ciphers": any(cipher.get('strength') != 'A' for ciphers in protocols.values() for cipher in ciphers if cipher.get('strength')),
            "sslv3": "SSLv3" in protocols,
            "tls1": "TLSv1.0" in protocols,
            "tls1_1": "TLSv1.1" in protocols,
        }
    except Exception as e:
        return {
            "success": False,
            "response": str(e)
        }
