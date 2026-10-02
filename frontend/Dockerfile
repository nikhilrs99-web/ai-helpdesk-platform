FROM node:20-alpine AS builder
WORKDIR /app

# Separate dependency layer from source so `npm ci` only re-runs when package*.json
# actually changes, not on every source edit.
COPY package.json package-lock.json* ./
RUN npm ci

COPY index.html vite.config.ts tsconfig.json tailwind.config.js postcss.config.js ./
COPY src/ ./src/
RUN npm run build

FROM nginx:alpine
# Run as nginx's own built-in unprivileged user instead of root. Binding to port 80
# specifically needs root (or CAP_NET_BIND_SERVICE) on Linux, so this also moves nginx to
# listen on 8080 - an ordinary, unprivileged port - to make running as non-root actually
# work rather than fail at startup with "Permission denied".
COPY --from=builder --chown=nginx:nginx /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN touch /var/run/nginx.pid && chown -R nginx:nginx /var/run/nginx.pid /var/cache/nginx

USER nginx
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
