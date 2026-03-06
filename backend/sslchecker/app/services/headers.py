'''
Initial Code : K.A.Draziotis (Nov.2024)
Licence : GPL v3
'''

import json
import sys
import socket
import ssl
import certifi
import http.client
from urllib.parse import urlparse

def is_port_open(host, port, timeout=5):
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return 1
    except (socket.timeout, socket.error):
        return 0


def get_headers_info(url, port):
    header_details = None
    if not url.startswith(('http://', 'https://')):
        url = "https://" + url + ":" + str(port)

    parsed_url = urlparse(url)
    hostname = parsed_url.hostname
    port = parsed_url.port if parsed_url.port else (443 if parsed_url.scheme == "https" else 80)
    path = parsed_url.path if parsed_url.path else '/'
    timeout = 2
    try:
        if parsed_url.scheme == "https":
            context = ssl.create_default_context()
            context.check_hostname = False
            context.verify_mode = ssl.CERT_NONE
            conn = http.client.HTTPSConnection(hostname, port=port, context=context)
        else:
            conn = http.client.HTTPConnection(hostname, port=port)

        conn.connect()

        sock = conn.sock
        ssl_version = sock.version()
        cipher = sock.cipher()

        conn.request("HEAD", path)
        response = conn.getresponse()

        headers = response.getheaders()
        header_data = {}
        for header, value in headers:
            header_lower = header.lower()
            if header_lower == 'expires':
                value = ''
            if header_lower == "server":
                value = value.split('/')[0].strip()

            header_data[header_lower] = value

        if "server" not in header_data.keys():
            header_data["server"] = "CDN"

        header_details = {
            "success": True,
            "ssl_version": ssl_version,
            "cipher_suite": cipher[0],
            "header_values": header_data,
        }

    except ssl.SSLError as e:
        header_details = {
            "success": False,
            "response": f"SSL error occurred: {str(e)}"
        }
    except socket.timeout:
        header_details = {
            "success": False,
            "response": f"Error: Connection to {hostname}:{port} timed out after {timeout} seconds"
        }
        header_details = {"error": f"Error: Connection to {hostname}:{port} timed out after {timeout} seconds."}
        return header_details
    except socket.error as e:
        header_details = {
            "success": False,
            "response": f"Socket error occurred: {str(e)}"
        }
    except Exception as e:
        header_details = {
            "success": False,
            "response": f"An unexpected error occurred: {str(e)}"
        }
    except (socket.gaierror, socket.timeout):
        header_details = {
            "success": False,
            "response": f"Error: Unable to reach {hostname}:{port}. The server may be down or unreachable"
        }
        return  header_details
    finally:
        try:
            conn.close()
            return header_details
        except Exception as e:
            return {
                "success": False,
                "response": str(e)
            }

def get_header(host, port):
    open_port = is_port_open(host, port, timeout=3)
    if open_port == 0:
        return {
            "success": False,
            "response": f"Cannot connect to {host}:{port}"
        }

    headers = get_headers_info(host, port)

    return headers