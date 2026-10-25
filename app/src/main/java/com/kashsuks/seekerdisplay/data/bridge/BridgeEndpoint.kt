package com.kashsuks.seekerdisplay.data.bridge

data class BridgeEndpoint(val host: String, val port: Int) {
    val baseUrl: String get() = "http://$host:$port"

    companion object {
        const val DEFAULT_PORT = 8765

        /** Parses "192.168.1.5", "192.168.1.5:9000" or "http://host:9000/"; null if blank or invalid. */
        fun parse(text: String): BridgeEndpoint? {
            val cleaned = text.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
            if (cleaned.isEmpty()) return null
            val host = cleaned.substringBeforeLast(':', cleaned)
            val portText = if (cleaned.contains(':')) cleaned.substringAfterLast(':') else ""
            val port = if (portText.isEmpty()) DEFAULT_PORT else portText.toIntOrNull() ?: return null
            if (host.isEmpty() || port !in 1..65535) return null
            return BridgeEndpoint(host, port)
        }
    }
}
