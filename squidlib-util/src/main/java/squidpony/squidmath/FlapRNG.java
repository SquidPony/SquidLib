package squidpony.squidmath;

import squidpony.StringKit;
import squidpony.annotation.Beta;

import java.io.Serializable;

/**
 * Similar to XoRoRNG, but like PintRNG this only uses 32-bit int math (good for GWT).
 * It's likely that the period of FlapRNG is a full 2 to the 64 (0 seed is allowed), since this uses a pair of ints for
 * its state, and acts like XoRoRNG modified to handle all initial states (hopefully, at least... there may be one seed
 * pair that corresponds to the original "bad seed" of 0) and, of course, use only int math where possible. This is
 * slightly faster than XoRoRNG at producing ints, and as you would expect, twice as slow when generating longs.
 * <br>
 * Created by Tommy Ettinger on 5/1/2017.
 */
@Beta
public class FlapRNG implements StatefulRandomness, Serializable {
    private static final long serialVersionUID = 1L;
    public FlapRNG(){
        this((int)((Math.random() * 2.0 - 1.0) * 0x80000000));
    }
    public FlapRNG(final int seed) {
        state0 = seed;
        state1 = seed ^ seed >>> (4 + (seed >>> 28));
        state1 *= 277803737;
        state1 ^= (state1 >>> 22);
    }
    public FlapRNG(final int seed0, final int seed1) {
        state0 = seed0;
        state1 = seed1;
    }
    public FlapRNG(final long seed) {
        state0 = (int)(seed & 0xFFFFFFFFL);
        state1 = (int)(seed >>> 32);
    }
    public FlapRNG(final CharSequence seed)
    {
        this(CrossHash.Wisp.hash64(seed));
    }

    public int state0, state1;
    /**
     * Get the current internal state of the StatefulRandomness as a long.
     *
     * @return the current internal state of this object.
     */
    @Override
    public long getState() {
        return (long)(state1) << 32 | (state0 & 0xFFFFFFFFL);
    }

    /**
     * Set the current internal state of this StatefulRandomness with a long.
     *
     * @param state a 64-bit long, but this is always truncated when used; the upper 32 bits are discarded
     */
    @Override
    public void setState(final long state) {
        state0 = (int)(state & 0xFFFFFFFFL);
        state1 = (int)(state >>> 32);
    }

    /**
     * Using this method, any algorithm that might use the built-in Java Random
     * can interface with this randomness source.
     *
     * @param bits the number of bits to be returned
     * @return the integer containing the appropriate number of bits
     */
    @Override
    public final int next( final int bits ) {
        return nextInt() >>> (32 - bits);
    }

    public final int nextInt()
    {
        final int s0 = state0;
        int s1 = state1;
        final int result = s0 + s1;
        s1 ^= s0 + 0x9E3779B9;
        state0 = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 7); // a, b
        state1 = (s1 << 18 | s1 >>> 14); // c
        return result;

        //0x632BE5AB
        //final int z = (state += 0x9E3779B9);
        //return (z >> (z & 15)) * 0xC6BC278D;
        //final int z = (state + 0x9E3779B9);
        //return state += ((state + 0x9E3779B9) >> 5) * 0xC6BC278D;
        //final int z = (state += 0x9E3779B9), r = (z & 15);
        //return (z >> r) * 0xC6BC278D;
        //return state += (state >> 5) * 0xC6BC279692B5CC83L + 0x9E3779B97F4A7C15L;

    }
    /**
     * Using this method, any algorithm that needs to efficiently generate more
     * than 32 bits of random data can interface with this randomness source.
     * This implementation produces a different result than calling {@link #nextInt()} twice and shifting the bits
     * to make a long from the two ints. It really just multiplies each int of the state by a different large prime
     * before adding them together (the adding is just like in nextInt(), but the multiplying is just here).
     * The end result is a long that should take only slightly longer to produce than an int, from a primarily-int
     * generator! Hooray.
     * <p>
     * Get a random long between Long.MIN_VALUE and Long.MAX_VALUE (both inclusive).
     *
     * @return a random long between Long.MIN_VALUE and Long.MAX_VALUE (both inclusive)
     */
    @Override
    public final long nextLong() {
        final int s0 = state0;
        int s1 = state1;
        final long result = s0 * 0xD0E89D2D311E289FL + s1 * 0xC6BC279692B5CC83L;
        s1 ^= s0 + 0x9E3779B9;
        state0 = (s0 << 26 | s0 >>> 6) ^ s1 ^ (s1 << 7); // a, b
        state1 = (s1 << 18 | s1 >>> 14); // c
        return result;

        /*
        int z = state + 0x9E3779B9;
        state += (z >> (z >>> 28)) * 0xC6BC279692B5CC83L;
        z = (state + 0x9E3779B9);
        return (state) ^ (long)(state += ((z >> (z >>> 28)) * 0xC6BC279692B5CC83L)) << 32;
        */
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just need to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public RandomnessSource copy() {
        return new FlapRNG(state0, state1);
    }

    /**
     * A simple "output stage" applied to state, this method does not update state on its own. If you expect to call
     * this method more than once, you should add a large int that is an odd number to state as part of the call, such
     * as with {@code FlapRNG.determine(state += 0x9E3779B9)}. Here, "large long" means at least two of the
     * upper 5 bits should be set to be safe. The golden-ratio-derived constant 0x9E3779B9 should be fine.
     * This method doesn't offer particularly good quality assurances, but should be very fast.
     * @param state should be changed when you call this by adding some large odd number, e.g. 0x9E3779B9
     * @return an altered version of state that should be very fast to compute but doesn't promise great quality
     */
    public static int determine(final int state)
    {
        return (state >> (state >>> 28)) * 0xC6BC278D;
    }
    /**
     * Gets a pseudo-random float between 0f (inclusive) and 1f (exclusive) using the given state. Calling this
     * repeatedly won't have correlation problems if you give it sequential ints.
     * @param state any int
     * @return a pseudo-random float from -0f (inclusive) to 1f (exclusive)
     */
    public static float randomFloat(int state)
    {
        return NumberTools.intBitsToFloat(((((state *= 0x9E3779B9) >> (state >>> 28)) * 0xC6BC278D) >>> 9) | 0x3f800000) - 1f;
    }

    /**
     * Gets a pseudo-random float between -1f (inclusive) and 1f (exclusive) using the given state. Calling this
     * repeatedly won't have correlation problems if you give it sequential ints.
     * @param state any int
     * @return a pseudo-random float from -1f (inclusive) to 1f (exclusive)
     */
    public static float randomSignedFloat(int state)
    {
        return NumberTools.intBitsToFloat(((((state *= 0x9E3779B9) >> (state >>> 28)) * 0xC6BC278D) >>> 9) | 0x40000000) - 3f;
    }

    @Override
    public String toString() {
        return "FlapRNG with state0 0x" + StringKit.hex(state0) + ", state1 0x" + StringKit.hex(state1);
    }

    @Override
    public int hashCode() {
        return 0x632BE5AB * state0 ^ state1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlapRNG flapRNG = (FlapRNG) o;

        if (state0 != flapRNG.state0) return false;
        return state1 == flapRNG.state1;
    }
}
