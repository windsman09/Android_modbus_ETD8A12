package com.example.modol

import android.graphics.Color
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.modol.databinding.ActivityModul2Binding // <-- IMPORT the correct binding
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.*
import com.ghgande.j2mod.modbus.procimg.SimpleRegister

class Modul2Activity : AppCompatActivity() {

    // Use the binding generated from activity_modul2.xml
    private val binding by lazy { ActivityModul2Binding.inflate(layoutInflater) }
    private val activityScope = CoroutineScope(Dispatchers.IO)
    private var modbus: ModbusTCPMaster? = null

    private val labels = mutableListOf<TextView>()
    private val switches = mutableListOf<Switch>()

    private val OUTPUT_ON = 0x100 // 256
    private val OUTPUT_OFF = 0x200 // 512

    // Consider making this configurable or passed via intent
    private val DEFAULT_IP_PORT = "10.21.240.2:5000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // The ID 'bgMdl' must exist in your new layout
        val title = binding.bgMdl
        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        title.startAnimation(animation)

        val lantai = intent.getIntExtra("lantai", 1)
        // You might want to update the title dynamically
        title.text = "Kontrol Relay Modul 2"

        updateConnectionStatus(false)
        buildChannelUI()

        binding.btnConnect.setOnClickListener {
            if (modbus == null) connectModbus() else disconnectModbus()
        }

        binding.btnRefresh.setOnClickListener {
            if (modbus != null) readAllChannels()
        }
    }

    // ... (the rest of your functions: onDestroy, buildChannelUI, etc. remain the same)

    // Make sure your other view IDs match the layout file

    /** Membuat UI channel relay */
    private fun buildChannelUI() {
        val customNames = listOf(
            "Lampu K1A", "Lampu K1C", "K1A AC1", "K1C AC1",
            "K1A AC2", "K1C AC2", "Lampu K1B", "Lampu K1D",
            "K1B AC1", "K1D AC1", "K1B AC2", "K1D AC2"
        )

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

        val frameLayout = FrameLayout(this).apply {
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.LTGRAY)
            addView(gridLayout)
        }

        // This line will now work correctly
        binding.channelsContainer.addView(frameLayout)
    }

    /** Menulis register ke Modbus */
    private fun writeRegister(channel: Int, isOn: Boolean) {
        activityScope.launch {
            try {
                val value = if (isOn) OUTPUT_ON else OUTPUT_OFF
                modbus?.writeSingleRegister(channel, SimpleRegister(value))
                withContext(Dispatchers.Main) {
                    readAllChannels()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Modul2Activity, "Failed to write: ${e.message}", Toast.LENGTH_LONG).show()
                    switches[channel].isChecked = !isOn
                }
            }
        }
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
        activityScope.launch {
            val parts = DEFAULT_IP_PORT.split(":")
            val host = parts[0]
            val port = if (parts.size == 2) parts[1].toIntOrNull() ?: 502 else 502

            try {
                modbus = ModbusTCPMaster(host, port)
                modbus?.connect()

                withContext(Dispatchers.Main) {
                    updateConnectionStatus(true)
                    Toast.makeText(this@Modul2Activity, "Connected to $host:$port", Toast.LENGTH_SHORT).show()
                    readAllChannels()
                    startAutoRefresh()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(false)
                    Toast.makeText(this@Modul2Activity, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Auto refresh setiap 5 detik */
    private fun startAutoRefresh() {
        activityScope.launch {
            while (modbus != null) {
                readAllChannels()
                delay(5000)
            }
        }
    }

    /** Membaca semua channel */
    private fun readAllChannels() {
        activityScope.launch {
            try {
                val response = modbus?.readMultipleRegisters(0, 12)
                response?.let {
                    withContext(Dispatchers.Main) {
                        for (i in 0 until 12) {
                            val value = it[i].value
                            val isOn = (value == OUTPUT_ON || value == 256)
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
                    Toast.makeText(this@Modul2Activity, "Failed to read: ${e.message}", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this@Modul2Activity, "Disconnected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(false)
                    Toast.makeText(this@Modul2Activity, "Error disconnecting: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
