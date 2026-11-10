"""Inspect uploaded ISO media with ffprobe; reject malformed or unsupported tracks."""
import json,os,subprocess,tempfile

def validate(raw,purpose):
    if os.environ.get('MEDIA_PROBE_ENABLED')!='1':return
    from .views import ApiError
    with tempfile.NamedTemporaryFile(suffix='.mp4') as source:
        source.write(raw);source.flush()
        try:
            result=subprocess.run(['ffprobe','-v','error','-show_streams','-show_format','-of','json',source.name],capture_output=True,timeout=15,check=True)
            if len(result.stdout)>262144:raise ValueError()
            data=json.loads(result.stdout);streams=data.get('streams',[])
            duration=float(data.get('format',{}).get('duration',0))
            if not 0<duration<=600:raise ValueError()
            videos=[s for s in streams if s.get('codec_type')=='video']
            audios=[s for s in streams if s.get('codec_type')=='audio']
            if purpose=='chat_audio':
                if not audios or videos or any(s.get('codec_name')!='aac' for s in audios):raise ValueError()
            elif not videos or any(s.get('codec_name') not in ('h264','hevc') or not 0<s.get('width',0)<=3840 or not 0<s.get('height',0)<=3840 for s in videos):raise ValueError()
        except (ValueError,subprocess.SubprocessError):raise ApiError('invalid_media')
        except OSError:raise ApiError('media_processing_unavailable',503)
