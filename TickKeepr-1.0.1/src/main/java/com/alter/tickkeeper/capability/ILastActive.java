package com.alter.tickkeeper.capability;

/**
 * Tracks the real-world (epoch millisecond) timestamp of the last moment a
 * block entity is known to have been loaded and ticking.
 *
 * <p>This is intentionally just a timestamp, not a tick counter: game ticks
 * stop existing entirely while the world is closed (single-player quit,
 * server stopped), so the only clock that keeps running during an "offline"
 * gap is the real one. When the block loads again, comparing this timestamp
 * to {@code System.currentTimeMillis()} tells us how much real time passed,
 * which we then convert to an equivalent number of game ticks to replay.
 */
public interface ILastActive {

    long getLastActiveTime();

    void setLastActiveTime(long epochMillis);
}
