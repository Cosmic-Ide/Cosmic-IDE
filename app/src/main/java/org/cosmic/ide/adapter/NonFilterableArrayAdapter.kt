package org.cosmic.ide.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filter.FilterResults

class NonFilterableArrayAdapter(context: Context, val items: Array<String>)
    : ArrayAdapter<String>(context, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, items.toList()) {

    private val noOpFilter = object : Filter() {
        private val noOpResult = FilterResults()
        override fun performFiltering(constraint: CharSequence?) = noOpResult
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {}
    }

    override fun getFilter() = noOpFilter
}