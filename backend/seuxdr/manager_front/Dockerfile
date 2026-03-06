# ---- Build Stage ----
    FROM node:18-alpine AS builder

    # Set working directory inside the container
    WORKDIR /app
    
    # Copy package.json and lock file separately (for caching)
    COPY package.json package-lock.json ./
    
    # Install dependencies
    RUN npm install --frozen-lockfile
    
    # Copy the rest of the application
    COPY . .
    
    # Build the production version of the app
    RUN npm run build
    
    # ---- Serve Stage ----
    FROM nginx:alpine
    
    # Set working directory inside the container
    WORKDIR /usr/share/nginx/html
    
    # Remove default Nginx static assets
    RUN rm -rf ./*

    # Copy custom Nginx configuration
    COPY nginx.conf /etc/nginx/conf.d/default.conf

    # ✅ Copy certificates into /etc/nginx/certs
    COPY certs /etc/nginx/certs

    # Copy build output from previous stage
    COPY --from=builder /app/dist .
    
    # Expose port 8080
    EXPOSE 8080
    
    # Start Nginx when the container runs
    CMD ["nginx", "-g", "daemon off;"]
    