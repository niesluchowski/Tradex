package com.tradex.sasd

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tradex.sasd.databinding.ItemEducationArticleBinding

class EducationAdapter : RecyclerView.Adapter<EducationAdapter.VH>() {

    private var items: List<EducationArticle> = emptyList()
    var onItemClick: ((EducationArticle) -> Unit)? = null

    class VH(val binding: ItemEducationArticleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEducationArticleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvTitle.text = item.title
            tvMeta.text = item.meta
            tvPreview.text = item.preview

            root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<EducationArticle>) {
        items = list
        notifyDataSetChanged()
    }
}