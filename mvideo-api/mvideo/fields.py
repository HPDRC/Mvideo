import json
from django.utils import six
from django.utils.encoding import smart_str

from rest_framework import serializers
from django.contrib.gis.geos import MultiPoint, Point
from drf_extra_fields.geo_fields import PointField


EMPTY_VALUES = (None, '', [], (), {})


class MultiPointField(serializers.ListField):
    """
    A field for handling GeoDjango MultiPoint fields in json format:
        [
            {
            "latitude": 49.8782482189424,
             "longitude": 24.452545489
            }
        ]

    """
    type_name = 'MultiPointField'
    type_label = 'multipoint'

    def to_representation(self, data):
        if data is None:
            return None

        return [self.child.to_representation(item) if item is not None else None for item in data]

    def to_internal_value(self, data):
        """
        Parse json data and return a multipoint object
        """
        if data is None:
            return None

        result = MultiPoint()
        points = super(MultiPointField, self).to_internal_value(data)

        for point in points:
            result.append(point)

        return result
