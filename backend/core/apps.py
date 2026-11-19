from django.apps import AppConfig

class CoreConfig(AppConfig):
    name='core'
    def ready(self):
        from . import activity_events
        from .public_presence import register_presence_signals
        register_presence_signals()
