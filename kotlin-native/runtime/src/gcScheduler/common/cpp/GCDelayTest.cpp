/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCDelay.hpp"

#include <future>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ClockTestSupport.hpp"
#include "ScopedThread.hpp"
#include "TestSupport.hpp"
#include "Utils.hpp"

using namespace kotlin;

namespace {

template <typename T>
class ScopedFuture : private MoveOnly {
public:
    T get() { return future_.get(); }

private:
    template <typename F, typename... Args>
    friend ScopedFuture<std::invoke_result_t<std::decay_t<F>, std::decay_t<Args>...>> scopedAsync(F&& f, Args&&... args) noexcept;

    ScopedFuture(std::future<T> future, ScopedThread thread) noexcept : future_(std::move(future)), thread_(std::move(thread)) {}

    std::future<T> future_;
    ScopedThread thread_;
};

template <typename F, typename... Args>
[[nodiscard]] ScopedFuture<std::invoke_result_t<std::decay_t<F>, std::decay_t<Args>...>> scopedAsync(F&& f, Args&&... args) noexcept {
    using Result = std::invoke_result_t<std::decay_t<F>, std::decay_t<Args>...>;
    std::promise<Result> promise;
    auto future = promise.get_future();
    ScopedThread thread([](std::promise<Result> promise, F&& f, Args&&... args) noexcept {
        if constexpr (std::is_void_v<Result>) {
            std::invoke(std::forward<F>(f), std::forward<Args>(args)...);
            promise.set_value();
        } else {
            promise.set_value(std::invoke(std::forward<F>(f), std::forward<Args>(args)...));
        }
    }, std::move(promise), std::forward<F>(f), std::forward<Args>(args)...);
    return ScopedFuture<Result>(std::move(future), std::move(thread));
}

using GCDelay = gcScheduler::GCDelay<test_support::manual_clock>;

template <typename WaitDuration, typename SleepDuration>
void waitAndSleep(WaitDuration waitDuration, SleepDuration sleepDuration) noexcept {
    auto expectedAt = test_support::manual_clock::now() + waitDuration;
    auto sleepUntil = test_support::manual_clock::now() + sleepDuration;
    test_support::manual_clock::waitForPending(expectedAt);
    test_support::manual_clock::sleep_until(sleepUntil);
}

template <typename Duration>
void waitAndSleep(Duration duration) noexcept {
    waitAndSleep(duration, duration);
}

template <typename WaitDuration, typename SleepDuration, typename Action>
[[nodiscard]] ScopedFuture<void> waitAndSleepAsync(WaitDuration waitDuration, SleepDuration sleepDuration, Action action) noexcept {
    return scopedAsync([waitDuration, sleepDuration, action]() noexcept {
        waitAndSleep(waitDuration, sleepDuration);
        action();
    });
}

template <typename WaitDuration, typename SleepDuration>
[[nodiscard]] ScopedFuture<void> waitAndSleepAsync(WaitDuration waitDuration, SleepDuration sleepDuration) noexcept {
    return waitAndSleepAsync(waitDuration, sleepDuration, []() noexcept {});
}

template <typename Duration>
[[nodiscard]] ScopedFuture<void> waitAndSleepAsync(Duration duration) noexcept {
    return waitAndSleepAsync(duration, duration, []() noexcept {});
}

inline constexpr auto defaultDuration = std::chrono::hours(10);
inline constexpr auto smallDuration = std::chrono::seconds(1);

}

class GCDelayTest : public testing::Test {
public:
    GCDelayTest() noexcept {
        test_support::manual_clock::reset();
    }
};

TEST(GCDelayTest, WaitGCAllowedUnactivated) {
    GCDelay delay;
    EXPECT_TRUE(delay.waitGCAllowed(1, defaultDuration));
}

TEST(GCDelayTest, TryGCAssistUnactivated) {
    GCDelay delay;
    EXPECT_TRUE(delay.tryGCAssist(1));
}

TEST(GCDelayTest, OnMutatorWillWaitForGCUnactivated) {
    GCDelay delay;
    delay.onMutatorWillWaitForGC(1);
}

TEST(GCDelayTest, WaitGCAllowedActivated) {
    GCDelay delay;
    delay.disallowGC();
    auto f = waitAndSleepAsync(defaultDuration);
    EXPECT_FALSE(delay.waitGCAllowed(1, defaultDuration));
}

