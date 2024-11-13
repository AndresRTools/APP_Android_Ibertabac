package com.retailtools.cameraapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)
        supportActionBar?.title = "IberTabac                                          v1.2"
        val edtPassword = findViewById<EditText>(R.id.editTextPassword)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val storedPassword = getString(R.string.passwordApp) // Obtener la contraseña desde los recursos

        btnLogin.setOnClickListener {
            val password = edtPassword.text.toString()
            if (password == storedPassword) {  // Compara con tu contraseña definida
                // Guarda el estado de login en SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putBoolean("isLoggedIn", true)
                editor.apply()
                startUserActivity(password)
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun startUserActivity(password: String) {
        val intent = Intent(this, UserActivity::class.java)
        intent.putExtra("password", password)  // Envía el nombre de usuario a PasswordActivity
        startActivity(intent)
        finish()
    }

}