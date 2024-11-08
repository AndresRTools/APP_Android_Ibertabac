package com.retailtools.cameraapp

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PhotoAdapter(
    private val context: Context,
    private val photoList: MutableList<String>,
    private val onDelete: (String) -> Unit // Callback para eliminar
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton) // Referencia al botón de eliminar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        // Verificar si la lista de fotos es válida y el índice está dentro de los límites
        if (position >= 0 && position < photoList.size) {
            Glide.with(context).load(photoList[position]).into(holder.imageView) // Cargamos imagen usando Glide
            // ACTIVIDAD PARA EL BOTON DE ELIMINAR FOTO
            holder.deleteButton.setOnClickListener {
                val adapterPosition = holder.bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition >= 0 && adapterPosition < photoList.size) {
                    val photoToDelete = photoList[adapterPosition]
                    onDelete(photoToDelete) // Llamar al callback para eliminar
                    removePhotoAtPosition(adapterPosition) // Eliminar la foto por posición del holder
                } else {
                    Log.e("PhotoAdapter", "Intento de eliminar en posición fuera de límites: $adapterPosition")
                }
            }
        } else {
            Log.e("PhotoAdapter", "Posición inválida: $position, tamaño de lista: ${photoList.size}")
        }
    }

    override fun getItemCount(): Int {
        return photoList.size
    }
    // Eliminar foto por su posición
    private fun removePhotoAtPosition(position: Int) {
        if (position >= 0 && position < photoList.size) {
            photoList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, photoList.size) // Notificar cambio en los elementos después de la eliminación
        } else {
            Log.e("PhotoAdapter", "Intento de eliminar en posición fuera de límites: $position")
        }
    }

    // Función para eliminar una foto por su ruta
    fun removePhoto(photoPath: String) {
        val position = photoList.indexOf(photoPath)
        if (position != -1) {
            removePhotoAtPosition(position)
        } else {
            Log.e("PhotoAdapter", "Foto no encontrada: $photoPath")
        }
    }
    // Función para eliminar todas las fotos de la lista de una vez
    @SuppressLint("NotifyDataSetChanged")
    fun removeAllPhotos() {
        photoList.clear() // Elimina todos los elementos de la lista
        notifyDataSetChanged() // Notifica al adaptador para vaciar la lista en el RecyclerView
    }


}
