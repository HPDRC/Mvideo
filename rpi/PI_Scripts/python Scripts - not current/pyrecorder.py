#!/usr/bin/env python3

import requests
from picamera import PiCamera
from time import sleep

from gps3 import agps3
from datetime import datetime
from ffmpy import FFmpeg
import uuid
from _thread import * #For background thread of sending updates

API_PREFIX= "http://131.94.133.214/api/"
FFMPEG_LINK_PREFIX = "rtmp://131.94.133.214:1935/live/"
API_TRAC_LINK = API_PREFIX + "tracks/"

# Background task for updating location 
def update_location(url):
    info = {}
    '''
    The 2 following variables are abstractiosn to connect to the current
    gps daemon running in the background. This is a service that creates
    a socket and binds to port 2947 commonly.
    '''

    try:
        gps_socket = agps3.GPSDSocket()
        data_stream = agps3.DataStream()
        gps_socket.connect()
        gps_socket.watch()
    except e:
        print("The error", e)
        print("gps could not connect")

    #using the watch function allows us to only send new gps updates
    for new_data in gps_socket:
        try:
            if new_data:
                data_stream.unpack(new_data)
                info["longitude"] = data_stream.lon
                info["latitude"] = data_stream.lat
                info["timestamp"] = datetime.now().isoformat()
                res = requests.post(url, json=info)
        except:
            print("could not send info")


def main():

    global API_PREFIX
    global FFMPEG_LINK_PREFIX
    global API_TRAC_LINK

    identifier = str(uuid.uuid1())


    print("uuidl", identifier)
    #TODO: make the last part variable, possibly with a command line arguement
    stream_url = FFMPEG_LINK_PREFIX + identifier + "__picats1__"
    update_url = API_TRAC_LINK + identifier +"/update_location/" 
    
    #send initial uuid
    init_request = requests.post(API_TRAC_LINK, json={"uuid": identifier})
    print(init_request)

    start_new_thread(update_location, (update_url,))

    ff = FFmpeg(
        inputs={'/dev/video0': '-f v4l2'},
        outputs={stream_url: '-ar 11025 -r 30.0 -f flv'}
    )
    ff.run()


if __name__ == "__main__":
    main()
