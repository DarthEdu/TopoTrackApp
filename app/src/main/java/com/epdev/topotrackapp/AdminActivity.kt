
package com.epdev.topotrackapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.epdev.topotrackapp.databinding.ActivityAdminBinding
import com.epdev.topotrackapp.utils.UserPreferences
import androidx.recyclerview.widget.LinearLayoutManager
import com.epdev.topotrackapp.utils.SupabaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Configurar RecyclerView
        adapter = UserAdapter(
            onDelete = { userId, email -> deleteUser(userId, email) },
            onViewLocations = { email -> viewLocations(email) }
        )
        binding.rvUsuarios.layoutManager = LinearLayoutManager(this)
        binding.rvUsuarios.adapter = adapter
        loadUsers()
    }

    private fun loadUsers() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = SupabaseManager.getAllUsers()
            withContext(Dispatchers.Main) {
                result.onSuccess { users ->
                    adapter.submitList(users)
                    if (users.isEmpty()) {
                        Toast.makeText(this@AdminActivity, "No hay usuarios registrados", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure {
                    Toast.makeText(this@AdminActivity, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteUser(userId: String, email: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar usuario")
            .setMessage("¿Estás seguro de eliminar el usuario $email?")
            .setPositiveButton("Eliminar") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val result = SupabaseManager.deleteUserByEmail(email)
                    if (result.isSuccess) {
                        // Eliminar de Auth por correo
                        val authResult = SupabaseManager.deleteUserFromAuthByEmail(email)
                        withContext(Dispatchers.Main) {
                            if (authResult.isSuccess) {
                                Toast.makeText(this@AdminActivity, "Usuario eliminado de la base y Auth", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@AdminActivity, "Eliminado de la base, pero error en Auth", Toast.LENGTH_LONG).show()
                            }
                            loadUsers()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AdminActivity, "Error al eliminar usuario", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun viewLocations(email: String) {
        com.epdev.topotrackapp.ui.map.UserLocationsMapActivity.start(this, email)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, R.id.action_logout, 0, "Cerrar sesión")
            .setIcon(android.R.drawable.ic_menu_revert)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        UserPreferences.clearUserData(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}