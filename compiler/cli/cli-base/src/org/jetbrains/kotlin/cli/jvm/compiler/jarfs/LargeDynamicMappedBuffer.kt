/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.google.common.primitives.Longs.min
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.zip.Inflater

private const val AVOID_COPYING_DATA_LENGTH_THRESHOLD = 1024 * 1024

internal class LargeDynamicMappedBuffer(
    private val channel: FileChannel,
    private val fileSystem: FastJarFileSystem,
    private val mapMode: FileChannel.MapMode,
    private val maxSize: Long
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
        currentMappedBuffer?.let {
            with(fileSystem) {
                it.unmapBuffer()
            }
        }
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

        fun Inflater.setInput(offset: Int, length: Int) {
            if (length < AVOID_COPYING_DATA_LENGTH_THRESHOLD) {
                setInput(getBytes(offset, length))
            } else {
                inflaterInputSetter.invoke(this, this@Mapping, offset, length)
            }
        }

        companion object {

            val inflaterInputSetter: (Inflater, Mapping, Int, Int) -> Unit

            init {
                val javaVersion = CompilerSystemProperties.JAVA_VERSION.value?.toIntOrNull()
                if (javaVersion != null && javaVersion >= 16) {
                    val slice = Class.forName("java.nio.MappedByteBuffer").getMethod("slice", Int::class.java, Int::class.java)
                    val setInput = Class.forName("java.util.zip.Inflater").getMethod("setInput", ByteBuffer::class.java)

                    inflaterInputSetter = { inflater: Inflater, mapping: Mapping, offset: Int, length: Int ->
                        val b1 = slice.invoke(mapping.buffer, mapping.baseOffset + offset, length)
                        setInput.invoke(inflater, b1)
                    }
                } else {
                    inflaterInputSetter = { inflater: Inflater, mapping: Mapping, offset: Int, length: Int ->
                        inflater.setInput(mapping.getBytes(offset, length))
                    }
                }
            }
        }
    }
}