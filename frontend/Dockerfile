FROM node:20-alpine AS builder
WORKDIR /app

# In a real environment we would copy package.json and run npm install here.
# Since we are just scaffolding the structure, we mock the dist folder creation.
COPY package.json ./
COPY src/ ./src/
RUN mkdir -p dist && echo "<h1>AI Helpdesk Frontend</h1><p>Served via NGINX</p>" > dist/index.html

FROM nginx:alpine
# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Provide a default nginx config for React SPA routing
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
