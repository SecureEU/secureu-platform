FROM python:3.13.1

# Set working directory inside the container
WORKDIR /app

# Copy everything from your project folder to /app inside the container
COPY . .
# # Install system dependencies (Tor)
RUN apt-get update && apt-get install -y tor

# Upgrade pip and install dependencies
RUN pip install --no-cache-dir -r requirements.txt

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]