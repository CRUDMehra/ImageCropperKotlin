package com.crudmehra.imagecropper.utils

import android.graphics.Matrix
import android.graphics.RectF
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView

internal class AnimImageCrop(
    private val imageView: ImageView,
    private val cropOverlayUI: CropOverlayUI
) : Animation(), Animation.AnimationListener {
    private val startBoundPoints = FloatArray(8)

    private val endBoundPoints = FloatArray(8)

    private val startCropWindowRect = RectF()

    private val endCropWindowRect = RectF()

    private val startImageMatrix = FloatArray(9)

    private val endImageMatrix = FloatArray(9)

    private val animRect = RectF()

    private val animPoints = FloatArray(8)

    private val animMatrix = FloatArray(9)

    init {
        duration = 300
        fillAfter = true
        interpolator = AccelerateDecelerateInterpolator()
        setAnimationListener(this)
    }

    fun setStartAnim(boundPoints: FloatArray?, imageMatrix: Matrix) {
        reset()
        if (boundPoints != null) {
            System.arraycopy(boundPoints, 0, startBoundPoints, 0, 8)
        }
        startCropWindowRect.set(cropOverlayUI.cropWindowRect!!)
        imageMatrix.getValues(startImageMatrix)
    }

    fun setEndAnim(boundPoints: FloatArray?, imageMatrix: Matrix) {
        if (boundPoints != null) {
            System.arraycopy(boundPoints, 0, endBoundPoints, 0, 8)
        }
        endCropWindowRect.set(cropOverlayUI.cropWindowRect!!)
        imageMatrix.getValues(endImageMatrix)
    }

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        animRect.left = (
                startCropWindowRect.left
                        + (endCropWindowRect.left - startCropWindowRect.left) * interpolatedTime)
        animRect.top = (
                startCropWindowRect.top
                        + (endCropWindowRect.top - startCropWindowRect.top) * interpolatedTime)
        animRect.right = (
                startCropWindowRect.right
                        + (endCropWindowRect.right - startCropWindowRect.right) * interpolatedTime)
        animRect.bottom = (
                startCropWindowRect.bottom
                        + (endCropWindowRect.bottom - startCropWindowRect.bottom) * interpolatedTime)
        cropOverlayUI.cropWindowRect = animRect

        for (i in animPoints.indices) {
            animPoints[i] =
                startBoundPoints[i] + (endBoundPoints[i] - startBoundPoints[i]) * interpolatedTime
        }
        cropOverlayUI.setBounds(animPoints, imageView.width, imageView.height)

        for (i in animMatrix.indices) {
            animMatrix[i] =
                startImageMatrix[i] + (endImageMatrix[i] - startImageMatrix[i]) * interpolatedTime
        }
        val m = imageView.imageMatrix
        m.setValues(animMatrix)
        imageView.imageMatrix = m

        imageView.invalidate()
        cropOverlayUI.invalidate()
    }

    override fun onAnimationStart(animation: Animation) {}

    override fun onAnimationEnd(animation: Animation) {
        imageView.clearAnimation()
    }

    override fun onAnimationRepeat(animation: Animation) {}
}
