
package com.bill.kotlin_bluetoothlegatt

import java.util.*

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
object SampleGattAttributes {
    var attributes: HashMap<String, String> = HashMap()
    var HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"


    val QCA402xServiceUUID: UUID = UUID.fromString("00001650-0000-1000-8000-00805f9b34fb")
    val QCA402xCharacteristicUUID: UUID = UUID.fromString("00001651-0000-1000-8000-00805f9b34fb")
    val QCA402xSendCharacteristicUUID: UUID = UUID.fromString("00001652-0000-1000-8000-00805f9b34fb")

    val CLIENT_CHARACTERISTIC_CONFIG:UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    init {
        attributes.put("00001650-0000-1000-8000-00805f9b34fb", "QCA402X Service")
        attributes.put("00001651-0000-1000-8000-00805f9b34fb", "Characteristic")
        attributes.put("00001652-0000-1000-8000-00805f9b34fb", "Send Characteristic")
    }

    fun lookup(uuid: String, defaultName: String): String {
        val name = attributes.get(uuid)
        return name ?: defaultName
    }
}
