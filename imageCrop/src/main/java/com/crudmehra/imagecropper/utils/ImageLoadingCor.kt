package com.crudmehra.imagecropper.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.crudmehra.imagecropper.utils.ImageCropperUtils.decodeSampledBitmap
import com.crudmehra.imagecropper.utils.ImageCropperUtils.rotateBitmapByExif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageLoadingCor(
    imageCropUI: ImageCropUI,
    val uri: Uri
) {

    private val cropImageViewReference = imageCropUI
    private val context: Context = imageCropUI.context

    private val width: Int
    private val height: Int

    init {
        val metrics = imageCropUI.resources.displayMetrics
        val densityAdj = if (metrics.density > 1) 1 / metrics.density else 1f
        width = (metrics.widthPixels * densityAdj).toInt()
        height = (metrics.heightPixels * densityAdj).toInt()
    }

    // Result class to hold the outcome of the task
    class Result(
        val uri: Uri,
        val bitmap: Bitmap? = null,
        val loadSampleSize: Int = 0,
        val degreesRotated: Int = 0,
        val error: Exception? = null
    )

    // Coroutine-based method to load the bitmap in the background
    suspend fun execute(): Result {
        return try {
            withContext(Dispatchers.IO) {
                // Decode the sampled bitmap
                val decodeResult = decodeSampledBitmap(context, uri, width, height)
                
                // Rotate the bitmap based on Exif data
                val rotateResult = rotateBitmapByExif(decodeResult.bitmap, context, uri)
                
                // Return the result with the bitmap and metadata
                Result(
                    uri = uri,
                    bitmap = rotateResult.bitmap,
                    loadSampleSize = decodeResult.sampleSize,
                    degreesRotated = rotateResult.degrees
                )
            }
        } catch (e: Exception) {
            // Handle any errors and return the result with the error
            Result(uri, error = e)
        }
    }

    // Method to handle the result on the main thread (UI)
    fun handleResult(result: Result) {
        val cropImageView = cropImageViewReference
        cropImageView.let {
            if (result.error == null) {
                it.onSetImageUriAsyncComplete(result)
            } else {
                Log.e("BitmapLoadingWorkerTask", "handleResult: ${result.error.message}" )
                // Handle the error appropriately (optional)
            }
        }
    }
}
