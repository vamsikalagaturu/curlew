package io.github.vamsikalagaturu.curlew

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.IntentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Release(val tag: String, val page: String, val apk: String?)

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data object NoReleases : UpdateStatus
    data class Available(val release: Release) : UpdateStatus
    data class Downloading(val progress: Float) : UpdateStatus
    data object Installing : UpdateStatus
    data class Failed(val message: String) : UpdateStatus
}

object Updater {
    const val REPO = "vamsikalagaturu/curlew"
    const val PAGE = "https://github.com/$REPO/releases"
    private const val DAY = 24 * 60 * 60 * 1000L

    private val scope = MainScope()
    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status

    fun currentVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    fun autoCheck(context: Context) {
        val prefs = Prefs.get(context)
        if (prefs.autoUpdate.value && System.currentTimeMillis() - prefs.lastUpdateCheck() > DAY) check(context)
    }

    fun check(context: Context) {
        val busy = _status.value
        if (busy is UpdateStatus.Checking || busy is UpdateStatus.Downloading || busy is UpdateStatus.Installing) return
        _status.value = UpdateStatus.Checking
        scope.launch {
            _status.value = runCatching { withContext(Dispatchers.IO) { latest() } }.fold(
                { release ->
                    Prefs.get(context).setLastUpdateCheck(System.currentTimeMillis())
                    when {
                        release == null -> UpdateStatus.NoReleases
                        release.apk != null && compareVersions(release.tag, currentVersion(context)) > 0 -> UpdateStatus.Available(release)
                        else -> UpdateStatus.UpToDate
                    }
                },
                { UpdateStatus.Failed(it.message ?: "Couldn't reach GitHub") },
            )
        }
    }

    fun install(context: Context, release: Release) {
        val url = release.apk ?: return
        val app = context.applicationContext
        _status.value = UpdateStatus.Downloading(0f)
        scope.launch {
            runCatching {
                val file = File(app.cacheDir, "curlew-update.apk")
                withContext(Dispatchers.IO) { download(url, file) { _status.value = UpdateStatus.Downloading(it) } }
                _status.value = UpdateStatus.Installing
                withContext(Dispatchers.IO) { commit(app, file) }
            }.onFailure { _status.value = UpdateStatus.Failed(it.message ?: "Update failed") }
        }
    }

    fun fail(message: String) {
        _status.value = UpdateStatus.Failed(message)
    }

    fun compareVersions(a: String, b: String): Int {
        fun parts(v: String): List<Int> {
            val nums = v.removePrefix("v").split('-', '+').first().split('.').map { it.toIntOrNull() ?: 0 }
            return (nums + List(3) { 0 }).take(3)
        }
        val x = parts(a)
        val y = parts(b)
        for (i in 0 until 3) {
            val c = x[i].compareTo(y[i])
            if (c != 0) return c
        }
        return 0
    }

    private fun latest(): Release? {
        val conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "curlew")
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        try {
            if (conn.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (conn.responseCode != HttpURLConnection.HTTP_OK) error("GitHub answered ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0))
                .map { assets!!.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") }
                ?.optString("browser_download_url")
            return Release(json.getString("tag_name"), json.optString("html_url", PAGE), apk)
        } finally {
            conn.disconnect()
        }
    }

    private fun download(url: String, file: File, progress: (Float) -> Unit) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", "curlew")
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) error("Download failed (${conn.responseCode})")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var reported = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        done += n
                        if (total > 0 && done - reported > 128 * 1024) {
                            progress(done.toFloat() / total)
                            reported = done
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun commit(context: Context, file: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("curlew.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            // The installer fills in the status extras, so this PendingIntent has to be mutable.
            val done = PendingIntent.getBroadcast(
                context, id, Intent(context, UpdateReceiver::class.java),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            session.commit(done.intentSender)
        }
    }
}

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                    ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> Updater.fail(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Install failed ($status)")
        }
    }
}
