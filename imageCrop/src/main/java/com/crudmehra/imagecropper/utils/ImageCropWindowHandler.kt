package com.crudmehra.imagecropper.utils

import android.graphics.RectF
import com.crudmehra.imagecropper.utils.ImageCropUI.CropShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class ImageCropWindowHandler {
    private val edgesView = RectF()

    private val getEdgesView = RectF()

    private var minCropWindowWidth = 0f

    private var minCropWindowHeight = 0f

    private var maxCropWindowWidth = 0f

    private var maxCropWindowHeight = 0f

    private var minCropResultWidth = 0f

    private var minCropResultHeight = 0f

    private var maxCropResultWidth = 0f

    private var maxCropResultHeight = 0f

    var scaleFactorWidth: Float = 1f
        private set

    var scaleFactorHeight: Float = 1f
        private set

    // endregion
    var rect: RectF?
        get() {
            getEdgesView.set(edgesView)
            return getEdgesView
        }
        set(rect) {
            edgesView.set(rect!!)
        }

    val minCropWidth: Float
        get() = max(
            minCropWindowWidth.toDouble(),
            (minCropResultWidth / scaleFactorWidth).toDouble()
        ).toFloat()

    val minCropHeight: Float
        get() = max(
            minCropWindowHeight.toDouble(),
            (minCropResultHeight / scaleFactorHeight).toDouble()
        ).toFloat()

    val maxCropWidth: Float
        get() = min(
            maxCropWindowWidth.toDouble(),
            (maxCropResultWidth / scaleFactorWidth).toDouble()
        ).toFloat()

    val maxCropHeight: Float
        get() = min(
            maxCropWindowHeight.toDouble(),
            (maxCropResultHeight / scaleFactorHeight).toDouble()
        ).toFloat()


    fun setMinCropResultSize(minCropResultWidth: Int, minCropResultHeight: Int) {
        this.minCropResultWidth = minCropResultWidth.toFloat()
        this.minCropResultHeight = minCropResultHeight.toFloat()
    }


    fun setMaxCropResultSize(maxCropResultWidth: Int, maxCropResultHeight: Int) {
        this.maxCropResultWidth = maxCropResultWidth.toFloat()
        this.maxCropResultHeight = maxCropResultHeight.toFloat()
    }

    fun setCropWindowLimits(
        maxWidth: Float, maxHeight: Float, scaleFactorWidth: Float, scaleFactorHeight: Float
    ) {
        maxCropWindowWidth = maxWidth
        maxCropWindowHeight = maxHeight
        this.scaleFactorWidth = scaleFactorWidth
        this.scaleFactorHeight = scaleFactorHeight
    }

    fun setInitialAttributeValues(options: ImageCropParams) {
        minCropWindowWidth = options.minCropWindowWidth.toFloat()
        minCropWindowHeight = options.minCropWindowHeight.toFloat()
        minCropResultWidth = options.minCropResultWidth.toFloat()
        minCropResultHeight = options.minCropResultHeight.toFloat()
        maxCropResultWidth = options.maxCropResultWidth.toFloat()
        maxCropResultHeight = options.maxCropResultHeight.toFloat()
    }

    fun showGuidelines(): Boolean {
        return !(edgesView.width() < 100 || edgesView.height() < 100)
    }


    fun getMoveHandler(
        x: Float, y: Float, targetRadius: Float, cropShape: CropShape
    ): ImageCroppingHandler? {
        val type =
            if (cropShape == CropShape.OVAL
            ) getOvalPressedMoveType(x, y)
            else getRectanglePressedMoveType(x, y, targetRadius)
        return if (type != null) ImageCroppingHandler(type, this, x, y) else null
    }


    private fun getRectanglePressedMoveType(
        x: Float, y: Float, targetRadius: Float
    ): ImageCroppingHandler.Type? {
        var moveType: ImageCroppingHandler.Type? = null

        // Note: corner-handles take precedence, then side-handles, then center.
        if (isInCornerTargetZone(x, y, edgesView.left, edgesView.top, targetRadius)) {
            moveType = ImageCroppingHandler.Type.TOP_LEFT
        } else if (isInCornerTargetZone(
                x, y, edgesView.right, edgesView.top, targetRadius
            )
        ) {
            moveType = ImageCroppingHandler.Type.TOP_RIGHT
        } else if (isInCornerTargetZone(
                x, y, edgesView.left, edgesView.bottom, targetRadius
            )
        ) {
            moveType = ImageCroppingHandler.Type.BOTTOM_LEFT
        } else if (isInCornerTargetZone(
                x, y, edgesView.right, edgesView.bottom, targetRadius
            )
        ) {
            moveType = ImageCroppingHandler.Type.BOTTOM_RIGHT
        } else if (isInCenterTargetZone(
                x, y, edgesView.left, edgesView.top, edgesView.right, edgesView.bottom
            )
            && focusCenter()
        ) {
            moveType = ImageCroppingHandler.Type.CENTER
        } else if (isInHorizontalTargetZone(
                x, y, edgesView.left, edgesView.right, edgesView.top, targetRadius
            )
        ) {
            moveType = ImageCroppingHandler.Type.TOP
        } else if (isInHorizontalTargetZone(
                x, y, edgesView.left, edgesView.right, edgesView.bottom, targetRadius
            )
        ) {
            moveType = ImageCroppingHandler.Type.BOTTOM
        } else if (isInVerticalTargetZone(
                x, y, edgesView.left, edgesView.top, edgesView.bottom, targetRadius
            )
        ) {
            moveType = ImageCroppingHandler.Type.LEFT
        } else if (isInVerticalTargetZone(
                x, y, edgesView.right, edgesView.top, edgesView.bottom, targetRadius
            )
        ) {
            moveType = ImageCroppingHandler.Type.RIGHT
        } else if (isInCenterTargetZone(
                x, y, edgesView.left, edgesView.top, edgesView.right, edgesView.bottom
            )
            && !focusCenter()
        ) {
            moveType = ImageCroppingHandler.Type.CENTER
        }

        return moveType
    }


    private fun getOvalPressedMoveType(x: Float, y: Float): ImageCroppingHandler.Type {
        /*
       Use a 6x6 grid system divided into 9 "handles", with the center the biggest region. While
       this is not perfect, it's a good quick-to-ship approach.

       TL T T T T TR
        L C C C C R
        L C C C C R
        L C C C C R
        L C C C C R
       BL B B B B BR
    */

        val cellLength = edgesView.width() / 6
        val leftCenter = edgesView.left + cellLength
        val rightCenter = edgesView.left + (5 * cellLength)

        val cellHeight = edgesView.height() / 6
        val topCenter = edgesView.top + cellHeight
        val bottomCenter = edgesView.top + 5 * cellHeight
        val moveType = if (x < leftCenter) {
            if (y < topCenter) {
                ImageCroppingHandler.Type.TOP_LEFT
            } else if (y < bottomCenter) {
                ImageCroppingHandler.Type.LEFT
            } else {
                ImageCroppingHandler.Type.BOTTOM_LEFT
            }
        } else if (x < rightCenter) {
            if (y < topCenter) {
                ImageCroppingHandler.Type.TOP
            } else if (y < bottomCenter) {
                ImageCroppingHandler.Type.CENTER
            } else {
                ImageCroppingHandler.Type.BOTTOM
            }
        } else {
            if (y < topCenter) {
                ImageCroppingHandler.Type.TOP_RIGHT
            } else if (y < bottomCenter) {
                ImageCroppingHandler.Type.RIGHT
            } else {
                ImageCroppingHandler.Type.BOTTOM_RIGHT
            }
        }

        return moveType
    }


    private fun focusCenter(): Boolean {
        return !showGuidelines()
    } // endregion

    companion object {

        private fun isInCornerTargetZone(
            x: Float, y: Float, handleX: Float, handleY: Float, targetRadius: Float
        ): Boolean {
            return abs((x - handleX).toDouble()) <= targetRadius && abs((y - handleY).toDouble()) <= targetRadius
        }


        private fun isInHorizontalTargetZone(
            x: Float,
            y: Float,
            handleXStart: Float,
            handleXEnd: Float,
            handleY: Float,
            targetRadius: Float
        ): Boolean {
            return x > handleXStart && x < handleXEnd && abs((y - handleY).toDouble()) <= targetRadius
        }


        private fun isInVerticalTargetZone(
            x: Float,
            y: Float,
            handleX: Float,
            handleYStart: Float,
            handleYEnd: Float,
            targetRadius: Float
        ): Boolean {
            return abs((x - handleX).toDouble()) <= targetRadius && y > handleYStart && y < handleYEnd
        }


        private fun isInCenterTargetZone(
            x: Float, y: Float, left: Float, top: Float, right: Float, bottom: Float
        ): Boolean {
            return x > left && x < right && y > top && y < bottom
        }
    }
}
