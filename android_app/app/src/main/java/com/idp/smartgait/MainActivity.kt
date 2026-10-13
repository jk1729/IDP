package com.idp.smartgait

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.idp.smartgait.repository.Repository
import com.idp.smartgait.ui.navigation.AppNavigation
import com.idp.smartgait.ui.theme.SmartGaitTheme
import com.idp.smartgait.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.appContext = applicationContext

        // Request Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )

            if (permissions.any {
                    checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                }) {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    1001
                )
            }

        } else {

            // Location permission is required for Bluetooth scanning
            // on older Android versions.
            if (
                checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1002
                )
            }
        }

        setContent {
            SmartGaitTheme {
                val mainViewModel =
                    ViewModelProvider(this@MainActivity)[MainViewModel::class.java]

                AppNavigation(mainViewModel)
            }
        }
    }
}