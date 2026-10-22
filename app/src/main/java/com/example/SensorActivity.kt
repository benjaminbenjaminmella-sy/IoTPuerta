package com.example.iotpuerta

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.iotpuerta.databinding.ActivitySensorBinding
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

/** Nodo sensor SIMULADO: publica temperatura y estado de la puerta en Firebase. */
class SensorActivity : AppCompatActivity() {

    private lateinit var b: ActivitySensorBinding
    private val db = FirebaseDatabase.getInstance().reference
    private val handler = Handler(Looper.getMainLooper())

    // Simulación automática: la temperatura varía un poco cada 3 segundos
    private val autoTask = object : Runnable {
        override fun run() {
            val nueva = b.slTemp.value + Random.nextFloat() * 2f - 1f
            b.slTemp.value = (Math.round(nueva.coerceIn(0f, 50f) * 2) / 2f) // múltiplo de 0.5
            publicar()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySensorBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.slTemp.addOnChangeListener { _, v, _ -> b.tvTemp.text = "$v °C" }
        b.slTemp.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) = publicar()
        })
        b.swPuerta.setOnCheckedChangeListener { _, abierta ->
            b.swPuerta.text = if (abierta) "Puerta ABIERTA" else "Puerta CERRADA"
            publicar()
        }
        b.swAuto.setOnCheckedChangeListener { _, on ->
            handler.removeCallbacks(autoTask)
            if (on) handler.post(autoTask)
        }
        publicar()
    }

    private fun publicar() {
        val datos = mapOf(
            "temperatura" to b.slTemp.value.toDouble(),
            "puertaAbierta" to b.swPuerta.isChecked,
            "timestamp" to System.currentTimeMillis()
        )
        db.child("dispositivos/puerta1").setValue(datos)   // estado actual
        db.child("historial/puerta1").push().setValue(datos) // histórico
            .addOnSuccessListener { b.tvEstado.text = "Último envío OK" }
            .addOnFailureListener { b.tvEstado.text = "Error al enviar: ${it.localizedMessage}" }
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoTask)
        super.onDestroy()
    }
}
