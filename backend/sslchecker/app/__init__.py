import os

from flask import Flask
from flask_cors import CORS
from .routes import main_blueprint


def create_app():
    app = Flask(__name__)
    # Default to a path the compose file mounts a volume on. It used to be a
    # bare relative filename inside the image, so every container rebuild threw
    # away the entire scan history.
    db_path = os.getenv("SSLCHECKER_DB", "/data/scans.db")
    parent = os.path.dirname(db_path)
    if parent:
        os.makedirs(parent, exist_ok=True)
    app.config["DATABASE"] = db_path
    CORS(app, supports_credentials=True)
    app.register_blueprint(main_blueprint)

    from . import db
    db.init_app(app)

    with app.app_context():
        db.init_db()

    return app


