package org.cosmic.ide.ui.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.content.res.getTextArrayOrThrow
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.cosmic.ide.R
import org.cosmic.ide.util.lazyReflectedField
import java.lang.reflect.Field

class IntListPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    val entries: Array<CharSequence>
    val values: IntArray
    private var offValue: Int? = -1
    private var icons: TypedArray? = null
    private var currentValue: Int? = null

    // Reflect into Preference to get the (normally inaccessible) default value.
    private val defValue: Int
        get() = PREFERENCE_DEFAULT_VALUE_FIELD.get(this) as Int

    init {
        val prefAttrs =
            context.obtainStyledAttributes(
                attrs,
                R.styleable.IntListPreference,
                defStyleAttr,
                defStyleRes
            )

        entries = prefAttrs.getTextArrayOrThrow(R.styleable.IntListPreference_entries)

        values =
            context.resources.getIntArray(
                prefAttrs.getResourceIdOrThrow(R.styleable.IntListPreference_entryValues)
            )

        val offValueId = prefAttrs.getResourceId(R.styleable.IntListPreference_offValue, -1)
        if (offValueId > -1) {
            offValue = context.resources.getInteger(offValueId)
        }

        val iconsId = prefAttrs.getResourceId(R.styleable.IntListPreference_entryIcons, -1)
        if (iconsId > -1) {
            icons = context.resources.obtainTypedArray(iconsId)
        }

        prefAttrs.recycle()

        summaryProvider = IntListSummaryProvider()
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int) = a.getInt(index, -1)

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)

        if (defaultValue != null) {
            // If were given a default value, we need to assign it.
            setValue(defaultValue as Int)
        } else {
            currentValue = getPersistedInt(defValue)
        }
    }

    override fun shouldDisableDependents(): Boolean = currentValue == offValue

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val index = getValueIndex()
        if (index > -1) {
            val resourceId = icons?.getResourceId(index, -1) ?: -1
            if (resourceId > -1) {
                (holder.findViewById(android.R.id.icon) as ImageView).setImageResource(resourceId)
            }
        }
    }

    fun getValueIndex(): Int {
        val curValue = currentValue

        if (curValue != null) {
            return values.indexOf(curValue)
        }

        return -1
    }

    /** Set a value using the index of it in [values] */
    fun setValueIndex(index: Int) {
        setValue(values[index])
    }

    private fun setValue(value: Int) {
        if (value != currentValue) {
            currentValue = value

            callChangeListener(value)
            notifyDependencyChange(shouldDisableDependents())
            persistInt(value)
            notifyChanged()
        }
    }

    private inner class IntListSummaryProvider : SummaryProvider<IntListPreference> {
        override fun provideSummary(preference: IntListPreference): CharSequence {
            val index = getValueIndex()

            if (index != -1) {
                return entries[index]
            }

            // Usually an invalid state, don't bother translating
            return "<not set>"
        }
    }

    companion object {
        private val PREFERENCE_DEFAULT_VALUE_FIELD: Field by
        lazyReflectedField(Preference::class, "mDefaultValue")
    }
}
