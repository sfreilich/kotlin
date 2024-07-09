/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.google.common.primitives.Longs.min
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

internal class LargeDynamicMappedBuffer(
    private val channel: FileChannel,
    private val mapMode: FileChannel.MapMode,
    private val maxSize: Long,
    private val unmapBuffer: MappedByteBuffer.() -> Unit
) {

    private var currentMappedBuffer: MappedByteBuffer? = null
    private var currentStart: Long = 0L
    private var currentEnd: Long = 0L

    @Synchronized
    fun <R> withMappedRange(start: Long, end: Long, body: Mapping.() -> R): R {
        require(end in (start + 1)..maxSize && end - start <= Int.MAX_VALUE)
        var currentSize = Int.MAX_VALUE.toLong()
        if (currentMappedBuffer == null || currentStart > start || currentEnd < end) {
            if (maxSize <= Int.MAX_VALUE) {
                currentStart = 0L
                currentEnd = maxSize
                currentSize = maxSize
            } else if (start + Int.MAX_VALUE > maxSize) {
                currentStart = maxSize - Int.MAX_VALUE
                currentEnd = maxSize
            } else {
                currentStart = start
                currentEnd = start + Int.MAX_VALUE
            }
            unmap()
            currentMappedBuffer = channel.map(mapMode, currentStart, currentSize).also {
                it.order(ByteOrder.LITTLE_ENDIAN)
            }
        }
        val buffer = currentMappedBuffer!!
        require(currentStart <= start && currentEnd >= end && start - currentStart < Int.MAX_VALUE)
        buffer.position((start - currentStart).toInt())
        return Mapping(buffer, buffer.position()).body()
    }

    fun <R> withMappedTail(body: Mapping.() -> R): R {
        val size = min(maxSize, Int.MAX_VALUE.toLong())
        return withMappedRange(maxSize - size, maxSize, body)
    }

    fun <R> withMappedRangeFrom(start: Long, body: Mapping.() -> R): R {
        require(start < maxSize)
        val size = min(maxSize - start, Int.MAX_VALUE.toLong())
        return withMappedRange(start, start + size, body)
    }

    fun unmap() {
        currentMappedBuffer?.unmapBuffer()
    }

    class Mapping(private val buffer: MappedByteBuffer, private val baseOffset: Int) {

        fun getInt(offset: Int) = buffer.getInt(baseOffset + offset)
        fun getLong(offset: Int) = buffer.getLong(baseOffset + offset)
        fun getShort(offset: Int) = buffer.getShort(baseOffset + offset)

        fun getBytes(offset: Int, length: Int): ByteArray {
            val bytes = ByteArray(length)
            buffer.position(baseOffset + offset)
            try {
                buffer.get(bytes, 0, length)
                return bytes
            } finally {
                buffer.position(baseOffset)
            }
        }

        fun endOffset() = buffer.capacity() - baseOffset
    }
}