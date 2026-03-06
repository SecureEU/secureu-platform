import json
from flask import Blueprint
from flask import jsonify, request
from .db import insert_scan_result, get_scans, get_scan, get_latest_scans
from app.services.certificate import get_certificate
from app.services.headers import get_header
from app.services.protocols import get_protocols_nmap
from app.services.ai_overview import get_deepseek_overview, analyze_with_deepseek
from app.services.heartbleed import check_heartbleed
from flask import session

main_blueprint = Blueprint('main', __name__)

@main_blueprint.route('/api/scan', methods=['POST'])
@main_blueprint.route('/api/check', methods=['POST'])
def check_ssl():
    data = request.get_json()
    hostname = data.get('hostname')
    port = int(data.get('port', 443))
    generate_ai = data.get("generateAI")

    try:
        headers = get_header(hostname, port)
        cert_details = get_certificate(hostname, port)
        cipher_details = get_protocols_nmap(hostname, port)
        heartbleed = check_heartbleed(hostname, port)

        status = True
        if headers.get("success") is False and cert_details.get("success") is False or cipher_details.get("protocols") is None:
            status = False

        supports_sslv3 = False
        if not status:
            if "SSL" in headers.get("response") or "SSLv3" in cert_details.get("response"):
                supports_sslv3 = True
            else:
                return f"{headers.get('response')}, {cert_details.get('response')}", 500

        ai_analysis_text = "No AI analysis was generated. Please make sure to set up your API key and have the checkbox in the scanner tab checked"
        if generate_ai:
            api_key = session.get("api_key")
            if api_key:
                ai_analysis_response = analyze_with_deepseek(headers,  cert_details, cipher_details, heartbleed, api_key)
                ai_analysis_text = ai_analysis_response.get("response")

        insert_scan_result(
            hostname,
            str(headers.get("header_values", {}).get("server")),
            str(headers.get("ssl_version")),
            str(headers.get("cipher_suite")),
            json.dumps(headers.get("header_values")),
            str(cert_details.get("key_type")),
            str(cert_details.get("key_size")),
            str(cert_details.get("serial_number")),
            str(cert_details.get("subject")),
            str(cert_details.get("issuer")),
            str(cert_details.get("valid_until")),
            str(cert_details.get("expires_in")),
            cert_details.get("has_expired"),
            status,
            str(cert_details.get("rsa_modulus_n")),
            str(cert_details.get("rsa_modulus_e")),
            str(json.dumps(cipher_details.get("protocols"))),
            str(ai_analysis_text),
            cipher_details.get("weak_ciphers"),
            cipher_details.get("sslv3") or supports_sslv3,
            cipher_details.get("tls1"),
            cipher_details.get("tls1_1"),
            heartbleed.get("heartbleed")
        )

        return "", 204

    except Exception as e:
        print(e)
        return jsonify({
            "error": str(e)
        }), 500


@main_blueprint.route('/api/scans', methods=['GET'])
def get_all_scans():
    results = get_scans()
    return results


@main_blueprint.route("/api/host", methods=["POST"])
def get_host_details():
    data = request.get_json()
    scan_id = data.get("id")
    if not scan_id:
        return jsonify({"error": "id is required"}), 400

    row = get_scan(int(scan_id))
    if row is None:
        return jsonify({"error": "Scan not found"}), 404

    # Parse header_values JSON
    header_values = {}
    if row.get("header_values"):
        try:
            header_values = json.loads(row["header_values"]) if isinstance(row["header_values"], str) else row["header_values"]
        except (json.JSONDecodeError, TypeError):
            header_values = {}

    # Parse protocols JSON
    protocols_json = None
    if row.get("protocols"):
        try:
            protocols_json = json.loads(row["protocols"]) if isinstance(row["protocols"], str) else row["protocols"]
        except (json.JSONDecodeError, TypeError):
            protocols_json = None

    return jsonify({
        "scan": {
            "host": row.get("host"),
            "port": 443,
            "server": row.get("server_type"),
            "date": row.get("scan_date"),
            "vulnerable": row.get("weak_ciphers") or row.get("heartbleed") or row.get("sslv3_supported"),
            "has_issues": not row.get("status"),
        },
        "certificate": {
            "ssl_version": row.get("ssl_version"),
            "cipher_suite": row.get("cipher"),
            "key_type": row.get("key_type"),
            "key_size": row.get("key_size"),
            "issuers": row.get("issuer"),
            "expire_date": row.get("valid_until"),
            "expires_in": row.get("expires_in"),
            "has_expired": row.get("has_expired"),
            "serial_number": row.get("serial_number"),
        },
        "protocols": {
            "success": True,
            "rc4": False,
            "heartbleed": row.get("heartbleed"),
            "poodle": row.get("sslv3_supported"),
            "beast": row.get("tls1_supported"),
            "crime": False,
            "freak": False,
            "logjam": False,
            "sweet32": False,
            "insecure_ciphers": row.get("weak_ciphers"),
            "weak_ciphers": row.get("weak_ciphers"),
            "protocols_json": protocols_json,
        },
        "headers": {
            "success": bool(header_values),
            "strict_transport_security": header_values.get("strict-transport-security"),
            "content_security_policy": header_values.get("content-security-policy"),
            "x_frame_options": header_values.get("x-frame-options"),
            "x_content_type_options": header_values.get("x-content-type-options"),
            "x_xss_protection": header_values.get("x-xss-protection"),
            "referrer_policy": header_values.get("referrer-policy"),
            "permissions_policy": header_values.get("permissions-policy"),
            "cross_origin_opener_policy": header_values.get("cross-origin-opener-policy"),
            "cross_origin_embedder_policy": header_values.get("cross-origin-embedder-policy"),
            "cross_origin_resource_policy": header_values.get("cross-origin-resource-policy"),
        },
        "ai": {
            "success": bool(row.get("ai_analysis")),
            "analysis": row.get("ai_analysis"),
        },
        "subjects": [{"value": row.get("subject")}] if row.get("subject") else [],
    })


@main_blueprint.route("/api/latest", methods=["GET"])
def get_latest():
    results = get_latest_scans()
    return results

@main_blueprint.route("/api/ask", methods=['POST'])
def get_ai_response():
    data = request.get_json()
    question = data.get('question')
    scan_id = int(data.get('scan_id'))

    if scan_id is None:
        return jsonify({
            "error": "There was an error"
        }), 500

    host_data = get_scan(int(scan_id))
    api_key = session.get("api_key")
    if api_key:
        response = get_deepseek_overview(question, host_data, api_key)
        return response
    else:
        return {
            "success": False,
            "response": "You haven't set your API key",
        }


@main_blueprint.route("/api/set", methods=["POST"])
def set_api_key():
    data = request.get_json()
    api_key = data.get("api_key")
    if api_key:
        session["api_key"] = api_key
        return "The API key is set", 204
    return "API key is required", 400


@main_blueprint.route("/api/get", methods=["GET"])
def get_api_key():
    api_key = session.get('api_key')
    if api_key:
        return jsonify({"api_key_set": True})
    else:
        return jsonify({"api_key_set": False})


@main_blueprint.route('/')
def home():
    return jsonify({
        "status": "running",
        "endpoints": {
            "/api/check": "POST - Check SSL/TLS configuration",
            "/api/scans": "GET - Get all scans",
            "/api/ask": "POST - Ask AI",
            "/api/set": "POST - Set API Key",
            "/api/latest": "GET = Get latest scans for each host"
        }
    })
