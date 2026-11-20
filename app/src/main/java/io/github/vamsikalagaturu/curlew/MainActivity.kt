package io.github.vamsikalagaturu.curlew

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.vamsikalagaturu.curlew.ui.ContactEditorScreen
import io.github.vamsikalagaturu.curlew.ui.CurlewTheme
import io.github.vamsikalagaturu.curlew.ui.EditorRequest
import io.github.vamsikalagaturu.curlew.ui.HomeScreen
import io.github.vamsikalagaturu.curlew.ui.SettingsScreen
import io.github.vamsikalagaturu.curlew.ui.Tab

class MainActivity : ComponentActivity() {
    private val number = mutableStateOf("")
    private val tab = mutableStateOf(Tab.Keypad)
    private val isDefault = mutableStateOf(false)
    private val editor = mutableStateOf<EditorRequest?>(null)
    private val settings = mutableStateOf(false)
    private lateinit var data: PhoneData

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshRole()
        requestPermissions()
    }

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        data.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        data = PhoneData(applicationContext, lifecycleScope)
        if (savedInstanceState == null) {
            tab.value = runCatching { Tab.valueOf(Prefs.get(this).defaultTab.value) }.getOrDefault(Tab.Keypad)
        }
        handle(intent)
        Updater.autoCheck(this)
        setContent {
            val contacts by data.contacts.collectAsStateWithLifecycle()
            val recents by data.recents.collectAsStateWithLifecycle()
            val blocked by data.blocked.collectAsStateWithLifecycle()
            val update by Updater.status.collectAsStateWithLifecycle()
            CurlewTheme {
                val editing = editor.value
                if (editing != null) {
                    BackHandler { editor.value = null }
                    ContactEditorScreen(editing, onClose = { editor.value = null })
                } else if (settings.value) {
                    BackHandler { settings.value = false }
                    SettingsScreen(onClose = { settings.value = false })
                } else {
                    HomeScreen(
                        number = number.value,
                        onNumberChange = { number.value = it },
                        tab = tab.value,
                        onTabChange = { tab.value = it },
                        contacts = contacts,
                        recents = recents,
                        blocked = blocked,
                        isDefaultDialer = isDefault.value,
                        onRequestDefault = ::requestRole,
                        onCall = {
                            dial(it)
                            number.value = ""
                        },
                        onRecentsSeen = { MissedCalls.clear(this) },
                        onVoicemail = { placeCall(Uri.fromParts("voicemail", "", null)) },
                        onEdit = { editor.value = it },
                        onSettings = { settings.value = true },
                        update = update,
                        onUpdate = {
                            (update as? UpdateStatus.Available)?.let { Updater.install(this, it.release) }
                            settings.value = true
                        },
                    )
                }
            }
        }
        if (savedInstanceState == null) requestPermissions()
    }

    override fun onStart() {
        super.onStart()
        data.start()
    }

    override fun onStop() {
        data.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshRole()
        if (tab.value == Tab.Recents && editor.value == null && !settings.value) MissedCalls.clear(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_RECENTS -> {
                tab.value = Tab.Recents
                editor.value = null
                settings.value = false
                MissedCalls.clear(this)
                return
            }
            ACTION_CALL_BACK -> {
                intent.data?.schemeSpecificPart?.let { dial(it) }
                MissedCalls.clear(this)
                return
            }
        }
        if (intent.action != Intent.ACTION_DIAL && intent.action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return
        if (data.scheme == "tel") {
            number.value = data.schemeSpecificPart.orEmpty()
            tab.value = Tab.Keypad
            editor.value = null
            settings.value = false
        }
    }

    companion object {
        const val ACTION_SHOW_RECENTS = "io.github.vamsikalagaturu.curlew.SHOW_RECENTS"
        const val ACTION_CALL_BACK = "io.github.vamsikalagaturu.curlew.CALL_BACK"
    }

    private fun refreshRole() {
        isDefault.value = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)
    }

    private fun requestRole() {
        val roles = getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            roleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    }

    private fun requestPermissions() {
        val wanted = buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.WRITE_CONTACTS)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.WRITE_CALL_LOG)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_PHONE_NUMBERS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) permissionRequest.launch(missing.toTypedArray())
    }
}
