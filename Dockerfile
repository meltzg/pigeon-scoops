ARG APP_HOME=/app

FROM clojure:lein

ARG APP_HOME
WORKDIR $APP_HOME

RUN apt update
RUN apt install -y npm wget
RUN npm install -g n
RUN n latest

COPY . ./

RUN npm install
RUN npx shadow-cljs release app

ENTRYPOINT lein run
