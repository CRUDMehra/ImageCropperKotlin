package com.crudmehra.imagecropper.utils

import android.content.res.Resources
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.TypedValue
import com.crudmehra.imagecropper.utils.ImageCropUI.CropShape
import com.crudmehra.imagecropper.utils.ImageCropUI.Guidelines
import com.crudmehra.imagecropper.utils.ImageCropUI.RequestSizeOptions

open class ImageCropParams : Parcelable {
    @JvmField
    var cropShape: CropShape


    @JvmField
    var snapRadius: Float


    @JvmField
    var touchRadius: Float

    @JvmField
    var guidelines: Guidelines

    @JvmField
    var scaleType: ImageCropUI.ScaleType


    @JvmField
    var showCropOverlay: Boolean

    @JvmField
    var showProgressBar: Boolean


    @JvmField
    var autoZoomEnabled: Boolean

    @JvmField
    var multiTouchEnabled: Boolean

    @JvmField
    var maxZoom: Int


    @JvmField
    var initialCropWindowPaddingRatio: Float

    @JvmField
    var fixAspectRatio: Boolean

    @JvmField
    var aspectRatioX: Int

    @JvmField
    var aspectRatioY: Int

    @JvmField
    var borderLineThickness: Float

    @JvmField
    var borderLineColor: Int

    @JvmField
    var borderCornerThickness: Float

    @JvmField
    var borderCornerOffset: Float

    @JvmField
    var borderCornerLength: Float

    @JvmField
    var borderCornerColor: Int

    @JvmField
    var guidelinesThickness: Float

    @JvmField
    var guidelinesColor: Int

    @JvmField
    var backgroundColor: Int

    @JvmField
    var minCropWindowWidth: Int

    @JvmField
    var minCropWindowHeight: Int

    @JvmField
    var minCropResultWidth: Int


    @JvmField
    var minCropResultHeight: Int


    @JvmField
    var maxCropResultWidth: Int


    @JvmField
    var maxCropResultHeight: Int

    var activityTitle: CharSequence

    var activityMenuIconColor: Int

    var outputUri: Uri?

    var outputCompressFormat: CompressFormat

    var outputCompressQuality: Int

    var outputRequestWidth: Int

    var outputRequestHeight: Int

    var outputRequestSizeOptions: RequestSizeOptions

    var noOutputImage: Boolean

    var initialCropWindowRectangle: Rect?

    var initialRotation: Int

    var allowRotation: Boolean

    var allowFlipping: Boolean

    var allowCounterRotation: Boolean

    var rotationDegrees: Int

    @JvmField
    var flipHorizontally: Boolean

    @JvmField
    var flipVertically: Boolean

    var cropMenuCropButtonTitle: CharSequence?

    var cropMenuCropButtonIcon: Int


    @JvmField
    var pickerType: PickerType? = null

    constructor() {
        val dm = Resources.getSystem().displayMetrics

        cropShape = CropShape.RECTANGLE
        snapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        touchRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, dm)
        guidelines = Guidelines.ON_TOUCH
        scaleType = ImageCropUI.ScaleType.FIT_CENTER
        showCropOverlay = true
        showProgressBar = true
        autoZoomEnabled = true
        multiTouchEnabled = false
        maxZoom = 4
        initialCropWindowPaddingRatio = 0.1f

        fixAspectRatio = false
        aspectRatioX = 1
        aspectRatioY = 1

        borderLineThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        borderLineColor = Color.argb(170, 255, 255, 255)
        borderCornerThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm)
        borderCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, dm)
        borderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, dm)
        borderCornerColor = Color.WHITE

        guidelinesThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm)
        guidelinesColor = Color.argb(170, 255, 255, 255)
        backgroundColor = Color.argb(119, 0, 0, 0)

        minCropWindowWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm).toInt()
        minCropWindowHeight =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm).toInt()
        minCropResultWidth = 40
        minCropResultHeight = 40
        maxCropResultWidth = 99999
        maxCropResultHeight = 99999

        activityTitle = ""
        activityMenuIconColor = 0

        outputUri = Uri.EMPTY
        outputCompressFormat = CompressFormat.JPEG
        outputCompressQuality = 100
        outputRequestWidth = 0
        outputRequestHeight = 0
        outputRequestSizeOptions = RequestSizeOptions.NONE
        noOutputImage = false

        initialCropWindowRectangle = null
        initialRotation = -1
        allowRotation = true
        allowFlipping = true
        allowCounterRotation = false
        rotationDegrees = 90
        flipHorizontally = false
        flipVertically = false
        cropMenuCropButtonTitle = null

        cropMenuCropButtonIcon = 0
        pickerType = PickerType.CAMERA_AND_GALLERY
    }

    protected constructor(`in`: Parcel) {
        cropShape = CropShape.entries.toTypedArray()[`in`.readInt()]
        snapRadius = `in`.readFloat()
        touchRadius = `in`.readFloat()
        guidelines = Guidelines.entries.toTypedArray()[`in`.readInt()]
        scaleType =
            ImageCropUI.ScaleType.entries.toTypedArray()[`in`.readInt()]
        showCropOverlay = `in`.readByte().toInt() != 0
        showProgressBar = `in`.readByte().toInt() != 0
        autoZoomEnabled = `in`.readByte().toInt() != 0
        multiTouchEnabled = `in`.readByte().toInt() != 0
        maxZoom = `in`.readInt()
        initialCropWindowPaddingRatio = `in`.readFloat()
        fixAspectRatio = `in`.readByte().toInt() != 0
        aspectRatioX = `in`.readInt()
        aspectRatioY = `in`.readInt()
        borderLineThickness = `in`.readFloat()
        borderLineColor = `in`.readInt()
        borderCornerThickness = `in`.readFloat()
        borderCornerOffset = `in`.readFloat()
        borderCornerLength = `in`.readFloat()
        borderCornerColor = `in`.readInt()
        guidelinesThickness = `in`.readFloat()
        guidelinesColor = `in`.readInt()
        backgroundColor = `in`.readInt()
        minCropWindowWidth = `in`.readInt()
        minCropWindowHeight = `in`.readInt()
        minCropResultWidth = `in`.readInt()
        minCropResultHeight = `in`.readInt()
        maxCropResultWidth = `in`.readInt()
        maxCropResultHeight = `in`.readInt()
        activityTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`)
        activityMenuIconColor = `in`.readInt()
        outputUri = `in`.readParcelable(Uri::class.java.classLoader)
        outputCompressFormat = CompressFormat.valueOf(`in`.readString()!!)
        outputCompressQuality = `in`.readInt()
        outputRequestWidth = `in`.readInt()
        outputRequestHeight = `in`.readInt()
        outputRequestSizeOptions =
            RequestSizeOptions.entries.toTypedArray()[`in`.readInt()]
        noOutputImage = `in`.readByte().toInt() != 0
        initialCropWindowRectangle = `in`.readParcelable(Rect::class.java.classLoader)
        initialRotation = `in`.readInt()
        allowRotation = `in`.readByte().toInt() != 0
        allowFlipping = `in`.readByte().toInt() != 0
        allowCounterRotation = `in`.readByte().toInt() != 0
        rotationDegrees = `in`.readInt()
        flipHorizontally = `in`.readByte().toInt() != 0
        flipVertically = `in`.readByte().toInt() != 0
        cropMenuCropButtonTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`)
        cropMenuCropButtonIcon = `in`.readInt()
        pickerType = PickerType.entries.toTypedArray()[`in`.readInt()]
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(cropShape.ordinal)
        dest.writeFloat(snapRadius)
        dest.writeFloat(touchRadius)
        dest.writeInt(guidelines.ordinal)
        dest.writeInt(scaleType.ordinal)
        dest.writeByte((if (showCropOverlay) 1 else 0).toByte())
        dest.writeByte((if (showProgressBar) 1 else 0).toByte())
        dest.writeByte((if (autoZoomEnabled) 1 else 0).toByte())
        dest.writeByte((if (multiTouchEnabled) 1 else 0).toByte())
        dest.writeInt(maxZoom)
        dest.writeFloat(initialCropWindowPaddingRatio)
        dest.writeByte((if (fixAspectRatio) 1 else 0).toByte())
        dest.writeInt(aspectRatioX)
        dest.writeInt(aspectRatioY)
        dest.writeFloat(borderLineThickness)
        dest.writeInt(borderLineColor)
        dest.writeFloat(borderCornerThickness)
        dest.writeFloat(borderCornerOffset)
        dest.writeFloat(borderCornerLength)
        dest.writeInt(borderCornerColor)
        dest.writeFloat(guidelinesThickness)
        dest.writeInt(guidelinesColor)
        dest.writeInt(backgroundColor)
        dest.writeInt(minCropWindowWidth)
        dest.writeInt(minCropWindowHeight)
        dest.writeInt(minCropResultWidth)
        dest.writeInt(minCropResultHeight)
        dest.writeInt(maxCropResultWidth)
        dest.writeInt(maxCropResultHeight)
        TextUtils.writeToParcel(activityTitle, dest, flags)
        dest.writeInt(activityMenuIconColor)
        dest.writeParcelable(outputUri, flags)
        dest.writeString(outputCompressFormat.name)
        dest.writeInt(outputCompressQuality)
        dest.writeInt(outputRequestWidth)
        dest.writeInt(outputRequestHeight)
        dest.writeInt(outputRequestSizeOptions.ordinal)
        dest.writeInt(if (noOutputImage) 1 else 0)
        dest.writeParcelable(initialCropWindowRectangle, flags)
        dest.writeInt(initialRotation)
        dest.writeByte((if (allowRotation) 1 else 0).toByte())
        dest.writeByte((if (allowFlipping) 1 else 0).toByte())
        dest.writeByte((if (allowCounterRotation) 1 else 0).toByte())
        dest.writeInt(rotationDegrees)
        dest.writeByte((if (flipHorizontally) 1 else 0).toByte())
        dest.writeByte((if (flipVertically) 1 else 0).toByte())
        TextUtils.writeToParcel(cropMenuCropButtonTitle, dest, flags)
        dest.writeInt(cropMenuCropButtonIcon)
        dest.writeInt(pickerType?.ordinal!!)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun validate() {
        require(maxZoom >= 0) { "Cannot set max zoom to a number < 1" }
        require(!(touchRadius < 0)) { "Cannot set touch radius value to a number <= 0 " }
        require(!(initialCropWindowPaddingRatio < 0 || initialCropWindowPaddingRatio >= 0.5)) { "Cannot set initial crop window padding value to a number < 0 or >= 0.5" }
        require(aspectRatioX > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        require(aspectRatioY > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        require(!(borderLineThickness < 0)) { "Cannot set line thickness value to a number less than 0." }
        require(!(borderCornerThickness < 0)) { "Cannot set corner thickness value to a number less than 0." }
        require(!(guidelinesThickness < 0)) { "Cannot set guidelines thickness value to a number less than 0." }
        require(minCropWindowHeight >= 0) { "Cannot set min crop window height value to a number < 0 " }
        require(minCropResultWidth >= 0) { "Cannot set min crop result width value to a number < 0 " }
        require(minCropResultHeight >= 0) { "Cannot set min crop result height value to a number < 0 " }
        require(maxCropResultWidth >= minCropResultWidth) { "Cannot set max crop result width to smaller value than min crop result width" }
        require(maxCropResultHeight >= minCropResultHeight) { "Cannot set max crop result height to smaller value than min crop result height" }
        require(outputRequestWidth >= 0) { "Cannot set request width value to a number < 0 " }
        require(outputRequestHeight >= 0) { "Cannot set request height value to a number < 0 " }
        require(!(rotationDegrees < 0 || rotationDegrees > 360)) { "Cannot set rotation degrees value to a number < 0 or > 360" }
        require(pickerType!=null) {"Can't used without set picker type"}
    }


    companion object CREATOR : Parcelable.Creator<ImageCropParams> {
        override fun createFromParcel(parcel: Parcel): ImageCropParams {
            return ImageCropParams(parcel)
        }

        override fun newArray(size: Int): Array<ImageCropParams?> {
            return arrayOfNulls(size)
        }
    }

    enum class PickerType {
        CAMERA_AND_GALLERY,
        GALLERY_ONLY,
        CAMERA_ONLY;
    }
}
