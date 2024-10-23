package com.crudmehra.imagecropper.utils

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.crudmehra.imagecropper.ImageCropActivity
import com.crudmehra.imagecropper.utils.ImageCropUI.CropResult
import com.crudmehra.imagecropper.utils.ImageCropUI.CropShape
import com.crudmehra.imagecropper.utils.ImageCropUI.Guidelines
import com.crudmehra.imagecropper.utils.ImageCropUI.RequestSizeOptions

@Suppress("unused")
object CropImage {
    const val CROP_IMAGE_EXTRA_SOURCE: String = "CROP_IMAGE_EXTRA_SOURCE"

    const val CROP_IMAGE_EXTRA_OPTIONS: String = "CROP_IMAGE_EXTRA_OPTIONS"

    const val CROP_IMAGE_EXTRA_BUNDLE: String = "CROP_IMAGE_EXTRA_BUNDLE"

    const val CROP_IMAGE_EXTRA_RESULT: String = "CROP_IMAGE_EXTRA_RESULT"

    const val CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE: Int = 204

    fun toOvalBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)

        val color = -0xbdbdbe
        val paint = Paint()

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color

        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawOval(rect, paint)
        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        bitmap.recycle()

        return output
    }

    fun isExplicitCameraPermissionRequired(context: Context): Boolean {
        return (context.checkSelfPermission(Manifest.permission.CAMERA)
                != PERMISSION_GRANTED)
    }

    fun isReadExternalStoragePermissionsRequired(
        context: Context, uri: Uri
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                context,
                READ_MEDIA_VISUAL_USER_SELECTED
            ) != PERMISSION_GRANTED && isUriRequiresPermissions(context, uri)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                READ_MEDIA_IMAGES
            ) != PERMISSION_GRANTED && isUriRequiresPermissions(context, uri)
        } else {
            ContextCompat.checkSelfPermission(
                context,
                READ_EXTERNAL_STORAGE
            ) != PERMISSION_GRANTED && isUriRequiresPermissions(context, uri)
        }
    }

    private fun isUriRequiresPermissions(context: Context, uri: Uri): Boolean {
        try {
            val resolver = context.contentResolver
            val stream = resolver.openInputStream(uri)
            stream?.close()
            return false
        } catch (e: Exception) {
            return true
        }
    }


    fun activity(): ActivityBuilder {
        return ActivityBuilder(null)
    }


    fun activity(uri: Uri?): ActivityBuilder {
        return ActivityBuilder(uri)
    }


    fun getActivityResult(data: Intent?): ActivityResult? {
        return if (data != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra(CROP_IMAGE_EXTRA_RESULT, ActivityResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getParcelableExtra(CROP_IMAGE_EXTRA_RESULT)
            }
        } else null
    }

    // region: Inner class: ActivityBuilder
    class ActivityBuilder(
        private val mSource: Uri?
    ) {
        private val mOptions = ImageCropParams()

        fun getIntent(context: Context): Intent {
            return getIntent(context, ImageCropActivity::class.java)
        }

        private fun getIntent(context: Context, cls: Class<*>?): Intent {
            mOptions.validate()
            val intent = Intent()
            intent.setClass(context, cls!!)
            val bundle = Bundle()
            bundle.putParcelable(CROP_IMAGE_EXTRA_SOURCE, mSource)
            bundle.putParcelable(CROP_IMAGE_EXTRA_OPTIONS, mOptions)
            intent.putExtra(CROP_IMAGE_EXTRA_BUNDLE, bundle)
            return intent
        }

        fun setPickerType(pickerType: ImageCropParams.PickerType): ActivityBuilder {
            mOptions.pickerType = pickerType
            return this
        }

        fun setCropShape(cropShape: CropShape): ActivityBuilder {
            mOptions.cropShape = cropShape
            return this
        }


        fun setSnapRadius(snapRadius: Float): ActivityBuilder {
            mOptions.snapRadius = snapRadius
            return this
        }


        fun setTouchRadius(touchRadius: Float): ActivityBuilder {
            mOptions.touchRadius = touchRadius
            return this
        }


        fun setGuidelines(guidelines: Guidelines): ActivityBuilder {
            mOptions.guidelines = guidelines
            return this
        }


        fun setScaleType(scaleType: ImageCropUI.ScaleType): ActivityBuilder {
            mOptions.scaleType = scaleType
            return this
        }


        fun setShowCropOverlay(showCropOverlay: Boolean): ActivityBuilder {
            mOptions.showCropOverlay = showCropOverlay
            return this
        }


        fun setAutoZoomEnabled(autoZoomEnabled: Boolean): ActivityBuilder {
            mOptions.autoZoomEnabled = autoZoomEnabled
            return this
        }


        fun setMultiTouchEnabled(multiTouchEnabled: Boolean): ActivityBuilder {
            mOptions.multiTouchEnabled = multiTouchEnabled
            return this
        }


        fun setMaxZoom(maxZoom: Int): ActivityBuilder {
            mOptions.maxZoom = maxZoom
            return this
        }

        fun setInitialCropWindowPaddingRatio(initialCropWindowPaddingRatio: Float): ActivityBuilder {
            mOptions.initialCropWindowPaddingRatio = initialCropWindowPaddingRatio
            return this
        }


        fun setFixAspectRatio(fixAspectRatio: Boolean): ActivityBuilder {
            mOptions.fixAspectRatio = fixAspectRatio
            return this
        }


        fun setAspectRatio(aspectRatioX: Int, aspectRatioY: Int): ActivityBuilder {
            mOptions.aspectRatioX = aspectRatioX
            mOptions.aspectRatioY = aspectRatioY
            mOptions.fixAspectRatio = true
            return this
        }


        fun setBorderLineThickness(borderLineThickness: Float): ActivityBuilder {
            mOptions.borderLineThickness = borderLineThickness
            return this
        }


        fun setBorderLineColor(borderLineColor: Int): ActivityBuilder {
            mOptions.borderLineColor = borderLineColor
            return this
        }


        fun setBorderCornerThickness(borderCornerThickness: Float): ActivityBuilder {
            mOptions.borderCornerThickness = borderCornerThickness
            return this
        }


        fun setBorderCornerOffset(borderCornerOffset: Float): ActivityBuilder {
            mOptions.borderCornerOffset = borderCornerOffset
            return this
        }


        fun setBorderCornerLength(borderCornerLength: Float): ActivityBuilder {
            mOptions.borderCornerLength = borderCornerLength
            return this
        }


        fun setBorderCornerColor(borderCornerColor: Int): ActivityBuilder {
            mOptions.borderCornerColor = borderCornerColor
            return this
        }


        fun setGuidelinesThickness(guidelinesThickness: Float): ActivityBuilder {
            mOptions.guidelinesThickness = guidelinesThickness
            return this
        }


        fun setGuidelinesColor(guidelinesColor: Int): ActivityBuilder {
            mOptions.guidelinesColor = guidelinesColor
            return this
        }


        fun setBackgroundColor(backgroundColor: Int): ActivityBuilder {
            mOptions.backgroundColor = backgroundColor
            return this
        }


        fun setMinCropWindowSize(
            minCropWindowWidth: Int,
            minCropWindowHeight: Int
        ): ActivityBuilder {
            mOptions.minCropWindowWidth = minCropWindowWidth
            mOptions.minCropWindowHeight = minCropWindowHeight
            return this
        }


        fun setMinCropResultSize(
            minCropResultWidth: Int,
            minCropResultHeight: Int
        ): ActivityBuilder {
            mOptions.minCropResultWidth = minCropResultWidth
            mOptions.minCropResultHeight = minCropResultHeight
            return this
        }


        fun setMaxCropResultSize(
            maxCropResultWidth: Int,
            maxCropResultHeight: Int
        ): ActivityBuilder {
            mOptions.maxCropResultWidth = maxCropResultWidth
            mOptions.maxCropResultHeight = maxCropResultHeight
            return this
        }


        fun setActivityTitle(activityTitle: CharSequence?): ActivityBuilder {
            mOptions.activityTitle = activityTitle!!
            return this
        }

        fun setActivityMenuIconColor(activityMenuIconColor: Int): ActivityBuilder {
            mOptions.activityMenuIconColor = activityMenuIconColor
            return this
        }


        fun setOutputUri(outputUri: Uri?): ActivityBuilder {
            mOptions.outputUri = outputUri
            return this
        }


        fun setOutputCompressFormat(outputCompressFormat: CompressFormat?): ActivityBuilder {
            mOptions.outputCompressFormat = outputCompressFormat!!
            return this
        }


        fun setOutputCompressQuality(outputCompressQuality: Int): ActivityBuilder {
            mOptions.outputCompressQuality = outputCompressQuality
            return this
        }


        fun setRequestedSize(reqWidth: Int, reqHeight: Int): ActivityBuilder {
            return setRequestedSize(reqWidth, reqHeight, RequestSizeOptions.RESIZE_INSIDE)
        }


        private fun setRequestedSize(
            reqWidth: Int,
            reqHeight: Int,
            @Suppress("SameParameterValue") options: RequestSizeOptions
        ): ActivityBuilder {
            mOptions.outputRequestWidth = reqWidth
            mOptions.outputRequestHeight = reqHeight
            mOptions.outputRequestSizeOptions = options
            return this
        }


        fun setNoOutputImage(noOutputImage: Boolean): ActivityBuilder {
            mOptions.noOutputImage = noOutputImage
            return this
        }


        fun setInitialCropWindowRectangle(initialCropWindowRectangle: Rect?): ActivityBuilder {
            mOptions.initialCropWindowRectangle = initialCropWindowRectangle
            return this
        }

        fun setInitialRotation(initialRotation: Int): ActivityBuilder {
            mOptions.initialRotation = (initialRotation + 360) % 360
            return this
        }

        fun setAllowRotation(allowRotation: Boolean): ActivityBuilder {
            mOptions.allowRotation = allowRotation
            return this
        }


        fun setAllowFlipping(allowFlipping: Boolean): ActivityBuilder {
            mOptions.allowFlipping = allowFlipping
            return this
        }

        fun setAllowCounterRotation(allowCounterRotation: Boolean): ActivityBuilder {
            mOptions.allowCounterRotation = allowCounterRotation
            return this
        }

        fun setRotationDegrees(rotationDegrees: Int): ActivityBuilder {
            mOptions.rotationDegrees = (rotationDegrees + 360) % 360
            return this
        }

        fun setFlipHorizontally(flipHorizontally: Boolean): ActivityBuilder {
            mOptions.flipHorizontally = flipHorizontally
            return this
        }

        fun setFlipVertically(flipVertically: Boolean): ActivityBuilder {
            mOptions.flipVertically = flipVertically
            return this
        }

        fun setCropMenuCropButtonTitle(title: CharSequence?): ActivityBuilder {
            mOptions.cropMenuCropButtonTitle = title
            return this
        }

        fun setCropMenuCropButtonIcon(@DrawableRes drawableResource: Int): ActivityBuilder {
            mOptions.cropMenuCropButtonIcon = drawableResource
            return this
        }
    }

    // endregion
    // region: Inner class: ActivityResult
    open class ActivityResult : CropResult, Parcelable {
        constructor(
            originalUri: Uri?,
            uri: Uri?,
            error: Exception?,
            cropPoints: FloatArray?,
            cropRect: Rect?,
            rotation: Int,
            wholeImageRect: Rect?,
            sampleSize: Int
        ) : super(
            null,
            originalUri,
            null,
            uri,
            error,
            cropPoints!!,
            cropRect,
            wholeImageRect,
            rotation,
            sampleSize
        )

        @Suppress("DEPRECATION")
        protected constructor(`in`: Parcel) : super(
            null,
            `in`.readParcelable<Parcelable>(Uri::class.java.classLoader) as Uri?,
            null,
            `in`.readParcelable<Parcelable>(Uri::class.java.classLoader) as Uri?,
            `in`.readSerializable() as Exception?,
            `in`.createFloatArray()!!,
            `in`.readParcelable<Parcelable>(Rect::class.java.classLoader) as Rect?,
            `in`.readParcelable<Parcelable>(Rect::class.java.classLoader) as Rect?,
            `in`.readInt(),
            `in`.readInt()
        )

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(originalUri, flags)
            dest.writeParcelable(uri, flags)
            dest.writeSerializable(error)
            dest.writeFloatArray(cropPoints)
            dest.writeParcelable(cropRect, flags)
            dest.writeParcelable(wholeImageRect, flags)
            dest.writeInt(rotation)
            dest.writeInt(sampleSize)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<ActivityResult> {
            override fun createFromParcel(parcel: Parcel): ActivityResult {
                return ActivityResult(parcel)
            }

            override fun newArray(size: Int): Array<ActivityResult?> {
                return arrayOfNulls(size)
            }
        }


    }
}
