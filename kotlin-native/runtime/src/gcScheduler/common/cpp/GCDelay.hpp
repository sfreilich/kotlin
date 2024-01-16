/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <chrono>
#include <condition_variable>
#include <mutex>

#include "Clock.hpp"
#include "GCStatistics.hpp"
#include "Utils.hpp"

namespace kotlin::gcScheduler {

template <typename Clock>
class GCDelay : private Pinned {
public:
    GCDelay() noexcept = default;

    void allowGC() noexcept {
        std::unique_lock guard{mutex_};
        --disallowCounter_;
        auto canAllow = disallowCounter_ == 0;
        guard.unlock();
        if (canAllow) {
            cv_.notify_all();
        }
    }

    void disallowGC() noexcept {
        std::unique_lock guard{mutex_};
        ++disallowCounter_;
        // Ideally, this should not happen, but protect just in case.
        auto canAllow = disallowCounter_ == 0;
        guard.unlock();
        if (canAllow) {
            cv_.notify_all();
        }
    }

    bool waitGCAllowed(int64_t epoch, std::chrono::microseconds maxDuration) noexcept {
        std::unique_lock guard{mutex_};
        auto canStop = [&]() noexcept { return disallowCounter_ == 0 || blockedEpoch_ >= epoch; };
        if (canStop()) {
            // Nothing to wait for.
            return true;
        }
        int64_t maxDurationUs = std::chrono::microseconds(maxDuration).count();
        ++gcDelayCount_;
        GCLogDebug(epoch, "In GC delay zone #%" PRIu64 ". Waiting to start GC for maximum of %" PRId64 " us", gcDelayCount_, maxDurationUs);
        bool gcAllowed = Clock::wait_for(cv_, guard, maxDuration, canStop);
        if (!gcAllowed) {
            GCLogWarning(epoch, "In GC delay zone #%" PRIu64 ". Timed out waiting to start GC", gcDelayCount_);
        }
        return gcAllowed;
    }

    bool tryGCAssist(int64_t epoch) noexcept {
        std::unique_lock guard{mutex_};
        if (blockedEpoch_ == epoch) {
            return true;
        }
        if (disallowCounter_ > 0) {
            return false;
        }
        blockedEpoch_ = epoch;
        return true;
    }

    void onMutatorWillWaitForGC(int64_t epoch) noexcept {
        std::unique_lock guard{mutex_};
        blockedEpoch_ = epoch;
        guard.unlock();
        cv_.notify_all();
    }

    uint64_t gcDelayCount() noexcept {
        std::unique_lock guard{mutex_};
        return gcDelayCount_;
    }

private:
    std::mutex mutex_;
    std::condition_variable cv_;
    int64_t disallowCounter_ = 0;
    int64_t blockedEpoch_ = 0;
    uint64_t gcDelayCount_ = 0;
};

}