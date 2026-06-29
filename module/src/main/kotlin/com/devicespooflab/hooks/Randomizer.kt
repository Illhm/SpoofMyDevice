package com.devicespooflab.hooks

import java.util.UUID
import kotlin.random.Random

class Randomizer {
    fun generateAll(): Map<String, String> {
        val map = mutableMapOf<String, String>()

        // 1. Build Info (20)
        map["phone_brand"] = listOf("google", "samsung", "xiaomi", "oneplus").random()
        map["phone_model"] = "Model-${Random.nextInt(1000, 9999)}"
        map["phone_manufacturer"] = map["phone_brand"]!!
        map["phone_device"] = "Device-${Random.nextInt(100, 999)}"
        map["phone_board"] = "Board-${Random.nextInt(100, 999)}"
        map["phone_hardware"] = "qcom"
        map["phone_name"] = "Product-${Random.nextInt(100, 999)}"
        map["phone_display"] = "Build.Display.${Random.nextInt(10000, 99999)}"
        map["phone_version_release"] = listOf("10", "11", "12", "13", "14").random()
        map["phone_incremental"] = "eng.${Random.nextInt(10000, 99999)}"
        map["phone_id"] = "ID-${Random.nextInt(100, 999)}"
        map["phone_tags"] = "release-keys"
        map["phone_host"] = "host-${Random.nextInt(10, 99)}"
        map["phone_user"] = "user-${Random.nextInt(10, 99)}"
        map["phone_type"] = "user"
        map["phone_baseband"] = "baseband-${Random.nextInt(1000, 9999)}"
        map["phone_patch"] = "2023-10-01"
        map["phone_build_date"] = System.currentTimeMillis().toString()
        map["phone_build_date_utc"] = (System.currentTimeMillis() / 1000L).toString()
        map["phone_fingerprint"] = "${map["phone_brand"]}/${map["phone_name"]}/${map["phone_device"]}:${map["phone_version_release"]}/${map["phone_id"]}/${map["phone_incremental"]}:user/release-keys"

        // 2. Telephony (8)
        map["sim_operator"] = listOf("310260", "310410", "310120").random()
        map["sim_operator_name"] = "T-Mobile"
        map["sim_country_iso"] = "us"
        map["sim_serial_number"] = "89012" + Random.nextLong(100000000000000L, 999999999999999L).toString()
        map["line_number"] = "+1" + Random.nextLong(1000000000L, 9999999999L).toString()
        map["subscriber_id"] = map["sim_operator"] + Random.nextLong(1000000000L, 9999999999L).toString()
        map["imei_1"] = Random.nextLong(100000000000000L, 999999999999999L).toString()
        map["imei_2"] = Random.nextLong(100000000000000L, 999999999999999L).toString()

        // 3. Identity (5)
        map["android_id"] = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        map["phone_serial"] = UUID.randomUUID().toString().replace("-", "").substring(0, 10).uppercase()
        map["gsf"] = Random.nextLong(1000000000000000L, 9999999999999999L).toString(16)
        map["ads_id"] = UUID.randomUUID().toString()

        // 4. WiFi (4)
        map["ssid"] = "WiFi-${Random.nextInt(1000, 9999)}"
        map["bssid"] = generateMac()
        map["wifi_mac"] = generateMac()
        map["wifi_state"] = "3" // WIFI_STATE_ENABLED

        // 5. Location (6)
        map["geo_latitude"] = (Random.nextDouble() * 180 - 90).toString()
        map["geo_longitude"] = (Random.nextDouble() * 360 - 180).toString()
        map["geo_altitude"] = Random.nextDouble(0.0, 100.0).toString()
        map["geo_accuracy"] = Random.nextDouble(1.0, 20.0).toString()
        map["geo_speed"] = Random.nextDouble(0.0, 30.0).toString()
        map["geo_bearing"] = Random.nextDouble(0.0, 360.0).toString()

        // 6. Bluetooth (1)
        map["bluetooth_mac"] = generateMac()

        // 7. DRM & WebView (2)
        map["drm"] = UUID.randomUUID().toString()
        map["webview_visible"] = "1"

        return map
    }

    private fun generateMac(): String {
        return (1..6).joinToString(":") {
            Random.nextInt(0, 256).toString(16).padStart(2, '0')
        }
    }
}
