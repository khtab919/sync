package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.data.api.LayaliApi
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Optional low-latency moderation signal for a REAL live session.
 * The authoritative decision remains Django/LiveKit. No client moderation writes.
 * A distinct FirebaseApp avoids overwriting existing Firebase Authentication.
 */
@Composable
internal fun RelaxiaRoomRealtimeGuard(
    api: LayaliApi,
    roomId: String,
    userId: Int,
    onEvicted: () -> Unit,
    onSuspended: () -> Unit = onEvicted,
    observedProfileId: Int = 0,
    onProfileChanged: () -> Unit = {}
) {
    if (userId <= 0 || (roomId.isBlank() && observedProfileId <= 0)) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestEvicted = rememberUpdatedState(onEvicted)
    val latestSuspended = rememberUpdatedState(onSuspended)
    val latestChange = rememberUpdatedState(onProfileChanged)
    DisposableEffect(roomId,userId,observedProfileId) {
        val disposed = AtomicBoolean(false)
        val registrations = mutableListOf<ListenerRegistration>()
        val work = scope.launch {
            val config = try { api.request("/api/firebase/custom-token") }
            catch(e:CancellationException){ throw e }
            catch(_:Exception){return@launch} // disabled Firebase feature: Django works normally
            if(disposed.get() || config.optString("uid") != userId.toString()) return@launch
            val project=config.optString("project_id")
            val token=config.optString("token")
            if(!project.matches(Regex("[a-zA-Z0-9-]{6,100}")) || token.length < 100) return@launch
            try {
                val default=FirebaseApp.getInstance()
                if(default.options.projectId != project) return@launch
                val name="relaxia-moderation-mirror"
                val secondary=FirebaseApp.getApps(context).firstOrNull{it.name==name}
                    ?: FirebaseApp.initializeApp(context.applicationContext,default.options,name)
                val auth=FirebaseAuth.getInstance(secondary)
                auth.signInWithCustomToken(token).addOnCompleteListener { result->
                    if(disposed.get() || !result.isSuccessful) return@addOnCompleteListener
                    val db=FirebaseFirestore.getInstance(secondary)
                    synchronized(registrations) {
                        if(disposed.get())return@addOnCompleteListener
                        if (observedProfileId > 0) {
                            registrations.add(db.collection("profileChanges").document(observedProfileId.toString())
                                .addSnapshotListener { snapshot, error ->
                                    if (!disposed.get() && error == null && snapshot?.exists() == true)
                                        latestChange.value()
                                })
                            return@synchronized
                        }
                        registrations.add(db.collection("users").document(userId.toString())
                            .addSnapshotListener { snapshot, error->
                                if(disposed.get() || error!=null || snapshot==null) return@addSnapshotListener
                                val status=snapshot.getString("accountStatus")
                                if(status=="suspended" || status=="banned_permanent")latestSuspended.value()
                            })
                        registrations.add(db.collection("rooms").document(roomId)
                            .collection("kickedUsers").document(userId.toString())
                            .addSnapshotListener { snapshot, error->
                                if(disposed.get() || error!=null || snapshot==null)return@addSnapshotListener
                                if(snapshot.getBoolean("kicked")==true)latestEvicted.value()
                            })
                    }
                }
            } catch (_: Exception) {
                // A misconfigured Firebase app must not disable the regular LiveKit controls.
            }
        }
        onDispose {
            disposed.set(true)
            work.cancel()
            synchronized(registrations){ registrations.forEach { it.remove() };registrations.clear() }
        }
    }
}
