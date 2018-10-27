import uuid
from django.contrib.gis.db import models
from django.db.models import Manager
from django.contrib.postgres.fields import JSONField, ArrayField

from mvideo.settings import VIDEO_ROOT


class Track(models.Model):
    uuid = models.UUIDField(
        primary_key=True,
        editable=False,
        default=uuid.uuid4
    )
    """
    created = models.DateTimeField(
        null=True,
        blank=True,
        auto_now_add=True
    )
    """
    timestamps = ArrayField(
        null=True,
        blank=True,
        base_field=models.DateTimeField(
            null=True,
            blank=True
        )
    )
    path = models.MultiPointField(
        null=True,
        blank=True
    )
    #objects = models.GeoManager()
    objects = Manager()

    def __str__(self):
        return "Track ID: %s" % self.uuid


class Video(models.Model):
    uuid = models.UUIDField(
        primary_key=True,
        editable=False,
        default=uuid.uuid4
    )
    name = models.CharField(
        max_length=80
    )
    created = models.DateTimeField(
        auto_now_add=True
    )
    thumbnail = models.FilePathField(
        max_length=260,
        path=str(VIDEO_ROOT),
        null=True,
        blank=True,
    )
    url = models.FilePathField(
        max_length=260,
        path=str(VIDEO_ROOT),
        null=True,
        blank=True,
    )
