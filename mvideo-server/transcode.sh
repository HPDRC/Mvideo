#!/bin/sh

echo "Creating thumbnail for UUID: $name"

ffmpeg -y -i /srv/videos/$1-pre.webm -ss 00:00:01.000 -frames:v 1 /srv/videos/$1.png;
ffmpeg -y -i /srv/videos/$1-pre.webm -c:v copy -c:a copy /srv/videos/$1.webm;
rm /srv/videos/$1-pre.webm;
