package com.crudmehra.imagecropper.utils

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min


internal class ImageCroppingHandler(
    private val typeValueInfo: Type, imageCropWindowHandler: ImageCropWindowHandler, touchX: Float, touchY: Float
) {
    private val minCropWid = imageCropWindowHandler.minCropWidth

    private val minCropHigh = imageCropWindowHandler.minCropHeight

    private val maxCropWid = imageCropWindowHandler.maxCropWidth

    private val maxCropHigh = imageCropWindowHandler.maxCropHeight


    private val touchOffset = PointF()

    // endregion
    init {
        calculateTouchOffset(imageCropWindowHandler.rect, touchX, touchY)
    }


    fun move(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapMargin: Float,
        fixedAspectRatio: Boolean,
        aspectRatio: Float
    ) {
        val adjX = x + touchOffset.x
        val adjY = y + touchOffset.y

        if (typeValueInfo == Type.CENTER) {
            moveCenter(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin)
        } else {
            if (fixedAspectRatio) {
                moveSizeWithFixedAspectRatio(
                    rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin, aspectRatio
                )
            } else {
                moveSizeWithFreeAspectRatio(
                    rect,
                    adjX,
                    adjY,
                    bounds,
                    viewWidth,
                    viewHeight,
                    snapMargin
                )
            }
        }
    }


    private fun calculateTouchOffset(rect: RectF?, touchX: Float, touchY: Float) {
        var touchOffsetX = 0f
        var touchOffsetY = 0f

        when (typeValueInfo) {
            Type.TOP_LEFT -> {
                touchOffsetX = rect!!.left - touchX
                touchOffsetY = rect.top - touchY
            }

            Type.TOP_RIGHT -> {
                touchOffsetX = rect!!.right - touchX
                touchOffsetY = rect.top - touchY
            }

            Type.BOTTOM_LEFT -> {
                touchOffsetX = rect!!.left - touchX
                touchOffsetY = rect.bottom - touchY
            }

            Type.BOTTOM_RIGHT -> {
                touchOffsetX = rect!!.right - touchX
                touchOffsetY = rect.bottom - touchY
            }

            Type.LEFT -> {
                touchOffsetX = rect!!.left - touchX
                touchOffsetY = 0f
            }

            Type.TOP -> {
                touchOffsetX = 0f
                touchOffsetY = rect!!.top - touchY
            }

            Type.RIGHT -> {
                touchOffsetX = rect!!.right - touchX
                touchOffsetY = 0f
            }

            Type.BOTTOM -> {
                touchOffsetX = 0f
                touchOffsetY = rect!!.bottom - touchY
            }

            Type.CENTER -> {
                touchOffsetX = rect!!.centerX() - touchX
                touchOffsetY = rect.centerY() - touchY
            }
        }
        touchOffset.x = touchOffsetX
        touchOffset.y = touchOffsetY
    }

    private fun moveCenter(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapRadius: Float
    ) {
        var dx = x - rect.centerX()
        var dy = y - rect.centerY()
        if (rect.left + dx < 0 || rect.right + dx > viewWidth || rect.left + dx < bounds.left || rect.right + dx > bounds.right) {
            dx /= 1.05f
            touchOffset.x -= dx / 2
        }
        if (rect.top + dy < 0 || rect.bottom + dy > viewHeight || rect.top + dy < bounds.top || rect.bottom + dy > bounds.bottom) {
            dy /= 1.05f
            touchOffset.y -= dy / 2
        }
        rect.offset(dx, dy)
        snapEdgesToBounds(rect, bounds, snapRadius)
    }

    private fun moveSizeWithFreeAspectRatio(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapMargin: Float
    ) {
        when (typeValueInfo) {
            Type.TOP_LEFT -> {
                adjustTop(rect, y, bounds, snapMargin, 0f, leftMoves = false, rightMoves = false)
                adjustLeft(rect, x, bounds, snapMargin, 0f, topMoves = false, bottomMoves = false)
            }

            Type.TOP_RIGHT -> {
                adjustTop(rect, y, bounds, snapMargin, 0f, leftMoves = false, rightMoves = false)
                adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f,
                    topMoves = false,
                    bottomMoves = false
                )
            }

            Type.BOTTOM_LEFT -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f,
                    leftMoves = false,
                    rightMoves = false
                )
                adjustLeft(rect, x, bounds, snapMargin, 0f, topMoves = false, bottomMoves = false)
            }

            Type.BOTTOM_RIGHT -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f,
                    leftMoves = false,
                    rightMoves = false
                )
                adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f,
                    topMoves = false,
                    bottomMoves = false
                )
            }

            Type.LEFT -> adjustLeft(rect, x, bounds, snapMargin, 0f,
                topMoves = false,
                bottomMoves = false
            )
            Type.TOP -> adjustTop(rect, y, bounds, snapMargin, 0f,
                leftMoves = false,
                rightMoves = false
            )
            Type.RIGHT -> adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f,
                topMoves = false,
                bottomMoves = false
            )
            Type.BOTTOM -> adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f,
                leftMoves = false,
                rightMoves = false
            )
            else -> {}
        }
    }


    private fun moveSizeWithFixedAspectRatio(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapMargin: Float,
        aspectRatio: Float
    ) {
        when (typeValueInfo) {
            Type.TOP_LEFT -> if (calculateAspectRatio(
                    x,
                    y,
                    rect.right,
                    rect.bottom
                ) < aspectRatio
            ) {
                adjustTop(rect, y, bounds, snapMargin, aspectRatio,
                    leftMoves = true,
                    rightMoves = false
                )
                adjustLeftByAspectRatio(rect, aspectRatio)
            } else {
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio,
                    topMoves = true,
                    bottomMoves = false
                )
                adjustTopByAspectRatio(rect, aspectRatio)
            }

            Type.TOP_RIGHT -> if (calculateAspectRatio(
                    rect.left,
                    y,
                    x,
                    rect.bottom
                ) < aspectRatio
            ) {
                adjustTop(rect, y, bounds, snapMargin, aspectRatio,
                    leftMoves = false,
                    rightMoves = true
                )
                adjustRightByAspectRatio(rect, aspectRatio)
            } else {
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio,
                    topMoves = true,
                    bottomMoves = false
                )
                adjustTopByAspectRatio(rect, aspectRatio)
            }

            Type.BOTTOM_LEFT -> if (calculateAspectRatio(
                    x,
                    rect.top,
                    rect.right,
                    y
                ) < aspectRatio
            ) {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio,
                    leftMoves = true,
                    rightMoves = false
                )
                adjustLeftByAspectRatio(rect, aspectRatio)
            } else {
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio,
                    topMoves = false,
                    bottomMoves = true
                )
                adjustBottomByAspectRatio(rect, aspectRatio)
            }

            Type.BOTTOM_RIGHT -> if (calculateAspectRatio(
                    rect.left,
                    rect.top,
                    x,
                    y
                ) < aspectRatio
            ) {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio,
                    leftMoves = false,
                    rightMoves = true
                )
                adjustRightByAspectRatio(rect, aspectRatio)
            } else {
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio,
                    topMoves = false,
                    bottomMoves = true
                )
                adjustBottomByAspectRatio(rect, aspectRatio)
            }

            Type.LEFT -> {
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio,
                    topMoves = true,
                    bottomMoves = true
                )
                adjustTopBottomByAspectRatio(rect, bounds, aspectRatio)
            }

            Type.TOP -> {
                adjustTop(rect, y, bounds, snapMargin, aspectRatio,
                    leftMoves = true,
                    rightMoves = true
                )
                adjustLeftRightByAspectRatio(rect, bounds, aspectRatio)
            }

            Type.RIGHT -> {
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio,
                    topMoves = true,
                    bottomMoves = true
                )
                adjustTopBottomByAspectRatio(rect, bounds, aspectRatio)
            }

            Type.BOTTOM -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio,
                    leftMoves = true,
                    rightMoves = true
                )
                adjustLeftRightByAspectRatio(rect, bounds, aspectRatio)
            }

            else -> {}
        }
    }

    private fun snapEdgesToBounds(edges: RectF, bounds: RectF, margin: Float) {
        if (edges.left < bounds.left + margin) {
            edges.offset(bounds.left - edges.left, 0f)
        }
        if (edges.top < bounds.top + margin) {
            edges.offset(0f, bounds.top - edges.top)
        }
        if (edges.right > bounds.right - margin) {
            edges.offset(bounds.right - edges.right, 0f)
        }
        if (edges.bottom > bounds.bottom - margin) {
            edges.offset(0f, bounds.bottom - edges.bottom)
        }
    }

    private fun adjustLeft(
        rect: RectF,
        left: Float,
        bounds: RectF,
        snapMargin: Float,
        aspectRatio: Float,
        topMoves: Boolean,
        bottomMoves: Boolean
    ) {
        var newLeft = left

        if (newLeft < 0) {
            newLeft /= 1.05f
            touchOffset.x -= newLeft / 1.1f
        }

        if (newLeft < bounds.left) {
            touchOffset.x -= (newLeft - bounds.left) / 2f
        }

        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left
        }

        // Checks if the window is too small horizontally
        if (rect.right - newLeft < minCropWid) {
            newLeft = rect.right - minCropWid
        }

        // Checks if the window is too large horizontally
        if (rect.right - newLeft > maxCropWid) {
            newLeft = rect.right - maxCropWid
        }

        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left
        }

        // check vertical bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newHeight = (rect.right - newLeft) / aspectRatio

            // Checks if the window is too small vertically
            if (newHeight < minCropHigh) {
                newLeft = max(
                    bounds.left.toDouble(),
                    (rect.right - minCropHigh * aspectRatio).toDouble()
                )
                    .toFloat()
                newHeight = ((rect.right - newLeft) / aspectRatio)
            }

            // Checks if the window is too large vertically
            if (newHeight > maxCropHigh) {
                newLeft = max(
                    bounds.left.toDouble(),
                    (rect.right - maxCropHigh * aspectRatio).toDouble()
                )
                    .toFloat()
                newHeight = ((rect.right - newLeft) / aspectRatio)
            }

            // if top AND bottom edge moves by aspect ratio check that it is within full height bounds
            if (topMoves && bottomMoves) {
                newLeft = max(
                    newLeft.toDouble(),
                    max(
                        bounds.left.toDouble(),
                        (rect.right - bounds.height() * aspectRatio).toDouble()
                    )
                ).toFloat()
            } else {
                // if top edge moves by aspect ratio check that it is within bounds
                if (topMoves && rect.bottom - newHeight < bounds.top) {
                    newLeft = max(
                        bounds.left.toDouble(),
                        (rect.right - (rect.bottom - bounds.top) * aspectRatio).toDouble()
                    ).toFloat()
                    newHeight = ((rect.right - newLeft) / aspectRatio)
                }

                // if bottom edge moves by aspect ratio check that it is within bounds
                if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                    newLeft = max(
                        newLeft.toDouble(),
                        max(
                            bounds.left.toDouble(),
                            (rect.right - (bounds.bottom - rect.top) * aspectRatio).toDouble()
                        )
                    ).toFloat()
                }
            }
        }

        rect.left = newLeft
    }

    private fun adjustRight(
        rect: RectF,
        right: Float,
        bounds: RectF,
        viewWidth: Int,
        snapMargin: Float,
        aspectRatio: Float,
        topMoves: Boolean,
        bottomMoves: Boolean
    ) {
        var newRight = right

        if (newRight > viewWidth) {
            newRight = viewWidth + (newRight - viewWidth) / 1.05f
            touchOffset.x -= (newRight - viewWidth) / 1.1f
        }

        if (newRight > bounds.right) {
            touchOffset.x -= (newRight - bounds.right) / 2f
        }

        // If close to the edge
        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right
        }

        // Checks if the window is too small horizontally
        if (newRight - rect.left < minCropWid) {
            newRight = rect.left + minCropWid
        }

        // Checks if the window is too large horizontally
        if (newRight - rect.left > maxCropWid) {
            newRight = rect.left + maxCropWid
        }

        // If close to the edge
        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right
        }

        // check vertical bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newHeight = (newRight - rect.left) / aspectRatio

            // Checks if the window is too small vertically
            if (newHeight < minCropHigh) {
                newRight = min(
                    bounds.right.toDouble(),
                    (rect.left + minCropHigh * aspectRatio).toDouble()
                )
                    .toFloat()
                newHeight = (newRight - rect.left) / aspectRatio
            }

            // Checks if the window is too large vertically
            if (newHeight > maxCropHigh) {
                newRight = min(
                    bounds.right.toDouble(),
                    (rect.left + maxCropHigh * aspectRatio).toDouble()
                )
                    .toFloat()
                newHeight = (newRight - rect.left) / aspectRatio
            }

            // if top AND bottom edge moves by aspect ratio check that it is within full height bounds
            if (topMoves && bottomMoves) {
                newRight = min(
                    newRight.toDouble(),
                    min(
                        bounds.right.toDouble(),
                        (rect.left + bounds.height() * aspectRatio).toDouble()
                    )
                ).toFloat()
            } else {
                // if top edge moves by aspect ratio check that it is within bounds
                if (topMoves && rect.bottom - newHeight < bounds.top) {
                    newRight = min(
                        bounds.right.toDouble(),
                        (rect.left + (rect.bottom - bounds.top) * aspectRatio).toDouble()
                    ).toFloat()
                    newHeight = (newRight - rect.left) / aspectRatio
                }

                // if bottom edge moves by aspect ratio check that it is within bounds
                if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                    newRight = min(
                        newRight.toDouble(),
                        min(
                            bounds.right.toDouble(),
                            (rect.left + (bounds.bottom - rect.top) * aspectRatio).toDouble()
                        )
                    ).toFloat()
                }
            }
        }

        rect.right = newRight
    }

    private fun adjustTop(
        rect: RectF,
        top: Float,
        bounds: RectF,
        snapMargin: Float,
        aspectRatio: Float,
        leftMoves: Boolean,
        rightMoves: Boolean
    ) {
        var newTop = top

        if (newTop < 0) {
            newTop /= 1.05f
            touchOffset.y -= newTop / 1.1f
        }

        if (newTop < bounds.top) {
            touchOffset.y -= (newTop - bounds.top) / 2f
        }

        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top
        }

        // Checks if the window is too small vertically
        if (rect.bottom - newTop < minCropHigh) {
            newTop = rect.bottom - minCropHigh
        }

        // Checks if the window is too large vertically
        if (rect.bottom - newTop > maxCropHigh) {
            newTop = rect.bottom - maxCropHigh
        }

        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top
        }

        // check horizontal bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newWidth = (rect.bottom - newTop) * aspectRatio

            // Checks if the crop window is too small horizontally due to aspect ratio adjustment
            if (newWidth < minCropWid) {
                newTop = max(
                    bounds.top.toDouble(),
                    (rect.bottom - (minCropWid / aspectRatio)).toDouble()
                ).toFloat()
                newWidth = ((rect.bottom - newTop) * aspectRatio)
            }

            // Checks if the crop window is too large horizontally due to aspect ratio adjustment
            if (newWidth > maxCropWid) {
                newTop = max(
                    bounds.top.toDouble(),
                    (rect.bottom - (maxCropWid / aspectRatio)).toDouble()
                ).toFloat()
                newWidth = ((rect.bottom - newTop) * aspectRatio)
            }

            // if left AND right edge moves by aspect ratio check that it is within full width bounds
            if (leftMoves && rightMoves) {
                newTop = max(
                    newTop.toDouble(),
                    max(
                        bounds.top.toDouble(),
                        (rect.bottom - bounds.width() / aspectRatio).toDouble()
                    )
                ).toFloat()
            } else {
                // if left edge moves by aspect ratio check that it is within bounds
                if (leftMoves && rect.right - newWidth < bounds.left) {
                    newTop = max(
                        bounds.top.toDouble(),
                        (rect.bottom - (rect.right - bounds.left) / aspectRatio).toDouble()
                    ).toFloat()
                    newWidth = ((rect.bottom - newTop) * aspectRatio)
                }

                // if right edge moves by aspect ratio check that it is within bounds
                if (rightMoves && rect.left + newWidth > bounds.right) {
                    newTop = max(
                        newTop.toDouble(),
                        max(
                            bounds.top.toDouble(),
                            (rect.bottom - (bounds.right - rect.left) / aspectRatio).toDouble()
                        )
                    ).toFloat()
                }
            }
        }

        rect.top = newTop
    }

    private fun adjustBottom(
        rect: RectF,
        bottom: Float,
        bounds: RectF,
        viewHeight: Int,
        snapMargin: Float,
        aspectRatio: Float,
        leftMoves: Boolean,
        rightMoves: Boolean
    ) {
        var newBottom = bottom

        if (newBottom > viewHeight) {
            newBottom = viewHeight + (newBottom - viewHeight) / 1.05f
            touchOffset.y -= (newBottom - viewHeight) / 1.1f
        }

        if (newBottom > bounds.bottom) {
            touchOffset.y -= (newBottom - bounds.bottom) / 2f
        }

        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom
        }

        // Checks if the window is too small vertically
        if (newBottom - rect.top < minCropHigh) {
            newBottom = rect.top + minCropHigh
        }

        // Checks if the window is too small vertically
        if (newBottom - rect.top > maxCropHigh) {
            newBottom = rect.top + maxCropHigh
        }

        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom
        }

        // check horizontal bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newWidth = (newBottom - rect.top) * aspectRatio

            // Checks if the window is too small horizontally
            if (newWidth < minCropWid) {
                newBottom = min(
                    bounds.bottom.toDouble(),
                    (rect.top + minCropWid / aspectRatio).toDouble()
                )
                    .toFloat()
                newWidth = (newBottom - rect.top) * aspectRatio
            }

            // Checks if the window is too large horizontally
            if (newWidth > maxCropWid) {
                newBottom = min(
                    bounds.bottom.toDouble(),
                    (rect.top + maxCropWid / aspectRatio).toDouble()
                )
                    .toFloat()
                newWidth = (newBottom - rect.top) * aspectRatio
            }

            // if left AND right edge moves by aspect ratio check that it is within full width bounds
            if (leftMoves && rightMoves) {
                newBottom = min(
                    newBottom.toDouble(),
                    min(
                        bounds.bottom.toDouble(),
                        (rect.top + bounds.width() / aspectRatio).toDouble()
                    )
                ).toFloat()
            } else {
                // if left edge moves by aspect ratio check that it is within bounds
                if (leftMoves && rect.right - newWidth < bounds.left) {
                    newBottom = min(
                        bounds.bottom.toDouble(),
                        (rect.top + (rect.right - bounds.left) / aspectRatio).toDouble()
                    ).toFloat()
                    newWidth = (newBottom - rect.top) * aspectRatio
                }

                // if right edge moves by aspect ratio check that it is within bounds
                if (rightMoves && rect.left + newWidth > bounds.right) {
                    newBottom = min(
                        newBottom.toDouble(),
                        min(
                            bounds.bottom.toDouble(),
                            (rect.top + (bounds.right - rect.left) / aspectRatio).toDouble()
                        )
                    ).toFloat()
                }
            }
        }

        rect.bottom = newBottom
    }


    private fun adjustLeftByAspectRatio(rect: RectF, aspectRatio: Float) {
        rect.left = rect.right - rect.height() * aspectRatio
    }


    private fun adjustTopByAspectRatio(rect: RectF, aspectRatio: Float) {
        rect.top = rect.bottom - rect.width() / aspectRatio
    }


    private fun adjustRightByAspectRatio(rect: RectF, aspectRatio: Float) {
        rect.right = rect.left + rect.height() * aspectRatio
    }

    private fun adjustBottomByAspectRatio(rect: RectF, aspectRatio: Float) {
        rect.bottom = rect.top + rect.width() / aspectRatio
    }


    private fun adjustLeftRightByAspectRatio(rect: RectF, bounds: RectF, aspectRatio: Float) {
        rect.inset((rect.width() - rect.height() * aspectRatio) / 2, 0f)
        if (rect.left < bounds.left) {
            rect.offset(bounds.left - rect.left, 0f)
        }
        if (rect.right > bounds.right) {
            rect.offset(bounds.right - rect.right, 0f)
        }
    }

    private fun adjustTopBottomByAspectRatio(rect: RectF, bounds: RectF, aspectRatio: Float) {
        rect.inset(0f, (rect.height() - rect.width() / aspectRatio) / 2)
        if (rect.top < bounds.top) {
            rect.offset(0f, bounds.top - rect.top)
        }
        if (rect.bottom > bounds.bottom) {
            rect.offset(0f, bounds.bottom - rect.bottom)
        }
    }


    enum class Type {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        LEFT,
        TOP,
        RIGHT,
        BOTTOM,
        CENTER
    } // endregion

    companion object {
        private fun calculateAspectRatio(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
        ): Float {
            return (right - left) / (bottom - top)
        }
    }
}
