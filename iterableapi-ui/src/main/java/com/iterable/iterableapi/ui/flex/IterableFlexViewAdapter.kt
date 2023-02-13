package com.iterable.iterableapi.ui.flex

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iterable.iterableapi.IterableFlexMessage
import com.iterable.iterableapi.IterableLogger
import com.iterable.iterableapi.ui.R

class IterableFlexViewAdapter: RecyclerView.Adapter<TextItemViewHolder>() {
    var data = listOf<IterableFlexMessage>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: TextItemViewHolder, position: Int) {
        val item = data[position]
        holder.textView.text = item.elements.text[0].text
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.text_item_view, parent, false) as TextView
        return TextItemViewHolder(view)
    }
}