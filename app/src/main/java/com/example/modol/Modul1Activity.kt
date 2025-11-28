
package com.example.modol1.

import android.graphics.Color
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.modol.databinding.ActivityModul1Binding
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import com.ghgande.j2mod.modbus.procimg.SimpleRegister
import kotlinx.coroutines.*ActivityModul1Binding


class Modul1Activity : extracted()() {
    private val binding by lazy { .inflate(layoutInflater) }
    private val activityScope = CoroutineScope(Dispatchers.IO)
    private var modbus: ModbusTCPMaster? = null

    private val labels = mutableListOf<TextView>()
    private val switches = mutableListOf<Switch>()

    private val OUTPUT_ON = 0x100
    private val OUTPUT_OFF = 0x200
    private val DEFAULT_IP_PORT = "10.21.240.2:5000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initChannelsFromXml() // ✅ Ambil dari XML
        updateConnectionStatus(false)

        binding.btnConnect.setOnClickListener {
            if (modbus == null) connectModbus() else disconnectModbus()
        }

        binding.btnRefresh.setOnClickListener {
            if (modbus != null) readAllChannels()
        }
    }

    /** Ambil semua channel dari XML */
    private fun initChannelsFromXml() {
        labels.add(binding.labelCh1)
        switches.add(binding.switchCh1)

        // Tambahkan semua channel sesuai XML
        // labels.add(binding.labelCh2)
        // switches.add(binding.switchCh2)
        // ...
        // Sampai labelCh12 dan switchCh12

        // Set listener untuk setiap switch
        switches.forEachIndexed { index, switch ->
            switch.isEnabled = false
            switch.setOnCheckedChangeListener { _, isChecked ->
                writeRegister(index, isChecked)
            }
        }
    }

    /** Update status koneksi */
    private fun updateConnectionStatus(isConnected: Boolean) {
        if (isConnected) {
            binding.btnConnect.text = "Disconnect"
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
                modbus = ModbusTCPMaster(host, port) // ✅ gunakan keepAlive
                modbus?.connect()
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(true)
                    Toast.makeText(this@Modul1Activity, "Connected to $host:$port", Toast.LENGTH_SHORT).show()
                    readAllChannels()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(false)
                    Toast.makeText(this@Modul1Activity, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this@Modul1Activity, "Disconnected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateConnectionStatus(false)
                    Toast.makeText(this@Modul1Activity, "Error disconnecting: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Menulis register */
    private fun writeRegister(channel: Int, isOn: Boolean) {
        activityScope.launch {
            try {
                val value = if (isOn) OUTPUT_ON else OUTPUT_OFF
                modbus?.writeSingleRegister(channel, SimpleRegister(value))
                delay(500)
                withContext(Dispatchers.Main) {
                    readAllChannels()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Modul1Activity, "Failed to write: ${e.message}", Toast.LENGTH_LONG).show()
                    switches[channel].isChecked = !isOn
                }
            }
        }
    }

    /** Membaca semua channel */

    private fun readAllChannels() {
        activityScope.launch {
            try {
                val response = modbus?.readMultipleRegisters(0, labels.size)
                response?.let {
                    withContext(Dispatchers.Main) {
                        for (i in labels.indices) {
                            val value = it[i].value
                            val isOn = (value == OUTPUT_ON || value == 256)
                            labels[i].text = "${labels[i].text.split(":")[0]} : ${if (isOn) "ON" else "OFF"}"
                            labels[i].setBackgroundColor(
                                if (isOn) Color.parseColor("#00AA00") else Color.parseColor("#4F4F4F")
                            )
                            switches[i].isChecked = isOn
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Modul1Activity, "Failed to read: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}

private fun extracted() {
    androidx.appcompat.app.AppCompatActivity.AppCompatActivity
}
