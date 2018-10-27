from rest_framework import viewsets
from django.conf import settings
from drf_extra_fields.geo_fields import PointField
from mvideo.fields import MultiPointField
import requests
from bs4 import BeautifulSoup

import rest_framework_filters as filters

from mvideo.models import Video, Track
from mvideo.serializers import (
    VideoSerializer,
    TrackSerializer,
    TrackUpdateSerializer,
    GeoJSONTrackSerializer,
)

from rest_framework_swagger.renderers import OpenAPIRenderer, SwaggerUIRenderer
from rest_framework.decorators import api_view, renderer_classes, detail_route, list_route, parser_classes
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework import response, schemas, status

baseUrl = "http://server/stat"

mvideo_server_token = "mvideoservertoken"

from rest_framework.authtoken.views import ObtainAuthToken
from rest_framework.authtoken.models import Token
from django.contrib.auth.models import User

def user_belongs_to_group(user, group_name):
    belongs = False
    try:
        belongs = user != None and user.is_authenticated() and user.groups.filter(name=group_name).exists()
    except:
        belongs = False
    return belongs

def user_can_delete_videos_and_tracks(user):
    return user_belongs_to_group(user, 'Video and Track deletion')

def user_can_add_change_videos_and_tracks(user):
    return user_belongs_to_group(user, 'Video and Track add and change')

def get_token_from_token_key(token_key):
    token = None
    #print("trying to get token from key " + token_key)
    try:
        token = Token.objects.get(pk=token_key)
        #print("got token from token key!")
    except Exception as e:
        #print("trying to get token from key exception: " + str(e))
        token = None
    return token

def get_user_and_token_from_token_key(token_key):
    #print("trying to get user from key " + token_key)
    token = get_token_from_token_key(token_key)
    user = None
    try:
        if token != None:
            #print("trying to get user from id " + str(token.user_id))
            user = User.objects.get(pk=token.user_id)
    except Exception as e:
        user = None
        #print("trying to get user from token key exception: " + str(e))
    """
    if user != None:
        print('found user! ' + user.username)
    else:
        print('user not found')
    """
    return user, token

def token_can_add_change_videos_and_tracks(token_key):
    user, _ = get_user_and_token_from_token_key(token_key)
    return user_can_add_change_videos_and_tracks(user)

class CustomAuthToken(ObtainAuthToken):
    def post(self, request, *args, **kwargs):
        #print("CUSTOM AUTH TOKEN")
        token = None
        user_id = None
        user_name = None
        email = None
        msg = "ok"
        can_delete = False
        can_add_change = False
        try:
            serializer = self.serializer_class(data=request.data, context={'request': request})
            serializer.is_valid(raise_exception=True)
            user = serializer.validated_data['user']
            token, created = Token.objects.get_or_create(user=user)
            can_delete = user_can_delete_videos_and_tracks(user)
            can_add_change = user_can_add_change_videos_and_tracks(user)
            """
            if user.username == "recorder":
                print ("updating recorder user token")
                token.key = '128349a35c6d151f3a3fb367752788cde7e17866'
                Token.objects.filter(user=user).update(key=token.key)
                print ("recorder user token updated")
            """
            token = token.key
            user_id = user.pk
            user_name = user.username
            email = user.email
        except Exception as e:
            token = user_id = email = user_name = None
            can_add = can_delete = False;
            msg = str(e)
        return response.Response({
            'msg': msg,
            'token': token,
            'user_id': user_id,
            'email': email,
            'user_name': user_name,
            'can_delete': can_delete,
            'can_add_change': can_add_change
        })

"""
@api_view()
@renderer_classes([SwaggerUIRenderer, OpenAPIRenderer])
def schema_view(request):
    generator = schemas.SchemaGenerator(title='Mvideo API')
    return response.Response(generator.get_schema(request=request))
"""

def get_uuid_name_from_stream_name(stream_name):
    uuid = ""
    full_name = name = "UnNamed"
    split_feed_name = "__"
    try:
        split_parts = stream_name.split(split_feed_name)
        uuid = split_parts[0]
        name = split_parts[1]
        full_name = uuid + split_feed_name + name + split_feed_name
    except:
        pass
    return uuid, name, full_name

def get_video_for_stream_name(stream_full_name, is_live):
    video_uuid, video_name, video_full_name = get_uuid_name_from_stream_name(stream_full_name)
    try:
        video = Video.objects.get(pk=video_uuid)
        created = False
        #print("    FOUND EXISTING VIDEO " + video_full_name)
    except:
        created = True
        video = Video()
        video.uuid = video_uuid
        video.name = video_name
        #print("    CREATED NEW VIDEO " + video_full_name)
    webm_file_name_end = "-pre" if is_live else ""
    video.url = settings.VIDEO_ROOT + video_full_name + webm_file_name_end + ".webm" 
    video.thumbnail = None if is_live else settings.VIDEO_ROOT + video_full_name + ".png"
    return video, created

