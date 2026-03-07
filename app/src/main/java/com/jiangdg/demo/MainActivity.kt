/*
 * Copyright 2017-2022 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.demo

import android.Manifest.permission.*
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.demo.databinding.ActivityMainBinding

/**
 * Demos of camera usage
 *
 * @author Created by jiangdg on 2021/12/27
 */
class MainActivity : AppCompatActivity() {
    private var mWakeLock: PowerManager.WakeLock? = null
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setStatusBar()
            viewBinding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(viewBinding.root)
            checkAndRequestPermissions()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            e.printStackTrace()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        mWakeLock = Utils.wakeLock(this)
    }

    override fun onStop() {
        super.onStop()
        mWakeLock?.apply {
            Utils.wakeUnLock(this)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(CAMERA)
        }

        // Check record audio permission
        if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(RECORD_AUDIO)
        }

        // Check storage permission (only for Android 12 and below)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
                ToastUtils.show("Camera permissions are required for this app")
            }
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_CAMERA
            )
        } else {
            // All permissions granted
            replaceDemoFragment(DemoFragment())
        }
    }

    private fun replaceDemoFragment(fragment: Fragment) {
        try {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error replacing fragment", e)
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CAMERA -> {
                var allGranted = true
                for (result in grantResults) {
                    if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                        break
                    }
                }

                if (allGranted) {
                    replaceDemoFragment(DemoFragment())
                } else {
                    ToastUtils.show("Camera permissions are required")
                }
            }
            REQUEST_STORAGE -> {
                val hasStoragePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                if (!hasStoragePermission) {
                    ToastUtils.show("Storage permission is required")
                }
            }
            else -> {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setStatusBar() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val REQUEST_CAMERA = 0
        private const val REQUEST_STORAGE = 1
    }
}
