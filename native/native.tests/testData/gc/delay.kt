// FREE_COMPILER_ARGS: -opt-in=kotlin.native.runtime.NativeRuntimeApi -opt-in=kotlin.ExperimentalStdlibApi

import kotlin.native.runtime.*
import kotlin.test.*
import platform.posix.usleep

inline fun <T> withGCDelay(block: () -> T): T {
    try {
        GC.Delay.disallowGC()
        return block()
    } finally {
        GC.Delay.allowGC()
    }
}

@Test
fun delayGCSchedule() {
    GC.collect() // ensure there was at least one GC.
    val lastGC = withGCDelay {
        val lastGC = GC.lastGCInfo!!
        val lastGCDelayCount = GC.Delay.gcDelayCount
        GC.schedule()
        // Wait for the scheduled GC to be delayed.
        while (GC.Delay.gcDelayCount == lastGCDelayCount) {
            usleep(1000U)
        }
        // Sleep 100ms and check that no GC happened.
        usleep(100_000U)
        assertEquals(lastGC.epoch, GC.lastGCInfo!!.epoch)
        lastGC
    }

    // And now wait for the GC to happen.
    while (GC.lastGCInfo!!.epoch == lastGC.epoch) {
        usleep(1000U)
    }
}

@Test
fun doNotDelayGCPerform() {
    GC.collect() // ensure there was at least one GC.
    withGCDelay {
        val lastGC = GC.lastGCInfo!!
        val lastGCDelayCount = GC.Delay.gcDelayCount
        GC.collect()
        assertEquals(lastGC.epoch + 1, GC.lastGCInfo!!.epoch)
        assertEquals(lastGCDelayCount, GC.Delay.gcDelayCount)
    }
}