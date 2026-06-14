ARG DOCKER_REGISTRY=docker.io

FROM ${DOCKER_REGISTRY}/node:20-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm config set registry https://mirrors.huaweicloud.com/repository/npm/ \
    && npm ci --no-audit --no-fund --loglevel=error

FROM deps AS builder

WORKDIR /app
COPY . .
RUN npm run build

FROM ${DOCKER_REGISTRY}/nginx:alpine

WORKDIR /usr/share/nginx/html

RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist .

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
