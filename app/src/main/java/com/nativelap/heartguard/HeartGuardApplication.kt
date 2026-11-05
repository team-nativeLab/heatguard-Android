package com.nativelap.heartguard

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HeartGuardApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader
            .Builder(context)
            .memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizeBytes(
                        minOf(
                            (Runtime.getRuntime().maxMemory() * MEMORY_CACHE_HEAP_PERCENT).toLong(),
                            MAX_IMAGE_MEMORY_CACHE_SIZE_BYTES,
                        ),
                    ).build()
            }.build()

    private companion object {
        const val MEMORY_CACHE_HEAP_PERCENT = 0.05
        const val MAX_IMAGE_MEMORY_CACHE_SIZE_BYTES = 4L * 1024L * 1024L
    }
}
