package com.iterable.iterableapi.ui.flex

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iterable.iterableapi.IterableFlexMessage
import com.iterable.iterableapi.IterableLogger
import com.iterable.iterableapi.ui.R

class IterableFlexViewAdapter: RecyclerView.Adapter<IterableFlexViewAdapter.ViewHolder>() {
    var data = listOf<IterableFlexMessage>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.flexMessageBody.text = item.elements.text[0].text
        holder.flexMessageButton.text = item.elements.buttons[0].title
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.list_item_flex_message, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val flexMessageBody: TextView = itemView.findViewById(R.id.flex_message_body)
        val flexMessageButton: Button = itemView.findViewById(R.id.flex_message_button)
    }
}