package com.crudmehra.imagecropper.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import android.util.Pair
import androidx.exifinterface.media.ExifInterface
import com.crudmehra.imagecropper.utils.ImageCropUI.RequestSizeOptions
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal object ImageCropperUtils {
    @JvmField
    val EMPTY_RECT: Rect = Rect()

    @JvmField
    val EMPTY_RECT_F: RectF = RectF()

    @JvmField
    val RECT: RectF = RectF()

    @JvmField
    val POINTS: FloatArray = FloatArray(6)

    @JvmField
    val POINTS2: FloatArray = FloatArray(6)

    private var mMaxTextureSize = 0

    @JvmField
    var stateBitmap: Pair<String, WeakReference<Bitmap>>? = null

    @Suppress("unused")
    @JvmStatic
    fun rotateBitmapByExif(bitmap: Bitmap?, context: Context, uri: Uri?): RotateBitmapResult {
        var ei: ExifInterface? = null
        try {
            val `is` = context.contentResolver.openInputStream(uri!!)
            if (`is` != null) {
                ei = ExifInterface(`is`)
                `is`.close()
            }
        } catch (ignored: Exception) {
        }
        return if (ei != null) rotateBitmapByExif(bitmap, ei) else RotateBitmapResult(bitmap, 0)
    }

    @JvmStatic
    fun rotateBitmapByExif(bitmap: Bitmap?, exif: ExifInterface): RotateBitmapResult {
        val degrees: Int
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        return RotateBitmapResult(bitmap, degrees)
    }

    @Suppress("unused")
    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): BitmapSampled {
        try {
            val resolver = context.contentResolver

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = decodeImageForOption(resolver, uri)

            if (options.outWidth == -1 && options.outHeight == -1) throw RuntimeException("File is not a picture")

            // Calculate inSampleSize
            options.inSampleSize = max(
                calculateInSampleSizeByReqestedSize(
                    options.outWidth, options.outHeight, reqWidth, reqHeight
                ).toDouble(),
                calculateInSampleSizeByMaxTextureSize(
                    options.outWidth,
                    options.outHeight
                ).toDouble()
            )
                .toInt()

            // Decode bitmap with inSampleSize set
            val bitmap = decodeImage(resolver, uri, options)

            return BitmapSampled(bitmap, options.inSampleSize)
        } catch (e: Exception) {
            throw RuntimeException(
                """
                    Failed to load sampled bitmap: $uri
                    ${e.message}
                    """.trimIndent(), e
            )
        }
    }


    @JvmStatic
    fun cropBitmapObjectHandleOOM(
        bitmap: Bitmap,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): BitmapSampled {
        var scale = 1
        while (true) {
            try {
                val cropBitmap =
                    cropBitmapObjectWithScale(
                        bitmap,
                        points,
                        degreesRotated,
                        fixAspectRatio,
                        aspectRatioX,
                        aspectRatioY,
                        1 / scale.toFloat(),
                        flipHorizontally,
                        flipVertically
                    )
                return BitmapSampled(cropBitmap, scale)
            } catch (e: OutOfMemoryError) {
                scale *= 2
                if (scale > 8) {
                    throw e
                }
            }
        }
    }

    private fun cropBitmapObjectWithScale(
        bitmap: Bitmap,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        scale: Float,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): Bitmap? {
        // get the rectangle in original image that contains the required cropped area (larger for non
        // rectangular crop)

        val rect =
            getRectFromPoints(
                points,
                bitmap.width,
                bitmap.height,
                fixAspectRatio,
                aspectRatioX,
                aspectRatioY
            )

        // crop and rotate the cropped image in one operation
        val matrix = Matrix()
        matrix.setRotate(
            degreesRotated.toFloat(),
            (bitmap.width / 2).toFloat(),
            (bitmap.height / 2).toFloat()
        )
        matrix.postScale(
            if (flipHorizontally) -scale else scale,
            if (flipVertically) -scale else scale
        )
        var result: Bitmap? =
            Bitmap.createBitmap(
                bitmap,
                rect.left,
                rect.top,
                rect.width(),
                rect.height(),
                matrix,
                true
            )

        if (result == bitmap) {
            // corner case when all bitmap is selected, no worth optimizing for it
            result = bitmap.copy(bitmap.config, false)
        }

        // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
        if (degreesRotated % 90 != 0) {
            // extra crop because non rectangular crop cannot be done directly on the image without
            // rotating first

            result =
                cropForRotatedImage(
                    result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY
                )
        }

        return result
    }

    @JvmStatic
    fun cropBitmap(
        context: Context,
        loadedImageUri: Uri,
        points: FloatArray,
        degreesRotated: Int,
        orgWidth: Int,
        orgHeight: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        reqWidth: Int,
        reqHeight: Int,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): BitmapSampled {
        var sampleMulti = 1
        while (true) {
            try {
                // if successful, just return the resulting bitmap
                return cropBitmap(
                    context,
                    loadedImageUri,
                    points,
                    degreesRotated,
                    orgWidth,
                    orgHeight,
                    fixAspectRatio,
                    aspectRatioX,
                    aspectRatioY,
                    reqWidth,
                    reqHeight,
                    flipHorizontally,
                    flipVertically,
                    sampleMulti
                )
            } catch (e: OutOfMemoryError) {
                // if OOM try to increase the sampling to lower the memory usage
                sampleMulti *= 2
                if (sampleMulti > 16) {
                    throw RuntimeException(
                        """
                            Failed to handle OOM by sampling ($sampleMulti): $loadedImageUri
                            ${e.message}
                            """.trimIndent(),
                        e
                    )
                }
            }
        }
    }

    @JvmStatic
    fun getRectLeft(points: FloatArray): Float {
        return min(
            min(
                min(
                    points[0].toDouble(),
                    points[2].toDouble()
                ), points[4].toDouble()
            ), points[6].toDouble()
        ).toFloat()
    }

    @JvmStatic
    fun getRectTop(points: FloatArray): Float {
        return min(
            min(
                min(
                    points[1].toDouble(),
                    points[3].toDouble()
                ), points[5].toDouble()
            ), points[7].toDouble()
        ).toFloat()
    }

    @JvmStatic
    fun getRectRight(points: FloatArray): Float {
        return max(
            max(
                max(
                    points[0].toDouble(),
                    points[2].toDouble()
                ), points[4].toDouble()
            ), points[6].toDouble()
        ).toFloat()
    }

    @JvmStatic
    fun getRectBottom(points: FloatArray): Float {
        return max(
            max(
                max(
                    points[1].toDouble(),
                    points[3].toDouble()
                ), points[5].toDouble()
            ), points[7].toDouble()
        ).toFloat()
    }

    @JvmStatic
    fun getRectWidth(points: FloatArray): Float {
        return getRectRight(points) - getRectLeft(points)
    }

    @JvmStatic
    fun getRectHeight(points: FloatArray): Float {
        return getRectBottom(points) - getRectTop(points)
    }

    @JvmStatic
    fun getRectCenterX(points: FloatArray): Float {
        return (getRectRight(points) + getRectLeft(points)) / 2f
    }

    @JvmStatic
    fun getRectCenterY(points: FloatArray): Float {
        return (getRectBottom(points) + getRectTop(points)) / 2f
    }

    @JvmStatic
    fun getRectFromPoints(
        points: FloatArray,
        imageWidth: Int,
        imageHeight: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int
    ): Rect {
        val left = Math.round(
            max(
                0.0,
                getRectLeft(points).toDouble()
            )
        ).toInt()
        val top = Math.round(
            max(
                0.0,
                getRectTop(points).toDouble()
            )
        ).toInt()
        val right = Math.round(
            min(
                imageWidth.toDouble(),
                getRectRight(points).toDouble()
            )
        ).toInt()
        val bottom = Math.round(
            min(
                imageHeight.toDouble(),
                getRectBottom(points).toDouble()
            )
        ).toInt()

        val rect = Rect(left, top, right, bottom)
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
        }

        return rect
    }

    private fun fixRectForAspectRatio(rect: Rect, aspectRatioX: Int, aspectRatioY: Int) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width()
            } else {
                rect.right -= rect.width() - rect.height()
            }
        }
    }

    @JvmStatic
    fun writeTempStateStoreBitmap(context: Context, bitmap: Bitmap, uris: Uri?): Uri? {
        var uri = uris
        try {
            var needSave = true
            if (uri == null) {
                uri =
                    Uri.fromFile(
                        File.createTempFile("aic_state_store_temp", ".jpg", context.cacheDir)
                    )
            } else if (File(uri.path.toString()).exists()) {
                needSave = false
            }
            if (needSave) {
                writeBitmapToUri(context, bitmap, uri, CompressFormat.JPEG, 100)
            }
            return uri
        } catch (e: Exception) {
            Log.w(
                "AIC",
                "Failed to write bitmap to temp file for image-cropper save instance state",
                e
            )
            return null
        }
    }

    @Throws(FileNotFoundException::class)
    fun writeBitmapToUri(
        context: Context,
        bitmap: Bitmap,
        uri: Uri?,
        compressFormat: CompressFormat?,
        compressQuality: Int
    ) {
        var outputStream: OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(uri!!)
            bitmap.compress(compressFormat!!, compressQuality, outputStream!!)
        } finally {
            closeSafe(outputStream)
        }
    }

    @JvmStatic
    fun resizeBitmap(
        bitmap: Bitmap, reqWidth: Int, reqHeight: Int, options: RequestSizeOptions
    ): Bitmap {
        try {
            if (reqWidth > 0 && reqHeight > 0 && (options == RequestSizeOptions.RESIZE_FIT || options == RequestSizeOptions.RESIZE_INSIDE || options == RequestSizeOptions.RESIZE_EXACT)) {
                var resized: Bitmap? = null
                if (options == RequestSizeOptions.RESIZE_EXACT) {
                    resized = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, false)
                } else {
                    val width = bitmap.width
                    val height = bitmap.height
                    val scale = max(
                        (width / reqWidth.toFloat()).toDouble(),
                        (height / reqHeight.toFloat()).toDouble()
                    )
                        .toFloat()
                    if (scale > 1 || options == RequestSizeOptions.RESIZE_FIT) {
                        resized =
                            Bitmap.createScaledBitmap(
                                bitmap, (width / scale).toInt(), (height / scale).toInt(), false
                            )
                    }
                }
                if (resized != null) {
                    if (resized != bitmap) {
                        bitmap.recycle()
                    }
                    return resized
                }
            }
        } catch (e: Exception) {
            Log.w("AIC", "Failed to resize cropped image, return bitmap before resize", e)
        }
        return bitmap
    }

    private fun cropBitmap(
        context: Context,
        loadedImageUri: Uri,
        points: FloatArray,
        degreesRotated: Int,
        orgWidth: Int,
        orgHeight: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        reqWidth: Int,
        reqHeight: Int,
        flipHorizontally: Boolean,
        flipVertically: Boolean,
        sampleMulti: Int
    ): BitmapSampled {
        // get the rectangle in original image that contains the required cropped area (larger for non
        // rectangular crop)

        val rect =
            getRectFromPoints(
                points,
                orgWidth,
                orgHeight,
                fixAspectRatio,
                aspectRatioX,
                aspectRatioY
            )

        val width = if (reqWidth > 0) reqWidth else rect.width()
        val height = if (reqHeight > 0) reqHeight else rect.height()

        var result: Bitmap? = null
        var sampleSize = 1
        try {
            // decode only the required image from URI, optionally sub-sampling if reqWidth/reqHeight is
            // given.
            val bitmapSampled =
                decodeSampledBitmapRegion(context, loadedImageUri, rect, width, height, sampleMulti)
            result = bitmapSampled.bitmap
            sampleSize = bitmapSampled.sampleSize
        } catch (ignored: Exception) {
        }

        if (result != null) {
            try {
                // rotate the decoded region by the required amount
                result =
                    rotateAndFlipBitmapInt(result, degreesRotated, flipHorizontally, flipVertically)

                // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
                if (degreesRotated % 90 != 0) {
                    // extra crop because non rectangular crop cannot be done directly on the image without
                    // rotating first

                    result =
                        cropForRotatedImage(
                            result,
                            points,
                            rect,
                            degreesRotated,
                            fixAspectRatio,
                            aspectRatioX,
                            aspectRatioY
                        )
                }
            } catch (e: OutOfMemoryError) {
                result.recycle()
                throw e
            }
            return BitmapSampled(result, sampleSize)
        } else {
            // failed to decode region, may be skia issue, try full decode and then crop
            return cropBitmap(
                context,
                loadedImageUri,
                points,
                degreesRotated,
                fixAspectRatio,
                aspectRatioX,
                aspectRatioY,
                sampleMulti,
                rect,
                width,
                height,
                flipHorizontally,
                flipVertically
            )
        }
    }

    private fun cropBitmap(
        context: Context,
        loadedImageUri: Uri,
        points: FloatArray,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int,
        sampleMulti: Int,
        rect: Rect,
        width: Int,
        height: Int,
        flipHorizontally: Boolean,
        flipVertically: Boolean
    ): BitmapSampled {
        var result: Bitmap? = null
        val sampleSize: Int
        try {
            val options = BitmapFactory.Options()
            sampleSize = (
                    sampleMulti
                            * calculateInSampleSizeByReqestedSize(
                        rect.width(),
                        rect.height(),
                        width,
                        height
                    ))
            options.inSampleSize = sampleSize

            val fullBitmap = decodeImage(context.contentResolver, loadedImageUri, options)
            if (fullBitmap != null) {
                try {
                    // adjust crop points by the sampling because the image is smaller
                    val points2 = FloatArray(points.size)
                    System.arraycopy(points, 0, points2, 0, points.size)
                    for (i in points2.indices) {
                        points2[i] = points2[i] / options.inSampleSize
                    }

                    result =
                        cropBitmapObjectWithScale(
                            fullBitmap,
                            points2,
                            degreesRotated,
                            fixAspectRatio,
                            aspectRatioX,
                            aspectRatioY,
                            1f,
                            flipHorizontally,
                            flipVertically
                        )
                } finally {
                    if (result != fullBitmap) {
                        fullBitmap.recycle()
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            result?.recycle()
            throw e
        } catch (e: Exception) {
            throw RuntimeException(
                """
                    Failed to load sampled bitmap: $loadedImageUri
                    ${e.message}
                    """.trimIndent(), e
            )
        }
        return BitmapSampled(result, sampleSize)
    }

    @Throws(FileNotFoundException::class)
    private fun decodeImageForOption(resolver: ContentResolver, uri: Uri): BitmapFactory.Options {
        var stream: InputStream? = null
        try {
            stream = resolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(stream, EMPTY_RECT, options)
            options.inJustDecodeBounds = false
            return options
        } finally {
            closeSafe(stream)
        }
    }

    @Throws(FileNotFoundException::class)
    private fun decodeImage(
        resolver: ContentResolver, uri: Uri, options: BitmapFactory.Options
    ): Bitmap? {
        do {
            var stream: InputStream? = null
            try {
                stream = resolver.openInputStream(uri)
                return BitmapFactory.decodeStream(stream, EMPTY_RECT, options)
            } catch (e: OutOfMemoryError) {
                options.inSampleSize *= 2
            } finally {
                closeSafe(stream)
            }
        } while (options.inSampleSize <= 512)
        throw RuntimeException("Failed to decode image: $uri")
    }


    private fun decodeSampledBitmapRegion(
        context: Context, uri: Uri, rect: Rect, reqWidth: Int, reqHeight: Int, sampleMulti: Int
    ): BitmapSampled {
        var stream: InputStream? = null
        var decoder: BitmapRegionDecoder? = null
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize = (
                    sampleMulti
                            * calculateInSampleSizeByReqestedSize(
                        rect.width(), rect.height(), reqWidth, reqHeight
                    ))

            stream = context.contentResolver.openInputStream(uri)
            decoder = BitmapRegionDecoder.newInstance(stream!!, false)
            do {
                try {
                    return BitmapSampled(
                        decoder!!.decodeRegion(rect, options),
                        options.inSampleSize
                    )
                } catch (e: OutOfMemoryError) {
                    options.inSampleSize *= 2
                }
            } while (options.inSampleSize <= 512)
        } catch (e: Exception) {
            throw RuntimeException(
                """
                    Failed to load sampled bitmap: $uri
                    ${e.message}
                    """.trimIndent(), e
            )
        } finally {
            closeSafe(stream)
            decoder?.recycle()
        }
        return BitmapSampled(null, 1)
    }


    private fun cropForRotatedImage(
        bitmaps: Bitmap?,
        points: FloatArray,
        rect: Rect,
        degreesRotated: Int,
        fixAspectRatio: Boolean,
        aspectRatioX: Int,
        aspectRatioY: Int
    ): Bitmap? {
        var bitmap = bitmaps
        if (degreesRotated % 90 != 0) {
            var adjLeft = 0
            var adjTop = 0
            var width = 0
            var height = 0
            val rads = Math.toRadians(degreesRotated.toDouble())
            val compareTo =
                if (degreesRotated < 90 || (degreesRotated > 180 && degreesRotated < 270)
                ) rect.left
                else rect.right
            var i = 0
            while (i < points.size) {
                if (points[i] >= compareTo - 1 && points[i] <= compareTo + 1) {
                    adjLeft = abs(sin(rads) * (rect.bottom - points[i + 1]))
                        .toInt()
                    adjTop = abs(cos(rads) * (points[i + 1] - rect.top))
                        .toInt()
                    width = abs((points[i + 1] - rect.top) / sin(rads))
                        .toInt()
                    height = abs((rect.bottom - points[i + 1]) / cos(rads))
                        .toInt()
                    break
                }
                i += 2
            }

            rect[adjLeft, adjTop, adjLeft + width] = adjTop + height
            if (fixAspectRatio) {
                fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
            }

            val bitmapTmp = bitmap
            bitmap = Bitmap.createBitmap(bitmap!!, rect.left, rect.top, rect.width(), rect.height())
            if (bitmapTmp != bitmap) {
                bitmapTmp!!.recycle()
            }
        }
        return bitmap
    }


    private fun calculateInSampleSizeByReqestedSize(
        width: Int, height: Int, reqWidth: Int, reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            while ((height / 2 / inSampleSize) > reqHeight && (width / 2 / inSampleSize) > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }


    private fun calculateInSampleSizeByMaxTextureSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        if (mMaxTextureSize == 0) {
            mMaxTextureSize = maxTextureSize
        }
        if (mMaxTextureSize > 0) {
            while ((height / inSampleSize) > mMaxTextureSize
                || (width / inSampleSize) > mMaxTextureSize
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }


    private fun rotateAndFlipBitmapInt(
        bitmap: Bitmap, degrees: Int, flipHorizontally: Boolean, flipVertically: Boolean
    ): Bitmap {
        if (degrees > 0 || flipHorizontally || flipVertically) {
            val matrix = Matrix()
            matrix.setRotate(degrees.toFloat())
            matrix.postScale(
                (if (flipHorizontally) -1 else 1).toFloat(),
                (if (flipVertically) -1 else 1).toFloat()
            )
            val newBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
            if (newBitmap != bitmap) {
                bitmap.recycle()
            }
            return newBitmap
        } else {
            return bitmap
        }
    }

    private val maxTextureSize: Int
        get() {
            // Safe minimum default size
            val IMAGE_MAX_BITMAP_DIMENSION = 2048

            try {
                // Get EGL Display
                val egl = EGLContext.getEGL() as EGL10
                val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

                // Initialise
                val version = IntArray(2)
                egl.eglInitialize(display, version)

                // Query total number of configurations
                val totalConfigurations = IntArray(1)
                egl.eglGetConfigs(display, null, 0, totalConfigurations)

                // Query actual list configurations
                val configurationsList = arrayOfNulls<EGLConfig>(
                    totalConfigurations[0]
                )
                egl.eglGetConfigs(
                    display,
                    configurationsList,
                    totalConfigurations[0],
                    totalConfigurations
                )

                val textureSize = IntArray(1)
                var maximumTextureSize = 0

                // Iterate through all the configurations to located the maximum texture size
                for (i in 0 until totalConfigurations[0]) {
                    // Only need to check for width since opengl textures are always squared
                    egl.eglGetConfigAttrib(
                        display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize
                    )

                    // Keep track of the maximum texture size
                    if (maximumTextureSize < textureSize[0]) {
                        maximumTextureSize = textureSize[0]
                    }
                }

                // Release
                egl.eglTerminate(display)

                // Return largest texture size found, or default
                return max(maximumTextureSize.toDouble(), IMAGE_MAX_BITMAP_DIMENSION.toDouble())
                    .toInt()
            } catch (e: Exception) {
                return IMAGE_MAX_BITMAP_DIMENSION
            }
        }

    private fun closeSafe(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (ignored: IOException) {
            }
        }
    }

    internal class BitmapSampled(
        @JvmField val bitmap: Bitmap?,
        val sampleSize: Int
    )

    internal class RotateBitmapResult(
        @JvmField val bitmap: Bitmap?,
        @JvmField val degrees: Int
    )
}
