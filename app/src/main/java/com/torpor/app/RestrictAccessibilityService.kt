package com.torpor.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RestrictAccessibilityService : AccessibilityService() {

    companion object {
        var onResult: ((Boolean) -> Unit)? = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName != "com.android.settings") return

        CoroutineScope(Dispatchers.Main).launch {
            delay(400)
            val root = rootInActiveWindow ?: return@launch
            val target = findDataUsageEntry(root) ?: findBackgroundDataSwitch(root)

            if (target == null) {
                onResult?.invoke(false)
                onResult = null
                return@launch
            }

            if (target.className == "android.widget.Switch") {
                if (target.isChecked) {
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                delay(300)
                onResult?.invoke(true)
                onResult = null
                performGlobalAction(GLOBAL_ACTION_BACK)
            } else {
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                delay(500)
                val newRoot = rootInActiveWindow
                val switchNode = newRoot?.let { findBackgroundDataSwitch(it) }
                if (switchNode != null && switchNode.isChecked) {
                    switchNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    delay(300)
                    onResult?.invoke(true)
                } else {
                    onResult?.invoke(switchNode != null)
                }
                onResult = null
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    private fun findDataUsageEntry(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeByTextContains(node, "Mobile data") ?: findNodeByTextContains(node, "Data usage")
    }

    private fun findBackgroundDataSwitch(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = findNodeByTextContains(node, "Background data")
        if (label != null) {
            return findSiblingSwitch(label)
        }
        return findNodeByClassName(node, "android.widget.Switch")
    }

    private fun findNodeByTextContains(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.contains(text, ignoreCase = true) == true) return node
        for (i in 0 until node.childCount) {
            val result = findNodeByTextContains(node.getChild(i), text)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeByClassName(node: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.className == className) return node
        for (i in 0 until node.childCount) {
            val result = findNodeByClassName(node.getChild(i), className)
            if (result != null) return result
        }
        return null
    }

    private fun findSiblingSwitch(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val parent = node.parent ?: return null
        return findNodeByClassName(parent, "android.widget.Switch")
    }

    override fun onInterrupt() {}
}
