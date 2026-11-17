package com.freeturn.app.freeturnx.socksclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Socks5ClientConfigTest {

    @Test
    fun urlRoundTrip() {
        val config = Socks5ClientConfig("192.168.43.1", 1081, "us er", "p@ss:w")
        assertEquals(config, Socks5ClientConfig.parseUrl(config.proxyUrl))
    }

    @Test
    fun urlWithoutCredentialsAndPort() {
        assertEquals(Socks5ClientConfig("10.0.0.1", 1080), Socks5ClientConfig.parseUrl("socks5://10.0.0.1"))
    }

    @Test
    fun notSocks() {
        assertNull(Socks5ClientConfig.parseUrl("freeturn://abc"))
        assertNull(Socks5ClientConfig.parseUrl("garbage"))
    }
}
