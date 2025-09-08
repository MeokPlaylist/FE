package com.example.meokpli.Main.Search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R

class RestaurantAdapter(
    private var restaurants: List<Restaurant>
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    inner class RestaurantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_restaurant_name)
        val tvAddress: TextView = view.findViewById(R.id.tv_restaurant_address)
        val starBtn: ImageView = view.findViewById(R.id.iv_favorite_star)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(v)
    }

    override fun onBindViewHolder(  holder: RestaurantViewHolder, position: Int) {
        val restaurant = restaurants[position]
        holder.tvName.text = restaurant.name
        holder.tvAddress.text = restaurant.address

        holder.starBtn.isSelected = restaurant.isFavorite

        holder.starBtn.setOnClickListener {
            restaurant.isFavorite = !restaurant.isFavorite
            holder.starBtn.isSelected = restaurant.isFavorite
        }
    }

    override fun getItemCount() = restaurants.size

    fun updateData(newItems: List<Restaurant>) {
        restaurants = newItems    // var로 선언된 필드 갱신
        notifyDataSetChanged()
    }
}

data class Restaurant(
    val name: String,
    val address: String,
    var isFavorite: Boolean = false
)
