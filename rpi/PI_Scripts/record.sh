#!/bin/bash

UUID=$(/usr/bin/python3 -c 'import uuid; print(str(uuid.uuid1()))')
res=$(http post utma-video.cs.fiu.edu/api/tracks/ uuid=$UUID)

base='rtmp://utma-video.cs.fiu.edu:1935/live/'
after='__AAxx-InsideCam__'
whole="$base$UUID$after"

# res=$(http post 131.94.133.214/api/tracks/ uuid=$UUID)
# resource tracking url address to append rtmp stream information
# base='rtmp://131.94.133.214:1935/live/'
# rtmp url for mvideo server to use
# after='__AAxx-InsideCam__"
# [this should be the label for the InnerCamera that will display on the Ops Center Video Feed]

echo whole is $whole

#
#`raspivid -ISO 0 -ex sport -awb sun  -vf -t 0 -w 450 -h 200 -fps 30 -b 2000000 -o - | ffmpeg -i - -r 30.0 -f flv $whole`
# --ISO [ 100 - 800 ] --exposure [ auto , night , nightpreview , backlight , spotlight , sports , snow , beach , verylong , fixedfps , antishake , fireworks ] --awb (Set Automatic White Balance mode) [ off , auto , sun ,  cloud , shade , tugnsten , fluorescent , incandescent , flash , horizon  ]


`raspivid -ISO 0 -n -ex auto -awb auto -t 0 -w 800 -h 600 -fps 30 -a 12 -b 2000000 -o - | ffmpeg -i - -r 30.0 -f flv $whole`
