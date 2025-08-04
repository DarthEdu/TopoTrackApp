package com.epdev.topotrackapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.epdev.topotrackapp.R
import com.epdev.topotrackapp.model.User

class UserAdapter(
    private var users: List<User> = emptyList(),
    private val onDelete: (userId: String, email: String) -> Unit,
    private val onViewLocations: (String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreUsuario)
        val tvCorreo: TextView = itemView.findViewById(R.id.tvCorreoUsuario)
        val tvRol: TextView = itemView.findViewById(R.id.tvRolUsuario)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminarUsuario)
        val btnVerUbicaciones: Button = itemView.findViewById(R.id.btnVerUbicaciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_usuario, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvNombre.text = user.nombre ?: "-"
        holder.tvCorreo.text = user.correo ?: "-"
        holder.tvRol.text = user.rol ?: "-"
        holder.btnEliminar.setOnClickListener { onDelete(user.id, user.correo) }
        holder.btnVerUbicaciones.setOnClickListener { user.correo.let { onViewLocations(it) } }
    }

    override fun getItemCount(): Int = users.size

    fun submitList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
