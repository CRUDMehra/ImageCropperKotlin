package com.crudmehra.imagecropper

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.net.toUri
import com.crudmehra.imagecropper.utils.CropImage
import com.crudmehra.imagecropper.utils.CropImage.isExplicitCameraPermissionRequired
import com.crudmehra.imagecropper.utils.CropImage.isReadExternalStoragePermissionsRequired
import com.crudmehra.imagecropper.utils.ImageCropParams
import com.crudmehra.imagecropper.utils.ImageCropUI
import com.crudmehra.imagecropper.utils.ImageCropUI.CropResult
import com.crudmehra.imagecropper.utils.ImageCropUI.OnCropImageCompleteListener
import com.crudmehra.imagecropper.utils.ImageCropUI.OnSetImageUriCompleteListener
import com.crudmehra.imagecropper.utils.dialogs.showImagePickerDialog
import java.io.File
import java.io.IOException


class ImageCropActivity : AppCompatActivity(), OnSetImageUriCompleteListener,
    OnCropImageCompleteListener {
    private var ivCropImage: ImageCropUI? = null

    private var cropImageUri: Uri? = null

    private var optionsHandler: ImageCropParams? = null
    private lateinit var pickGalleryMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickCameraMedia: ActivityResultLauncher<Uri>


    private lateinit var imageUri: Uri


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crop_image_activity)
        initPickGallery()
        initPickCamera()
        handleBackPress()

        ivCropImage = findViewById(R.id.ivCropImage)

        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
        @Suppress("DEPRECATION")
        cropImageUri = bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
        @Suppress("DEPRECATION")
        optionsHandler = bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS)

        if (savedInstanceState == null) {
            if (cropImageUri == null || cropImageUri == Uri.EMPTY) {
                when (optionsHandler?.pickerType) {
                    ImageCropParams.PickerType.CAMERA_AND_GALLERY -> {
                        showImagePickerDialog(this, onItemSelected = { optionItem ->
                            when (optionItem) {
                                "Camera" -> {
                                    prepareOpenCamera()
                                }

                                "Gallery" -> {
                                    prepareOpenGallery()
                                }

                                else -> {
                                    setResultCancel()
                                }
                            }

                        })
                    }

                    ImageCropParams.PickerType.CAMERA_ONLY -> {
                        prepareOpenCamera()
                    }

                    ImageCropParams.PickerType.GALLERY_ONLY -> {
                        prepareOpenGallery()
                    }

                    null -> {
                        setResultCancel()
                    }
                }
            } else {
                // no permissions required or already grunted, can start crop image activity
                ivCropImage?.setImageUriAsync(cropImageUri)
            }
        }

        val actionBar = supportActionBar
        if (actionBar != null) {
            val title =
                if (optionsHandler != null && optionsHandler!!.activityTitle.isNotEmpty()
                ) optionsHandler!!.activityTitle
                else resources.getString(R.string.crop_image_activity_title)
            actionBar.title = title
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

    }

    private fun prepareOpenGallery() {
        if (isReadExternalStoragePermissionsRequired(
                this,
                cropImageUri.toString().toUri()
            )
        ) {
            permissionHandler()
        } else {
            openGallerySystemDialog()
        }
    }

    private fun prepareOpenCamera() {
        if (isExplicitCameraPermissionRequired(this)) {
            requestPermissionsForCamera.launch(arrayOf(Manifest.permission.CAMERA))
        } else {
            openSystemCamera()
        }
    }

    private fun openSystemCamera() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/")  // Optional: specify folder
        }
        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!

        pickCameraMedia.launch(imageUri)
    }

    private fun openGallerySystemDialog() {
        val intentLaunch =
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        pickGalleryMedia.launch(intentLaunch)
    }


    private fun initPickGallery() {
        pickGalleryMedia =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    uri.let {
                        try {
                            try {
                                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                contentResolver.takePersistableUriPermission(it, flag)
                            }catch (_:Exception){ }

                            cropImageUri = it

                            if (isReadExternalStoragePermissionsRequired(this, cropImageUri!!)) {
                                permissionHandler()
                            } else {
                                ivCropImage!!.setImageUriAsync(cropImageUri)
                            }
                        } catch (ex: Exception) {
                            setResultCancel()
                        }
                    }
                } else {
                    setResultCancel()
                }
            }
    }

    private fun initPickCamera() {
        pickCameraMedia = registerForActivityResult(ActivityResultContracts.TakePicture()) { uri ->
            if (uri != null) {
                uri.let {
                    try {
                        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        contentResolver.takePersistableUriPermission(imageUri, flag)
                    }catch (_:Exception){

                    }

                    val realImage = getRealPathFromUri(imageUri).toString()
                    val file = File(realImage)

                    cropImageUri = file.toUri()

                    if (file.exists()) {
                        if (isExplicitCameraPermissionRequired(this)) {
                            requestPermissionsForCamera.launch(arrayOf(Manifest.permission.CAMERA))
                        } else {
                            ivCropImage!!.setImageUriAsync(cropImageUri)
                        }
                    } else {
                        setResultCancel()
                    }
                }
            } else {
                setResultCancel()
            }
        }
    }

    private fun getRealPathFromUri(contentUri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(contentUri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        }
        return null
    }

    private fun permissionHandler() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    READ_MEDIA_VISUAL_USER_SELECTED
                ) != PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(READ_MEDIA_VISUAL_USER_SELECTED)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES) != PERMISSION_GRANTED) {
                permissionsToRequest.add(READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    READ_EXTERNAL_STORAGE
                ) != PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsReadData.launch(permissionsToRequest.toTypedArray())
        } else {
            openGallerySystemDialog()
        }
    }


    private val requestPermissionsReadData = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted =
            permissions.entries.all { it.value } // Check if all permissions are granted
        if (allGranted) {
            openGallerySystemDialog()
        } else {
            setResultCancel()
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionsForCamera = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted =
            permissions.entries.all { it.value } // Check if all permissions are granted
        if (allGranted) {
            openSystemCamera()
        } else {
            setResultCancel()
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        ivCropImage!!.setOnSetImageUriCompleteListener(this)
        ivCropImage!!.setOnCropImageCompleteListener(this)
    }

    override fun onStop() {
        super.onStop()
        ivCropImage!!.setOnSetImageUriCompleteListener(null)
        ivCropImage!!.setOnCropImageCompleteListener(null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.crop_image_menu, menu)

        if (!optionsHandler!!.allowRotation) {
            menu.removeItem(R.id.crop_image_menu_rotate_left)
            menu.removeItem(R.id.crop_image_menu_rotate_right)
        } else if (optionsHandler!!.allowCounterRotation) {
            menu.findItem(R.id.crop_image_menu_rotate_left).setVisible(true)
        }

        if (!optionsHandler!!.allowFlipping) {
            menu.removeItem(R.id.crop_image_menu_flip)
        }

        if (optionsHandler!!.cropMenuCropButtonTitle != null) {
            menu.findItem(R.id.crop_image_menu_crop)
                .setTitle(optionsHandler!!.cropMenuCropButtonTitle)
        }

        var cropIcon: Drawable? = null
        try {
            if (optionsHandler!!.cropMenuCropButtonIcon != 0) {
                cropIcon = ContextCompat.getDrawable(this, optionsHandler!!.cropMenuCropButtonIcon)
                menu.findItem(R.id.crop_image_menu_crop).setIcon(cropIcon)
            }
        } catch (e: Exception) {
            Log.w("AIC", "Failed to read menu crop drawable", e)
        }

        if (optionsHandler!!.activityMenuIconColor != 0) {
            updateMenuItemIconColor(
                menu, R.id.crop_image_menu_rotate_left, optionsHandler!!.activityMenuIconColor
            )
            updateMenuItemIconColor(
                menu, R.id.crop_image_menu_rotate_right, optionsHandler!!.activityMenuIconColor
            )
            updateMenuItemIconColor(
                menu,
                R.id.crop_image_menu_flip,
                optionsHandler!!.activityMenuIconColor
            )
            if (cropIcon != null) {
                updateMenuItemIconColor(
                    menu,
                    R.id.crop_image_menu_crop,
                    optionsHandler!!.activityMenuIconColor
                )
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.crop_image_menu_crop) {
            cropImage()
            return true
        }
        if (item.itemId == R.id.crop_image_menu_rotate_left) {
            rotateImage(-optionsHandler!!.rotationDegrees)
            return true
        }
        if (item.itemId == R.id.crop_image_menu_rotate_right) {
            rotateImage(optionsHandler!!.rotationDegrees)
            return true
        }
        if (item.itemId == R.id.crop_image_menu_flip_horizontally) {
            ivCropImage!!.flipImageHorizontally()
            return true
        }
        if (item.itemId == R.id.crop_image_menu_flip_vertically) {
            ivCropImage!!.flipImageVertically()
            return true
        }
        if (item.itemId == android.R.id.home) {
            setResultCancel()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleBackPress() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                setResultCancel()
            }
        } else {
            onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        setResultCancel()
                    }
                })
        }
    }

    override fun onSetImageUriComplete(view: ImageCropUI?, uri: Uri?, error: Exception?) {
        if (error == null) {
            if (optionsHandler!!.initialCropWindowRectangle != null) {
                ivCropImage!!.cropRect = optionsHandler!!.initialCropWindowRectangle
            }
            if (optionsHandler!!.initialRotation > -1) {
                ivCropImage!!.rotatedDegrees = optionsHandler!!.initialRotation
            }
        } else {
            setResult(null, error, 1)
        }
    }

    override fun onCropImageComplete(view: ImageCropUI?, result: CropResult?) {
        setResult(result!!.uri, result.error, result.sampleSize)
    }

    // region: Private methods
    private fun cropImage() {
        if (optionsHandler!!.noOutputImage) {
            setResult(null, null, 1)
        } else {
            val outputUri = outputUri
            ivCropImage!!.saveCroppedImageAsync(
                outputUri,
                optionsHandler!!.outputCompressFormat,
                optionsHandler!!.outputCompressQuality,
                optionsHandler!!.outputRequestWidth,
                optionsHandler!!.outputRequestHeight,
                optionsHandler!!.outputRequestSizeOptions
            )
        }
    }

    private fun rotateImage(degrees: Int) {
        ivCropImage!!.rotateImage(degrees)
    }

    private val outputUri: Uri?
        get() {
            var outputUri = optionsHandler!!.outputUri
            if (outputUri == null || outputUri == Uri.EMPTY) {
                try {
                    val ext =
                        when (optionsHandler!!.outputCompressFormat) {
                            Bitmap.CompressFormat.JPEG -> ".jpg"
                            Bitmap.CompressFormat.PNG -> ".png"
                            else -> ".webp"
                        }
                    outputUri = Uri.fromFile(File.createTempFile("cropped", ext, cacheDir))
                } catch (e: IOException) {
                    throw RuntimeException("Failed to create temp file for output image", e)
                }
            }
            return outputUri
        }

    private fun setResult(uri: Uri?, error: Exception?, sampleSize: Int) {
        val resultCode =
            if (error == null) RESULT_OK else CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE
        setResult(resultCode, getResultIntent(uri, error, sampleSize))
        finish()
    }

    private fun setResultCancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun getResultIntent(uri: Uri?, error: Exception?, sampleSize: Int): Intent {
        val result =
            CropImage.ActivityResult(
                ivCropImage!!.imageUri,
                uri,
                error,
                ivCropImage!!.cropPoints,
                ivCropImage!!.cropRect,
                ivCropImage!!.rotatedDegrees,
                ivCropImage!!.wholeImageRect,
                sampleSize
            )
        val intent = Intent()
        intent.putExtras(getIntent())
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_RESULT, result)
        return intent
    }

    private fun updateMenuItemIconColor(menu: Menu, itemId: Int, color: Int) {
        val menuItem = menu.findItem(itemId)
        if (menuItem != null) {
            val menuItemIcon = menuItem.icon
            if (menuItemIcon != null) {
                try {
                    menuItemIcon.mutate()
                    menuItemIcon.colorFilter =
                        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            color,
                            BlendModeCompat.SRC_ATOP
                        )
                    menuItem.setIcon(menuItemIcon)
                } catch (e: Exception) {
                    Log.w("AIC", "Failed to update menu item color", e)
                }
            }
        }
    }
}
