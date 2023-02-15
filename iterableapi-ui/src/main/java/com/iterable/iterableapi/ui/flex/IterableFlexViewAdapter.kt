package com.iterable.iterableapi.ui.flex

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iterable.iterableapi.IterableFlexMessage
import com.iterable.iterableapi.ui.databinding.ListItemFlexMessageBinding

class IterableFlexViewAdapter: ListAdapter<IterableFlexMessage, IterableFlexViewAdapter.ViewHolder>(FlexMessageDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: ListItemFlexMessageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IterableFlexMessage) {
            binding.flexMessage = item
            binding.executePendingBindings()
        }
        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemFlexMessageBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class FlexMessageDiffCallback: DiffUtil.ItemCallback<IterableFlexMessage>() {
    override fun areItemsTheSame(oldItem: IterableFlexMessage, newItem: IterableFlexMessage): Boolean {
        return oldItem.metadata.id == newItem.metadata.id
    }
    override fun areContentsTheSame(oldItem: IterableFlexMessage, newItem: IterableFlexMessage): Boolean {
        return oldItem == newItem
    }
}