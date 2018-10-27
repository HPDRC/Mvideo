This is the main Mvideo repository.

It contains links to each Mvideo subproject. This entire repository can be used
to setup a single contained Mvideo app.

### Getting Started

This project depends on certain environmental variables which you must provide in a .env file
These variables are:

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

To start using this repo you must first have docker installed, then follow
these instructions:

1. Clone this repo
   - `git clone git@example.com/mvideo/mvideo`
2. Update your submodules
   - `git submodule  update --recursive --init`
3. Start the docker images
   - `docker-compose up`
   - `docker-compose up -f docker-compose.yml -f docker-compose.dev.yml` to start in developement mode.


## Current Deployment

Most details about the deployment setup can be found in `nginx/nginx.conf`,
`docker-compose.yml` and from the `mvideo-docs` project but I will give a brief
description here.

Volumes - Docker volumes are used to store persistent data and also to share the
data across different containers. Currently there are 3 volumes:

1. psqldata - postgres container data goes here. It is mounted where psql stores
   all its data, `/var/lib/postgresql/data`
2. streamdata - When streaming, the nginx-rtmp-module creates temporary files
   which we store in `/data`. This volume is unncessary as these files do not
   have to be (and are not) persistent but it's useful for debugging purposes.
3. archivedata - Mounted at `/srv/video` this volume is where we store the
   video files created from each stream.
