package com.shopmate.app.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import com.shopmate.app.util.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Talks to classic Bluetooth thermal printers (58mm / 80mm, ESC/POS compatible)
 * over the SPP serial profile.
 *
 * How to use:
 *  1. Pair the printer once in Android Settings -> Bluetooth.
 *  2. Pick it in the app (Settings -> Select printer).
 *  3. Call [print] with the receipt text.
 */
class ThermalPrinter(private val context: Context) {

    sealed interface PrintResult {
        data object Success : PrintResult
        data object NoPrinterConfigured : PrintResult
        data class Error(val message: String) : PrintResult
    }

    data class PairedDevice(val name: String, val address: String)

    /** Devices already paired in Android Bluetooth settings. */
    @SuppressLint("MissingPermission") // callers ensure BLUETOOTH_CONNECT is granted
    fun pairedDevices(): List<PairedDevice> {
        val adapter = bluetoothAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices
                ?.asSequence()
                ?.map { PairedDevice(it.name ?: it.address, it.address) }
                ?.sortedBy { it.name }
                ?.toList()
                ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** Prints [text] on the printer selected in Settings. */
    @SuppressLint("MissingPermission")
    suspend fun print(text: String): PrintResult = withContext(Dispatchers.IO) {
        val address = Settings(context).printerAddress
            ?: return@withContext PrintResult.NoPrinterConfigured
        val adapter = bluetoothAdapter()
            ?: return@withContext PrintResult.Error("Bluetooth is not available on this device")

        try {
            val device = adapter.getRemoteDevice(address)
            adapter.cancelDiscovery()

            device.createRfcommSocketToServiceRecord(SPP_UUID).use { socket ->
                socket.connect()
                socket.outputStream.write(escPos(text))
                socket.outputStream.flush()
            }
            PrintResult.Success
        } catch (e: SecurityException) {
            PrintResult.Error("Bluetooth permission is missing")
        } catch (e: IOException) {
            PrintResult.Error("Printer not reachable — is it switched on and paired?")
        }
    }

    private fun bluetoothAdapter() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    /** ESC/POS commands limited to what almost every thermal printer supports. */
    private fun escPos(text: String): ByteArray {
        val init = byteArrayOf(0x1B, 0x40)              // ESC @  -> reset
        val cut = byteArrayOf(0x1D, 0x56, 0x00)         // GS V 0 -> full cut
        // CP437 is the standard English code page on thermal printers
        val body = (text + "\n\n\n").toByteArray(charset("Cp437"))
        return init + body + cut
    }

    companion object {
        private val SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        const val TEST_TEXT =
            "--------------------------------\n" +
            "      SHOPMATE PRINTER OK\n" +
            "  Printer is connected & ready\n" +
            "--------------------------------\n"
    }
}
