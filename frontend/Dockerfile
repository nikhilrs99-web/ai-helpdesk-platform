FROM node:26-alpine AS builder
WORKDIR /app

# In a real environment we would copy package.json and run npm install here.
# Since we are just scaffolding the structure, we mock the dist folder creation.
COPY package.json ./
COPY src/ ./src/
RUN mkdir -p dist && echo "<h1>AI Helpdesk Frontend</h1><p>Served via NGINX</p>" > dist/index.html

FROM nginx:alpine
# Run as nginx's own built-in unprivileged user instead of root. Binding to port 80
# specifically needs root (or CAP_NET_BIND_SERVICE) on Linux, so this also moves nginx to
# listen on 8080 - an ordinary, unprivileged port - to make running as non-root actually
# work rather than fail at startup with "Permission denied".
COPY --from=builder --chown=nginx:nginx /app/dist /usr/share/nginx/html

# Provide a default nginx config for React SPA routing
RUN echo 'server { \
    listen 8080; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf \
    && touch /var/run/nginx.pid \
    && chown -R nginx:nginx /var/run/nginx.pid /var/cache/nginx

USER nginx
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
