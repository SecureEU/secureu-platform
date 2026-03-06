from flask import Flask
from flask_cors import CORS
from .routes import main_blueprint
from flask_session import Session
import os
from datetime import timedelta

def create_app():
    app = Flask(__name__)
    app.config["DATABASE"] = 'scans.db'
    app.config["SECRET_KEY"] = os.urandom(24)
    app.config['SESSION_TYPE'] = 'filesystem'
    app.config['SESSION_FILE_DIR'] = 'flask_session'
    app.config["SESSION_PERMANENT"] = False
    CORS(app, supports_credentials=True)
    app.register_blueprint(main_blueprint)
    Session(app)

    from . import db
    db.init_app(app)

    with app.app_context():
        db.init_db()

    return app


