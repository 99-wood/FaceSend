package com.example.mainactivity.data

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.buffer

class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (progress: Float) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        val countingSink = CountingSink(sink, totalBytes, onProgress)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private class CountingSink(
        delegate: okio.Sink,
        private val totalBytes: Long,
        private val onProgress: (Float) -> Unit
    ) : okio.ForwardingSink(delegate) {
        private var bytesWritten = 0L

        override fun write(source: okio.Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            // contentLength 为 -1 时上游不会调用 onProgress，避免除零
            if (totalBytes > 0) {
                onProgress(bytesWritten.toFloat() / totalBytes.toFloat())
            }
        }
    }
}
