FROM python:3.11-slim

WORKDIR /app

# Install NLTK data at build time to avoid runtime downloads
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt && \
    python -c "import nltk; nltk.download('stopwords'); nltk.download('punkt'); nltk.download('punkt_tab'); nltk.download('wordnet')"

COPY . .

EXPOSE 5002
CMD ["python", "Vulnerability_Score_Prediction.py"]
