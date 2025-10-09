package com.sens.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

/**
 * Created by Sens on 2021/12/12.
 * {@see <a href="https://github.com/senswrong/ClipLayout">ClipLayout</a>}
 */
open class ClipMask @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    var mask: Bitmap? = null
        get() {
            val w = (parent as View?)?.width ?: width
            val h = (parent as View?)?.height ?: height
            if (w > 0 && h > 0)
                if (field == null || field!!.width != w || field!!.height != h) {
                    field?.recycle()
                    field = Bitmap.createBitmap(
                        w,
                        h,
                        Bitmap.Config.ARGB_8888
                    )
                }
            return field
        }

    override fun onDetachedFromWindow() {
        mask?.recycle()
        mask = null
        super.onDetachedFromWindow()
    }
}

class ClipLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    //////////////////////////////<refresh>//////////////////////////////

    // 记录上一次View树的「绘制状态指纹」
    private var lastDrawFingerprint: String? = null

    init {
        viewTreeObserver.addOnPreDrawListener {
            val currentFingerprint = generateViewTreeFingerprint(this)
            if (currentFingerprint != lastDrawFingerprint) {
                invalidate()
                lastDrawFingerprint = currentFingerprint
            }
            true // 允许View树继续绘制
        }
    }

    /**
     * 递归生成整个View树的状态指纹（描述所有View的关键视觉状态）
     */
    private fun generateViewTreeFingerprint(view: View): String {
        val sb = StringBuilder()
        sb.append("id:").append(view.id)
            .append(";left:").append(view.left)
            .append(";top:").append(view.top)
            .append(";w:").append(view.width)
            .append(";h:").append(view.height)
            .append(";alpha:").append(view.alpha)
            .append(";scrollX:").append(view.scrollX)
            .append(";scrollY:").append(view.scrollY)
            .append(";bg:").append(view.background?.hashCode() ?: 0)
            // 针对特定View添加额外状态（如TextView文本、ImageView图片）
            .append(
                when (view) {
                    is TextView -> ";text:${view.text.hashCode()}"
                    is ImageView -> ";drawable:${view.drawable?.hashCode() ?: 0}"
                    else -> ""
                }
            )
            .append("|")
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                sb.append(generateViewTreeFingerprint(view.getChildAt(i)))
            }
        }
        return sb.toString()
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

    private fun Bitmap.clear() {
        Canvas(this).drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

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

    //    private var zeroPixels: IntArray? = null
    private var lastMask: ClipMask? = null
    var cache: Bitmap? = null
        get() {
            if (width > 0 && height > 0)
                if (field == null || field!!.width != width || field!!.height != height) {
                    field?.recycle()
                    field = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                }
            return field
        }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cache?.recycle()
        cache = null
    }

    override fun dispatchDraw(proxy: Canvas) {
        cache?.clear()
        val canvas = Canvas(cache)
        lastMask = null
        val layer = canvas.saveLayer()
        super.dispatchDraw(canvas)
        lastMask?.apply { mask?.clipCut(canvas) }
        lastMask = null
        canvas.restoreToCount(layer)
        proxy.drawBitmap(cache, 0f, 0f, null)
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        if (child is ClipMask) {
            child.mask?.clear()
            val state = super.drawChild(Canvas(child.mask), child, drawingTime)
            lastMask?.mask?.apply {
                clipCut(canvas)
                clipCut(Canvas(child.mask!!))
            }
            child.mask?.apply {
                makeHole(canvas)
            }
            canvas.saveLayer()
            lastMask = child
            return state
        }
        return super.drawChild(canvas, child, drawingTime)
    }

    //////////////////////////////<touch>//////////////////////////////
    private val mChildrenF by lazy {
        ViewGroup::class.java.getDeclaredField("mChildren").also { field ->
            field.isAccessible = true
        }
    }

    private fun getChildrens(): Array<View> = mChildrenF.get(this) as Array<View>
    private fun setChildrens(children: Array<View>) = mChildrenF.set(this, children)
    private var maskAt = -1

    /**
     * 触摸穿透
     * 启用后，若点击位置为透明区域，则穿透事件
     * */
    var touchThrough = false
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        maskAt = -1
        if (event.action != MotionEvent.ACTION_DOWN) return super.dispatchTouchEvent(event)
        //事件穿透
        if (touchThrough && cache?.getPixel(event.x.toInt(), event.y.toInt()) == Color.TRANSPARENT) return false
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is ClipMask) {
                child.mask ?: continue
                if (
                    event.x.toInt() in child.left until child.right
                    && event.y.toInt() in child.top until child.bottom
                    && child.mask!!.getPixel(event.x.toInt(), event.y.toInt()) == Color.TRANSPARENT
                ) {
                    maskAt = i
                    break
                }
            }
        }

        if (Build.VERSION.SDK_INT <= 15 && maskAt != -1) {
            val old: Array<View> = getChildrens()
            val temp = Array(old.size) {
                old[reOrder(it)]
            }
            setChildrens(temp)
            val need = super.dispatchTouchEvent(event)
            setChildrens(old)
            maskAt = -1
            return need
        }
        val need = super.dispatchTouchEvent(event)
        maskAt = -1
        return need
    }

    private fun reOrder(it: Int) = if (maskAt > 0) (it + (maskAt - 1)) % maskAt else it
    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int) = reOrder(super.getChildDrawingOrder(childCount, drawingPosition))
    override fun isChildrenDrawingOrderEnabled() = maskAt != -1
}