from django.contrib import admin
from django.urls import path
from core import views
urlpatterns = [path('admin/', admin.site.urls), path('health/', views.health), path('api/register', views.register), path('api/login', views.login), path('api/logout', views.logout), path('api/me', views.me), path('api/services', views.services), path('api/bookings', views.bookings), path('api/messages/<int:peer_id>', views.messages), path('api/reels', views.reels), path('api/wallet', views.wallet), path('api/withdrawals', views.withdrawals), path('api/reports', views.reports)]
