package io.github.fallenleaves089.retrofitkit

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter
import java.math.BigDecimal

class PriceTypeAdapterTest {

    private val adapter = PriceTypeAdapter()

    @Test
    fun readsNumberTokenAsBigDecimal() {
        val reader = JsonReader(StringReader("123.4500"))

        assertEquals(BigDecimal("123.4500"), adapter.read(reader))
    }

    @Test
    fun readsStringTokenAsBigDecimal() {
        val reader = JsonReader(StringReader("\"19.99\""))

        assertEquals(BigDecimal("19.99"), adapter.read(reader))
    }

    @Test
    fun readsNullAsNull() {
        val reader = JsonReader(StringReader("null"))

        assertNull(adapter.read(reader))
    }

    @Test
    fun writesBigDecimalAsFixedScaleString() {
        val out = StringWriter()
        val writer = JsonWriter(out)

        adapter.write(writer, BigDecimal("19.999"))
        writer.flush()

        assertEquals("\"20.00\"", out.toString())
    }

    @Test
    fun writesNullAsNull() {
        val out = StringWriter()
        val writer = JsonWriter(out)

        adapter.write(writer, null)
        writer.flush()

        assertEquals("null", out.toString())
    }
}
