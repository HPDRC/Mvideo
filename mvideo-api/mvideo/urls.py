"""mvideo URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/1.9/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  url(r'^$', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  url(r'^$', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.conf.urls import url, include
    2. Add a URL to urlpatterns:  url(r'^blog/', include('blog.urls'))
"""
from django.conf.urls import url, include
from django.contrib import admin
from rest_framework import routers

from django.contrib.auth import views as auth_views

from mvideo import views, settings

#router = routers.DefaultRouter(schema_title='Mvideo API')
router = routers.DefaultRouter(trailing_slash=True)
router.register(r'videos', views.VideoViewSet, 'videos')
router.register(r'tracks', views.TrackViewSet, 'tracks')
router.register(r'geotracks', views.GeoTrackViewSet, 'geotracks')


urlpatterns = [
    #url(r'^accounts/', include('django.contrib.auth.urls')),
    #url(r'^rest-auth/', include('rest_auth.urls')),
    #url(r'^api/user_auth$', views.user_auth),
    #url(r'^docs$', views.schema_view),
    url(r'^admin/', admin.site.urls),
    url(r'^record_start/$', views.record_start),
    url(r'^record_update/$', views.record_update),
    url(r'^record_done/$', views.record_done),
    url(r'^api/streams$', views.streams),
    url(r'^api/hsls$', views.hsls),
    url(r'^api-token-auth/', views.CustomAuthToken.as_view()),
    url(r'^api/', include(router.urls)),
]

if settings.DEBUG is True:
    from django.contrib.staticfiles.urls import staticfiles_urlpatterns
    urlpatterns += staticfiles_urlpatterns()
