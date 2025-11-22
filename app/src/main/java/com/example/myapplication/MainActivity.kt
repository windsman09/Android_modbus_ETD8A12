
package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val activityScope = CoroutineScope(Dispatchers.IO)
    private var modbus: ModbusTCPMaster? = null

    private val labels = mutableListOf<TextView>()
    private val switches = mutableListOf<Switch>()

    private val OUTPUT_ON = 0x100
    private val OUTPUT_OFF = 0x200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateConnectionStatus(false)
        buildChannelUI()

        binding.btnConnect.setOnClickListener {
            if (modbus == null) connectModbus() else disconnectModbus()
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
                    readAllChannels() // Read initial state after connect
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
        for (i in 0 until 12) {
            val label = TextView(this).apply {
                text = "Channel ${i + 1}"
                textSize = 18f
                setBackgroundColor(Color.parseColor("#4F4F4F")) // Default OFF color
                setTextColor(Color.WHITE)
            }
            val toggle = Switch(this).apply {
                isEnabled = false
                setOnCheckedChangeListener { _, isChecked ->
                    if (modbus != null) {
                        writeRegister(i, isChecked)
                        // Update UI immediately after write
                        label.text = if (isChecked) "ON" else "OFF"
                        label.setBackgroundColor(if (isChecked) Color.parseColor("#00AA00") else Color.parseColor("#4F4F4F"))
                    }
                }
            }

            labels.add(label)
            switches.add(toggle)

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(label)
                addView(toggle)
            }

            binding.channelContainer.addView(layout)
        }
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
                // This returns a Register[] array
                val response = modbus?.readMultipleRegisters(0, 12)
                response?.let {
                    withContext(Dispatchers.Main) {
                        for (i in 0 until 12) {
                            // Access the register by its index in the array
                            val value = it[i].value
                            val isOn = (value == OUTPUT_ON)
                            switches[i].isChecked = isOn
                            labels[i].text = if (isOn) "ON" else "OFF"
                            labels[i].setBackgroundColor(if (isOn) Color.parseColor("#00AA00") else Color.parseColor("#4F4F4F"))
                            
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

}
