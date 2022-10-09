package org.cosmic.ide.activity.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

class UnfilteredArrayAdapter<T> : ArrayAdapter<T> {

    private val filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults = FilterResults()

        override fun publishResults(constraint: CharSequence, results: FilterResults)
    }

    constructor(
        context: Context,
        objects: List<T> = emptyList()
    ) : super(context, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, objects)

    constructor(
        context: Context,
        objects: Array<out T>
    ) : super(context, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, objects)

    override fun getFilter(): Filter = filter
}
