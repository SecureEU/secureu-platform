import sys
import os

project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../'))
if project_root not in sys.path:
    sys.path.insert(0, project_root)

from app import create_app

app = create_app()

if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True, port=5000)