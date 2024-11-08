package com.retailtools.cameraapp
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class UserActivity : AppCompatActivity() {
    private val prefsName = "MyPrefs"   // Nombre de las preferencias
    private val usernameKey = "username"  // Clave para el nombre de usuario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        supportActionBar?.title = "IberTabac                                          v1.1"

        val textViewUser2: TextView = findViewById(R.id.textViewUser2)
        val edtUser = findViewById<EditText>(R.id.editTextUsername)
        val buttonNext = findViewById<Button>(R.id.buttonNext)

        // Obtener SharedPreferences
        val sharedPreferences = getSharedPreferences(prefsName, MODE_PRIVATE)
        val savedUsername = sharedPreferences.getString(usernameKey, null)

        // Si hay un usuario guardado, mostrarlo en el EditText
        if (savedUsername != null) {
            textViewUser2.text=savedUsername
            //edtUser.setText(savedUsername)  // Establecer el nombre de usuario existente en el EditText
        }

        buttonNext.setOnClickListener {
            val username = edtUser.text.toString()
            if (username.isNotEmpty()) {
                saveUsername(username)  // Guardar el nombre de usuario
                startMainActivity(username)
            } else {
                Toast.makeText(this, "Por favor, ingresa un nombre de usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Método para guardar el nombre de usuario en SharedPreferences
    private fun saveUsername(username: String) {
        val sharedPreferences = getSharedPreferences(prefsName, MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(usernameKey, username)  // Guardar el nombre de usuario
            apply()  // Aplicar los cambios
        }
    }

    private fun startMainActivity(username: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("username", username)  // Envía el nombre de usuario a MainActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) // Limpiar la pila de activity main
        startActivity(intent)
        finish()  // Evitar que el usuario vuelva a la pantalla de login de Usuario al presionar atrás
    }
}
