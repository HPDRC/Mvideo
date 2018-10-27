
The main project in this system is the `mvideo-api` project. It is the main
interface to the database and most if not all API calls will go through this
service.


### Requirements:

Among the requirements for this project here are a few:

1. Stream live video from a phone to the server
  1. record this video in an HTML5 compatible format
  2. re-stream this video:
    - using rtmp (requires flash)
    - using dash.js (no flash needed)
2. Stream live GPS updates to the server
  1. store GPS updates with video stream


### Research:

In our research we found there are a few ways to accomplish this:

* Method 1
  - Android RTSP client with [libstreaming][2]
  - Wowza as the RTSP server
  - Notes:
    - Wowza is a commercial tool and requires a license
    - Wowza performance is not very good
    + Streams originate from Wowza server
* Method 2
  - Android RTSP server with [libstreaming][2]
  - Custom server or API
  - Notes:
    - The server will have to re-stream the android video or all streams will be
    direct from the phone, which means poor performance.
* Method 3
  - Android RTMP client with [javacv][3]
  - Nginx server with [rtmp][1] module
  - Notes:
    + Good performance
    + Can live encode/re-stream in any format


For Mvideo we've gone with the third option.


[1]: https://github.com/arut/nginx-rtmp-module
[2]: https://github.com/fyhertz/libstreaming
[3]: https://github.com/bytedeco/javacv
