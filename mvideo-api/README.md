
# NOTES:

## Build A

This build consists of the rtsp client + rtsp server setup. It is what we
currently have in use except we will be changing the server side.

- Phone will continue to be rtsp client
- Nginx will be used as the rtsp server


# TODO:

__Tasks:__

* Rebuild Mvideo-android with new IP as rtsp server
* Build Nginx based rtsp server
* Test configuration performance/stability

__URGENT:__

* Install [docker for windows][1] on alaska.cs.fiu.edu
* Share the videos folder between alaska and the docker-machine VM
* Ensure files are stored in the correct format/location by the VM

# Documentation

## Usage:

To use the API use the following command or url when creating a stream.

`ffmpeg -i $INPUT -f flv rtmp://$API_HOST/stream/$UUID?name='Video Display Name'\
&description="Description goes here"&uuid=$UUID`

Alternatively you can first POST to the api and then start the stream

`http POST $API_HOST/videos name="Display Name" description="Description goes \
here" uuid="$GENERATED_UUID"`



[1]: https://docs.docker.com/docker-for-windows/
