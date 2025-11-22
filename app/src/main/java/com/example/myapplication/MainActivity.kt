
package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.extensions.viewBinding
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import com.ghgande.j2mod.modbus.procimg.SimpleRegister
import kotlinx.coroutines.*
import android.widget.FrameLayout

class MainActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val activityScope = CoroutineScope(Dispatchers.IO)
    private var modbus: ModbusTCPMaster? = null

    private val labels = mutableListOf<TextView>()
    private val switches = mutableListOf<Switch>()

    private val OUTPUT_ON = 0x100 // 256
    private val OUTPUT_OFF = 0x200 // 512

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val lantai = intent.getIntExtra("lantai", 1)
        title = "Kontrol Relay Lantai $lantai"

        updateConnectionStatus(false)
        buildChannelUI()

        binding.btnConnect.setOnClickListener {
            if (modbus == null) connectModbus() else disconnectModbus()
        }

        binding.btnRefresh.setOnClickListener {
            if (modbus != null) readAllChannels()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectModbus()
        activityScope.cancel()
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        if (isConnected) {
            binding.btnConnect.text = "Connected"
            binding.btnConnect.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
            switches.forEach { it.isEnabled = true }
        } else {
            binding.btnConnect.text = "Connect"
            binding.btnConnect.setBackgroundColor(Color.parseColor("#F44336")) // Red
            switches.forEach {
                it.isEnabled = false
                it.isChecked = false
            }
        }
    }

    private fun connectModbus() {
        activityScope.launch {
            val ipInput = binding.editIp.text.toString().trim()
            if (ipInput.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Please enter IP address", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val parts = ipInput.split(":")
            val host = parts[0]
            val port = if (parts.size == 2) parts[1].toIntOrNull() ?: 502 else 502

            try {
                modbus = ModbusTCPMaster(host, port)
                modbus?.connect()

                withContext(Dispatchers.Main) {
                    updateConnectionStatus(true)
                    Toast.makeText(this@MainActivity, "Connected to $host:$port", Toast.LENGTH_SHORT).show()
                    readAllChannels() // ✅ Baca status awal
                    startAutoRefresh() // ✅ Auto-refresh setiap 5 detik
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(false)
                    Toast.makeText(this@MainActivity, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun disconnectModbus() {
        activityScope.launch {
            try {
                modbus?.disconnect()
                modbus = null
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(false)
                    Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(false)
                    Toast.makeText(this@MainActivity, "Error disconnecting: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }



    private fun buildChannelUI() {
        val customNames = listOf(
            "Lampu K1A", "K1A AC1", "K1A AC2", "Kelas 2B",
            "Kelas 3A", "Kelas 3B", "Kelas 4A", "Kelas 4B",
            "Kelas 5A", "Kelas 5B", "Kelas 6A", "Kelas 6B"
        )

        // Gunakan GridLayout sebagai container
        val gridLayout = GridLayout(this).apply {
            rowCount = (customNames.size + 1) / 2
            columnCount = 2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        for (i in customNames.indices) {
            val label = TextView(this).apply {
                text = customNames[i]
                textSize = 18f
                setTextColor(Color.BLACK)
            }

            val toggle = Switch(this).apply {
                isEnabled = false
            }

            val innerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                addView(label)
                addView(toggle)
            }

            val card = com.google.android.material.card.MaterialCardView(this).apply {
                radius = 12f
                setCardBackgroundColor(Color.WHITE)
                cardElevation = 8f
                useCompatPadding = true
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                addView(innerLayout)
            }

            labels.add(label)
            switches.add(toggle)
            gridLayout.addView(card)
        }

        // Tambahkan frame di sekitar grid
        val frameLayout = FrameLayout(this).apply {
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.LTGRAY) // Warna frame
            addView(gridLayout)
        }

        binding.channelContainer.addView(frameLayout)
    }

    private fun writeRegister(index: Int, state: Boolean) {
        activityScope.launch {
            try {
                val value = if (state) OUTPUT_ON else OUTPUT_OFF
                modbus?.writeSingleRegister(index, SimpleRegister(value))
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to write: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun readAllChannels() {
        activityScope.launch {
            try {
                val response = modbus?.readMultipleRegisters(0, 12)
                response?.let {
                    withContext(Dispatchers.Main) {
                        for (i in 0 until 12) {
                            val value = it[i].value
                            val isOn = (value == OUTPUT_ON || value == 256) // ✅ 256 = ON
                            switches[i].isChecked = isOn
                            labels[i].text = if (isOn) "ON" else "OFF"
                            labels[i].setBackgroundColor(
                                if (isOn) Color.parseColor("#00AA00") else Color.parseColor("#4F4F4F")
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to read: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startAutoRefresh() {
        activityScope.launch {
            while (modbus != null) {
                readAllChannels()
                delay(5000) // refresh setiap 5 detik
            }
        }
    }
}
