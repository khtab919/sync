package com.example.data.api

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Real downloaded message data, encrypted and isolated by account; never seeded. */
internal class MessageInbox(context:Context,origin:String,private val myId:Int) {
    private val store=SecureSessionStore(context,"$origin/inbox/$myId",400000)
    private fun all():List<JSONObject>{val raw=store.read()?:return emptyList();val array=JSONObject(raw).optJSONArray("items")?:return emptyList();return(0 until array.length()).map{array.getJSONObject(it)}}
    fun read(peerId:Int)=synchronized(lock){all().filter{(it.optInt("sender_id")==myId&&it.optInt("recipient_id")==peerId)||(it.optInt("sender_id")==peerId&&it.optInt("recipient_id")==myId)}.sortedBy{it.optInt("id")} }
    fun save(messages:List<JSONObject>)=synchronized(lock){val byId=(all()+messages).associateBy{it.optInt("id")};val retained=byId.values.sortedByDescending{it.optInt("id")}.take(100);store.write(JSONObject().put("items",JSONArray(retained)).toString())}
    companion object { private val lock=Any() }
}
