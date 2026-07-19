package com.torpor.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.torpor.app.data.AppInfo
import com.torpor.app.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppListAdapter
    private val appList = mutableListOf<AppInfo>()
    private var restrictQueue = mutableListOf<AppInfo>()
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AppListAdapter(appList) { app, checked ->
            if (checked) queueRestrict(listOf(app)) else app.isRestricted = false
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnRestrictAll.setOnClickListener {
            queueRestrict(appList.filter { !it.isRestricted && !it.isLocked })
        }

        binding.btnEnableAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        loadApps()
        updateAccessibilityStatus()
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityStatus()
        if (restrictQueue.isNotEmpty() && !isProcessing) {
            processNext()
        }
    }

    private fun loadApps() {
        CoroutineScope(Dispatchers.IO).launch {
            val pm = packageManager
            val installed = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            val filtered = installed
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null || it.packageName.contains("heytap") || it.packageName.contains("coloros") }
                .map {
                    AppInfo(
                        packageName = it.packageName,
                        appName = pm.getApplicationLabel(it).toString(),
                        icon = pm.getApplicationIcon(it)
                    )
                }
                .sortedBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                appList.clear()
                appList.addAll(filtered)
                adapter.notifyDataSetChanged()
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun queueRestrict(apps: List<AppInfo>) {
        restrictQueue.addAll(apps)
        if (!isServiceEnabled()) {
            showAccessibilityDialog()
            return
        }
        if (!isProcessing) processNext()
    }

    private fun processNext() {
        if (restrictQueue.isEmpty()) {
            isProcessing = false
            return
        }
        isProcessing = true
        val target = restrictQueue.removeAt(0)

        RestrictAccessibilityService.onResult = { success ->
            runOnUiThread {
                target.isRestricted = success
                target.isLocked = !success
                adapter.notifyDataSetChanged()
                if (!success) {
                    Snackbar.make(binding.root, "${target.appName} is locked by system", Snackbar.LENGTH_SHORT).show()
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                delay(600)
                processNext()
            }
        }

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${target.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun isServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun updateAccessibilityStatus() {
        val enabled = isServiceEnabled()
        binding.statusText.text = if (enabled) "Accessibility service active" else "Accessibility service disabled"
        binding.btnEnableAccessibility.visibility = if (enabled) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility")
            .setMessage("This app needs Accessibility permission to toggle background data automatically.")
            .setPositiveButton("Open Settings") { _, _ -> openAccessibilitySettings() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
