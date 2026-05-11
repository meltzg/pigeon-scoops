# --- Build stage ---
FROM clojure AS build

WORKDIR /build

RUN apt update && apt install -y npm

RUN npm install -g corepack
RUN corepack enable
RUN yarn set version berry

COPY shadow-cljs.edn deps.edn package.json yarn.lock .yarnrc.yml ./
COPY src ./src
COPY resources ./resources

RUN yarn install
RUN yarn run shadow-cljs release app

# --- Runtime stage ---
FROM nginx:alpine

# Remove default nginx site
RUN rm -rf /usr/share/nginx/html/*

# Copy nginx configuration for SPA routing
COPY config/nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled assets
COPY --from=build /build/resources/public /usr/share/nginx/html

EXPOSE 80