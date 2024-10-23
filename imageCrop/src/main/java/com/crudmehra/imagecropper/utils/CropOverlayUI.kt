package com.crudmehra.imagecropper.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectBottom
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectLeft
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectRight
import com.crudmehra.imagecropper.utils.ImageCropperUtils.getRectTop
import com.crudmehra.imagecropper.utils.ImageCropUI.CropShape
import com.crudmehra.imagecropper.utils.ImageCropUI.Guidelines
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class CropOverlayUI  // endregion
@JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) : View(context, attrs) {
    // region: Fields and Const
    private var scaleDetector: ScaleGestureDetector? = null

    private var multiTouchEnabled = false

    private val imageCropWindowHandler = ImageCropWindowHandler()

    private var cropWindowChangeListener: CropWindowChangeListener? = null

    private val drawRect = RectF()

    private var borderPaint: Paint? = null

    private var borderCornerPaint: Paint? = null

    private var guidelinePaint: Paint? = null

    private var backgroundPaint: Paint? = null

    private val path = Path()

    private val boundsPoints = FloatArray(8)

    private val calcBounds = RectF()

    private var viewWidth = 0

    private var viewHeight = 0

    private var borderCornerOffset = 0f

    private var borderCornerLength = 0f

    private var initialCropWindowPaddingRatio = 0f

    private var touchRadius = 0f

    private var snapRadius = 0f

    private var moveHandler: ImageCroppingHandler? = null


    var isFixAspectRatio: Boolean = false
        private set

    private var aspectViewRatioX = 0

    private var aspectViewRatioY = 0

    private var targetAspectRatio = (aspectViewRatioX.toFloat()) / aspectViewRatioY

    private var guidelinesView: Guidelines? = null

    private var cropShapeView: CropShape? = null

    private val initialCropViewWindowRect = Rect()

    private var initializedCropWindow = false
    private var pickerType : ImageCropParams.PickerType ?=null


    fun setCropWindowChangeListener(listener: CropWindowChangeListener?) {
        cropWindowChangeListener = listener
    }

    var cropWindowRect: RectF?
        get() = imageCropWindowHandler.rect
        set(rect) {
            imageCropWindowHandler.rect = rect
        }

    fun fixCurrentCropWindowRect() {
        val rect = cropWindowRect!!
        fixCropWindowRectByRules(rect)
        imageCropWindowHandler.rect = rect
    }


    fun setBounds(boundsPoints: FloatArray?, viewWidth: Int, viewHeight: Int) {
        if (boundsPoints == null || !this.boundsPoints.contentEquals(boundsPoints)) {
            if (boundsPoints == null) {
                Arrays.fill(this.boundsPoints, 0f)
            } else {
                System.arraycopy(boundsPoints, 0, this.boundsPoints, 0, boundsPoints.size)
            }
            this.viewWidth = viewWidth
            this.viewHeight = viewHeight
            val cropRect = imageCropWindowHandler.rect!!
            if (cropRect.width() == 0f || cropRect.height() == 0f) {
                initCropWindow()
            }
        }
    }

    fun resetCropOverlayView() {
        if (initializedCropWindow) {
            cropWindowRect = ImageCropperUtils.EMPTY_RECT_F
            initCropWindow()
            invalidate()
        }
    }

    var cropShape: CropShape?
        get() = cropShapeView
        set(cropShape) {
            if (cropShapeView != cropShape) {
                cropShapeView = cropShape
                invalidate()
            }
        }

    var guidelines: Guidelines?
        get() = guidelinesView
        set(guidelines) {
            if (guidelinesView != guidelines) {
                guidelinesView = guidelines
                if (initializedCropWindow) {
                    invalidate()
                }
            }
        }

    fun setFixedAspectRatio(fixAspectRatio: Boolean) {
        if (isFixAspectRatio != fixAspectRatio) {
            isFixAspectRatio = fixAspectRatio
            if (initializedCropWindow) {
                initCropWindow()
                invalidate()
            }
        }
    }

    var aspectRatioX: Int
        get() = aspectViewRatioX
        set(aspectRatioX) {
            require(aspectRatioX > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
            if (aspectViewRatioX != aspectRatioX) {
                aspectViewRatioX = aspectRatioX
                targetAspectRatio = (aspectViewRatioX.toFloat()) / aspectViewRatioY

                if (initializedCropWindow) {
                    initCropWindow()
                    invalidate()
                }
            }
        }

    var aspectRatioY: Int
        get() = aspectViewRatioY
        set(aspectRatioY) {
            require(aspectRatioY > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
            if (aspectViewRatioY != aspectRatioY) {
                aspectViewRatioY = aspectRatioY
                targetAspectRatio = (aspectViewRatioX.toFloat()) / aspectViewRatioY

                if (initializedCropWindow) {
                    initCropWindow()
                    invalidate()
                }
            }
        }


    fun setSnapRadius(snapRadius: Float) {
        this.snapRadius = snapRadius
    }

    fun setMultiTouchEnabled(multiTouchEnabled: Boolean): Boolean {
        if (this.multiTouchEnabled != multiTouchEnabled) {
            this.multiTouchEnabled = multiTouchEnabled
            if (this.multiTouchEnabled && scaleDetector == null) {
                scaleDetector = ScaleGestureDetector(context, ScaleListener())
            }
            return true
        }
        return false
    }


    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        imageCropWindowHandler.setMinCropResultSize(minCropResultWidth, minCropResultHeight)
    }


    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        imageCropWindowHandler.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight)
    }


    fun setCropWindowLimits(
        maxWidth: Float, maxHeight: Float, scaleFactorWidth: Float, scaleFactorHeight: Float
    ) {
        imageCropWindowHandler.setCropWindowLimits(
            maxWidth, maxHeight, scaleFactorWidth, scaleFactorHeight
        )
    }

    var initialCropWindowRect: Rect?
        get() = initialCropViewWindowRect
        set(rect) {
            initialCropViewWindowRect.set(rect ?: ImageCropperUtils.EMPTY_RECT)
            if (initializedCropWindow) {
                initCropWindow()
                invalidate()
                callOnCropWindowChanged(false)
            }
        }

    fun resetCropWindowRect() {
        if (initializedCropWindow) {
            initCropWindow()
            invalidate()
            callOnCropWindowChanged(false)
        }
    }


    fun setInitialAttributeValues(options: ImageCropParams) {
        imageCropWindowHandler.setInitialAttributeValues(options)

        cropShape = options.cropShape

        setSnapRadius(options.snapRadius)

        guidelines = options.guidelines

        setFixedAspectRatio(options.fixAspectRatio)

        aspectRatioX = options.aspectRatioX

        aspectRatioY = options.aspectRatioY

        setMultiTouchEnabled(options.multiTouchEnabled)

        touchRadius = options.touchRadius

        initialCropWindowPaddingRatio = options.initialCropWindowPaddingRatio

        borderPaint = getNewPaintOrNull(options.borderLineThickness, options.borderLineColor)

        borderCornerOffset = options.borderCornerOffset
        borderCornerLength = options.borderCornerLength
        borderCornerPaint =
            getNewPaintOrNull(options.borderCornerThickness, options.borderCornerColor)

        guidelinePaint = getNewPaintOrNull(options.guidelinesThickness, options.guidelinesColor)

        backgroundPaint = getNewPaint(options.backgroundColor)
        pickerType = options.pickerType
    }


    private fun initCropWindow() {
        val leftLimit = max(
            getRectLeft(boundsPoints).toDouble(), 0.0
        ).toFloat()
        val topLimit = max(
            getRectTop(boundsPoints).toDouble(), 0.0
        ).toFloat()
        val rightLimit = min(
            getRectRight(boundsPoints).toDouble(),
            width.toDouble()
        ).toFloat()
        val bottomLimit = min(
            getRectBottom(boundsPoints).toDouble(),
            height.toDouble()
        ).toFloat()

        if (rightLimit <= leftLimit || bottomLimit <= topLimit) {
            return
        }

        val rect = RectF()

        // Tells the attribute functions the crop window has already been initialized
        initializedCropWindow = true

        val horizontalPadding = initialCropWindowPaddingRatio * (rightLimit - leftLimit)
        val verticalPadding = initialCropWindowPaddingRatio * (bottomLimit - topLimit)

        if (initialCropViewWindowRect.width() > 0 && initialCropViewWindowRect.height() > 0) {
            // Get crop window position relative to the displayed image.
            rect.left =
                leftLimit + initialCropViewWindowRect.left / imageCropWindowHandler.scaleFactorWidth
            rect.top = topLimit + initialCropViewWindowRect.top / imageCropWindowHandler.scaleFactorHeight
            rect.right =
                rect.left + initialCropViewWindowRect.width() / imageCropWindowHandler.scaleFactorWidth
            rect.bottom =
                rect.top + initialCropViewWindowRect.height() / imageCropWindowHandler.scaleFactorHeight

            // Correct for floating point errors. Crop rect boundaries should not exceed the source Bitmap
            // bounds.
            rect.left = max(leftLimit.toDouble(), rect.left.toDouble()).toFloat()
            rect.top = max(topLimit.toDouble(), rect.top.toDouble()).toFloat()
            rect.right = min(rightLimit.toDouble(), rect.right.toDouble()).toFloat()
            rect.bottom = min(bottomLimit.toDouble(), rect.bottom.toDouble()).toFloat()
        } else if (isFixAspectRatio && rightLimit > leftLimit && bottomLimit > topLimit) {
            // If the image aspect ratio is wider than the crop aspect ratio,
            // then the image height is the determining initial length. Else, vice-versa.

            val bitmapAspectRatio = (rightLimit - leftLimit) / (bottomLimit - topLimit)
            if (bitmapAspectRatio > targetAspectRatio) {
                rect.top = topLimit + verticalPadding
                rect.bottom = bottomLimit - verticalPadding

                val centerX = width / 2f

                // dirty fix for wrong crop overlay aspect ratio when using fixed aspect ratio
                targetAspectRatio = aspectViewRatioX.toFloat() / aspectViewRatioY

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                val cropWidth = max(
                    imageCropWindowHandler.minCropWidth.toDouble(),
                    (rect.height() * targetAspectRatio).toDouble()
                ).toFloat()

                val halfCropWidth = cropWidth / 2f
                rect.left = centerX - halfCropWidth
                rect.right = centerX + halfCropWidth
            } else {
                rect.left = leftLimit + horizontalPadding
                rect.right = rightLimit - horizontalPadding

                val centerY = height / 2f

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                val cropHeight = max(
                    imageCropWindowHandler.minCropHeight.toDouble(),
                    (rect.width() / targetAspectRatio).toDouble()
                ).toFloat()

                val halfCropHeight = cropHeight / 2f
                rect.top = centerY - halfCropHeight
                rect.bottom = centerY + halfCropHeight
            }
        } else {
            // Initialize crop window to have 10% padding w/ respect to image.
            rect.left = leftLimit + horizontalPadding
            rect.top = topLimit + verticalPadding
            rect.right = rightLimit - horizontalPadding
            rect.bottom = bottomLimit - verticalPadding
        }

        fixCropWindowRectByRules(rect)

        imageCropWindowHandler.rect = rect
    }

    private fun fixCropWindowRectByRules(rect: RectF) {
        if (rect.width() < imageCropWindowHandler.minCropWidth) {
            val adj = (imageCropWindowHandler.minCropWidth - rect.width()) / 2
            rect.left -= adj
            rect.right += adj
        }
        if (rect.height() < imageCropWindowHandler.minCropHeight) {
            val adj = (imageCropWindowHandler.minCropHeight - rect.height()) / 2
            rect.top -= adj
            rect.bottom += adj
        }
        if (rect.width() > imageCropWindowHandler.maxCropWidth) {
            val adj = (rect.width() - imageCropWindowHandler.maxCropWidth) / 2
            rect.left += adj
            rect.right -= adj
        }
        if (rect.height() > imageCropWindowHandler.maxCropHeight) {
            val adj = (rect.height() - imageCropWindowHandler.maxCropHeight) / 2
            rect.top += adj
            rect.bottom -= adj
        }

        calculateBounds(rect)
        if (calcBounds.width() > 0 && calcBounds.height() > 0) {
            val leftLimit = max(calcBounds.left.toDouble(), 0.0).toFloat()
            val topLimit = max(calcBounds.top.toDouble(), 0.0).toFloat()
            val rightLimit = min(calcBounds.right.toDouble(), width.toDouble())
                .toFloat()
            val bottomLimit = min(calcBounds.bottom.toDouble(), height.toDouble())
                .toFloat()
            if (rect.left < leftLimit) {
                rect.left = leftLimit
            }
            if (rect.top < topLimit) {
                rect.top = topLimit
            }
            if (rect.right > rightLimit) {
                rect.right = rightLimit
            }
            if (rect.bottom > bottomLimit) {
                rect.bottom = bottomLimit
            }
        }
        if (isFixAspectRatio && abs((rect.width() - rect.height() * targetAspectRatio).toDouble()) > 0.1) {
            if (rect.width() > rect.height() * targetAspectRatio) {
                val adj =
                    (abs((rect.height() * targetAspectRatio - rect.width()).toDouble()) / 2).toFloat()
                rect.left += adj
                rect.right -= adj
            } else {
                val adj =
                    (abs((rect.width() / targetAspectRatio - rect.height()).toDouble()) / 2).toFloat()
                rect.top += adj
                rect.bottom -= adj
            }
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw translucent background for the cropped area.
        drawBackground(canvas)

        if (imageCropWindowHandler.showGuidelines()) {
            // Determines whether guidelines should be drawn or not
            if (guidelinesView == Guidelines.ON) {
                drawGuidelines(canvas)
            } else if (guidelinesView == Guidelines.ON_TOUCH && moveHandler != null) {
                // Draw only when resizing
                drawGuidelines(canvas)
            }
        }

        drawBorders(canvas)

        drawCorners(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val rect = imageCropWindowHandler.rect!!

        val left = max(
            getRectLeft(boundsPoints).toDouble(), 0.0
        ).toFloat()
        val top = max(
            getRectTop(boundsPoints).toDouble(), 0.0
        ).toFloat()
        val right = min(
            getRectRight(boundsPoints).toDouble(),
            width.toDouble()
        ).toFloat()
        val bottom = min(
            getRectBottom(boundsPoints).toDouble(),
            height.toDouble()
        ).toFloat()

        if (cropShapeView == CropShape.RECTANGLE) {
            if (!isNonStraightAngleRotated) {
                canvas.drawRect(left, top, right, rect.top, backgroundPaint!!)
                canvas.drawRect(left, rect.bottom, right, bottom, backgroundPaint!!)
                canvas.drawRect(left, rect.top, rect.left, rect.bottom, backgroundPaint!!)
                canvas.drawRect(rect.right, rect.top, right, rect.bottom, backgroundPaint!!)
            } else {
                path.reset()
                path.moveTo(boundsPoints[0], boundsPoints[1])
                path.lineTo(boundsPoints[2], boundsPoints[3])
                path.lineTo(boundsPoints[4], boundsPoints[5])
                path.lineTo(boundsPoints[6], boundsPoints[7])
                path.close()

                canvas.save()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canvas.clipOutPath(path)
                } else {
                    canvas.clipPath(path)
                }
                canvas.clipRect(rect)
                canvas.drawRect(left, top, right, bottom, backgroundPaint!!)
                canvas.restore()
            }
        } else {
            path.reset()
            if (cropShapeView == CropShape.OVAL) {
                drawRect[rect.left + 2, rect.top + 2, rect.right - 2] = rect.bottom - 2
            } else {
                drawRect[rect.left, rect.top, rect.right] = rect.bottom
            }
            path.addOval(drawRect, Path.Direction.CW)
            canvas.save()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(path)
            } else {
                canvas.clipPath(path)
            }
            canvas.drawRect(left, top, right, bottom, backgroundPaint!!)
            canvas.restore()
        }
    }


    private fun drawGuidelines(canvas: Canvas) {
        if (guidelinePaint != null) {
            val sw = if (borderPaint != null) borderPaint!!.strokeWidth else 0f
            val rect = imageCropWindowHandler.rect!!
            rect.inset(sw, sw)

            val oneThirdCropWidth = rect.width() / 3
            val oneThirdCropHeight = rect.height() / 3

            if (cropShapeView == CropShape.OVAL) {
                val w = rect.width() / 2 - sw
                val h = rect.height() / 2 - sw

                // Draw vertical guidelines.
                val x1 = rect.left + oneThirdCropWidth
                val x2 = rect.right - oneThirdCropWidth
                val yv = (h * sin(acos(((w - oneThirdCropWidth) / w).toDouble()))).toFloat()
                canvas.drawLine(x1, rect.top + h - yv, x1, rect.bottom - h + yv, guidelinePaint!!)
                canvas.drawLine(x2, rect.top + h - yv, x2, rect.bottom - h + yv, guidelinePaint!!)

                // Draw horizontal guidelines.
                val y1 = rect.top + oneThirdCropHeight
                val y2 = rect.bottom - oneThirdCropHeight
                val xv = (w * cos(asin(((h - oneThirdCropHeight) / h).toDouble()))).toFloat()
                canvas.drawLine(rect.left + w - xv, y1, rect.right - w + xv, y1, guidelinePaint!!)
                canvas.drawLine(rect.left + w - xv, y2, rect.right - w + xv, y2, guidelinePaint!!)
            } else {
                // Draw vertical guidelines.

                val x1 = rect.left + oneThirdCropWidth
                val x2 = rect.right - oneThirdCropWidth
                canvas.drawLine(x1, rect.top, x1, rect.bottom, guidelinePaint!!)
                canvas.drawLine(x2, rect.top, x2, rect.bottom, guidelinePaint!!)

                // Draw horizontal guidelines.
                val y1 = rect.top + oneThirdCropHeight
                val y2 = rect.bottom - oneThirdCropHeight
                canvas.drawLine(rect.left, y1, rect.right, y1, guidelinePaint!!)
                canvas.drawLine(rect.left, y2, rect.right, y2, guidelinePaint!!)
            }
        }
    }

    private fun drawBorders(canvas: Canvas) {
        if (borderPaint != null) {
            val w = borderPaint!!.strokeWidth
            val rect = imageCropWindowHandler.rect!!
            rect.inset(w / 2, w / 2)

            if (cropShapeView == CropShape.RECTANGLE) {
                // Draw rectangle crop window border.
                canvas.drawRect(rect, borderPaint!!)
            } else {
                // Draw circular crop window border
                canvas.drawOval(rect, borderPaint!!)
            }
        }
    }

    private fun drawCorners(canvas: Canvas) {
        if (borderCornerPaint != null) {
            val lineWidth = if (borderPaint != null) borderPaint!!.strokeWidth else 0f
            val cornerWidth = borderCornerPaint!!.strokeWidth

            // for rectangle crop shape we allow the corners to be offset from the borders
            val w = (
                    cornerWidth / 2
                            + (if (cropShapeView == CropShape.RECTANGLE) borderCornerOffset else 0f))

            val rect = imageCropWindowHandler.rect!!
            rect.inset(w, w)

            val cornerOffset = (cornerWidth - lineWidth) / 2
            val cornerExtension = cornerWidth / 2 + cornerOffset

            // Top left
            canvas.drawLine(
                rect.left - cornerOffset,
                rect.top - cornerExtension,
                rect.left - cornerOffset,
                rect.top + borderCornerLength,
                borderCornerPaint!!
            )
            canvas.drawLine(
                rect.left - cornerExtension,
                rect.top - cornerOffset,
                rect.left + borderCornerLength,
                rect.top - cornerOffset,
                borderCornerPaint!!
            )

            // Top right
            canvas.drawLine(
                rect.right + cornerOffset,
                rect.top - cornerExtension,
                rect.right + cornerOffset,
                rect.top + borderCornerLength,
                borderCornerPaint!!
            )
            canvas.drawLine(
                rect.right + cornerExtension,
                rect.top - cornerOffset,
                rect.right - borderCornerLength,
                rect.top - cornerOffset,
                borderCornerPaint!!
            )

            // Bottom left
            canvas.drawLine(
                rect.left - cornerOffset,
                rect.bottom + cornerExtension,
                rect.left - cornerOffset,
                rect.bottom - borderCornerLength,
                borderCornerPaint!!
            )
            canvas.drawLine(
                rect.left - cornerExtension,
                rect.bottom + cornerOffset,
                rect.left + borderCornerLength,
                rect.bottom + cornerOffset,
                borderCornerPaint!!
            )

            // Bottom left
            canvas.drawLine(
                rect.right + cornerOffset,
                rect.bottom + cornerExtension,
                rect.right + cornerOffset,
                rect.bottom - borderCornerLength,
                borderCornerPaint!!
            )
            canvas.drawLine(
                rect.right + cornerExtension,
                rect.bottom + cornerOffset,
                rect.right - borderCornerLength,
                rect.bottom + cornerOffset,
                borderCornerPaint!!
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // If this View is not enabled, don't allow for touch interactions.
        if (isEnabled) {
            if (multiTouchEnabled) {
                scaleDetector!!.onTouchEvent(event)
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDown(event.x, event.y)
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    onActionUp()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    onActionMove(event.x, event.y)
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }

                else -> return false
            }
        } else {
            return false
        }
    }

    private fun onActionDown(x: Float, y: Float) {
        moveHandler = imageCropWindowHandler.getMoveHandler(x, y, touchRadius, cropShapeView!!)
        if (moveHandler != null) {
            invalidate()
        }
    }

    private fun onActionUp() {
        if (moveHandler != null) {
            moveHandler = null
            callOnCropWindowChanged(false)
            invalidate()
        }
    }

    private fun onActionMove(x: Float, y: Float) {
        if (moveHandler != null) {
            var snapRadius = snapRadius
            val rect = imageCropWindowHandler.rect!!

            if (calculateBounds(rect)) {
                snapRadius = 0f
            }

            moveHandler!!.move(
                rect,
                x,
                y,
                calcBounds,
                viewWidth,
                viewHeight,
                snapRadius,
                isFixAspectRatio,
                targetAspectRatio
            )
            imageCropWindowHandler.rect = rect
            callOnCropWindowChanged(true)
            invalidate()
        }
    }

    private fun calculateBounds(rect: RectF): Boolean {
        var left = getRectLeft(boundsPoints)
        var top = getRectTop(boundsPoints)
        var right = getRectRight(boundsPoints)
        var bottom = getRectBottom(boundsPoints)

        if (!isNonStraightAngleRotated) {
            calcBounds[left, top, right] = bottom
            return false
        } else {
            var x0 = boundsPoints[0]
            var y0 = boundsPoints[1]
            var x2 = boundsPoints[4]
            var y2 = boundsPoints[5]
            var x3 = boundsPoints[6]
            var y3 = boundsPoints[7]

            if (boundsPoints[7] < boundsPoints[1]) {
                if (boundsPoints[1] < boundsPoints[3]) {
                    x0 = boundsPoints[6]
                    y0 = boundsPoints[7]
                    x2 = boundsPoints[2]
                    y2 = boundsPoints[3]
                    x3 = boundsPoints[4]
                    y3 = boundsPoints[5]
                } else {
                    x0 = boundsPoints[4]
                    y0 = boundsPoints[5]
                    x2 = boundsPoints[0]
                    y2 = boundsPoints[1]
                    x3 = boundsPoints[2]
                    y3 = boundsPoints[3]
                }
            } else if (boundsPoints[1] > boundsPoints[3]) {
                x0 = boundsPoints[2]
                y0 = boundsPoints[3]
                x2 = boundsPoints[6]
                y2 = boundsPoints[7]
                x3 = boundsPoints[0]
                y3 = boundsPoints[1]
            }

            val a0 = (y3 - y0) / (x3 - x0)
            val a1 = -1f / a0
            val b0 = y0 - a0 * x0
            val b1 = y0 - a1 * x0
            val b2 = y2 - a0 * x2
            val b3 = y2 - a1 * x2

            val c0 = (rect.centerY() - rect.top) / (rect.centerX() - rect.left)
            val c1 = -c0
            val d0 = rect.top - c0 * rect.left
            val d1 = rect.top - c1 * rect.right

            left = max(
                left.toDouble(),
                (if ((d0 - b0) / (a0 - c0) < rect.right) (d0 - b0) / (a0 - c0) else left).toDouble()
            ).toFloat()
            left = max(
                left.toDouble(),
                (if ((d0 - b1) / (a1 - c0) < rect.right) (d0 - b1) / (a1 - c0) else left).toDouble()
            ).toFloat()
            left = max(
                left.toDouble(),
                (if ((d1 - b3) / (a1 - c1) < rect.right) (d1 - b3) / (a1 - c1) else left).toDouble()
            ).toFloat()
            right = min(
                right.toDouble(),
                (if ((d1 - b1) / (a1 - c1) > rect.left) (d1 - b1) / (a1 - c1) else right).toDouble()
            ).toFloat()
            right = min(
                right.toDouble(),
                (if ((d1 - b2) / (a0 - c1) > rect.left) (d1 - b2) / (a0 - c1) else right).toDouble()
            ).toFloat()
            right = min(
                right.toDouble(),
                (if ((d0 - b2) / (a0 - c0) > rect.left) (d0 - b2) / (a0 - c0) else right).toDouble()
            ).toFloat()

            top = max(
                top.toDouble(),
                max((a0 * left + b0).toDouble(), (a1 * right + b1).toDouble())
            ).toFloat()
            bottom = min(
                bottom.toDouble(),
                min((a1 * left + b3).toDouble(), (a0 * right + b2).toDouble())
            ).toFloat()

            calcBounds.left = left
            calcBounds.top = top
            calcBounds.right = right
            calcBounds.bottom = bottom
            return true
        }
    }

    private val isNonStraightAngleRotated: Boolean
        get() = boundsPoints[0] != boundsPoints[6] && boundsPoints[1] != boundsPoints[7]

    private fun callOnCropWindowChanged(inProgress: Boolean) {
        try {
            if (cropWindowChangeListener != null) {
                cropWindowChangeListener!!.onCropWindowChanged(inProgress)
            }
        } catch (e: Exception) {
            Log.e("AIC", "Exception in crop window changed", e)
        }
    }

    // endregion
    // region: Inner class: CropWindowChangeListener
    interface CropWindowChangeListener {

        fun onCropWindowChanged(inProgress: Boolean)
    }

    // endregion
    // region: Inner class: ScaleListener
    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val rect = imageCropWindowHandler.rect

            val x = detector.focusX
            val y = detector.focusY
            val dY = detector.currentSpanY / 2
            val dX = detector.currentSpanX / 2

            val newTop = y - dY
            val newLeft = x - dX
            val newRight = x + dX
            val newBottom = y + dY

            if (newLeft < newRight && newTop <= newBottom && newLeft >= 0 && newRight <= imageCropWindowHandler.maxCropWidth && newTop >= 0 && newBottom <= imageCropWindowHandler.maxCropHeight) {
                rect?.set(newLeft, newTop, newRight, newBottom)
                imageCropWindowHandler.rect = rect
                invalidate()
            }

            return true
        }
    } // endregion

    companion object {
        private fun getNewPaint(color: Int): Paint {
            val paint = Paint()
            paint.color = color
            return paint
        }

        private fun getNewPaintOrNull(thickness: Float, color: Int): Paint? {
            if (thickness > 0) {
                val borderPaint = Paint()
                borderPaint.color = color
                borderPaint.strokeWidth = thickness
                borderPaint.style = Paint.Style.STROKE
                borderPaint.isAntiAlias = true
                return borderPaint
            } else {
                return null
            }
        }
    }
}
