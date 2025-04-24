package com.stinkmtul.mytarget.ui.detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.databinding.ItemLeaderboardBinding
import com.stinkmtul.mytarget.viewmodel.DetailViewModel

class DetailAdapter(private val viewModel: DetailViewModel) :
    ListAdapter<Leaderboard, DetailAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, viewModel)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemLeaderboardBinding,
        private val viewModel: DetailViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Leaderboard) {
            binding.rankNumber.text = "${item.the_champion}"
            binding.scoreText.text = "Skor : ${item.score}"

            item.person_id?.let {
                viewModel.getNamePerson(it).observeForever { name ->
                    binding.nameText.text = "${name ?: "Unknown"}"
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Leaderboard>() {
            override fun areItemsTheSame(oldItem: Leaderboard, newItem: Leaderboard): Boolean {
                return oldItem.leaderboard_id == newItem.leaderboard_id
            }

            override fun areContentsTheSame(oldItem: Leaderboard, newItem: Leaderboard): Boolean {
                return oldItem == newItem
            }
        }
    }
}

