FROM python:3.4

ENV PYTHONUNBUFFERED 1
MAINTAINER HPDRC

RUN apt-get update -y
RUN apt-get install -y apt-utils
RUN apt-get install -y binutils libproj-dev gdal-bin libgdal-dev python3-gdal

ENV USER mvideo

RUN mkdir -p /usr/src/app
RUN mkdir -p /usr/src/app/mvideo
WORKDIR /usr/src/app

COPY docker-entrypoint.sh /usr/src/app/
COPY manage.py /usr/src/app/
COPY ./mvideo/ /usr/src/app/mvideo/

COPY requirements.txt /usr/src/app/

RUN pip install --upgrade pip
RUN pip install --no-cache-dir -r requirements.txt

EXPOSE 8060
ENTRYPOINT ["/bin/bash", "docker-entrypoint.sh"]
