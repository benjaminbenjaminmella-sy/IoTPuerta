package com.example.iotpuerta

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.iotpuerta.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Si ya hay sesión iniciada, saltamos el login
        if (auth.currentUser != null) { irARol(); return }

        b.btnLogin.setOnClickListener {
            val (email, pass) = leerCredenciales() ?: return@setOnClickListener
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { irARol() }
                // Mensaje genérico: no revelamos si falló el correo o la clave
                .addOnFailureListener { msg("Credenciales incorrectas") }
        }

        b.btnRegistro.setOnClickListener {
            val (email, pass) = leerCredenciales() ?: return@setOnClickListener
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { msg("Cuenta creada"); irARol() }
                .addOnFailureListener { msg("No se pudo crear la cuenta: ${it.localizedMessage}") }
        }
    }

    private fun leerCredenciales(): Pair<String, String>? {
        val email = b.etEmail.text.toString().trim()
        val pass = b.etPass.text.toString()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { msg("Correo inválido"); return null }
        if (pass.length < 6) { msg("La contraseña debe tener al menos 6 caracteres"); return null }
        return email to pass
    }

    private fun irARol() {
        startActivity(Intent(this, RoleActivity::class.java))
        finish()
    }

    private fun msg(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()
}
