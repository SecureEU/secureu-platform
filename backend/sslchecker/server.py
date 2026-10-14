import sys
import os

project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../'))
if project_root not in sys.path:
    sys.path.insert(0, project_root)

from app import create_app

app = create_app()

if __name__ == '__main__':
    # Local development only. In the container gunicorn imports `app` from this
    # module instead - see the Dockerfile. debug=True here would expose the
    # Werkzeug console, which executes arbitrary Python from the browser.
    app.run(host='127.0.0.1', debug=os.getenv("FLASK_DEBUG") == "1", port=5000)