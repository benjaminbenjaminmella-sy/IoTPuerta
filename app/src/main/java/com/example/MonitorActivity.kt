package com.example.iotpuerta

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.iotpuerta.databinding.ActivityMonitorBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Monitor: recibe en tiempo real los datos del nodo sensor y genera alertas. */
class MonitorActivity : AppCompatActivity() {

    private lateinit var b: ActivityMonitorBinding
    private val ref = FirebaseDatabase.getInstance().reference
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val items = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private var umbral = 30.0
    private var temp: Double? = null
    private var abierta = false

    private var estadoListener: ValueEventListener? = null
    private var histListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMonitorBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        b.lista.adapter = adapter

        b.etUmbral.doAfterTextChanged {
            umbral = it.toString().toDoubleOrNull() ?: 30.0
            evaluarAlerta()
        }
    }

    override fun onStart() {
        super.onStart()

        // Estado actual en tiempo real
        estadoListener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                temp = s.child("temperatura").getValue(Double::class.java)
                abierta = s.child("puertaAbierta").getValue(Boolean::class.java) ?: false
                val ts = s.child("timestamp").getValue(Long::class.java)
                b.tvTemp.text = temp?.let { "$it °C" } ?: "-- °C"
                b.tvPuerta.text = if (abierta) "🚪 Puerta ABIERTA" else "🔒 Puerta CERRADA"
                b.tvHora.text = ts?.let { "Última lectura: ${fmt.format(Date(it))}" } ?: "Sin datos"
                evaluarAlerta()
            }
            override fun onCancelled(e: DatabaseError) { b.tvHora.text = "Error: ${e.message}" }
        }
        ref.child("dispositivos/puerta1").addValueEventListener(estadoListener!!)

        // Últimas 15 lecturas del historial
        histListener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                items.clear()
                for (c in s.children.reversed()) {
                    val t = c.child("temperatura").getValue(Double::class.java)
                    val p = c.child("puertaAbierta").getValue(Boolean::class.java) ?: false
                    val ts = c.child("timestamp").getValue(Long::class.java) ?: 0L
                    items.add("${fmt.format(Date(ts))}  |  $t °C  |  ${if (p) "Abierta" else "Cerrada"}")
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.child("historial/puerta1").orderByChild("timestamp").limitToLast(15)
            .addValueEventListener(histListener!!)
    }

    override fun onStop() {
        estadoListener?.let { ref.child("dispositivos/puerta1").removeEventListener(it) }
        histListener?.let { ref.child("historial/puerta1").removeEventListener(it) }
        super.onStop()
    }

    private fun evaluarAlerta() {
        val t = temp
        val calor = t != null && t > umbral
        when {
            abierta && calor -> alerta("⚠ Puerta abierta y temperatura alta", "#C62828")
            abierta -> alerta("⚠ Puerta abierta", "#EF6C00")
            calor -> alerta("⚠ Temperatura sobre el umbral ($umbral °C)", "#EF6C00")
            else -> alerta("Todo normal", "#2E7D32")
        }
    }

    private fun alerta(texto: String, color: String) {
        b.tvAlerta.text = texto
        b.tvAlerta.setBackgroundColor(Color.parseColor(color))
    }
}