TEST(GCDelayTest, TryGCAssistActivated) {
    GCDelay delay;
    delay.disallowGC();
    EXPECT_FALSE(delay.tryGCAssist(1));
}

TEST(GCDelayTest, OnMutatorWillWaitForGCActivated) {
    GCDelay delay;
    delay.disallowGC();
    delay.onMutatorWillWaitForGC(1);
}

TEST(GCDelayTest, WaitGCAllowedDeactivatedAsync) {
    GCDelay delay;
    delay.disallowGC();
    auto f = waitAndSleepAsync(defaultDuration, smallDuration, [&]() noexcept { delay.allowGC(); });
    EXPECT_TRUE(delay.waitGCAllowed(1, defaultDuration));
}

TEST(GCDelayTest, WaitGCAllowedAfterAssistsStarted) {
    GCDelay delay;
    ASSERT_TRUE(delay.tryGCAssist(1));
    delay.disallowGC();
    EXPECT_TRUE(delay.waitGCAllowed(1, defaultDuration));
}

TEST(GCDelayTest, TryGCAssistAfterAssistsStarted) {
    GCDelay delay;
    ASSERT_TRUE(delay.tryGCAssist(1));
    delay.disallowGC();
    EXPECT_TRUE(delay.tryGCAssist(1));
}

TEST(GCDelayTest, WaitGCAllowedAfterAssistsForPreviousEpoch) {
    GCDelay delay;
    ASSERT_TRUE(delay.tryGCAssist(1));
    delay.disallowGC();
    auto f = waitAndSleepAsync(defaultDuration);
    EXPECT_FALSE(delay.waitGCAllowed(2, defaultDuration));
}

TEST(GCDelayTest, TryGCAssistAfterAssistsForPreviousEpoch) {
    GCDelay delay;
    ASSERT_TRUE(delay.tryGCAssist(1));
    delay.disallowGC();
    EXPECT_FALSE(delay.tryGCAssist(2));
}

TEST(GCDelayTest, WaitGCAllowedWhenMutatorWaited) {
    GCDelay delay;
    delay.disallowGC();
    delay.onMutatorWillWaitForGC(1);
    EXPECT_TRUE(delay.waitGCAllowed(1, defaultDuration));
}

TEST(GCDelayTest, WaitGCAllowedInterruptedByWait) {
    GCDelay delay;
    delay.disallowGC();
    auto f = waitAndSleepAsync(defaultDuration, smallDuration, [&]() noexcept { delay.onMutatorWillWaitForGC(1); });
    EXPECT_TRUE(delay.waitGCAllowed(1, defaultDuration));
}

TEST(GCDelayTest, ConcurrentStressTest) {
    // The only condition being tested: the test does not hang.
    GCDelay delay;
    int64_t scheduledEpoch = 0;
    int64_t completedEpoch = 0;
    bool done = false;
    std::mutex epochMutex;
    std::condition_variable scheduledEpochCV;
    std::condition_variable completedEpochCV;
    auto gcThread = ScopedThread([&]() noexcept {
        while (true) {
            std::unique_lock guard{epochMutex};
            scheduledEpochCV.wait(guard, [&]() noexcept { return scheduledEpoch > completedEpoch || done; });
            if (done) {
                return;
            }
            completedEpoch = scheduledEpoch;
            guard.unlock();
            completedEpochCV.notify_all();
        }
    });
    std::vector<ScopedThread> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back([&]() noexcept {
            while (true) {
                std::unique_lock guard{epochMutex};
                if (done)
                    return;
                bool scheduled = false;
                if (scheduledEpoch == completedEpoch) {
                    scheduledEpoch = completedEpoch + 1;
                    scheduled = true;
                }
                auto epoch = scheduledEpoch;
                guard.unlock();
                if (scheduled) {
                    scheduledEpochCV.notify_all();
                }
                if (epoch % 100 != 0) {
                    continue;
                }
                if (!delay.tryGCAssist(epoch)) {
                    continue;
                }
                guard.lock();
                completedEpochCV.wait(guard, [&]() noexcept { return completedEpoch == epoch || done; });
            }
        });
    }
    auto mainThread = ScopedThread([&]() noexcept {
        for (int i = 0; i < 10000000; ++i) {
            delay.disallowGC();
            delay.allowGC();
        }
        {
            std::unique_lock guard{epochMutex};
            done = true;
        }
        scheduledEpochCV.notify_all();
    });
}