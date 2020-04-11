package dev.patrickgold.florisboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.getDrawable
import com.google.android.flexbox.FlexboxLayout
import java.util.*


class CustomKey : androidx.appcompat.widget.AppCompatButton, View.OnTouchListener {

    private var isKeyPressed: Boolean = false
        set(v) {
            super.setBackgroundTintList(ColorStateList.valueOf(getColorFromAttr(when {
                v -> R.attr.key_bgColorPressed
                else -> R.attr.key_bgColor
            })))
            field = v
        }
    private val osHandler = Handler()
    private var osTimer: Timer? = null
    var cmd: Int?
        set(v) { field = v; updateUI() }
    var code: Int?
        set(v) { field = v; updateUI() }
    var keyboard: CustomKeyboard? = null
    var isRepeatable: Boolean = false
    var popupCodes = listOf<Int>()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.customKeyStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs) {
        context.obtainStyledAttributes(
            attrs, R.styleable.CustomKey, defStyleAttrs,
            R.style.Widget_AppCompat_Button_Borderless_CustomKey).apply {
            try {
                cmd = KeyCodes.fromString(getString(R.styleable.CustomKey_cmd) ?: "")
                val tmp = getInteger(R.styleable.CustomKey_code, 0)
                code =  when (tmp) {
                    0 -> null
                    else -> tmp
                }
            } finally {}
        }.recycle()

        super.setOnTouchListener(this)
    }

    private fun updateUI() {
        super.setText(getLabel())
        when (cmd) {
            KeyCodes.DELETE -> (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
            KeyCodes.ENTER -> (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
            KeyCodes.SHIFT -> (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
            KeyCodes.SPACE -> (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 5.0f
            KeyCodes.VIEW_SYMOBLS -> {
                (layoutParams as FlexboxLayout.LayoutParams).flexGrow = 1.0f
                super.setText("?123")
            }
        }
        val drawable: Drawable? = when (cmd) {
            KeyCodes.ENTER -> getDrawable(context, R.drawable.ic_arrow_forward)
            KeyCodes.DELETE -> getDrawable(context, R.drawable.ic_backspace)
            KeyCodes.LANGUAGE_SWITCH -> getDrawable(context, R.drawable.ic_language)
            KeyCodes.SHIFT -> getDrawable(context, R.drawable.ic_capslock)
            else -> null
        }
        drawable?.setBounds(0, 0, resources.getDimension(R.dimen.key_width).toInt(), resources.getDimension(R.dimen.key_width).toInt())
        if (drawable != null) {
            overlay.clear()
            overlay.add(drawable)
        }
        requestLayout()
        invalidate()
    }

    /**
     * Creates a label text from the key's code.
     */
    private fun getLabel(): String {
        val label = (code?.toChar() ?: "").toString()
        return when {
            keyboard?.caps ?: false -> label.toUpperCase(Locale.getDefault())
            else -> label
        }
    }

    private fun showPopup() {
        if (code == 32 || code == null) {
            return
        }
        val keyPopupWidth = resources.getDimension(R.dimen.key_popup_width).toInt()
        val keyPopupHeight = resources.getDimension(R.dimen.key_popup_height).toInt()
        val keyWidth = resources.getDimension((R.dimen.key_width)).toInt()
        val keyHeight = resources.getDimension((R.dimen.key_height)).toInt()
        val keyboardRowMarginV = resources.getDimension((R.dimen.keyboard_row_marginV)).toInt()
        val popupView = View.inflate(context, R.layout.key_popup, null) as LinearLayout
        popupView.measure(MeasureSpec.makeMeasureSpec(keyPopupWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(keyPopupHeight, MeasureSpec.EXACTLY))
        popupView.layout(0, 0, keyPopupWidth, keyPopupHeight)
        popupView.x = x - ((keyPopupWidth - keyWidth).toFloat() / 2.0f)
        popupView.y = (parent as CustomKeyboardRow).y - (keyPopupHeight - keyHeight - keyboardRowMarginV)
        popupView.findViewById<TextView>(R.id.key_popup_text).text = getLabel()
        popupView.findViewById<ImageView>(R.id.key_popup_threedots).visibility = when {
            popupCodes.isEmpty() -> View.INVISIBLE
            else -> View.VISIBLE
        }
        keyboard?.overlay?.add(popupView)
    }
    private fun hidePopup() {
        if (code == 32 || code == null) {
            return
        }
        keyboard?.overlay?.clear()
    }

    private fun getColorFromAttr(
        attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        context.theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }


    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            isKeyPressed = true
            showPopup()
            if (cmd != null && isRepeatable) {
                osTimer = Timer()
                osTimer?.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        keyboard?.onKeyClicked(code ?: cmd ?: 0)
                        if (!isKeyPressed) {
                            osTimer?.cancel()
                            osTimer = null
                        }
                    }
                }, 500, 50)
            }
            osHandler.postDelayed({
                //
            }, 300)
        }
        if (event.action == MotionEvent.ACTION_UP) {
            isKeyPressed = false
            osHandler.removeCallbacksAndMessages(null)
            osTimer?.cancel()
            osTimer = null
            hidePopup()
            keyboard?.onKeyClicked(code ?: cmd ?: 0)
        }
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val keyMarginH = resources.getDimension((R.dimen.key_marginH)).toInt()
        val keyboardWidth = (parent as FlexboxLayout).measuredWidth
        val keyboardRowMarginH = resources.getDimension((R.dimen.keyboard_row_marginH)).toInt()
        val keyWidthPlusMargin = (keyboardWidth - 2 * keyboardRowMarginH) / 10
        val keyWidth = keyWidthPlusMargin - 2 * keyMarginH
        layoutParams = layoutParams.apply {
            width = keyWidth
        }
    }
}
