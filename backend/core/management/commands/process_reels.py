import hashlib
import json
import logging
import math
from pathlib import Path
import subprocess
import tempfile
import time
import uuid

from django.core.files import File
from django.core.management.base import BaseCommand
from django.db import transaction, close_old_connections
from django.db.models import Sum
from core.models import UploadSession, UploadChunk, UploadedMedia
from core.uploads import authorize_purpose
from core.views import User, ApiError

def compress(source, output, start, end):
    probe=subprocess.run(["ffprobe","-v","error","-protocol_whitelist","file,pipe","-f","mov",
        "-show_streams","-show_format","-of","json",str(source)],capture_output=True,timeout=15,check=True)
    data=json.loads(probe.stdout)
    video=next((v for v in data["streams"] if v["codec_type"]=="video"),None)
    duration=float(data["format"]["duration"])
    if not video or video["codec_name"] not in ("h264","hevc") or max(video["width"],video["height"])>3840 or not 0<duration<=600 or start>=duration or end>math.ceil(duration):
        raise ValueError("invalid_media")
    length=min(end or duration,duration)-start
    bitrate=min(1600000,int(18*1024*1024*8/length)-96000)
    subprocess.run(["ffmpeg","-nostdin","-v","error","-protocol_whitelist","file,pipe","-f","mov","-threads","2",
        "-ss",str(start),"-i",str(source),"-t",str(length),"-map","0:v:0","-map","0:a:0?",
        "-map_metadata","-1","-vf","scale=720:1280:force_original_aspect_ratio=decrease:force_divisible_by=2,fps=30",
        "-c:v","libx264","-preset","veryfast","-threads","2","-pix_fmt","yuv420p","-b:v",str(bitrate),
        "-maxrate",str(bitrate),"-bufsize",str(2*bitrate),"-c:a","aac","-b:a","64k",
        "-movflags","+faststart",str(output)],timeout=900,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    if not 16<=output.stat().st_size<=20*1024*1024:raise ValueError("output_size")

def process_one():
    with transaction.atomic():
        session=UploadSession.objects.select_for_update(skip_locked=True).filter(processing="queued",media__isnull=True).order_by("created_at").first()
        if session is None:return False
        try:
            authorize_purpose(session.user,"reel")
            with tempfile.TemporaryDirectory() as directory:
                source=Path(directory)/"source.mp4"; output=Path(directory)/"ready.mp4"
                digest=hashlib.sha256()
                with source.open("wb") as f:
                    chunks=UploadChunk.objects.filter(session=session).order_by("offset").values_list("data",flat=True)
                    for chunk in chunks.iterator(chunk_size=4):f.write(chunk);digest.update(chunk)
                if source.stat().st_size!=session.size or digest.hexdigest()!=session.digest:raise ValueError("checksum")
                compress(source,output,session.clip_start,session.clip_end)
                User.objects.select_for_update().get(pk=session.user_id)
                used=UploadedMedia.objects.filter(owner_id=session.user_id).aggregate(total=Sum("size"))["total"] or 0
                size=output.stat().st_size
                if used+size>500*1024*1024:raise ValueError("storage_limit")
                media=UploadedMedia(owner_id=session.user_id,purpose="reel",mime="video/mp4",size=size)
                with output.open("rb") as src:media.file.save(uuid.uuid4().hex+".mp4",File(src),save=False)
                media.save();session.media=media;session.processing="done"
        except (OSError,ValueError,KeyError,ApiError,subprocess.SubprocessError):
            logging.getLogger(__name__).exception("Reel processing failed for upload %s",session.pk)
            session.processing="failed"
        session.save(update_fields=["media","processing"])
        UploadChunk.objects.filter(session=session).delete()
        return True

class Command(BaseCommand):
    def add_arguments(self,parser):
        parser.add_argument("--loop",action="store_true")
    def handle(self,*args,**options):
        while True:
            handled=process_one()
            if not options["loop"]:return
            close_old_connections()
            if not handled:time.sleep(2)
