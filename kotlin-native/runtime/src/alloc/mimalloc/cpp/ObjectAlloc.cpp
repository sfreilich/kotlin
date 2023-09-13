/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectAlloc.hpp"

#include <mutex>

#include "Alignment.hpp"
#include "CompilerConstants.hpp"
#include "Memory.h"
#include "mimalloc.h"

#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
#include <dispatch/dispatch.h>
#endif

using namespace kotlin;

namespace {

std::once_flag initOptions;

#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
std::atomic_flag scheduledCompactOnMainThread = ATOMIC_FLAG_INIT;
#endif

// Only used when `compiler::gcMemoryBigChunks()` is false.
std::atomic<size_t> allocatedPreciseBytes = 0;

} // namespace

void alloc::initObjectPool() noexcept {
    if (!compiler::mimallocUseDefaultOptions()) {
        std::call_once(initOptions, [] {
            mi_option_enable(mi_option_reset_decommits);
            if (compiler::mimallocUseCompaction()) {
                mi_option_set(mi_option_reset_delay, 0);
            }
        });
    }
    mi_thread_init();
}

void* alloc::allocateInObjectPool(size_t size) noexcept {
    auto ptr = mi_calloc_aligned(1, size, kObjectAlignment);
    if (!compiler::gcMemoryBigChunks()) {
        auto totalAllocatedBytes = allocatedPreciseBytes.fetch_add(size, std::memory_order_relaxed);
        OnMemoryAllocation(totalAllocatedBytes + size);
    }
    return ptr;
}

void alloc::freeInObjectPool(void* ptr, size_t size) noexcept {
    if (!compiler::gcMemoryBigChunks()) {
        allocatedPreciseBytes.fetch_sub(size, std::memory_order_relaxed);
    }
    mi_free(ptr);
}

void alloc::compactObjectPoolInCurrentThread() noexcept {
    if (!compiler::mimallocUseCompaction()) return;
    mi_collect(true);
}

void alloc::compactObjectPoolInMainThread() noexcept {
    if (!compiler::mimallocUseCompaction()) return;
#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
    if (scheduledCompactOnMainThread.test_and_set()) {
        // If it's already scheduled, do nothing.
        return;
    }
    dispatch_async_f(dispatch_get_main_queue(), nullptr, [](void*) {
        if (mm::IsCurrentThreadRegistered()) {
            alloc::compactObjectPoolInCurrentThread();
        }
        scheduledCompactOnMainThread.clear();
    });
#endif
}

size_t alloc::allocatedBytes() noexcept {
    if (compiler::gcMemoryBigChunks()) {
        return mi_allocated_size();
    } else {
        return allocatedPreciseBytes.load(std::memory_order_relaxed);
    }
}

extern "C" void mi_hook_allocation(size_t allocated_size) mi_attr_noexcept {
    if (compiler::gcMemoryBigChunks()) {
        OnMemoryAllocation(allocated_size);
    }
}
