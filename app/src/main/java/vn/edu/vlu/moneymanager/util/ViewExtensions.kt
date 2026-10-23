package vn.edu.vlu.moneymanager.util

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import vn.edu.vlu.moneymanager.data.model.Category

/**
 * Lifecycle-aware collection for Fragments: [block] runs while the *view* is STARTED and is cancelled
 * in onStop, so flows are never collected into a destroyed binding.
 */
fun Fragment.repeatOnViewStarted(block: suspend CoroutineScope.() -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
}

/** Same as [repeatOnViewStarted] for Activities, which have no separate view lifecycle. */
fun ComponentActivity.repeatOnStarted(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
}

/** Edge-to-edge: pads the view by the system bars (and keyboard) on top of its own padding. */
fun View.applySystemBarsPadding(top: Boolean = true, bottom: Boolean = true) {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    val initialLeft = paddingLeft
    val initialRight = paddingRight
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        view.updatePadding(
            left = initialLeft + bars.left,
            top = initialTop + if (top) bars.top else 0,
            right = initialRight + bars.right,
            bottom = initialBottom + if (bottom) bars.bottom else 0
        )
        insets
    }
}

/** Colored circle with the first letter of the category (grey "?" when uncategorized). */
fun TextView.showCategoryInitial(category: Category?) {
    text = category?.name?.firstOrNull()?.uppercase() ?: "?"
    val color = category?.colorHex?.let { runCatching { it.toColorInt() }.getOrNull() }
        ?: MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
    backgroundTintList = ColorStateList.valueOf(color)
}
