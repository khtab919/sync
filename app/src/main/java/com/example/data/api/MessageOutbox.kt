package com.example.data.api

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/** Account/peer scoped, encrypted retry metadata; attachments stay in private no-backup files. */
internal class MessageOutbox(private val context: Context, origin: String, myId: Int, peerId: Int) {
    private val scope = "$origin/outbox/$myId/$peerId"
    private val store = SecureSessionStore(context, scope, 200000)
    private val directory = File(context.noBackupFilesDir, "outbox-"+MessageDigest.getInstance("SHA-256").digest(scope.toByteArray()).take(12).joinToString(""){"%02x".format(it)}).apply{mkdirs()}
    @Synchronized fun read(): List<JSONObject> {
        val raw=store.read()?:return emptyList()
        val array=JSONObject(raw).optJSONArray("items")?:return emptyList()
        return (0 until array.length()).map{array.getJSONObject(it)}
    }
    @Synchronized private fun write(rows:List<JSONObject>){if(rows.isEmpty())store.clear()else store.write(JSONObject().put("items",JSONArray(rows)).toString())}
    @Synchronized fun text(body:String):JSONObject {
        require(body.isNotBlank()&&body.length<=2000)
        val rows=read();require(rows.size<50)
        val row=JSONObject().put("client_key",UUID.randomUUID().toString()).put("body",body).put("created_at",Instant.now().toString()).put("kind","text")
        write(rows+row);return row
    }
    @Synchronized fun file(uri:Uri,purpose:String):JSONObject {
        require(purpose in listOf("chat_image","chat_audio","chat_video"))
        val rows=read();require(rows.size<50)
        val key=UUID.randomUUID().toString()
        val file=File(directory,key+when(purpose){"chat_audio"->".m4a";"chat_video"->".mp4";else->".image"})
        try {
            context.contentResolver.openInputStream(uri)!!.use{input->file.outputStream().use{output->val buffer=ByteArray(8192);var total=0;while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=8*1024*1024);output.write(buffer,0,n)}}}
            val row=JSONObject().put("client_key",key).put("body","").put("kind",purpose).put("file",file.name).put("created_at",Instant.now().toString())
            write(rows+row);return row
        }catch(e:Exception){file.delete();throw e}
    }
    @Synchronized fun update(row:JSONObject){write(read().map{if(it.optString("client_key")==row.optString("client_key"))row else it})}
    fun fileFor(row:JSONObject):File {val name=row.getString("file");require(name.matches(Regex("[a-f0-9-]+\\.(m4a|mp4|image)")));return File(directory,name)}
    @Synchronized fun remove(key:String){val rows=read();val row=rows.firstOrNull{it.optString("client_key")==key};write(rows.filterNot{it.optString("client_key")==key});if(row?.has("file")==true)fileFor(row).delete()}
    fun pendingJson(row:JSONObject,senderId:Int):JSONObject {
        val result=JSONObject(row.toString()).put("id",-1).put("sender_id",senderId).put("pending",true)
        if(row.has("file"))result.put("attachment_url",Uri.fromFile(fileFor(row)).toString()).put("attachment_mime",when(row.optString("kind")){"chat_audio"->"audio/mp4";"chat_video"->"video/mp4";else->"image/jpeg"})
        return result
    }
}
