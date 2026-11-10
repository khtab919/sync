package com.example.data.api

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Encrypted per-account and per-conversation cache; history remains paginated on the server. */
internal class MessageInbox(private val context:Context,private val origin:String,private val myId:Int) {
    private val legacy=SecureSessionStore(context,"$origin/inbox/$myId",400000)
    private fun store(peer:Int)=SecureSessionStore(context,"$origin/inbox/$myId/peer/$peer",600000)
    private fun decode(raw:String?):List<JSONObject>{if(raw==null)return emptyList();val array=JSONObject(raw).optJSONArray("items")?:return emptyList();return(0 until array.length()).map{array.getJSONObject(it)}}
    private fun peer(row:JSONObject)=if(row.optInt("sender_id")==myId)row.optInt("recipient_id")else row.optInt("sender_id")
    fun read(peerId:Int)=synchronized(lock){
        val raw=store(peerId).read()
        (if(raw!=null)decode(raw)else decode(legacy.read()).filter{peer(it)==peerId}).sortedBy{it.optInt("id")}
    }
    fun save(messages:List<JSONObject>)=synchronized(lock){
        messages.filter{it.optInt("sender_id")==myId||it.optInt("recipient_id")==myId}.groupBy{peer(it)}.forEach{(peerId,incoming)->
            val rows=(read(peerId)+incoming).associateBy{it.optInt("id")}.values.sortedByDescending{it.optInt("id")}.take(100).toMutableList()
            var encoded=JSONObject().put("items",JSONArray(rows)).toString()
            while(encoded.length>590000 && rows.size>1){rows.removeAt(rows.lastIndex);encoded=JSONObject().put("items",JSONArray(rows)).toString()}
            store(peerId).write(encoded)
        }
    }
    companion object { private val lock=Any() }
}
