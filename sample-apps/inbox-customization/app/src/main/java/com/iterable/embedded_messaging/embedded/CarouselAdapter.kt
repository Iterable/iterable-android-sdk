package com.iterable.embedded_messaging.embedded

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iterable.inbox_customization.R
import com.iterable.iterableapi.IterableEmbeddedMessage

class CarouselAdapter: ListAdapter<IterableEmbeddedMessage, CarouselAdapter.ViewHolder>(EmbeddedMessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

//        holder.itemView.setOnClickListener {
//            IterableApi.getInstance().trackEmbeddedClick(
//                item,
//                null,
//                item.elements?.defaultAction?.data
//            )
//
//
//            IterableApi.getInstance().embeddedManager.handleEmbeddedClick(
//                item,
//                null,
//                item.elements?.defaultAction?.data
//            )
//        }

        holder.bind(item)
    }

    class ViewHolder private constructor(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val embeddedTitleView: TextView = itemView.findViewById(R.id.embedded_message_title)
        private val embeddedBodyView: TextView = itemView.findViewById(R.id.embedded_message_body)
        private val embeddedButton: Button = itemView.findViewById(R.id.embedded_message_first_button)
        private val embeddedButton2: Button = itemView.findViewById(R.id.embedded_message_second_button)

        fun bind(item: IterableEmbeddedMessage) {
            embeddedTitleView.text = item.elements?.title
            embeddedBodyView.text = item.elements?.body

            val buttons = item.elements?.buttons

            if (buttons != null) {
                embeddedButton.visibility = if (buttons.getOrNull(0)?.title == null) View.GONE else View.VISIBLE
                embeddedButton.text = buttons.getOrNull(0)?.title.orEmpty()

//                embeddedButton.setOnClickListener {
//                    IterableApi.getInstance().embeddedManager.handleEmbeddedClick(
//                        item,
//                        item.elements?.buttons?.get(0)?.id,
//                        "action://bookKickBoxingClass"
//                    )
//
//                    IterableApi.getInstance().trackEmbeddedClick(
//                        item,
//                        item.elements?.buttons?.get(0)?.id,
//                        item.elements?.defaultAction?.data
//                    )
//                }

                if (buttons.size > 1) {
                    embeddedButton2.visibility = if (buttons[1].title == null) View.GONE else View.VISIBLE
                    embeddedButton2.text = buttons[1].title.orEmpty()

//                    embeddedButton2.setOnClickListener {
//                        IterableApi.getInstance().embeddedManager.handleEmbeddedClick(
//                            item,
//                            item.elements?.buttons?.get(1)?.id,
//                            item.elements?.buttons?.get(1)?.action?.data
//                        )
//
//                        IterableApi.getInstance().trackEmbeddedClick(
//                            item,
//                            item.elements?.buttons?.get(1)?.id,
//                            item.elements?.defaultAction?.data
//                        )
//                    }

                } else {
                    embeddedButton2.visibility = View.GONE
                }
            } else {
                embeddedButton.visibility = View.GONE
                embeddedButton2.visibility = View.GONE
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.banner_view, parent, false)
                return ViewHolder(view)
            }
        }
    }
}

class EmbeddedMessageDiffCallback: DiffUtil.ItemCallback<IterableEmbeddedMessage>() {
    override fun areItemsTheSame(oldItem: IterableEmbeddedMessage, newItem: IterableEmbeddedMessage): Boolean {
        return oldItem.metadata.messageId == newItem.metadata.messageId
    }

    override fun areContentsTheSame(oldItem: IterableEmbeddedMessage, newItem: IterableEmbeddedMessage): Boolean {
        return oldItem == newItem
    }
}