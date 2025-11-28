package com.example.modol

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
//import androidx.gridlayout.widget.GridLayout
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.*
import com.ghgande.j2mod.modbus.procimg.SimpleRegister
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import android.view.View
import android.view.ViewGroup
import com.example.modol.databinding.ActivityCustomBinding
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import android.util.Log


class CustomActivity : AppCompatActivity() {

    private val binding by lazy { ActivityCustomBinding.inflate(layoutInflater) }
    private val activityScope = CoroutineScope(Dispatchers.IO)
    private var modbus: ModbusTCPMaster? = null

    private val labels = mutableListOf<TextView>()
    private val switches = mutableListOf<Switch>()

    private val OUTPUT_ON = 0x100 // 256
    private val OUTPUT_OFF = 0x200 // 512
    private val UNIT_ID = 1 // Slave ID default
    private val START_ADDRESS = 0 // Jika PLC mulai dari 40001, ubah ke 40001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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

    /** Update status koneksi */
    private fun updateConnectionStatus(isConnected: Boolean) {
        if (isConnected) {
            binding.btnConnect.text = "Connected"
            binding.btnConnect.setBackgroundColor(Color.parseColor("#4CAF50"))
            switches.forEach { it.isEnabled = true }
        } else {
            binding.btnConnect.text = "Connect"
            binding.btnConnect.setBackgroundColor(Color.parseColor("#F44336"))
            switches.forEach {
                it.isEnabled = false
                it.isChecked = false
            }
        }
    }

    /** Koneksi ke Modbus */
    private fun connectModbus() {
        val ipPort = binding.etIpPort.text.toString().trim() // Ambil dari EditText
        val parts = ipPort.split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 502

        activityScope.launch {
            try {
                val client = ModbusTCPMaster(host, port)
                client.connect()
                modbus = client

                withContext(Dispatchers.Main) {
                    updateConnectionStatus(true)
                    Toast.makeText(this@CustomActivity, "Connected to $host:$port", Toast.LENGTH_SHORT).show()
                    readAllChannels()
                }
            } catch (e: Exception) {
                modbus = null
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(false)
                    Toast.makeText(this@CustomActivity, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Disconnect Modbus */
    private fun disconnectModbus() {
        activityScope.launch {
            try {
                modbus?.disconnect()
                modbus = null
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(false)
                    Toast.makeText(this@CustomActivity, "Disconnected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CustomActivity, "Error disconnecting: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Membuat UI 12 channel */
    private fun buildChannelUI() {
        val customNames = (1..12).map { "Channel $it" }

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
                setOnCheckedChangeListener { _, isChecked ->
                    writeRegister(i, isChecked)
                }
            }

            val innerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                addView(label)
                addView(toggle)
            }

            val card = MaterialCardView(this).apply {
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

        binding.channelContainer.addView(gridLayout)
    }

    /** Menulis register */
    private fun writeRegister(index: Int, state: Boolean) {
        activityScope.launch {
            try {
                val address = START_ADDRESS + index
                val value = if (state) OUTPUT_ON else OUTPUT_OFF
                modbus?.writeSingleRegister(UNIT_ID, address, SimpleRegister(value))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CustomActivity, "Write OK: Addr=$address, Value=$value", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CustomActivity, "Failed to write: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Membaca semua channel */


    private fun readAllChannels() {
        activityScope.launch {
            try {
                val response = modbus?.readMultipleRegisters(START_ADDRESS, 12)
                response?.let { registers ->
                    withContext(Dispatchers.Main) {
                        for (i in 0 until 12) {
                            val value = registers[i].value
                            val isOn = (value != 0) // ✅ ON jika bukan 0
                            switches[i].isChecked = isOn
                            labels[i].text = if (isOn) "ON" else "OFF"
                            labels[i].setBackgroundColor(
                                if (isOn) Color.parseColor("#00AA00") else Color.parseColor("#D41900")
                            )
                            Log.d("Modbus", "Register[$i] = $value")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CustomActivity, "Failed to read: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


}
