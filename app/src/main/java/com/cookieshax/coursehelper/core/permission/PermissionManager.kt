package com.cookieshax.coursehelper.core.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object PermissionManager : DefaultLifecycleObserver {
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var onResult: ((Boolean) -> Unit)? = null

    // 在 Activity 的 onCreate 中初始化
    fun init(activity: ComponentActivity) {
        activity.lifecycle.addObserver(this)
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val allGranted = results.values.all { it }
            onResult?.invoke(allGranted)
        }
    }

    fun hasPermission(context: Context, string: String): Boolean {
        return ContextCompat.checkSelfPermission(context, string) == PackageManager.PERMISSION_GRANTED
    }

    // 统一的申请接口
    fun requestPermissions(
        context: Context,
        permissions: Array<String>,
        callback: (Boolean) -> Unit
    ) {
        // 检查是否授权
        val isGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (isGranted) {
            callback(true)
        } else {
            // 发起申请
            this.onResult = callback
            permissionLauncher?.launch(permissions)
        }
    }

    // Activity 销毁时清理引用
    override fun onDestroy(owner: LifecycleOwner) {
        permissionLauncher = null
        onResult = null
    }
}