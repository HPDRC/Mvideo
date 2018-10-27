#!/bin/bash

# Starting by enabling the hardware of the gps
# stty -F /dev/ttyAMA0 raw 9600 cs8 clocal -cstopb

#Ensures that all previous gps daemons are off
# sudo killall gpsd

# Turning the hardware stream into a connectable socket
# sudo gpsd /dev/ttyAMA0 -F /var/run/gpsd.sock


#Executing python script that runs streaming. See script for further details
/usr/bin/python3 /home/pi/workspace/recorder/pyrecorder.py


