from datetime import datetime
from rest_framework import serializers
from drf_extra_fields.geo_fields import PointField
from rest_framework_gis.serializers import GeoFeatureModelSerializer
from django.contrib.gis.geos import MultiPoint, Point

from mvideo import settings
from mvideo.models import Track, Video
from mvideo.fields import MultiPointField


class TrackSerializer(serializers.ModelSerializer):

    """
    created = serializers.DateTimeField(
        format=settings.DATETIME_FORMAT,
        required=False
    )
    """
    timestamps = serializers.ListField(
        child=serializers.DateTimeField(
            format=settings.DATETIME_FORMAT,
            required=False
        ),
        required=False,
        style={'base_template': 'textarea.html', 'rows': 8},
        help_text='Array of "YYYY-MM-DDT00:00:00Z" strings'
    )
    path = MultiPointField(
        child=PointField(
            required=False,
        ),
        style={'base_template': 'textarea.html', 'rows': 8},
        help_text='Array of {latitude,longitude} JSON objects',
        required=False
    )

    class Meta:
        model = Track
        fields = '__all__'
        name = 'track'


class GeoJSONTrackSerializer(GeoFeatureModelSerializer):

    timestamps = serializers.ListField(
        child=serializers.DateTimeField(
            format=settings.DATETIME_FORMAT,
            required=False
        ),
        required=False,
        style={'base_template': 'textarea.html', 'rows': 8},
        help_text='Array of "YYYY-MM-DDT00:00:00Z" strings'
    )

    class Meta:
        model = Track
        fields = '__all__'
        name = 'TrackGeo'
        geo_field = 'path'


class VideoSerializer(serializers.ModelSerializer):

    class Meta:
        model = Video
        fields = '__all__'
        name = 'video'


class TrackUpdateSerializer(serializers.Serializer):
    timestamp = serializers.DateTimeField()
    longitude = serializers.DecimalField(max_digits=24, decimal_places=16)
    latitude = serializers.DecimalField(max_digits=24, decimal_places=16)
