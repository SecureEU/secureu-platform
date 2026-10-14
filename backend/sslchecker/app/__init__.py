from flask import Flask
from flask_cors import CORS
from .routes import main_blueprint

def create_app():
    app = Flask(__name__)
    app.config["DATABASE"] = 'scans.db'
    CORS(app, supports_credentials=True)
    app.register_blueprint(main_blueprint)

    from . import db
    db.init_app(app)

    with app.app_context():
        db.init_db()

    return app


