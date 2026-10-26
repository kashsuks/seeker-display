package com.kashsuks.seekerdisplay.data.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BridgeEndpointTest {
    @Test
    fun hostOnlyUsesDefaultPort() {
        assertEquals(BridgeEndpoint("192.168.1.5", 8765), BridgeEndpoint.parse("192.168.1.5"))
    }

    @Test
    fun hostAndPort() {
        assertEquals(BridgeEndpoint("mac.local", 9000), BridgeEndpoint.parse(" mac.local:9000 "))
    }

    @Test
    fun urlFormIsAccepted() {
        assertEquals(BridgeEndpoint("10.0.0.2", 8765), BridgeEndpoint.parse("http://10.0.0.2:8765/"))
    }

    @Test
    fun blankAndInvalidReturnNull() {
        assertNull(BridgeEndpoint.parse(""))
        assertNull(BridgeEndpoint.parse("   "))
        assertNull(BridgeEndpoint.parse("host:abc"))
        assertNull(BridgeEndpoint.parse("host:99999"))
        assertNull(BridgeEndpoint.parse(":8765"))
    }

    @Test
    fun baseUrl() {
        assertEquals("http://10.0.0.2:8765", BridgeEndpoint("10.0.0.2", 8765).baseUrl)
    }
}
