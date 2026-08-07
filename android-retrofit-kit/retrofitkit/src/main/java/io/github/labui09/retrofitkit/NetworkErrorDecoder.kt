package io.github.labui09.retrofitkit

import android.content.Context
import okhttp3.Response

/**
 * 网络错误解码接口 — 用户实现此接口以提供自定义的错误消息。
 *
 * 典型用法：根据服务端返回的错误码或网络状态返回用户可读的中文提示。
 *
 * 示例：
 * ```
 * val errorDecoder = object : NetworkErrorDecoder {
 *     override fun getErrorMessage(context: Context, response: Response?, isNetworkAvailable: Boolean): String {
 *         return when (response?.code()) {
 *             500 -> "服务器内部错误，请稍后重试"
 *             else -> if (!isNetworkAvailable) "网络不可用，请检查网络连接" else "请求失败"
 *         }
 *     }
 * }
 * ```
 */
interface NetworkErrorDecoder {

    /**
     * 根据 HTTP 响应和网络状态返回错误消息。
     *
     * @param context          Android Context
     * @param response         OkHttp Response（可能为 null，表示网络层错误）
     * @param isNetworkAvailable 当前是否有网络连接
     * @return 用户可读的错误消息字符串
     */
    fun getErrorMessage(context: Context, response: Response?, isNetworkAvailable: Boolean): String
}
