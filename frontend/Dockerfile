# Build context: repository root (the build generates the client from api/openapi.yaml)
#   docker build -f frontend/Dockerfile .

FROM node:22-alpine AS build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY api /workspace/api
COPY frontend/ ./
RUN npm run build

# unprivileged nginx listens on 8080 and runs under OpenShift's random user id
FROM nginxinc/nginx-unprivileged:1.27-alpine
ENV BACKEND_URL=http://backend:8080 \
    KEYCLOAK_ISSUER=http://localhost:8180/realms/onboarding \
    KEYCLOAK_CLIENT_ID=onboarding-portal \
    REQUIRE_HTTPS=false
COPY frontend/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --chmod=755 frontend/nginx/40-runtime-config.sh /docker-entrypoint.d/40-runtime-config.sh
COPY --from=build /workspace/frontend/dist/onboarding-portal/browser /usr/share/nginx/html
EXPOSE 8080
