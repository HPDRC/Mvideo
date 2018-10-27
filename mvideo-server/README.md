NGINX RTMP Dockerfile
=====================

This Dockerfile installs NGINX configured with `nginx-rtmp-module`, ffmpeg
and some default settings for HLS live streaming.


How to use
----------

1. Build and run the container (`docker build -t nginx_rtmp .` &
   `docker run -p 1935:1935 -p 8080:80 --rm nginx_rtmp`).

2. Stream your live content to `rtmp://localhost:1935/live/stream_name` where
   `stream_name` is the name of your stream.

3. In Safari, VLC or any HLS compatible browser / player, open
   `http://localhost:8080/hls/stream_name.m3u8`. Note that the first time,
   it might take a few (10-15) seconds before the stream works. This is because
   when you start streaming to the server, it needs to generate the first
   segments and the related playlists.


How it works
------------------

The `live` block in `rtmp` will create 2 simultanous streams. One to `/hls`
where an incoming stream is converted to HLS format in real time for HTML5
playback, and another to a local file, which will then be processed by the
`transcode.sh` script when the stream is finished. Working together these two
streams provide both, HTML5 live streaming, and HTML5 archived video playback.


Links
-----

* http://nginx.org/
* https://github.com/arut/nginx-rtmp-module
* https://www.ffmpeg.org/
* https://obsproject.com/
