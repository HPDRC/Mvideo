FROM node:latest

MAINTAINER David Rodriguez

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

RUN npm set progress=false
RUN npm -g install gitbook-cli

COPY . /usr/src/app

CMD ["gitbook", "serve"]
