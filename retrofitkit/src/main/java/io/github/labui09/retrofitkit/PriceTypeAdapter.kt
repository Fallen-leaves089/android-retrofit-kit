package io.github.labui09.retrofitkit

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * BigDecimal 自定义 TypeAdapter — 保护精度不被 Gson 默认的 double 转换截断。
 *
 * Gson 默认将 JSON 数字解析为 double，会导致超过 15 位有效数字的金额等字段精度丢失。
 * 此适配器确保 BigDecimal 字段按原始字符串解析，保留完整精度。
 */
class PriceTypeAdapter : TypeAdapter<BigDecimal>() {

    override fun write(out: JsonWriter, value: BigDecimal?) {
        if (value != null) {
            // 输出为字符串以避免前端 JSON 精度丢失（JavaScript Number 最大安全整数 2^53-1）
            out.value(value.setScale(2, RoundingMode.HALF_UP).toPlainString())
        } else {
            out.nullValue()
        }
    }

    override fun read(reader: JsonReader): BigDecimal? {
        return when (reader.peek()) {
            com.google.gson.stream.JsonToken.NUMBER -> {
                // 用 reader.nextString() 代替 nextDouble() 以保留完整精度
                BigDecimal(reader.nextString())
            }
            com.google.gson.stream.JsonToken.STRING -> {
                val str = reader.nextString()
                if (str.isBlank()) null else BigDecimal(str)
            }
            com.google.gson.stream.JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            else -> null
        }
    }
}