def get_track_for_stream_name(stream_full_name):
    stream_uuid, _, full_name = get_uuid_name_from_stream_name(stream_full_name)
    try:
        track = Track.objects.get(pk=stream_uuid)
        created = False
        #print("    FOUND EXISTING TRACK " + full_name)
    except:
        track = Track()
        track.uuid = stream_uuid
        created = True
        #print("    CREATED NEW TRACK " + full_name)
    return track, created

def get_ip(request):
    x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
    if x_forwarded_for:
        ip_addr = x_forwarded_for.split(',')[0]
    else:
        ip_addr = request.META.get('REMOTE_ADDR')
    return ip_addr

@api_view(['POST'])
@parser_classes((FormParser,))
def record_start(request):
    ok = False
    created = False
    req_mvideo_server_token = request.GET.get("token", None)
    #print('mvideo_server_token = ' + str(req_mvideo_server_token))

    if req_mvideo_server_token == mvideo_server_token:
        req_recorder_token = request.data.get("recorder_token", None)
        #print('recorder_token = ' + str(req_recorder_token))
        can_add_change = token_can_add_change_videos_and_tracks(req_recorder_token)

        #can_add_change_str = "YES" if can_add_change else "NO"
        #print('can add / change? ' + can_add_change_str)
        if can_add_change:
            stream_full_name = request.data.get('name')
            if stream_full_name != None:
                #print("START STREAM " + stream_full_name)
                track, created = get_track_for_stream_name(stream_full_name);
                if created:
                    ok = True
                    track.save()
                    video, _ = get_video_for_stream_name(stream_full_name, True);
                    video.save();
    status_return = status.HTTP_200_OK if ok else status.HTTP_400_BAD_REQUEST
    return response.Response({ 'ok': ok, 'created': created }, status= status_return)

@api_view(['POST'])
@parser_classes((FormParser,))
def record_done(request):
    ok = False
    req_mvideo_server_token = request.GET.get("token", None)
    #print('mvideo_server_token = ' + str(req_mvideo_server_token))
    if req_mvideo_server_token == mvideo_server_token:
        stream_full_name = request.data.get('name')
        if stream_full_name != None:
            #print("END STREAM " + stream_full_name)
            video, _ = get_video_for_stream_name(stream_full_name, False);
            if video != None:
                ok = True
                video.save();
    return response.Response({ 'ok': ok })

@api_view(['POST'])
@parser_classes((FormParser,))
def record_update(request):
    request_data_name = request.data.get('name')
    request_timestamp = request.data.get('timestamp');
    #print("RECORD UPDATE " + request_data_name + ' ' + str(request_timestamp))
    return response.Response({ 'ok': True })

def get_live_or_hls_streams(getLive):
    #print ("getting stat page")
    statPage = requests.get(baseUrl).text
    soup = BeautifulSoup(statPage, "lxml-xml")
    index = 0 if getLive else 1
    try:
        liveSection = soup.findAll("live")[index]
        allStreams = liveSection.findAll("name")
    except Exception as e:
        allStreams = []

    allStreams = [i.string for i in allStreams]
    return allStreams

def translate_stream_data(streams):
    cameras = [
        {'name': 'mpv1', 'busId': 5012},
        {'name': 'mpv2', 'busId': 25002},
        {'name': 'mpv3', 'busId': 5011},
        {'name': 'sw1', 'busId': 5667},
        {'name': 'sw2', 'busId': 7140},
        {'name': 'sw3', 'busId': 8828},
        {'name': 'sw4', 'busId': 1103},
        {'name': 'sw5', 'busId': 4056},
        {'name': 'sw6', 'busId': 4061},
        {'name': 'sw7', 'busId': 25001}
    ]

    feedData = []
    for name in streams:
        busId = 0
        cameraType = 0

        sections = name.split('__')
        if len(sections) >= 2:
            cameraName = sections[1].lower().replace('-', '')
            cameraType = 2
            if 'inside' in cameraName:
                cameraType = 1
            for c in cameras:
                if c['name'] in cameraName:
                    busId = c['busId']

        feedData.append({
            'busId': busId,
            'cameraType': cameraType,
            'name': name,
        })
    return feedData

@api_view(['GET'])
def streams(request):
    #print("GETTING STREAMS")
    streams = get_live_or_hls_streams(True)
    feedData = translate_stream_data(streams)
    return response.Response({ 'names': streams, 'feedData': feedData })

@api_view(['GET'])
def hsls(request):
    #print("GETTING HSLS")
    streams = get_live_or_hls_streams(False)
    feedData = translate_stream_data(streams)
    return response.Response({ 'names': streams, 'feedData': feedData })

from django.contrib.auth import authenticate, login

from django.middleware.csrf import get_token

