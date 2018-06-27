package com.imitate_genic_wall

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import java.util.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.maps.model.*
import com.imitate_genic_wall.DB.DatabaseMap
import com.imitate_genic_wall.DB.MapLocation.ListDataMap
import com.imitate_genic_wall.DB.MapLocation.ListDataParserMap
import com.imitate_genic_wall.Dialog.OriginalDialog
import org.jetbrains.anko.db.IntParser
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select


class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMyLocationButtonClickListener, LocationSource,
        GoogleMap.OnPoiClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener {

    private val TAG = MapsActivity::class.java.simpleName

    //各種パラメータ
    private lateinit var mMap: GoogleMap
    private lateinit var latlng: LatLng
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var marker: Marker
    private lateinit var beforeMarker: Marker
    private lateinit var locationList: List<ListDataMap>
    private var zoomLevel: Float = 10f
    private var IdTappedmarker = 0
    private var nameFromMarker: String? = null
    private var timeFromMarker: String? = null
    private val dataBaseMap = DatabaseMap(this)
    private val manager: android.app.FragmentManager? = fragmentManager

    //リスナー
    private var onLocationChangedListener: LocationSource.OnLocationChangedListener? = null

    //priority
    private val priority = intArrayOf(LocationRequest.PRIORITY_HIGH_ACCURACY,
            LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, LocationRequest.PRIORITY_LOW_POWER, LocationRequest.PRIORITY_NO_POWER)
    private var locationPriority: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // LocationRequest を生成して精度、インターバルを設定
        setLocationRequest()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()

    }
    /**
     * onResumeフェーズに入ったら接続
     * DB にレコードがあれば取得してマップに表示
     */
    override fun onResume() {
        super.onResume()
        mGoogleApiClient.connect()

    }

    // onPauseで切断
    public override fun onPause() {
        super.onPause()
        mGoogleApiClient.disconnect()
    }

    /**
     * Manipulates the map once available.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("onMapReady", "permission granted")

            mMap = googleMap
            // default の LocationSource から自前のsourceに変更する
            mMap.setLocationSource(this)
            mMap.isMyLocationEnabled = true
            mMap.setOnMyLocationButtonClickListener(this)
            mMap.setOnMapClickListener(this)//どちらかしか反応しない
            mMap.setOnPoiClickListener(this)//どちらかしか反応しない
            mMap.setOnMarkerClickListener(this)
        }

        /**
         * ルート検索
         * intentでGoogle map に飛ぶ
         */
        //        setRoute()
    }
    /**
     * 端末のロケーション取得
     * zoomMap() で Location 移動
     */
    override fun onLocationChanged(location: Location) {

        Log.d("onLocationChanged", " locationNOW = (${location.latitude}, ${location.longitude})")

        onLocationChangedListener?.onLocationChanged(location)

        setMarkerFromDB()

        //現時点のズームレベルを取得
        val zoom = mMap.cameraPosition.zoom
        if (zoomLevel < zoom){
            zoomMap(location.latitude, location.longitude, zoom)
        } else {
            zoomMap(location.latitude, location.longitude, null)
        }
    }

    /**
     * POI以外タップ時に情報取得
     * マーカー表示
     */
    override fun onMapClick(tapLocation: LatLng?) {
        if (tapLocation == null)return
        nameFromMarker = null
        timeFromMarker = android.text.format.DateFormat.format("yyyy/MM/dd kk:mm", Calendar.getInstance()).toString()

        //一つ前に立てたマーカーを削除
        try {
            if (beforeMarker.position != tapLocation) {
                beforeMarker.remove()
            } else return
        } catch (e: Exception){
            Log.e("Exception ", "$TAG onMapClick " + e.toString())
        }

        val option = MarkerOptions().position(tapLocation)
        beforeMarker = mMap.addMarker(option.title(timeFromMarker))
        beforeMarker.showInfoWindow()
    }

    /**
     * POI(有名スポット)タップ時に情報取得
     */
    override fun onPoiClick(tapLocation: PointOfInterest?) {
        if (tapLocation == null) return
        nameFromMarker = tapLocation.name
        timeFromMarker = android.text.format.DateFormat.format("yyyy/MM/dd kk:mm", Calendar.getInstance()).toString()

        //一つ前に立てたマーカーを削除
        try {
            if (beforeMarker.position != tapLocation.latLng) {
                beforeMarker.remove()
            } else return
        } catch (e: Exception){
            Log.e("Exception ", "$TAG onMapClick " + e.toString())
        }

        val option = MarkerOptions().position(tapLocation.latLng)
        beforeMarker = mMap.addMarker(option
                .title(timeFromMarker)
                .snippet(nameFromMarker))
        beforeMarker.showInfoWindow()
    }

    /**
     * マーカークリック
     * 保存確認
     * YES → 保存
     * NO → マーカー削除
     */
    override fun onMarkerClick(tapLocation: Marker?): Boolean {
        if (tapLocation == null) return false
        if (checkRecordByLocation(tapLocation)) return false

        Log.d("onMarkerClick", " active")

        val dialog = OriginalDialog()
        dialog.title = "保存しますか？"
        dialog.okText = "YES"
        dialog.onOkClickListener = DialogInterface.OnClickListener { dialog, which ->
            saveLocation(tapLocation.position, timeFromMarker, nameFromMarker)
        }
        dialog.cancelText = "NO"
        dialog.onCancelClickListener = DialogInterface.OnClickListener { dialog, which ->
            tapLocation.remove()
        }
        dialog.show(manager, "tag")

        return true
    }

    /**
     * 情報ウィンドウクリック
     */
    override fun onInfoWindowClick(p0: Marker?) {

    }

    /**
     * 接続された時に現在地取得
     */
    override fun onConnected(bundle: Bundle?) {
        // check permission
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // FusedLocationApi

            Log.d("onConnected", " active")

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, locationRequest, this)
        } else {
            Log.d("debug", "permission error")
            return
        }
    }

    // OnLocationChangedListener calls activate() method
    override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener) {
        this.onLocationChangedListener = onLocationChangedListener
        Log.d("activate", " active")
    }

    override fun deactivate() {
        this.onLocationChangedListener = null
        Log.d("deactivate", " active")
    }

    override fun onConnectionSuspended(i: Int) {
        Log.d("debug", "onConnectionSuspended")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d("debug", "onConnectionFailed")
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "onMyLocationButtonClick", Toast.LENGTH_SHORT).show()

        return false
    }
    /**
     * DB 内に渡されたLocation と一致するレコードがあるかチェック
     * ある true
     * ない false
     * 渡されたマーカーと同じロケーションがDB内に存在する場合
     * そのIdを IdTappedmarker に入れる
     */
    private fun checkRecordByLocation(marker: Marker): Boolean{
        var check = false
        dataBaseMap.use {
            try {
                val location = this.select(dataBaseMap.TABLE_NAME_LOCATION)
                        .parseList(ListDataParserMap(this@MapsActivity))
                for (i in 0.. location.lastIndex){
                    Log.i("checkRecordByLocation ", "location = ${location[i]}")
                    Log.i("checkRecordByLocation  ", "marker = ${marker.position}")
                    if (marker.position == LatLng(location[i].latitude, location[i].longitude)) {
                        IdTappedmarker = location[i].id.toInt()
                        setMarker(location[i].latitude, location[i].longitude, location[i].time, location[i].name)
                        check = true
                        break
                    }
                }
            } catch (e: Exception){
                Log.e("Exception ", "$TAG checkRecordByLocation " + e.toString())
            }
        }
        Log.d("$TAG checkRecordByLocation", " check = $check")
        return check
    }
    /**
     * 渡された場所にマーカー立ててズームする
     */
    private fun setMarker(latitude: Double, longitude: Double, title: String?, snippet: String?) {

        Log.i("setMarker ", "latitude = $latitude longitude = $longitude name = $snippet")

        latlng = LatLng(latitude, longitude)

        val markerOptions = MarkerOptions()
        markerOptions.position(latlng)
        marker = if (title != null || snippet != null){
            mMap.addMarker(markerOptions
                    .title(title)
                    .snippet(snippet))
        } else {
            mMap.addMarker(markerOptions)
        }
        marker.showInfoWindow()

        // ズーム
        zoomMap(latitude, longitude, mMap.cameraPosition.zoom)
    }

    /**
     * DB に データがあればそのロケーションをセットする
     * 初期表示ではインフォは表示しない
     * タップによって表示されたインフォを保持する
     */
    private fun setMarkerFromDB(){
        dataBaseMap.use {
            try {
                locationList = this.select(dataBaseMap.TABLE_NAME_LOCATION)
                        .parseList(ListDataParserMap(this@MapsActivity))
            } catch (e: Exception){
                Log.e("Exception ", "$TAG setMarkerFromDB1 " + e.toString())
            }
            if (locationList.isNotEmpty()){
                for (i in 0.. locationList.lastIndex){
                    try {
                        Log.d("setMarkerFromDB ", "locationList[$i] = ${locationList[i]}")
                        if (IdTappedmarker == locationList[i].id.toInt()){
                            setMarker(locationList[i].latitude, locationList[i].longitude, locationList[i].time, locationList[i].name)
                        } else {
                            setMarker(locationList[i].latitude, locationList[i].longitude, null, null)
                        }
                    } catch (e: Exception){
                        Log.e("Exception ", "$TAG setMarkerFromDB2 " + e.toString())
                    }
                }
            }
        }
    }
    /**
     * 指定した距離までズーム
     * zoomLevel がある場合は、その level のままの状態をキープ
     */
    private fun zoomMap(latitude: Double, longitude: Double, zoomLevel: Float?) {
        // 表示する東西南北の緯度経度を設定
        val south = latitude * (1 - 0.00005)
        val west = longitude * (1 - 0.00005)
        val north = latitude * (1 + 0.00005)
        val east = longitude * (1 + 0.00005)

        // LatLngBounds (LatLng southwest, LatLng northeast)
        val bounds = LatLngBounds.builder()
                .include(LatLng(south, west))
                .include(LatLng(north, east))
                .build()

        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels

        if (zoomLevel != null){
            mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel))
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, 0))
        }
    }
    /**
     * 渡されたLocationをDBに保存する
     */
    private fun saveLocation(tapLocation: LatLng, time: String?, name: String?){
        var maxId = 0
        dataBaseMap.use {
            try {
                maxId = this.select(dataBaseMap.TABLE_NAME_LOCATION, "MAX(${dataBaseMap.ID_LOCATION})")
                        .parseSingle(IntParser)
            } catch (e: Exception){
                Log.e("Exception ", TAG + " mMap.setOnMapClickListener1 " + e.toString())
            }
            try {
                this.insert(dataBaseMap.TABLE_NAME_LOCATION,
                        dataBaseMap.ID_LOCATION to (maxId + 1),
                        dataBaseMap.TIME to time,
                        dataBaseMap.NAME_LOCATION to name,
                        dataBaseMap.LATITUDE to tapLocation.latitude,
                        dataBaseMap.LONGITUDE to tapLocation.longitude)
            } catch (e: Exception){
                Log.e("Exception ", TAG + " mMap.setOnMapClickListener2 " + e.toString())
            }
        }
    }
    /**
     * ロケーションの制度を優先づけ
     */
    private fun setLocationRequest(){

        locationRequest = LocationRequest.create()

        // 測位の精度、消費電力の優先度
        locationPriority = priority[1]

        when (locationPriority) {
            priority[0] -> {
                // 位置情報の精度を優先する場合
                locationRequest.priority = locationPriority
                locationRequest.interval = 5000
                locationRequest.fastestInterval = 16
            }
            priority[1] -> {
                // 消費電力を考慮する場合
                locationRequest.priority = locationPriority
                locationRequest.interval = 60000
                locationRequest.fastestInterval = 16
            }
            priority[2] -> // "city" level accuracy
                locationRequest.priority = locationPriority
            else -> // 外部からのトリガーでの測位のみ
                locationRequest.priority = locationPriority
        }
    }
}
