# Mvideo Documentation


This is the documentation for Mvideo. It includes overall designs, patterns, and
any general architecture. Most other, more detailed documentation or reference
material can be gained by looking at the README's in those specific projects.

The overall Mvideo project contains a few sub-projects:

1. `mvideo-api`
  - The main API server. Communicates with the datbase and
2. `postgis`
  - Postgresql database with GIS extensions. Used to store all video and track
  data.
3. `mvideo-server`
  - The nginx/ffmpeg streaming server that receives and broadcasts live video.
4. `mvideo-docs`
  - This document.


All of the Mvideo projects share a set of environmental variables that are read
from the `.env` file. Below is an example `.env` file used for development:

```
DEBUG=True
POSTGRES_HOST=db
POSTGRES_USER=user
POSTGRES_PASSWORD=pass
POSTGRES_DB=example_db
VIDEO_ROOT=/srv/videos
API_HOST=api
API_PORT=8060
PGDATA=/var/lib/postgresql/data
```

The majority of these settings are used by the `mvideo-api` project.


### Getting Started

To start using Mvideo you must first have [Docker][1] and docker-compose
installed, then follow these instructions:

1. Clone this repo
   - `git clone git@example.com/mvideo/mvideo`
2. Update your submodules
   - `git submodule init --recursive`
3. Start the docker images
   - `docker-compose up`

You should now see the logs of each Mvideo project being displayed on screen,
and you can now reach each of the services at their respective ports:

1. mvideo documentation:
  - http://192.168.99.100:4000
2. mvideo-api documentation:
  - http://192.168.99.100:8060/api
3. mvideo stream rtmp port:
  - http://192.168.99.100:1935
4. mvideo dash.js HTML5 stream playback:
  - http://192.168.99.100:8080

__NOTE:__ The IP address used above is the default docker-machine ip.


[1]: https://docs.docker.com/docker-for-windows/
