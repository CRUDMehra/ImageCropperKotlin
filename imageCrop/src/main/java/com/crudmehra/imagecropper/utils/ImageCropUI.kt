package com.crudmehra.imagecropper.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.exifinterface.media.ExifInterface
import com.crudmehra.imagecropper.R
import com.crudmehra.imagecropper.utils.ImageCropperUtils.cropBitmap
import com.crudmehra.imagecropper.utils.ImageCropperUtils.cropBitmapObjectHandleOOM
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectBottom
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectCenterX
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectCenterY
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectFromPoints
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectHeight
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectLeft
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectRight
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectTop
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectWidth
import com.crudmehra.imagecropper.utils.ImageCropperUtils.resizeBitmap
import com.crudmehra.imagecropper.utils.ImageCropperUtils.rotateBitmapByExif
import com.crudmehra.imagecropper.utils.ImageCropperUtils.writeTempStateStoreBitmap
import com.crudmehra.imagecropper.utils.CropOverlayUI.CropWindowChangeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class ImageCropUI(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {
    // region: Fields and Const
    private val imageView: ImageView

    private val cropOverlayUI: CropOverlayUI?

    private val imageMatrix = Matrix()

    private val imageInverseMatrix = Matrix()

    private val myProgressBar: ProgressBar

    private val imagePoints = FloatArray(8)

    private val scaleImagePoints = FloatArray(8)

    private var animationView: AnimImageCrop? = null

    private var bitmapImage: Bitmap? = null

    private var initialDegreesRotated = 0

    private var degreesRotated = 0

    private var flipHorizontally: Boolean

    private var flipVertically: Boolean

    private var layoutWidth = 0

    private var layoutHeight = 0

    private var imageViewResource = 0

    private var scaleViewType: ScaleType


    private var isSaveBitmapToInstanceState: Boolean = false


    private var mShowCropOverlay = true


    private var mShowProgressBar = true

    private var mAutoZoomEnabled = true

    private var mMaxZoom: Int

    private var mOnCropOverlayReleasedListener: OnSetCropOverlayReleasedListener? = null

    private var mOnSetCropOverlayMovedListener: OnSetCropOverlayMovedListener? = null

    private var mOnSetCropWindowChangeListener: OnSetCropWindowChangeListener? = null

    private var mOnSetImageUriCompleteListener: OnSetImageUriCompleteListener? = null

    private var mOnCropImageCompleteListener: OnCropImageCompleteListener? = null

    var imageUri: Uri? = null
        private set

    private var mLoadedSampleSize = 1

    private var mZoom = 1f

    private var mZoomOffsetX = 0f

    private var mZoomOffsetY = 0f

    private var mRestoreCropWindowRect: RectF? = null

    private var mRestoreDegreesRotated = 0

    private var mSizeChanged = false


    private var mSaveInstanceStateBitmapUri: Uri? = null

    private var mImageLoadingCor: ImageLoadingCor? = null

    private var mImageCroppingCor: WeakReference<ImageCroppingCor>? = null

    // endregion
    init {
        var options: ImageCropParams? = null
        val intent = if (context is Activity) context.intent else null
        if (intent != null) {
            val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
            if (bundle != null) {
                options = bundle.getParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS)
            }
        }

        if (options == null) {
            options = ImageCropParams()

            if (attrs != null) {
                val ta = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0, 0)
                try {
                    options.pickerType = ImageCropParams.PickerType.CAMERA_AND_GALLERY
                    options.fixAspectRatio =
                        ta.getBoolean(
                            R.styleable.CropImageView_cropFixAspectRatio,
                            options.fixAspectRatio
                        )
                    options.aspectRatioX =
                        ta.getInteger(
                            R.styleable.CropImageView_cropAspectRatioX,
                            options.aspectRatioX
                        )
                    options.aspectRatioY =
                        ta.getInteger(
                            R.styleable.CropImageView_cropAspectRatioY,
                            options.aspectRatioY
                        )
                    options.scaleType =
                        ScaleType.entries[ta.getInt(
                            R.styleable.CropImageView_cropScaleType,
                            options.scaleType.ordinal
                        )]
                    options.autoZoomEnabled =
                        ta.getBoolean(
                            R.styleable.CropImageView_cropAutoZoomEnabled,
                            options.autoZoomEnabled
                        )
                    options.multiTouchEnabled =
                        ta.getBoolean(
                            R.styleable.CropImageView_cropMultiTouchEnabled,
                            options.multiTouchEnabled
                        )
                    options.maxZoom =
                        ta.getInteger(R.styleable.CropImageView_cropMaxZoom, options.maxZoom)
                    options.cropShape =
                        CropShape.entries[ta.getInt(
                            R.styleable.CropImageView_cropShape,
                            options.cropShape.ordinal
                        )]
                    options.guidelines =
                        Guidelines.entries[ta.getInt(
                            R.styleable.CropImageView_cropGuidelines, options.guidelines.ordinal
                        )]
                    options.snapRadius =
                        ta.getDimension(
                            R.styleable.CropImageView_cropSnapRadius,
                            options.snapRadius
                        )
                    options.touchRadius =
                        ta.getDimension(
                            R.styleable.CropImageView_cropTouchRadius,
                            options.touchRadius
                        )
                    options.initialCropWindowPaddingRatio =
                        ta.getFloat(
                            R.styleable.CropImageView_cropInitialCropWindowPaddingRatio,
                            options.initialCropWindowPaddingRatio
                        )
                    options.borderLineThickness =
                        ta.getDimension(
                            R.styleable.CropImageView_cropBorderLineThickness,
                            options.borderLineThickness
                        )
                    options.borderLineColor =
                        ta.getInteger(
                            R.styleable.CropImageView_cropBorderLineColor,
                            options.borderLineColor
                        )
                    options.borderCornerThickness =
                        ta.getDimension(
                            R.styleable.CropImageView_cropBorderCornerThickness,
                            options.borderCornerThickness
                        )
                    options.borderCornerOffset =
                        ta.getDimension(
                            R.styleable.CropImageView_cropBorderCornerOffset,
                            options.borderCornerOffset
                        )
                    options.borderCornerLength =
                        ta.getDimension(
                            R.styleable.CropImageView_cropBorderCornerLength,
                            options.borderCornerLength
                        )
                    options.borderCornerColor =
                        ta.getInteger(
                            R.styleable.CropImageView_cropBorderCornerColor,
                            options.borderCornerColor
                        )
                    options.guidelinesThickness =
                        ta.getDimension(
                            R.styleable.CropImageView_cropGuidelinesThickness,
                            options.guidelinesThickness
                        )
                    options.guidelinesColor =
                        ta.getInteger(
                            R.styleable.CropImageView_cropGuidelinesColor,
                            options.guidelinesColor
                        )
                    options.backgroundColor =
                        ta.getInteger(
                            R.styleable.CropImageView_cropBackgroundColor,
                            options.backgroundColor
                        )
                    options.showCropOverlay =
                        ta.getBoolean(
                            R.styleable.CropImageView_cropShowCropOverlay,
                            mShowCropOverlay
                        )
                    options.showProgressBar =
                        ta.getBoolean(
                            R.styleable.CropImageView_cropShowProgressBar,
                            mShowProgressBar
                        )
                    options.borderCornerThickness =
                        ta.getDimension(
                            R.styleable.CropImageView_cropBorderCornerThickness,
                            options.borderCornerThickness
                        )
                    options.minCropWindowWidth = ta.getDimension(
                        R.styleable.CropImageView_cropMinCropWindowWidth,
                        options.minCropWindowWidth.toFloat()
                    ).toInt()
                    options.minCropWindowHeight = ta.getDimension(
                        R.styleable.CropImageView_cropMinCropWindowHeight,
                        options.minCropWindowHeight.toFloat()
                    ).toInt()
                    options.minCropResultWidth = ta.getFloat(
                        R.styleable.CropImageView_cropMinCropResultWidthPX,
                        options.minCropResultWidth.toFloat()
                    ).toInt()
                    options.minCropResultHeight = ta.getFloat(
                        R.styleable.CropImageView_cropMinCropResultHeightPX,
                        options.minCropResultHeight.toFloat()
                    ).toInt()
                    options.maxCropResultWidth = ta.getFloat(
                        R.styleable.CropImageView_cropMaxCropResultWidthPX,
                        options.maxCropResultWidth.toFloat()
                    ).toInt()
                    options.maxCropResultHeight = ta.getFloat(
                        R.styleable.CropImageView_cropMaxCropResultHeightPX,
                        options.maxCropResultHeight.toFloat()
                    ).toInt()
                    options.flipHorizontally =
                        ta.getBoolean(
                            R.styleable.CropImageView_cropFlipHorizontally, options.flipHorizontally
                        )
                    options.flipVertically =
                        ta.getBoolean(
                            R.styleable.CropImageView_cropFlipHorizontally,
                            options.flipVertically
                        )

                    isSaveBitmapToInstanceState =
                        ta.getBoolean(
                            R.styleable.CropImageView_cropSaveBitmapToInstanceState,
                            isSaveBitmapToInstanceState
                        )

                    // if aspect ratio is set then set fixed to true
                    if (ta.hasValue(R.styleable.CropImageView_cropAspectRatioX)
                        && ta.hasValue(R.styleable.CropImageView_cropAspectRatioX)
                        && !ta.hasValue(R.styleable.CropImageView_cropFixAspectRatio)
                    ) {
                        options.fixAspectRatio = true
                    }
                } finally {
                    ta.recycle()
                }
            }
        }

        options.validate()

        scaleViewType = options.scaleType
        mAutoZoomEnabled = options.autoZoomEnabled
        mMaxZoom = options.maxZoom
        mShowCropOverlay = options.showCropOverlay
        mShowProgressBar = options.showProgressBar
        flipHorizontally = options.flipHorizontally
        flipVertically = options.flipVertically


        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.crop_image_view, this, true)

        imageView = v.findViewById(R.id.ImageView_image)
        imageView.scaleType = ImageView.ScaleType.MATRIX

        cropOverlayUI = v.findViewById(R.id.CropOverlayView)
        cropOverlayUI.setCropWindowChangeListener(
            object : CropWindowChangeListener {
                override fun onCropWindowChanged(inProgress: Boolean) {
                    handleCropWindowChanged(inProgress, true)
                    val listener = mOnCropOverlayReleasedListener
                    if (listener != null && !inProgress) {
                        listener.onCropOverlayReleased(cropRect)
                    }
                    val movedListener = mOnSetCropOverlayMovedListener
                    if (movedListener != null && inProgress) {
                        movedListener.onCropOverlayMoved(cropRect)
                    }
                }
            })
        cropOverlayUI.setInitialAttributeValues(options)

        myProgressBar = v.findViewById(R.id.CropProgressBar)
        setProgressBarVisibility()
    }

    var scaleType: ScaleType
        get() = scaleViewType
        set(scaleType) {
            if (scaleType != scaleViewType) {
                scaleViewType = scaleType
                mZoom = 1f
                mZoomOffsetY = 0f
                mZoomOffsetX = mZoomOffsetY
                cropOverlayUI!!.resetCropOverlayView()
                requestLayout()
            }
        }

    var cropShape: CropShape?
        get() = cropOverlayUI!!.cropShape

        set(cropShape) {
            cropOverlayUI!!.cropShape = cropShape
        }

    var isAutoZoomEnabled: Boolean
        get() = mAutoZoomEnabled
        set(autoZoomEnabled) {
            if (mAutoZoomEnabled != autoZoomEnabled) {
                mAutoZoomEnabled = autoZoomEnabled
                handleCropWindowChanged(false, false)
                cropOverlayUI!!.invalidate()
            }
        }

    fun setMultiTouchEnabled(multiTouchEnabled: Boolean) {
        if (cropOverlayUI!!.setMultiTouchEnabled(multiTouchEnabled)) {
            handleCropWindowChanged(false, false)
            cropOverlayUI.invalidate()
        }
    }

    var maxZoom: Int
        get() = mMaxZoom
        set(maxZoom) {
            if (mMaxZoom != maxZoom && maxZoom > 0) {
                mMaxZoom = maxZoom
                handleCropWindowChanged(false, false)
                cropOverlayUI!!.invalidate()
            }
        }


    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        cropOverlayUI!!.setMinCropResultSize(minCropResultWidth, minCropResultHeight)
    }

    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        cropOverlayUI!!.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight)
    }

    var rotatedDegrees: Int
        get() = degreesRotated
        set(degrees) {
            if (degreesRotated != degrees) {
                rotateImage(degrees - degreesRotated)
            }
        }

    val isFixAspectRatio: Boolean
        get() = cropOverlayUI!!.isFixAspectRatio


    fun setFixedAspectRatio(fixAspectRatio: Boolean) {
        cropOverlayUI!!.setFixedAspectRatio(fixAspectRatio)
    }

    var isFlippedHorizontally: Boolean
        get() = flipHorizontally
        set(flipHorizontally) {
            if (this.flipHorizontally != flipHorizontally) {
                this.flipHorizontally = flipHorizontally
                applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            }
        }

    var isFlippedVertically: Boolean
        get() = flipVertically
        set(flipVertically) {
            if (this.flipVertically != flipVertically) {
                this.flipVertically = flipVertically
                applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            }
        }

    var guidelines: Guidelines?
        get() = cropOverlayUI!!.guidelines

        set(guidelines) {
            cropOverlayUI!!.guidelines = guidelines
        }

    val aspectRatio: Pair<Int, Int>
        get() = Pair(cropOverlayUI!!.aspectRatioX, cropOverlayUI.aspectRatioY)

    fun setAspectRatio(aspectRatioX: Int, aspectRatioY: Int) {
        cropOverlayUI!!.aspectRatioX = aspectRatioX
        cropOverlayUI.aspectRatioY = aspectRatioY
        setFixedAspectRatio(true)
    }

    fun clearAspectRatio() {
        cropOverlayUI!!.aspectRatioX = 1
        cropOverlayUI.aspectRatioY = 1
        setFixedAspectRatio(false)
    }

    fun setSnapRadius(snapRadius: Float) {
        if (snapRadius >= 0) {
            cropOverlayUI!!.setSnapRadius(snapRadius)
        }
    }

    var isShowProgressBar: Boolean
        get() = mShowProgressBar
        set(showProgressBar) {
            if (mShowProgressBar != showProgressBar) {
                mShowProgressBar = showProgressBar
                setProgressBarVisibility()
            }
        }

    var isShowCropOverlay: Boolean
        get() = mShowCropOverlay
        set(showCropOverlay) {
            if (mShowCropOverlay != showCropOverlay) {
                mShowCropOverlay = showCropOverlay
                setCropOverlayVisibility()
            }
        }

    var imageResource: Int
        get() = imageViewResource
        set(resId) {
            if (resId != 0) {
                cropOverlayUI!!.initialCropWindowRect = null
                val bitmap = BitmapFactory.decodeResource(resources, resId)
                setBitmap(bitmap, resId, null, 1, 0)
            }
        }

    val wholeImageRect: Rect?
        get() {
            val loadedSampleSize = mLoadedSampleSize
            val bitmap = bitmapImage ?: return null

            val orgWidth = bitmap.width * loadedSampleSize
            val orgHeight = bitmap.height * loadedSampleSize
            return Rect(0, 0, orgWidth, orgHeight)
        }

    var cropRect: Rect?
        get() {
            val loadedSampleSize = mLoadedSampleSize
            val bitmap = bitmapImage ?: return null

            // get the points of the crop rectangle adjusted to source bitmap
            val points = cropPoints

            val orgWidth = bitmap.width * loadedSampleSize
            val orgHeight = bitmap.height * loadedSampleSize

            return getRectFromPoints(
                points,
                orgWidth,
                orgHeight,
                cropOverlayUI!!.isFixAspectRatio,
                cropOverlayUI.aspectRatioX,
                cropOverlayUI.aspectRatioY
            )
        }
        set(rect) {
            cropOverlayUI!!.initialCropWindowRect = rect
        }

    val cropWindowRect: RectF?
        get() {
            if (cropOverlayUI == null) {
                return null
            }
            return cropOverlayUI.cropWindowRect
        }

    val cropPoints: FloatArray
        get() {
            val cropWindowRect = cropOverlayUI!!.cropWindowRect

            val points =
                floatArrayOf(
                    cropWindowRect!!.left,
                    cropWindowRect.top,
                    cropWindowRect.right,
                    cropWindowRect.top,
                    cropWindowRect.right,
                    cropWindowRect.bottom,
                    cropWindowRect.left,
                    cropWindowRect.bottom
                )

            imageMatrix.invert(imageInverseMatrix)
            imageInverseMatrix.mapPoints(points)

            for (i in points.indices) {
                points[i] *= mLoadedSampleSize.toFloat()
            }

            return points
        }

    fun resetCropRect() {
        mZoom = 1f
        mZoomOffsetX = 0f
        mZoomOffsetY = 0f
        degreesRotated = initialDegreesRotated
        flipHorizontally = false
        flipVertically = false
        applyImageMatrix(width.toFloat(), height.toFloat(), false, false)
        cropOverlayUI!!.resetCropWindowRect()
    }

    val croppedImage: Bitmap?
        get() = getCroppedImage(0, 0, RequestSizeOptions.NONE)

    fun getCroppedImage(reqWidth: Int, reqHeight: Int): Bitmap? {
        return getCroppedImage(reqWidth, reqHeight, RequestSizeOptions.RESIZE_INSIDE)
    }
    fun getCroppedImage(reqWidths: Int, reqHeights: Int, options: RequestSizeOptions): Bitmap? {
        var reqWidth = reqWidths
        var reqHeight = reqHeights
        var croppedBitmap: Bitmap? = null
        if (bitmapImage != null) {
            imageView.clearAnimation()

            reqWidth = if (options != RequestSizeOptions.NONE) reqWidth else 0
            reqHeight = if (options != RequestSizeOptions.NONE) reqHeight else 0

            if (imageUri != null
                && (mLoadedSampleSize > 1 || options == RequestSizeOptions.SAMPLING)
            ) {
                val orgWidth = bitmapImage!!.width * mLoadedSampleSize
                val orgHeight = bitmapImage!!.height * mLoadedSampleSize
                val bitmapSampled =
                    cropBitmap(
                        context,
                        imageUri!!,
                        cropPoints,
                        degreesRotated,
                        orgWidth,
                        orgHeight,
                        cropOverlayUI!!.isFixAspectRatio,
                        cropOverlayUI.aspectRatioX,
                        cropOverlayUI.aspectRatioY,
                        reqWidth,
                        reqHeight,
                        flipHorizontally,
                        flipVertically
                    )
                croppedBitmap = bitmapSampled.bitmap
            } else {
                croppedBitmap =
                    cropBitmapObjectHandleOOM(
                        bitmapImage!!,
                        cropPoints,
                        degreesRotated,
                        cropOverlayUI!!.isFixAspectRatio,
                        cropOverlayUI.aspectRatioX,
                        cropOverlayUI.aspectRatioY,
                        flipHorizontally,
                        flipVertically
                    )
                        .bitmap
            }

            croppedBitmap = resizeBitmap(croppedBitmap!!, reqWidth, reqHeight, options)
        }

        return croppedBitmap
    }

    val croppedImageAsync: Unit

        get() {
            getCroppedImageAsync(0, 0, RequestSizeOptions.NONE)
        }


    fun getCroppedImageAsync(reqWidth: Int, reqHeight: Int) {
        getCroppedImageAsync(reqWidth, reqHeight, RequestSizeOptions.RESIZE_INSIDE)
    }


    fun getCroppedImageAsync(reqWidth: Int, reqHeight: Int, options: RequestSizeOptions) {
        requireNotNull(mOnCropImageCompleteListener) { "mOnCropImageCompleteListener is not set" }
        startCropWorkerTask(reqWidth, reqHeight, options, null, null, 0)
    }

    fun saveCroppedImageAsync(
        saveUri: Uri?,
        saveCompressFormat: CompressFormat?,
        saveCompressQuality: Int,
        reqWidth: Int,
        reqHeight: Int
    ) {
        saveCroppedImageAsync(
            saveUri,
            saveCompressFormat,
            saveCompressQuality,
            reqWidth,
            reqHeight,
            RequestSizeOptions.RESIZE_INSIDE
        )
    }

    fun saveCroppedImageAsync(
        saveUri: Uri?,
        saveCompressFormat: CompressFormat? = CompressFormat.JPEG,
        saveCompressQuality: Int = 100,
        reqWidth: Int = 0,
        reqHeight: Int = 0,
        options: RequestSizeOptions = RequestSizeOptions.NONE
    ) {
        requireNotNull(mOnCropImageCompleteListener) { "mOnCropImageCompleteListener is not set" }
        startCropWorkerTask(
            reqWidth, reqHeight, options, saveUri, saveCompressFormat, saveCompressQuality
        )
    }

    fun setOnSetCropOverlayReleasedListener(listener: OnSetCropOverlayReleasedListener?) {
        mOnCropOverlayReleasedListener = listener
    }

    fun setOnSetCropOverlayMovedListener(listener: OnSetCropOverlayMovedListener?) {
        mOnSetCropOverlayMovedListener = listener
    }

    fun setOnCropWindowChangedListener(listener: OnSetCropWindowChangeListener?) {
        mOnSetCropWindowChangeListener = listener
    }

    fun setOnSetImageUriCompleteListener(listener: OnSetImageUriCompleteListener?) {
        mOnSetImageUriCompleteListener = listener
    }

    fun setOnCropImageCompleteListener(listener: OnCropImageCompleteListener?) {
        mOnCropImageCompleteListener = listener
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        cropOverlayUI!!.initialCropWindowRect = null
        setBitmap(bitmap, 0, null, 1, 0)
    }

    fun setImageBitmap(bitmap: Bitmap?, exif: ExifInterface?) {
        val setBitmap: Bitmap?
        var degreesRotated = 0
        if (bitmap != null && exif != null) {
            val result = rotateBitmapByExif(bitmap, exif)
            setBitmap = result.bitmap
            degreesRotated = result.degrees
            initialDegreesRotated = result.degrees
        } else {
            setBitmap = bitmap
        }
        cropOverlayUI!!.initialCropWindowRect = null
        setBitmap(setBitmap, 0, null, 1, degreesRotated)
    }

    fun setImageUriAsync(uri: Uri?) {
        if (uri != null) {
            val currentTask =
                if (mImageLoadingCor != null) mImageLoadingCor else null
//            currentTask?.cancel(true)

            // either no existing task is working or we canceled it, need to load new URI
            clearImageInt()
            mRestoreCropWindowRect = null
            mRestoreDegreesRotated = 0
            cropOverlayUI!!.initialCropWindowRect = null
            mImageLoadingCor = ImageLoadingCor(this, uri)
            CoroutineScope(Dispatchers.Main).launch {
                // Execute the bitmap loading task
                val result = mImageLoadingCor?.execute()

                // Handle the result
                mImageLoadingCor?.handleResult(result!!)
            }
            //mBitmapLoadingWorkerTask!!.get()!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            setProgressBarVisibility()
        }
    }

    fun clearImage() {
        clearImageInt()
        cropOverlayUI!!.initialCropWindowRect = null
    }

    fun rotateImage(degree: Int) {
        var degrees = degree
        if (bitmapImage != null) {
            // Force degrees to be a non-zero value between 0 and 360 (inclusive)
            degrees = if (degrees < 0) {
                degrees % 360 + 360
            } else {
                degrees % 360
            }

            val flipAxes = (
                    !cropOverlayUI!!.isFixAspectRatio
                            && ((degrees in 46..134) || (degrees in 216..304)))
            ImageCropperUtils.RECT.set(cropOverlayUI.cropWindowRect!!)
            var halfWidth =
                (if (flipAxes) ImageCropperUtils.RECT.height() else ImageCropperUtils.RECT.width()) / 2f
            var halfHeight =
                (if (flipAxes) ImageCropperUtils.RECT.width() else ImageCropperUtils.RECT.height()) / 2f
            if (flipAxes) {
                val isFlippedHorizontally = flipHorizontally
                flipHorizontally = flipVertically
                flipVertically = isFlippedHorizontally
            }

            imageMatrix.invert(imageInverseMatrix)

            ImageCropperUtils.POINTS[0] = ImageCropperUtils.RECT.centerX()
            ImageCropperUtils.POINTS[1] = ImageCropperUtils.RECT.centerY()
            ImageCropperUtils.POINTS[2] = 0f
            ImageCropperUtils.POINTS[3] = 0f
            ImageCropperUtils.POINTS[4] = 1f
            ImageCropperUtils.POINTS[5] = 0f
            imageInverseMatrix.mapPoints(ImageCropperUtils.POINTS)

            // This is valid because degrees is not negative.
            degreesRotated = (degreesRotated + degrees) % 360

            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)

            // adjust the zoom so the crop window size remains the same even after image scale change
            imageMatrix.mapPoints(ImageCropperUtils.POINTS2, ImageCropperUtils.POINTS)
            mZoom /= sqrt(
                (ImageCropperUtils.POINTS2[4] - ImageCropperUtils.POINTS2[2]).pow(2.0.toFloat()) +
                        (ImageCropperUtils.POINTS2[5] - ImageCropperUtils.POINTS2[3]).pow(2.0.toFloat())
            )
            mZoom = max(mZoom.toDouble(), 1.0).toFloat()

            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)

            imageMatrix.mapPoints(ImageCropperUtils.POINTS2, ImageCropperUtils.POINTS)

            // adjust the width/height by the changes in scaling to the image
            val change = sqrt(
                (ImageCropperUtils.POINTS2[4] - ImageCropperUtils.POINTS2[2]).pow(2.0.toFloat())
                        + (ImageCropperUtils.POINTS2[5] - ImageCropperUtils.POINTS2[3]).pow(
                    2.0.toFloat()
                )
            )
            halfWidth *= change
            halfHeight *= change

            // calculate the new crop window rectangle to center in the same location and have proper
            // width/height
            ImageCropperUtils.RECT[ImageCropperUtils.POINTS2[0] - halfWidth, ImageCropperUtils.POINTS2[1] - halfHeight, ImageCropperUtils.POINTS2[0] + halfWidth] =
                ImageCropperUtils.POINTS2[1] + halfHeight

            cropOverlayUI.resetCropOverlayView()
            cropOverlayUI.cropWindowRect = ImageCropperUtils.RECT
            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
            handleCropWindowChanged(false, false)

            // make sure the crop window rectangle is within the cropping image bounds after all the
            // changes
            cropOverlayUI.fixCurrentCropWindowRect()
        }
    }

    fun flipImageHorizontally() {
        flipHorizontally = !flipHorizontally
        applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
    }

    fun flipImageVertically() {
        flipVertically = !flipVertically
        applyImageMatrix(width.toFloat(), height.toFloat(), true, false)
    }


    fun onSetImageUriAsyncComplete(result: ImageLoadingCor.Result) {
        mImageLoadingCor = null
        setProgressBarVisibility()

        if (result.error == null) {
            initialDegreesRotated = result.degreesRotated
            setBitmap(result.bitmap, 0, result.uri,
                result.loadSampleSize, result.degreesRotated)
        }

        val listener = mOnSetImageUriCompleteListener
        listener?.onSetImageUriComplete(this, result.uri, result.error)
    }

    fun onImageCroppingAsyncComplete(result: ImageCroppingCor.Result) {
        mImageCroppingCor = null
        setProgressBarVisibility()

        val listener = mOnCropImageCompleteListener
        if (listener != null) {
            val cropResult =
                CropResult(
                    bitmapImage,
                    imageUri,
                    result.bitmap,
                    result.uri,
                    result.error,
                    cropPoints,
                    cropRect,
                    wholeImageRect,
                    rotatedDegrees,
                    result.sampleSize
                )
            listener.onCropImageComplete(this, cropResult)
        }
    }

    private fun setBitmap(
        bitmap: Bitmap?,
        imageResource: Int,
        imageUri: Uri?,
        loadSampleSize: Int,
        degreesRotated: Int
    ) {
        if (bitmapImage == null || bitmapImage != bitmap) {
            imageView.clearAnimation()

            clearImageInt()

            bitmapImage = bitmap
            imageView.setImageBitmap(bitmapImage)

            this.imageUri = imageUri
            imageViewResource = imageResource
            mLoadedSampleSize = loadSampleSize
            this.degreesRotated = degreesRotated

            applyImageMatrix(width.toFloat(), height.toFloat(), true, false)

            if (cropOverlayUI != null) {
                cropOverlayUI.resetCropOverlayView()
                setCropOverlayVisibility()
            }
        }
    }

    private fun clearImageInt() {
        // if we allocated the bitmap, release it as fast as possible

        if (bitmapImage != null && (imageViewResource > 0 || imageUri != null)) {
            bitmapImage!!.recycle()
        }
        bitmapImage = null

        // clean the loaded image flags for new image
        imageViewResource = 0
        imageUri = null
        mLoadedSampleSize = 1
        degreesRotated = 0
        mZoom = 1f
        mZoomOffsetX = 0f
        mZoomOffsetY = 0f
        imageMatrix.reset()
        mSaveInstanceStateBitmapUri = null

        imageView.setImageBitmap(null)

        setCropOverlayVisibility()
    }

    private fun startCropWorkerTask(
        reqWidths: Int,
        reqHeights: Int,
        options: RequestSizeOptions,
        saveUri: Uri?,
        saveCompressFormat: CompressFormat?,
        saveCompressQuality: Int
    ) {
        var reqWidth = reqWidths
        var reqHeight = reqHeights
        val bitmap = bitmapImage
        if (bitmap != null) {
            imageView.clearAnimation()

            reqWidth = if (options != RequestSizeOptions.NONE) reqWidth else 0
            reqHeight = if (options != RequestSizeOptions.NONE) reqHeight else 0

            val orgWidth = bitmap.width * mLoadedSampleSize
            val orgHeight = bitmap.height * mLoadedSampleSize
            mImageCroppingCor = if (imageUri != null
                && (mLoadedSampleSize > 1 || options == RequestSizeOptions.SAMPLING)
            ) {
                WeakReference(
                    ImageCroppingCor(
                        this,
                        bitmap,
                        imageUri,
                        cropPoints,
                        degreesRotated,
                        orgWidth,
                        orgHeight,
                        cropOverlayUI!!.isFixAspectRatio,
                        cropOverlayUI.aspectRatioX,
                        cropOverlayUI.aspectRatioY,
                        reqWidth,
                        reqHeight,
                        flipHorizontally,
                        flipVertically,
                        options,
                        saveUri,
                        saveCompressFormat!!,
                        saveCompressQuality
                    )
                )
            } else {
                WeakReference(
                    ImageCroppingCor(
                        this,
                        bitmap,
                        imageUri,
                        cropPoints,
                        degreesRotated,
                        orgWidth,
                        orgHeight,
                        cropOverlayUI!!.isFixAspectRatio,
                        cropOverlayUI.aspectRatioX,
                        cropOverlayUI.aspectRatioY,
                        reqWidth,
                        reqHeight,
                        flipHorizontally,
                        flipVertically,
                        options,
                        saveUri,
                        saveCompressFormat!!,
                        saveCompressQuality
                    )
                )
            }
            CoroutineScope(Dispatchers.Main).launch {
                val result = mImageCroppingCor?.get()?.execute()
                mImageCroppingCor?.get()?.handleResult(result!!)
            }
            //mBitmapCroppingWorkerTask!!.get()!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            setProgressBarVisibility()
        }
    }

    public override fun onSaveInstanceState(): Parcelable? {
        if (imageUri == null && bitmapImage == null && imageViewResource < 1) {
            return super.onSaveInstanceState()
        }

        val bundle = Bundle()
        var imageUri = imageUri
        if (isSaveBitmapToInstanceState && imageUri == null && imageViewResource < 1) {
            imageUri =
                writeTempStateStoreBitmap(
                    context, bitmapImage!!, mSaveInstanceStateBitmapUri
                )
            mSaveInstanceStateBitmapUri = imageUri
        }
        if (imageUri != null && bitmapImage != null) {
            val key = UUID.randomUUID().toString()
            ImageCropperUtils.stateBitmap = Pair(key, WeakReference(bitmapImage))
            bundle.putString("LOADED_IMAGE_STATE_BITMAP_KEY", key)
        }
        if (mImageLoadingCor != null) {
            val task = mImageLoadingCor
            if (task != null) {
                bundle.putParcelable("LOADING_IMAGE_URI", task.uri)
            }
        }
        bundle.putParcelable("instanceState", super.onSaveInstanceState())
        bundle.putParcelable("LOADED_IMAGE_URI", imageUri)
        bundle.putInt("LOADED_IMAGE_RESOURCE", imageViewResource)
        bundle.putInt("LOADED_SAMPLE_SIZE", mLoadedSampleSize)
        bundle.putInt("DEGREES_ROTATED", degreesRotated)
        bundle.putParcelable("INITIAL_CROP_RECT", cropOverlayUI!!.initialCropWindowRect)

        ImageCropperUtils.RECT.set(cropOverlayUI.cropWindowRect!!)

        imageMatrix.invert(imageInverseMatrix)
        imageInverseMatrix.mapRect(ImageCropperUtils.RECT)

        bundle.putParcelable("CROP_WINDOW_RECT", ImageCropperUtils.RECT)
        bundle.putString("CROP_SHAPE", cropOverlayUI.cropShape!!.name)
        bundle.putBoolean("CROP_AUTO_ZOOM_ENABLED", mAutoZoomEnabled)
        bundle.putInt("CROP_MAX_ZOOM", mMaxZoom)
        bundle.putBoolean("CROP_FLIP_HORIZONTALLY", flipHorizontally)
        bundle.putBoolean("CROP_FLIP_VERTICALLY", flipVertically)

        return bundle
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {

            // prevent restoring state if already set by outside code
            if (mImageLoadingCor == null && imageUri == null && bitmapImage == null && imageViewResource == 0) {
                var uri = state.getParcelable<Uri>("LOADED_IMAGE_URI")
                if (uri != null) {
                    val key = state.getString("LOADED_IMAGE_STATE_BITMAP_KEY")
                    if (key != null) {
                        val stateBitmap =
                            if (ImageCropperUtils.stateBitmap != null && ImageCropperUtils.stateBitmap!!.first == key
                            ) ImageCropperUtils.stateBitmap!!.second.get()
                            else null
                        ImageCropperUtils.stateBitmap = null
                        if (stateBitmap != null && !stateBitmap.isRecycled) {
                            setBitmap(stateBitmap, 0, uri, state.getInt("LOADED_SAMPLE_SIZE"), 0)
                        }
                    }
                    if (imageUri == null) {
                        setImageUriAsync(uri)
                    }
                } else {
                    val resId = state.getInt("LOADED_IMAGE_RESOURCE")
                    if (resId > 0) {
                        imageResource = resId
                    } else {
                        uri = state.getParcelable("LOADING_IMAGE_URI")
                        if (uri != null) {
                            setImageUriAsync(uri)
                        }
                    }
                }

                mRestoreDegreesRotated = state.getInt("DEGREES_ROTATED")
                degreesRotated = mRestoreDegreesRotated

                val initialCropRect = state.getParcelable<Rect>("INITIAL_CROP_RECT")
                if (initialCropRect != null
                    && (initialCropRect.width() > 0 || initialCropRect.height() > 0)
                ) {
                    cropOverlayUI!!.initialCropWindowRect = initialCropRect
                }

                val cropWindowRect = state.getParcelable<RectF>("CROP_WINDOW_RECT")
                if (cropWindowRect != null && (cropWindowRect.width() > 0 || cropWindowRect.height() > 0)) {
                    mRestoreCropWindowRect = cropWindowRect
                }

                cropOverlayUI!!.cropShape = CropShape.valueOf(
                    state.getString("CROP_SHAPE")!!
                )

                mAutoZoomEnabled = state.getBoolean("CROP_AUTO_ZOOM_ENABLED")
                mMaxZoom = state.getInt("CROP_MAX_ZOOM")

                flipHorizontally = state.getBoolean("CROP_FLIP_HORIZONTALLY")
                flipVertically = state.getBoolean("CROP_FLIP_VERTICALLY")
            }

            super.onRestoreInstanceState(state.getParcelable("instanceState"))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (bitmapImage != null) {
            // Bypasses a baffling bug when used within a ScrollView, where heightSize is set to 0.

            if (heightSize == 0) {
                heightSize = bitmapImage!!.height
            }

            val desiredWidth: Int
            val desiredHeight: Int

            var viewToBitmapWidthRatio = Double.POSITIVE_INFINITY
            var viewToBitmapHeightRatio = Double.POSITIVE_INFINITY

            // Checks if either width or height needs to be fixed
            if (widthSize < bitmapImage!!.width) {
                viewToBitmapWidthRatio = widthSize.toDouble() / bitmapImage!!.width.toDouble()
            }
            if (heightSize < bitmapImage!!.height) {
                viewToBitmapHeightRatio = heightSize.toDouble() / bitmapImage!!.height.toDouble()
            }

            // If either needs to be fixed, choose smallest ratio and calculate from there
            if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY
                || viewToBitmapHeightRatio != Double.POSITIVE_INFINITY
            ) {
                if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                    desiredWidth = widthSize
                    desiredHeight = (bitmapImage!!.height * viewToBitmapWidthRatio).toInt()
                } else {
                    desiredHeight = heightSize
                    desiredWidth = (bitmapImage!!.width * viewToBitmapHeightRatio).toInt()
                }
            } else {
                // Otherwise, the picture is within frame layout bounds. Desired width is simply picture
                // size
                desiredWidth = bitmapImage!!.width
                desiredHeight = bitmapImage!!.height
            }

            val width = getOnMeasureSpec(widthMode, widthSize, desiredWidth)
            val height = getOnMeasureSpec(heightMode, heightSize, desiredHeight)

            layoutWidth = width
            layoutHeight = height

            setMeasuredDimension(layoutWidth, layoutHeight)
        } else {
            setMeasuredDimension(widthSize, heightSize)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (layoutWidth > 0 && layoutHeight > 0) {
            // Gets original parameters, and creates the new parameters
            val origParams = this.layoutParams
            origParams.width = layoutWidth
            origParams.height = layoutHeight
            layoutParams = origParams

            if (bitmapImage != null) {
                applyImageMatrix((r - l).toFloat(), (b - t).toFloat(), true, false)

                // after state restore we want to restore the window crop, possible only after widget size
                // is known
                if (mRestoreCropWindowRect != null) {
                    if (mRestoreDegreesRotated != initialDegreesRotated) {
                        degreesRotated = mRestoreDegreesRotated
                        applyImageMatrix((r - l).toFloat(), (b - t).toFloat(), true, false)
                    }
                    imageMatrix.mapRect(mRestoreCropWindowRect)
                    cropOverlayUI!!.cropWindowRect = mRestoreCropWindowRect
                    handleCropWindowChanged(false, false)
                    cropOverlayUI.fixCurrentCropWindowRect()
                    mRestoreCropWindowRect = null
                } else if (mSizeChanged) {
                    mSizeChanged = false
                    handleCropWindowChanged(false, false)
                }
            } else {
                updateImageBounds(true)
            }
        } else {
            updateImageBounds(true)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mSizeChanged = oldw > 0 && oldh > 0
    }

    private fun handleCropWindowChanged(inProgress: Boolean, animate: Boolean) {
        val width = width
        val height = height
        if (bitmapImage != null && width > 0 && height > 0) {
            val cropRect = cropOverlayUI!!.cropWindowRect
            if (inProgress) {
                if (cropRect!!.left < 0 || cropRect.top < 0 || cropRect.right > width || cropRect.bottom > height) {
                    applyImageMatrix(width.toFloat(), height.toFloat(), false, false)
                }
            } else if (mAutoZoomEnabled || mZoom > 1) {
                var newZoom = 0f
                // keep the cropping window covered area to 50%-65% of zoomed sub-area
                if (mZoom < mMaxZoom && cropRect!!.width() < width * 0.5f && cropRect.height() < height * 0.5f) {
                    newZoom = min(
                        mMaxZoom.toDouble(),
                        min(
                            (width / (cropRect.width() / mZoom / 0.64f)).toDouble(),
                            (height / (cropRect.height() / mZoom / 0.64f)).toDouble()
                        )
                    ).toFloat()
                }
                if (mZoom > 1 && (cropRect!!.width() > width * 0.65f || cropRect.height() > height * 0.65f)) {
                    newZoom = max(
                        1.0,
                        min(
                            (width / (cropRect.width() / mZoom / 0.51f)).toDouble(),
                            (height / (cropRect.height() / mZoom / 0.51f)).toDouble()
                        )
                    ).toFloat()
                }
                if (!mAutoZoomEnabled) {
                    newZoom = 1f
                }

                if (newZoom > 0 && newZoom != mZoom) {
                    if (animate) {
                        if (animationView == null) {
                            // lazy create animation single instance
                            animationView = AnimImageCrop(imageView, cropOverlayUI)
                        }
                        // set the state for animation to start from
                        animationView!!.setStartAnim(imagePoints, imageMatrix)
                    }

                    mZoom = newZoom

                    applyImageMatrix(width.toFloat(), height.toFloat(), true, animate)
                }
            }
            if (mOnSetCropWindowChangeListener != null && !inProgress) {
                mOnSetCropWindowChangeListener!!.onCropWindowChanged()
            }
        }
    }

    private fun applyImageMatrix(width: Float, height: Float, center: Boolean, animate: Boolean) {
        if (bitmapImage != null && width > 0 && height > 0) {
            imageMatrix.invert(imageInverseMatrix)
            val cropRect = cropOverlayUI!!.cropWindowRect
            imageInverseMatrix.mapRect(cropRect)

            imageMatrix.reset()

            // move the image to the center of the image view first so we can manipulate it from there
            imageMatrix.postTranslate(
                (width - bitmapImage!!.width) / 2, (height - bitmapImage!!.height) / 2
            )
            mapImagePointsByImageMatrix()

            // rotate the image the required degrees from center of image
            if (degreesRotated > 0) {
                imageMatrix.postRotate(
                    degreesRotated.toFloat(),
                    getRectCenterX(imagePoints),
                    getRectCenterY(imagePoints)
                )
                mapImagePointsByImageMatrix()
            }

            // scale the image to the image view, image rect transformed to know new width/height
            val scale = min(
                (width / getRectWidth(imagePoints)).toDouble(),
                (height / getRectHeight(imagePoints)).toDouble()
            ).toFloat()
            if (scaleViewType == ScaleType.FIT_CENTER || (scaleViewType == ScaleType.CENTER_INSIDE && scale < 1)
                || (scale > 1 && mAutoZoomEnabled)
            ) {
                imageMatrix.postScale(
                    scale,
                    scale,
                    getRectCenterX(imagePoints),
                    getRectCenterY(imagePoints)
                )
                mapImagePointsByImageMatrix()
            }

            // scale by the current zoom level
            val scaleX = if (flipHorizontally) -mZoom else mZoom
            val scaleY = if (flipVertically) -mZoom else mZoom
            imageMatrix.postScale(
                scaleX,
                scaleY,
                getRectCenterX(imagePoints),
                getRectCenterY(imagePoints)
            )
            mapImagePointsByImageMatrix()

            imageMatrix.mapRect(cropRect)

            if (center) {
                // set the zoomed area to be as to the center of cropping window as possible
                mZoomOffsetX =
                    if (width > getRectWidth(imagePoints)
                    ) 0f
                    else (max(
                        min(
                            (width / 2 - cropRect!!.centerX()).toDouble(),
                            -getRectLeft(imagePoints).toDouble()
                        ),
                        (getWidth() - getRectRight(imagePoints)).toDouble()
                    ) / scaleX).toFloat()
                mZoomOffsetY =
                    if (height > getRectHeight(imagePoints)
                    ) 0f
                    else (max(
                        min(
                            (height / 2 - cropRect!!.centerY()).toDouble(),
                            -getRectTop(imagePoints).toDouble()
                        ),
                        (getHeight() - getRectBottom(imagePoints)).toDouble()
                    ) / scaleY).toFloat()
            } else {
                // adjust the zoomed area so the crop window rectangle will be inside the area in case it
                // was moved outside
                mZoomOffsetX = (min(
                    max(
                        (mZoomOffsetX * scaleX).toDouble(),
                        -cropRect!!.left.toDouble()
                    ), (-cropRect.right + width).toDouble()
                ) / scaleX).toFloat()
                mZoomOffsetY = (min(
                    max(
                        (mZoomOffsetY * scaleY).toDouble(),
                        -cropRect.top.toDouble()
                    ), (-cropRect.bottom + height).toDouble()
                ) / scaleY).toFloat()
            }

            // apply to zoom offset translate and update the crop rectangle to offset correctly
            imageMatrix.postTranslate(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY)
            cropRect!!.offset(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY)
            cropOverlayUI.cropWindowRect = cropRect
            mapImagePointsByImageMatrix()
            cropOverlayUI.invalidate()

            // set matrix to apply
            if (animate) {
                // set the state for animation to end in, start animation now
                animationView!!.setEndAnim(imagePoints, imageMatrix)
                imageView.startAnimation(animationView)
            } else {
                imageView.imageMatrix = imageMatrix
            }

            // update the image rectangle in the crop overlay
            updateImageBounds(false)
        }
    }

    private fun mapImagePointsByImageMatrix() {
        imagePoints[0] = 0f
        imagePoints[1] = 0f
        imagePoints[2] = bitmapImage!!.width.toFloat()
        imagePoints[3] = 0f
        imagePoints[4] = bitmapImage!!.width.toFloat()
        imagePoints[5] = bitmapImage!!.height.toFloat()
        imagePoints[6] = 0f
        imagePoints[7] = bitmapImage!!.height.toFloat()
        imageMatrix.mapPoints(imagePoints)
        scaleImagePoints[0] = 0f
        scaleImagePoints[1] = 0f
        scaleImagePoints[2] = 100f
        scaleImagePoints[3] = 0f
        scaleImagePoints[4] = 100f
        scaleImagePoints[5] = 100f
        scaleImagePoints[6] = 0f
        scaleImagePoints[7] = 100f
        imageMatrix.mapPoints(scaleImagePoints)
    }

    private fun setCropOverlayVisibility() {
        if (cropOverlayUI != null) {
            cropOverlayUI.visibility =
                if (mShowCropOverlay && bitmapImage != null) VISIBLE else INVISIBLE
        }
    }

    private fun setProgressBarVisibility() {
        val visible = (
                mShowProgressBar
                        && (bitmapImage == null && mImageLoadingCor != null
                        || mImageCroppingCor != null))
        myProgressBar.visibility =
            if (visible) VISIBLE else INVISIBLE
    }

    private fun updateImageBounds(clear: Boolean) {
        if (bitmapImage != null && !clear) {
            // Get the scale factor between the actual Bitmap dimensions and the displayed dimensions for
            // width/height.

            val scaleFactorWidth =
                100f * mLoadedSampleSize / getRectWidth(scaleImagePoints)
            val scaleFactorHeight =
                100f * mLoadedSampleSize / getRectHeight(scaleImagePoints)
            cropOverlayUI!!.setCropWindowLimits(
                width.toFloat(), height.toFloat(), scaleFactorWidth, scaleFactorHeight
            )
        }

        cropOverlayUI!!.setBounds(if (clear) null else imagePoints, width, height)
    }

    enum class CropShape {
        RECTANGLE,
        OVAL
    }

    enum class ScaleType {
        FIT_CENTER,
        CENTER,
        CENTER_CROP,
        CENTER_INSIDE
    }

    enum class Guidelines {
        OFF,
        ON_TOUCH,
        ON
    }

    enum class RequestSizeOptions {
        NONE,
        SAMPLING,
        RESIZE_INSIDE,
        RESIZE_FIT,
        RESIZE_EXACT
    }

    interface OnSetCropOverlayReleasedListener {
        fun onCropOverlayReleased(rect: Rect?)
    }

    interface OnSetCropOverlayMovedListener {
        fun onCropOverlayMoved(rect: Rect?)
    }

    interface OnSetCropWindowChangeListener {
        fun onCropWindowChanged()
    }

    interface OnSetImageUriCompleteListener {
        fun onSetImageUriComplete(view: ImageCropUI?, uri: Uri?, error: Exception?)
    }
    interface OnCropImageCompleteListener {
        fun onCropImageComplete(view: ImageCropUI?, result: CropResult?)
    }

    open class CropResult internal constructor(
        val originalBitmap: Bitmap?,
        val originalUri: Uri?,
        val bitmap: Bitmap?,
        val uri: Uri?,
        val error: Exception?,
        val cropPoints: FloatArray,
        val cropRect: Rect?,
        val wholeImageRect: Rect?,
        val rotation: Int,
        val sampleSize: Int
    ) {
        val isSuccessful: Boolean
            get() = error == null
    }

    companion object {
        private fun getOnMeasureSpec(
            measureSpecMode: Int,
            measureSpecSize: Int,
            desiredSize: Int
        ): Int {
            val spec = if (measureSpecMode == MeasureSpec.EXACTLY) {
                measureSpecSize
            } else if (measureSpecMode == MeasureSpec.AT_MOST) {
                min(desiredSize.toDouble(), measureSpecSize.toDouble()).toInt()
            } else {
                desiredSize
            }

            return spec
        }
    }
}
