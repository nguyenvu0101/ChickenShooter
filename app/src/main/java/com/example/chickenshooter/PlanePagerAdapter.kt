package com.example.chickenshooter

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PlanePagerAdapter(private val planes: List<Plane>) : RecyclerView.Adapter<PlanePagerAdapter.PlaneViewHolder>() {
    class PlaneViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaneViewHolder {
        val img = ImageView(parent.context)
        img.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        img.scaleType = ImageView.ScaleType.CENTER_INSIDE
        return PlaneViewHolder(img)
    }

    override fun getItemCount(): Int = planes.size

    override fun onBindViewHolder(holder: PlaneViewHolder, position: Int) {
        holder.imageView.setImageResource(planes[position].displayResId)
    }
}