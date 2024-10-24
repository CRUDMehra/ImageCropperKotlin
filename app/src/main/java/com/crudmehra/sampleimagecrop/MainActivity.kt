package com.crudmehra.sampleimagecrop

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.crudmehra.imagecropper.utils.CropImage
import com.crudmehra.imagecropper.utils.CropImage.getActivityResult
import com.crudmehra.imagecropper.utils.ImageCropParams
import com.crudmehra.imagecropper.utils.ImageCropUI

class MainActivity : AppCompatActivity() {
    private var ivCroppedImage: ImageView? = null
    private var btnPickImage: Button? = null


    private lateinit var outputImageUri : Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ivCroppedImage = findViewById(R.id.ivCroppedImage)
        btnPickImage = findViewById(R.id.tvPickImage)

        btnPickImage?.setOnClickListener {
             val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/")  // Optional: specify folder
            }
            outputImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!

            val intent = CropImage.activity()
                // to set guidelines on crop
                .setGuidelines(ImageCropUI.Guidelines.ON)
                // to change title of activity
                .setActivityTitle("Choose Image")
                // image flip option by default true
                .setAllowFlipping(false)
                // image rotate option by default true
                .setAllowRotation(false)
                // change crop shape
                .setCropShape(ImageCropUI.CropShape.RECTANGLE)
                // set crop button title
                .setCropMenuCropButtonTitle("Done")
                // set icon
//              .setCropMenuCropButtonIcon(R.drawable.ic_launcher_background)
                // to change resolution size of cropped image // this will change image quality // you also can skip this step for high quality image
                //.setRequestedSize(400, 400)
                // picker type like camera or gallery or both
                .setPickerType(ImageCropParams.PickerType.CAMERA_AND_GALLERY)
                // to change crop viewer background
//              .setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                // to change menu icon color
                .setActivityMenuIconColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                // set crop image size like 1:1 , 9:12 or something else
                .setAspectRatio(9, 12)
                // set zoom enable while cropping image by default true
                .setAutoZoomEnabled(false)
                // save image in local storage
                .setOutputUri(outputImageUri)
                // set output quality by default 100
                .setOutputCompressQuality(100)
                // default JPEG
                .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                // get intent value and set in your launcher
                .getIntent(this)

            launcherRequest.launch(intent)
        }
    }

    private val launcherRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultURI = getActivityResult(result.data)
        ivCroppedImage?.setImageURI(resultURI?.uri)
        Log.e("MainActivity", "${resultURI?.uri.toString()} ")
    }

}