import json
import sqlite3
from flask import current_app, g, jsonify
import os
from datetime import datetime

def get_db():
    if "db" not in g:
        g.db = sqlite3.connect(
            current_app.config['DATABASE'],
            detect_types=sqlite3.PARSE_DECLTYPES
        )
        g.db.row_factory = sqlite3.Row
    return g.db


def close_db(e=None):
    db = g.pop("db", None)
    if db is not None:
        db.close()
    return


def init_db():
    db_path = current_app.config["DATABASE"]

    if not os.path.exists(db_path):
        db = get_db()
        with current_app.open_resource("./schema.sql") as f:
            db.executescript(f.read().decode('utf-8'))
        return


def init_app(app):
    app.teardown_appcontext(close_db)


def insert_scan_result(
    host, server_type, ssl_version, cipher, header_values, key_type, key_size,
    serial_number, subject, issuer, valid_until, expires_in, has_expired, status,
    rsa_modulus_n, rsa_modulus_e, protocols, ai_analysis, weak_ciphers, sslv3_supported, tls1_supported, tls1_1_supported, heartbleed
):
    db = get_db()
    cursor = db.cursor()

    query = '''
    INSERT INTO scans (
        host, server_type, ssl_version, cipher, header_values, key_type, key_size, 
        serial_number, subject, issuer, valid_until, expires_in, 
        has_expired, scan_month, status, rsa_modulus_n, rsa_modulus_e, protocols, ai_analysis, weak_ciphers, sslv3_supported, tls1_supported, tls1_1_supported, heartbleed 
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    '''

    scan_date = datetime.now()
    month_number = scan_date.month

    cursor.execute(query, (
        host,
        server_type,
        ssl_version,
        cipher,
        header_values,
        key_type,
        key_size,
        serial_number,
        subject,
        issuer,
        valid_until,
        expires_in,
        has_expired,
        month_number,
        status,
        rsa_modulus_n,
        rsa_modulus_e,
        protocols,
        ai_analysis,
        weak_ciphers,
        sslv3_supported,
        tls1_supported,
        tls1_1_supported,
        heartbleed,
    ))

    db.commit()
    db.close()

    return


def get_scans():
    db = get_db()
    cursor = db.cursor()

    cursor.execute("SELECT * FROM scans ORDER BY id DESC")
    scans = cursor.fetchall()

    results = []
    for row in scans:
        scan = dict(row)
        if 'header_values' in scan:
            try:
                scan['header_values'] = json.loads(scan['header_values'])
            except json.JSONDecodeError:
                scan['header_values'] = None
        if 'scan_date' in scan:
            scan['scan_date'] = scan['scan_date'].strftime('%Y-%m-%d %H:%M:%S')
        results.append(scan)


    return jsonify(results)


def get_latest_scans():
    db = get_db()
    cursor = db.cursor()

    cursor.execute(
        """SELECT * 
            FROM scans s
            WHERE (s.host, s.scan_date) IN (
                SELECT host, MAX(scan_date)
                FROM scans
                GROUP BY host
            )
        """
    )

    scans = cursor.fetchall()

    results = []
    for row in scans:
        scan = dict(row)
        if 'header_values' in scan:
            try:
                scan['header_values'] = json.loads(scan['header_values'])
            except json.JSONDecodeError:
                scan['header_values'] = None
        if 'scan_date' in scan:
            scan['scan_date'] = scan['scan_date'].strftime('%Y-%m-%d %H:%M:%S')
        results.append(scan)

    return jsonify(results)


def get_scan(scan_id):
    db = get_db()
    cursor = db.cursor()

    cursor.execute("SELECT * FROM scans WHERE id=?", (scan_id, ))

    result = cursor.fetchone()

    if result is None:
        return None

    if 'scan_date' in result:
        result['scan_date'] = result['scan_date'].strftime('%Y-%m-%d %H:%M:%S')

    columns = [column[0] for column in cursor.description]

    result = dict(zip(columns, result))

    return result

