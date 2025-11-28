package com.example.modol

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.modol.databinding.ActivityModul1Binding
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import com.ghgande.j2mod.modbus.procimg.SimpleRegister
import kotlinx.coroutines.*

class Modul1Activity : AppCompatActivity() { // FIX 1: Inherit from AppCompatActivity
    // FIX 2: Correctly initialize the view binding property
    private val binding by lazy { ActivityModul1Binding.inflate(layoutInflater) }
    private val activityScope = CoroutineScope(Dispatchers.IO)
    private var modbus: ModbusTCPMaster? = null

    private val labels = mutableListOf<TextView>()
    private val switches = mutableListOf<Switch>()
    private val OUTPUT_ON = 0x100 // 256
    private val OUTPUT_OFF = 0x200 // 512
    private val UNIT_ID = 1 // Slave ID default
    private val START_ADDRESS = 0 // Jika PLC mulai dari 40001, ubah ke 40001


    // It's good practice to define constants like these in a companion object.
    companion object {
        private const val OUTPUT_ON = 0x100
        private const val OUTPUT_OFF = 0x200
        private const val DEFAULT_IP_PORT = "10.21.240.2:5000"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initChannelsFromXml() // ✅ Ambil dari XML
        updateConnectionStatus(true)

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
        labels.add(binding.labelCh2)
        switches.add(binding.switchCh2)
        labels.add(binding.labelCh3)
        switches.add(binding.switchCh3)
        labels.add(binding.labelCh4)
        switches.add(binding.switchCh4)
        labels.add(binding.labelCh5)
        switches.add(binding.switchCh5)
        labels.add(binding.labelCh6)
        switches.add(binding.switchCh6)
        labels.add(binding.labelCh7)
        switches.add(binding.switchCh7)
        labels.add(binding.labelCh8)
        switches.add(binding.switchCh8)
        labels.add(binding.labelCh9)
        switches.add(binding.switchCh9)
        labels.add(binding.labelCh10)
        switches.add(binding.switchCh10)
        labels.add(binding.labelCh11)
        switches.add(binding.switchCh11)
        labels.add(binding.labelCh12)
        switches.add(binding.switchCh12)

        // Tambahkan semua channel sesuai XML
        // labels.add(binding.labelCh2)
        // switches.add(binding.switchCh2)
        // ...
        // Sampai labelCh12 dan switchCh12

        // Set listener untuk setiap switch
        switches.forEachIndexed { index, switch ->
            switch.isEnabled = true
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
                // The j2mod library suggests setting keepAlive to true for TCP connections.
                modbus = ModbusTCPMaster(host, port)
                //modbus?.setKeepAlive(true) // FIX: Use the setKeepAlive(boolean) method
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
    private fun writeRegister(index: Int, state: Boolean) {
        activityScope.launch {
            try {
                val address = START_ADDRESS + index
                val value = if (state) OUTPUT_ON else OUTPUT_OFF
                modbus?.writeSingleRegister(UNIT_ID, address, SimpleRegister(value))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Modul1Activity, "Write OK: Addr=$address, Value=$value", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Modul1Activity, "Failed to write: ${e.message}", Toast.LENGTH_LONG).show()
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

                            // Jangan ubah nama label, hanya ubah warna
                            labels[i].setBackgroundColor(
                                if (isOn) Color.parseColor("#00AA00") else Color.parseColor("#D41900")
                            )

                            Log.d("Modbus", "Register[$i] = $value")
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


    // This function can be removed as it's not needed.
    // private fun extracted() {
    //    androidx.appcompat.app.AppCompatActivity()
    // }
}
