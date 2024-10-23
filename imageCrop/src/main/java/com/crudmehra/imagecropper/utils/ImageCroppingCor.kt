package com.crudmehra.imagecropper.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import com.crudmehra.imagecropper.utils.ImageCropperUtils.cropBitmap
import com.crudmehra.imagecropper.utils.ImageCropperUtils.cropBitmapObjectHandleOOM
import com.crudmehra.imagecropper.utils.ImageCropperUtils.resizeBitmap
import com.crudmehra.imagecropper.utils.ImageCropperUtils.writeBitmapToUri
import com.crudmehra.imagecropper.utils.ImageCropUI.RequestSizeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class ImageCroppingCor(
    private val imageCropUI: ImageCropUI,
    private val bitmap: Bitmap?,
    private val uri: Uri?,
    private val cropPoints: FloatArray,
    private val degreesRotated: Int,
    private val orgWidth: Int,
    private val orgHeight: Int,
    private val fixAspectRatio: Boolean,
    private val aspectRatioX: Int,
    private val aspectRatioY: Int,
    private val reqWidth: Int,
    private val reqHeight: Int,
    private val flipHorizontally: Boolean,
    private val flipVertically: Boolean,
    private val reqSizeOptions: RequestSizeOptions,
    private val saveUri: Uri?,
    private val saveCompressFormat: CompressFormat,
    private val saveCompressQuality: Int
) {
    private val imageCropUIReference: WeakReference<ImageCropUI> = WeakReference(imageCropUI)
    private val context: Context = imageCropUI.context

    class Result(
        val bitmap: Bitmap? = null,
        val uri: Uri? = null,
        val error: Exception? = null,
        val sampleSize: Int = 1
    )

    suspend fun execute(): Result {
        return try {
            // Process in IO thread
            withContext(Dispatchers.IO) {
                val bitmapSampled = if (uri != null) {
                    cropBitmap(
                        context, uri, cropPoints, degreesRotated, orgWidth, orgHeight,
                        fixAspectRatio, aspectRatioX, aspectRatioY, reqWidth, reqHeight,
                        flipHorizontally, flipVertically
                    )
                } else if (bitmap != null) {
                    cropBitmapObjectHandleOOM(
                        bitmap, cropPoints, degreesRotated, fixAspectRatio, aspectRatioX,
                        aspectRatioY, flipHorizontally, flipVertically
                    )
                } else {
                    return@withContext Result(null, null, null,  1)
                }

                val resizedBitmap = resizeBitmap(bitmapSampled.bitmap!!, reqWidth, reqHeight, reqSizeOptions)

                if (saveUri == null) {
                    Result(resizedBitmap, null, null,  bitmapSampled.sampleSize)
                } else {
                    writeBitmapToUri(
                        context, resizedBitmap, saveUri, saveCompressFormat, saveCompressQuality
                    )
                    resizedBitmap.recycle()
                    Result(null, saveUri, null,  bitmapSampled.sampleSize)
                }
            }
        } catch (e: Exception) {
            Result(null, null, e,  1)
        }
    }

    // Function to handle result
    fun handleResult(result: Result) {
        imageCropUIReference.get()?.onImageCroppingAsyncComplete(result)
        result.bitmap?.recycle() // Release the unused bitmap
    }
}
