package com.imitate_genic_wall

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import android.util.Log
import com.facebook.stetho.Stetho
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast


class MainActivity : AppCompatActivity(){

    val TAG = MainActivity::class.java.simpleName
    private val REQUEST_PERMISSION = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initStetho()

        if(Build.VERSION.SDK_INT >= 23){
            checkPermission()
        }
        else{
            locationActivity()
        }
    }

    // 位置情報許可の確認
    fun checkPermission() {

        // 既に許可している
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationActivity()
        } else {
            requestLocationPermission()
        }
    }

    // 許可を求める
    private fun requestLocationPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION)

        } else {
            val toast = Toast.makeText(this, "許可されないとアプリが実行できません", Toast.LENGTH_SHORT)
            toast.show()

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION)

        }
    }

    // 結果の受け取り
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        if (requestCode == REQUEST_PERMISSION) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationActivity()
                return

            } else {
                // それでも拒否された時の対応
                toast("これ以上なにもできません")
            }
        }
    }

    // Intent でLocation
    private fun locationActivity() {

        startActivity<MapsActivity>()

        finish()
    }

    private fun initStetho() {
        if (BuildConfig.DEBUG) {
            Stetho.initialize(
                    Stetho.newInitializerBuilder(this)
                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                            .build()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("Now ", TAG)
    }

}