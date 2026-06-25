package com.example.projectapplication2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.projectapplication2.databinding.ItemForecastBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ForecastAdapter(private val forecastList: List<ForecastItem>) :
    RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(val binding: ItemForecastBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val binding = ItemForecastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val item = forecastList[position]
        
        // Format date/time
        val sdf = SimpleDateFormat("EEE, HH:mm", Locale.getDefault())
        val date = Date(item.dt * 1000)
        holder.binding.tvForecastDate.text = sdf.format(date)
        
        holder.binding.tvForecastTemp.text = "${item.main.temp.toInt()}°C"
        
        val iconCode = item.weather.firstOrNull()?.icon
        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
        holder.binding.ivForecastIcon.load(iconUrl)
    }

    override fun getItemCount(): Int = forecastList.size
}
