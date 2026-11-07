package com.example.ui.screens

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.net.URL
import java.nio.ByteOrder
import kotlin.math.abs

internal data class AudioEnvelope(val peaks: List<Float>, val durationMs: Int)
/** Decode actual PCM into a bounded envelope; never use decorative/random samples. */
internal fun decodeAudioEnvelope(context: Context, source: String): AudioEnvelope? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    var temporary: File? = null
    try {
        val uri = Uri.parse(source)
        if (uri.scheme == "file") extractor.setDataSource(context, uri, null)
        else {
            require(uri.scheme == "https")
            val connection = URL(source).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 7000; connection.readTimeout = 7000
            connection.instanceFollowRedirects = false
            try {
                require(connection.responseCode == 200)
                temporary = File.createTempFile("voice-envelope-", ".m4a", context.cacheDir)
                connection.inputStream.use { input -> temporary!!.outputStream().use { output ->
                    val buffer = ByteArray(8192); var total = 0
                    while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= 8*1024*1024); output.write(buffer,0,n) }
                } }
                extractor.setDataSource(temporary!!.absolutePath)
            } finally { connection.disconnect() }
        }
        val track = (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true } ?: return null
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val durationUs = if(format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else return null
        require(durationUs in 1..120_000_000)
        val peaks = FloatArray(40)
        val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec = decoder; decoder.configure(format,null,null,0); decoder.start()
        val info = MediaCodec.BufferInfo(); var inputEnded = false; var outputEnded = false
        val deadline = android.os.SystemClock.elapsedRealtime()+10000
        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var floatPcm = false
        while(!outputEnded && android.os.SystemClock.elapsedRealtime()<deadline) {
            if(!inputEnded) {
                val i=decoder.dequeueInputBuffer(10000)
                if(i>=0) {
                    val n=extractor.readSampleData(decoder.getInputBuffer(i)!!,0)
                    if(n<0){decoder.queueInputBuffer(i,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnded=true}
                    else {decoder.queueInputBuffer(i,0,n,extractor.sampleTime,0);extractor.advance()}
                }
            }
            val i=decoder.dequeueOutputBuffer(info,10000)
            if(i==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val out=decoder.outputFormat;sampleRate=out.getInteger(MediaFormat.KEY_SAMPLE_RATE);channels=out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                floatPcm=out.containsKey(MediaFormat.KEY_PCM_ENCODING)&&out.getInteger(MediaFormat.KEY_PCM_ENCODING)==android.media.AudioFormat.ENCODING_PCM_FLOAT
            } else if(i>=0) {
                decoder.getOutputBuffer(i)?.let { buffer ->
                    buffer.position(info.offset);buffer.limit(info.offset+info.size);buffer.order(ByteOrder.nativeOrder())
                    var sample=0
                    while(buffer.remaining()>=if(floatPcm)4 else 2) {
                        val amplitude=if(floatPcm)abs(buffer.float) else abs(buffer.short.toInt())/32768f
                        val at=info.presentationTimeUs+(sample/channels)*1_000_000L/sampleRate
                        val bin=(at*40/durationUs).toInt().coerceIn(0,39)
                        peaks[bin]=maxOf(peaks[bin],amplitude.coerceIn(0f,1f));sample++
                    }
                }
                outputEnded=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                decoder.releaseOutputBuffer(i,false)
            }
        }
        if(!outputEnded)return null
        val maximum=peaks.maxOrNull()?.coerceAtLeast(.01f)?:1f
        return AudioEnvelope(peaks.map{it/maximum},(durationUs/1000).toInt())
    } catch(_:Exception) { return null }
    finally { runCatching{codec?.stop()};runCatching{codec?.release()};extractor.release();temporary?.delete() }
}