"""
@api_view(['POST'])
def user_auth(request):
    print("USER AUTH")
    username = request.data.get('username')
    password = request.data.get('password')
    auth_ok = False
    msg = 'missing username or password'
    try:
        if username and password and len(username) > 0 and len(password) > 0:
            print("   username " + username)
            user = authenticate(username=username, password=password)
            if user is not None:
                print("   authenticated OK")
                login(request, user)
                msg = 'welcome'
                auth_ok = True
            else:
                print("   unknown user")
                msg = 'failed to authenticate'
    except Exception as e:
        msg = 'exception while processing ' + str(e)
        auth_ok = False
    return response.Response({ 'ok': auth_ok, 'msg': msg })
"""

from django.views.decorators.csrf import requires_csrf_token
from rest_framework import authentication, permissions

class VideoViewSet(viewsets.ModelViewSet):

    """
    A Video resource represents a video stream along with track data.

    Tracks are a JSON object that holds all video track information in the
    following format:

        [{
            timestamp: 12345678,
            latitude: 987.4321,
            longitude: 123.456
        }]

    The Video resource is automatically posted to AFTER you have started a stream.

    1. Start streaming video to server:
        - `ffmpeg -i $INPUT -f flv rtmp://$SERVER_URL:1935/live/$UUID_$NAME`
    2. Send track updates to the Track of the same UUID:
        - Using httpie:
            `http PATCH $SERVER_URL/track/$UUID \
            track:='[{"timestamp":0000002, "latitude": 1, "longitude": 1}]'`
        - Using curl:
            `curl -H "Content-Type: application/json" -X POST \
            -d '{"name":"example name","url":"/$UUID", track: \
            [{"timestamp":0000002, "latitude": 1, "longitude": 1}]}' \
            http://$SERVER_URL/api/videos`
        - (__recommended for testing__) Using the browsable API:
            Open your browser and navigate to `http://localhost:8060/videos`
            then use the "Raw data" form to POST a new video. NOTE: Using the
            "HTML form" option currently does not work.

    Alternatively you can also post the video together when starting the stream:

    Field Description:
        {
            name: "Display name for video",
            created: "video creation date",
            url: "Path to video, same name as stream name",
        }
    """

    #print("VIDEO VIEW SET")

    authentication_classes = (authentication.TokenAuthentication,)
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,)

    queryset = Video.objects.all()
    serializer_class = VideoSerializer
    filter_fields = ['uuid', 'name', 'created']
    pagination_class = None

    def destroy(self, request, pk=None):
        #print("destroy called")
        can_delete = is_auth = did_delete = False
        user = request.user
        if user.is_authenticated():
            is_auth = True
            can_delete = user_can_delete_videos_and_tracks(user)
            if can_delete:
                try:
                    instance = self.get_object()
                    self.perform_destroy(instance)
                    did_delete = True
                except:
                    did_delete = False
        """
            else:
                print('authenticated video delete attempt without privilege')
        else:
            print('unauthenticated video delete attempt')
        """
        return response.Response({ 'ok': True, 'deleted': did_delete, 'is_auth': is_auth, 'can_delete': can_delete })

class TrackViewSet(viewsets.ModelViewSet):

    """
    Tracks are a JSON object that holds all video track information in the
    following format:

        [{
            timestamp: 12345678,
            latitude: 987.4321,
            longitude: 123.456
        }]
    """

    #print("TRACK VIEW SET")

    authentication_classes = (authentication.TokenAuthentication,)
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,)

    queryset = Track.objects.all()
    serializer_class = TrackSerializer
    filter_fields = list()

    @detail_route(methods=['POST', 'PATCH'], serializer_class=TrackUpdateSerializer)
    def update_location(self, request, pk=None):
        #print("UPDATE LOCATION")
        serializer = self.get_serializer(data=request.data)
        if serializer.is_valid():

            track = self.get_object()

            point_serializer = PointField()
            #print(track)

            if track.path is None:
                track.path = MultiPointField(
                    child=PointField(required=False)
                ).to_internal_value([])

            track.path.append(point_serializer.to_internal_value(request.data))

            timestamp = request.data.pop('timestamp', '')
            if track.timestamps is None:
                track.timestamps = []

            track.timestamps.append(timestamp)
            track.save()
            return response.Response({ 'ok': True })
        else:
            #print(serializer.errors)
            return response.Response(
                data=serializer.errors,
                status=status.HTTP_400_BAD_REQUEST
            )


class GeoTrackViewSet(viewsets.ModelViewSet):
    """
    GeoTracks are a GeoJSON object that holds all video track information in GeoJSON format.
    """

    authentication_classes = (authentication.TokenAuthentication,)
    permission_classes = (permissions.IsAuthenticatedOrReadOnly,)

    queryset = Track.objects.all()
    serializer_class = GeoJSONTrackSerializer
    filter_fields = list()
