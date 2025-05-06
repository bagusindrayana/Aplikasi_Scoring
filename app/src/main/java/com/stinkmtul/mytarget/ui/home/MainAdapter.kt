package com.stinkmtul.mytarget.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.databinding.ItemTrainingBinding

class MainAdapter(
    private val listener: OnItemClickListener
) : ListAdapter<Training, MainAdapter.TrainingViewHolder>(DIFF_CALLBACK) {

    interface OnItemClickListener {
        fun onItemClick(training: Training)
        fun onDeleteClick(training: Training)
        fun onClickItem(training: Training)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val binding = ItemTrainingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrainingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val training = getItem(position)
        holder.bind(training)
    }

    inner class TrainingViewHolder(private val binding: ItemTrainingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(training: Training) {
            binding.tvDate.text = training.date
            val formattedDescription = training.description
                ?.split(" ")
                ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } ?: ""
            binding.tvDescription.text = formattedDescription

            binding.root.setOnClickListener {
                listener.onItemClick(training)
            }

            binding.btnDelete.setOnClickListener {
                listener.onDeleteClick(training)
            }
            binding.btnEdit.setOnClickListener {
                listener.onClickItem(training)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Training>() {
            override fun areItemsTheSame(oldItem: Training, newItem: Training): Boolean {
                return oldItem.training_id == newItem.training_id
            }

            override fun areContentsTheSame(oldItem: Training, newItem: Training): Boolean {
                return oldItem == newItem
            }
        }
    }
}
