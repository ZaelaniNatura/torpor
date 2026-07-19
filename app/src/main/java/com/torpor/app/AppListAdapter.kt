package com.torpor.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.torpor.app.data.AppInfo
import com.torpor.app.databinding.ItemAppBinding

class AppListAdapter(
    private val items: List<AppInfo>,
    private val onToggle: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = items[position]
        holder.binding.appIcon.setImageDrawable(app.icon)
        holder.binding.appName.text = app.appName
        holder.binding.packageName.text = app.packageName
        holder.binding.switchToggle.setOnCheckedChangeListener(null)
        holder.binding.switchToggle.isChecked = app.isRestricted
        holder.binding.switchToggle.isEnabled = !app.isLocked
        holder.binding.lockedLabel.visibility = if (app.isLocked) android.view.View.VISIBLE else android.view.View.GONE
        holder.binding.switchToggle.setOnCheckedChangeListener { _, checked ->
            onToggle(app, checked)
        }
    }

    override fun getItemCount() = items.size
}
