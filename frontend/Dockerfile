# User-defined build stages
ARG BUILD_TARGET=default

# Multi-architecture build support
FROM --platform=linux/amd64 --target=linux/amd64 as base-amd64
FROM --platform=linux/arm64 --target=linux/arm64 as base-arm64
FROM --platform=linux/arm64/v8 --target=linux/arm64 as base-arm64v8

# Node.js base image
FROM node:18-alpine AS base
ARG NODE_ENV=production
ENV NODE_ENV=$NODE_ENV
ARG VITE_API_URL=http://localhost:8080
ARG VITE_WS_URL=ws://localhost:8085

WORKDIR /app

# Install dependencies
RUN apk add --no-cache curl git

# Copy package files
COPY package*.json ./

# Install npm dependencies
RUN npm ci --only=production

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Development stage
FROM base AS development
RUN npm install
EXPOSE 3000
CMD ["npm", "start"]

# Production stage
FROM base AS production
RUN npm ci --only=production

# Copy static files
RUN mkdir -p /app/dist
COPY --from=builder /app/dist /app/dist

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3000/health || exit 1

EXPOSE 3000
CMD ["node", "server.js"]

# Production-static stage (using nginx for better performance)
FROM nginx:alpine AS production-static
RUN apk add --no-cache curl

# Copy custom nginx configuration
COPY ./infrastructure/nginx/frontend-nginx-prod.conf /etc/nginx/nginx.conf

# Copy built static files
COPY --from=production /app/dist /usr/share/nginx/html

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3000/health || exit 1

EXPOSE 3000

# Serve static files with nginx
CMD ["sh", "-c", "nginx -g 'daemon off;' -c '/etc/nginx/nginx.conf'"]