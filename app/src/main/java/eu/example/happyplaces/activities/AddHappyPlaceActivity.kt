package eu.example.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import eu.example.happyplaces.R
import eu.example.happyplaces.database.DatabaseHandler
import eu.example.happyplaces.databinding.ActivityAddHappyPlaceBinding
import eu.example.happyplaces.models.HappyPlaceModel
import eu.example.happyplaces.utils.GetAddressFromLatLng
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Suppress("DEPRECATION")
class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {

    private var binding: ActivityAddHappyPlaceBinding?= null

    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener:DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude:Double = 0.0
    private var mLongitude = 0.0

    private var mHappyPlaceDetails: HappyPlaceModel? = null

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarAddPlace)
        if(supportActionBar!=null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        mFusedLocationClient= LocationServices.getFusedLocationProviderClient(this)

        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,
                resources.getString(R.string.google_maps_api_key))
        }

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getParcelableExtra(
                MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel?
        }

        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)

            updateDateInView()

        }
        updateDateInView()

        if(mHappyPlaceDetails !=null){
            supportActionBar?.title = "Edit Happy Place"

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)
            binding?.btnSave?.text = "UPDATE"
        }

        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        binding?.btnSave?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
        binding?.tvSelectCurrentLocation?.setOnClickListener(this)
    }

    private fun isLocationEnabled():Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,mLocationCallBack, Looper.myLooper())
    }

    private val mLocationCallBack = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            mLatitude = mLastLocation!!.latitude
            Log.i("Current Latitude","$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.i("Current Longitude","$mLongitude")

            val addressTask = GetAddressFromLatLng(this@AddHappyPlaceActivity,mLatitude,mLongitude)
            addressTask.setAddressListener(object : GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address: String?) {
                    binding?.etLocation?.setText(address)
                }
                override fun onError() {
                    Log.e("Get Address:: ", "Something went wrong")
                }
            })
            addressTask.getAddress()
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            binding?.etDate?.id ->{
                DatePickerDialog(this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            binding?.tvAddImage?.id ->{
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select photo from Gallery",
                    "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems){
                    _, which ->
                    when(which){
                        0 -> choosePhotoFromGallery()
                        1 ->takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            binding?.btnSave?.id ->{
                when {
                    binding?.etTitle?.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Please enter title", Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding?.etDescription?.text.isNullOrEmpty()->{
                        Toast.makeText(this@AddHappyPlaceActivity,
                            "Please enter a description", Toast.LENGTH_SHORT).show()
                    }
                    binding?.etLocation?.text.isNullOrEmpty()->{
                        Toast.makeText(this@AddHappyPlaceActivity,
                            "Please enter a location", Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage == null ->{
                        Toast.makeText(this@AddHappyPlaceActivity,
                            "Please select an image",Toast.LENGTH_SHORT).show()
                    }
                    else ->{
                        val happyPlaceModel = HappyPlaceModel(
                            if(mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            binding?.etTitle?.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding?.etDescription?.text.toString(),
                            binding?.etDate?.text.toString(),
                            binding?.etLocation?.text.toString(),
                            mLatitude,
                            mLongitude
                        )

                        val dbHandler = DatabaseHandler(this)

                        if(mHappyPlaceDetails == null){
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                            if(addHappyPlace>0){
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }else{
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if(updateHappyPlace>0){
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }
            binding?.etLocation?.id ->{
                try {
                    val fields = listOf(
                        Place.Field.ID,Place.Field.NAME,Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )

                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,fields)
                        .build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
            binding?.tvSelectCurrentLocation?.id ->{
                if(!isLocationEnabled()){
                    Toast.makeText(this,
                        "Your location is turned off. Please turn it on",Toast.LENGTH_SHORT).show()

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }else{
                    Dexter.withActivity(this).withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ).withListener(object : MultiplePermissionsListener{
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            if(report!!.areAllPermissionsGranted()){
                                requestNewLocationData()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permissions: MutableList<PermissionRequest>?,
                            token: PermissionToken?,
                        ) {
                            showRationalDialogForPermissions()
                        }

                    }).onSameThread().check()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GALLERY){
                if(data != null){
                    val contentURI = data.data
                    try{
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,contentURI)

                        saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("Saved image: ","Path :: $saveImageToInternalStorage")

                        binding?.ivPlaceImage?.setImageBitmap(selectedImageBitmap)
                    }catch (e:IOException){

                        e.printStackTrace()
                        Toast.makeText(this@AddHappyPlaceActivity,
                            "Failed to load image from gallery",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }else if(requestCode == CAMERA){
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap

                saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)
                Log.e("Saved image", "Path :: $saveImageToInternalStorage")

                binding?.ivPlaceImage?.setImageBitmap(thumbnail)

            }else if(requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){
                val place:Place = Autocomplete.getPlaceFromIntent(data!!)
                binding?.etLocation?.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude=place.latLng!!.longitude
            }
        }
    }

    private fun takePhotoFromCamera(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if(report!!.areAllPermissionsGranted()){
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(cameraIntent, CAMERA)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun choosePhotoFromGallery(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object: MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if(report!!.areAllPermissionsGranted()){
                    val galleryIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent, GALLERY)
                }
            }
            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>,
                token: PermissionToken
            ) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }


    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage("It looks like you have turned " +
                "off permissions required for this feature." +
                " It can be enabled under Applications Settings ")
            .setPositiveButton("Go TO SETTINGS"){
                _,_ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName,null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){
                dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")

        try{
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e:IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    private fun updateDateInView(){
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }

    companion object{
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }
}