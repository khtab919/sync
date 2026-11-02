package com.example.platform

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal typealias Record = Map<String, Any?>

internal fun Record.str(key: String) = this[key] as? String ?: ""

internal fun Record.num(key: String) = (this[key] as? Number)?.toLong() ?: 0L

@Suppress("UNCHECKED_CAST")
internal fun Record.rows(key: String) = this[key] as? List<Record> ?: emptyList()

internal data class PlatformState(
    val profile: Record = emptyMap(),
    val services: List<Record> = emptyList(),
    val reels: List<Record> = emptyList(),
    val history: Record = emptyMap(),
    val admin: Record = emptyMap(),
    val bookings: List<Record> = emptyList(),
    val settings: Record = emptyMap(),
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val signedIn: Boolean = false,
)

internal class PlatformViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = FirebaseAuth.getInstance()
    private val api = FirebaseFunctions.getInstance("europe-west1")
    private val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
    private val mutable = MutableStateFlow(PlatformState())
    val state = mutable.asStateFlow()
    private val listeners = mutableListOf<ListenerRegistration>()
    private var generation = 0
    private val authListener =
        FirebaseAuth.AuthStateListener {
            generation++
            listeners.forEach { it.remove() }
            listeners.clear()
            mutable.value = PlatformState(signedIn = it.currentUser != null)
            refresh()
        }

    init {
        auth.addAuthStateListener(authListener)
    }

    suspend fun call(name: String, data: Record = emptyMap()): Record {
        @Suppress("UNCHECKED_CAST")
        return api.getHttpsCallable(name).call(data).await().data as? Record ?: emptyMap()
    }

    fun clearMessage() {
        mutable.value = mutable.value.copy(error = null, notice = null)
    }

    fun fail(message: String) {
        mutable.value = mutable.value.copy(error = message)
    }

    private suspend fun load(g: Int) {
        val services = call("listServices").rows("items")
        val settings = call("paymentSettings")
        var p: Record = emptyMap()
        var h: Record = emptyMap()
        var a: Record = emptyMap()
        var b: List<Record> = emptyList()
        if (auth.currentUser != null) {
            p = call("bootstrap")
            h = call("financeHistory")
            b = call("myBookings").rows("items")
            if (p["isAdmin"] == true)
                a = call("adminQueue") + mapOf("bookings" to call("adminBookings").rows("items"))
        }
        val reels = call("listReels").rows("items")
        if (generation != g) return
        mutable.value =
            mutable.value.copy(
                profile = p,
                history = h,
                admin = a,
                services = services,
                reels = reels,
                settings = settings,
                bookings = b,
            )
        if (auth.currentUser != null && listeners.isEmpty()) {
            val uid = auth.currentUser!!.uid
            listOf("accounts", "wallets", "creatorBalances").forEach { collection ->
                var first = true
                listeners +=
                    db.collection(collection).document(uid).addSnapshotListener { _, error ->
                        if (generation != g) return@addSnapshotListener
                        if (first) {
                            first = false
                            return@addSnapshotListener
                        }
                        if (error == null && !mutable.value.busy) refresh()
                    }
            }
        }
    }

    fun refresh() {
        if (mutable.value.busy) return
        val g = generation
        viewModelScope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null)
            try {
                load(g)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (g == generation) {
                    if (
                        e is FirebaseFunctionsException &&
                            e.code == FirebaseFunctionsException.Code.PERMISSION_DENIED
                    ) {
                        mutable.value =
                            PlatformState(
                                signedIn = auth.currentUser != null,
                                error = e.localizedMessage,
                            )
                    } else
                        mutable.value =
                            mutable.value.copy(error = e.localizedMessage ?: "تعذر الاتصال بالخادم")
                }
            } finally {
                if (g == generation) mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun login(email: String, password: String, register: Boolean) {
        if (mutable.value.busy) return
        viewModelScope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null)
            try {
                if (register) auth.createUserWithEmailAndPassword(email.trim(), password).await()
                else auth.signInWithEmailAndPassword(email.trim(), password).await()
            } catch (e: Exception) {
                mutable.value =
                    mutable.value.copy(
                        busy = false,
                        error = e.localizedMessage ?: "تعذر تسجيل الدخول",
                    )
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                mutable.value =
                    mutable.value.copy(notice = "إذا كان البريد مسجلًا ستصلك رسالة الاستعادة")
            } catch (e: Exception) {
                fail(e.localizedMessage ?: "تعذر إرسال رسالة الاستعادة")
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun perform(
        name: String,
        data: Record,
        message: String,
        uri: Uri? = null,
        folder: String? = null,
        after: () -> Unit = {},
    ) {
        if (mutable.value.busy) return
        val g = generation
        viewModelScope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null, notice = null)
            try {
                var payload = data
                if (uri != null && folder != null) {
                    val uid = auth.currentUser?.uid ?: error("سجل الدخول أولًا")
                    val path = "$folder/$uid/${UUID.randomUUID()}"
                    val type =
                        getApplication<Application>().contentResolver.getType(uri)
                            ?: "application/octet-stream"
                    FirebaseStorage.getInstance()
                        .reference
                        .child(path)
                        .putFile(uri, StorageMetadata.Builder().setContentType(type).build())
                        .await()
                    payload = payload + ("storagePath" to path)
                }
                call(name, payload)
                if (g == generation) {
                    after()
                    mutable.value = mutable.value.copy(notice = message)
                }
                load(g)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (g == generation)
                    fail(e.localizedMessage ?: "لم تكتمل العملية؛ أعد المحاولة بنفس الطلب")
            } finally {
                if (g == generation) mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        listeners.forEach { it.remove() }
    }
}
