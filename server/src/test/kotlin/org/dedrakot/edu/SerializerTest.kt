package org.dedrakot.edu

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dedrakot.impl.ItemInfoImpl
import org.dedrakot.impl.PathInfoImpl
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SerializerTest {
    private val json = Json

    @Test
    fun serializePathInfo() {
        val inf = PathInfoImpl("directory")
        val str = json.encodeToString(inf)

        assertEquals(inf.name, json.decodeFromString<PathInfoImpl>(str).name)
    }

    @Test
    fun serializeItemInfo() {
        val inf = ItemInfoImpl("testItem", 100L)

        val str = json.encodeToString(inf)
        val decoded = json.decodeFromString<ItemInfoImpl>(str)

        assertEquals(inf.name, decoded.name)
        assertEquals(inf.size, decoded.size)
    }
}