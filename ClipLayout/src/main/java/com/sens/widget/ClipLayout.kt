package com.sens.widget

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Created by Sens on 2021/12/12.
 * {@see <a href="https://github.com/senswrong/ClipLayout">ClipLayout</a>}
 */
open class ClipMask @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr)

class ClipLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    //////////////////////////////<refresh>//////////////////////////////
    init {
        var needInvalidate = false
        viewTreeObserver.addOnPreDrawListener {
            //first PreDraw by children then invalidate by self (skip self)
            needInvalidate = !needInvalidate
            if (!needInvalidate) return@addOnPreDrawListener true
            invalidate()
            true
        }
    }

    //////////////////////////////<draw>//////////////////////////////
    private var paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private fun Canvas.saveLayer() = saveLayer(
        0f, 0f, width.toFloat(), height.toFloat(),
        paint, Canvas.ALL_SAVE_FLAG
    )

    private fun Bitmap.makeHole(canvas: Canvas) {
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        canvas.drawBitmap(this, 0f, 0f, paint)
        paint.xfermode = null
    }

    private fun Bitmap.clipCut(canvas: Canvas) {
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(this, 0f, 0f, paint)
        paint.xfermode = null
    }

    private fun Bitmap.makeHole(bitmap: Bitmap) {
        for (x in 0 until width)
            for (y in 0 until height)
                if (getPixel(x, y) != 0)
                    bitmap.setPixel(x, y, 0)
    }

    private fun Bitmap.clipCut(bitmap: Bitmap) {
        for (x in 0 until width)
            for (y in 0 until height)
                if (getPixel(x, y) == 0)
                    bitmap.setPixel(x, y, 0)
    }

    private var zeroPixels: IntArray? = null
    private var mask: Bitmap? = null

    //changeReversal
    var isReversal: Boolean = false

    override fun dispatchDraw(canvas: Canvas) {
        canvas.saveLayer()
        super.dispatchDraw(canvas)
        mask?.apply { if (isReversal) makeHole(canvas) else clipCut(canvas) }
        lastAlpha = null
    }

    private var lastAlpha: Bitmap? = null

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long) =
        if (child is ClipMask) {
            initMask(canvas.width, canvas.height)
            mask?.apply {
                if (zeroPixels != null) setPixels(zeroPixels, 0, width, 0, 0, width, height)
                super.drawChild(Canvas(this), child, drawingTime)
            }
            lastAlpha?.apply {
                if (isReversal) {
                    makeHole(Canvas(mask!!))
                    makeHole(canvas)
                } else {
                    clipCut(Canvas(mask!!))
                    clipCut(canvas)
                }
            }
            getMask(child).apply {
                Canvas(this).drawBitmap(mask!!, 0f, 0f, null)
                if (isReversal) clipCut(canvas) else makeHole(canvas)
                canvas.saveLayer()
                lastAlpha = this
            }
            false
        } else super.drawChild(canvas, child, drawingTime)

    private fun getMask(child: View): Bitmap {
        var alpha = child.tag as Bitmap?
        if (alpha == null || alpha.width != width || alpha.height != height) {
            alpha = Bitmap.createBitmap(
                width, height,
                if (Build.VERSION.SDK_INT < 20)
                    Bitmap.Config.ARGB_4444
                else
                    Bitmap.Config.ALPHA_8
            )
            child.tag = alpha//save Alpha for dispatchTouchEvent
        } else {
            if (zeroPixels == null) zeroPixels = IntArray(width * height)
            alpha.setPixels(zeroPixels, 0, width, 0, 0, width, height)
        }
        return alpha!!
    }

    private fun initMask(width: Int, height: Int) {
        if (mask == null || mask!!.width != width || mask!!.height != height) {
            mask = Bitmap.createBitmap(
                width, height,
                if (Build.VERSION.SDK_INT < 20)
                    Bitmap.Config.ARGB_4444
                else
                    Bitmap.Config.ALPHA_8
            )
            if (zeroPixels != null) zeroPixels = IntArray(width * height)
        } else if (zeroPixels == null || zeroPixels!!.size != width * height)
            zeroPixels = IntArray(width * height)
    }

    //////////////////////////////<touch>//////////////////////////////
    private var maskAt = -1
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.dispatchTouchEvent(event)
        maskAt = -1
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is ClipMask) {
                child.getTag()?.also {
                    if (it is Bitmap)
                        it.apply {
                            if (
                                event.x.toInt() in 0 until width && event.y.toInt() in 0 until height
                                && getPixel(event.x.toInt(), event.y.toInt()) == 0
                            ) maskAt = i
                        }
                }
            }
            if (maskAt != -1) break
        }

        if (Build.VERSION.SDK_INT <= 15 && maskAt != -1)
            ViewGroup::class.java.getDeclaredField("mChildren").also { field ->
                field.isAccessible = true
                val old: Array<View> = field.get(this) as Array<View>
                val temp = Array(old.size) {
                    old[reOrder(it)]
                }
                field.set(this, temp)
                val need = super.dispatchTouchEvent(event)
                field.set(this, old)
                maskAt = -1
                return need
            }
        val need = super.dispatchTouchEvent(event)
        maskAt = -1
        return need
    }

    private fun reOrder(it: Int) = if (maskAt > 0) (it + (maskAt - 1)) % maskAt else it
    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int) =
        super.getChildDrawingOrder(childCount, drawingPosition).let {
            reOrder(it)
        }

    override fun isChildrenDrawingOrderEnabled() = maskAt != -1

    //////////////////////////////<recycle>//////////////////////////////
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child is ClipMask) {
                child.getTag()?.also {
                    if (it is Bitmap)
                        it.recycle()
                }
                child.setTag(null)
            }
        }
        mask?.recycle()
        mask = null
    }
}