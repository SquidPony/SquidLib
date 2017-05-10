package squidpony.squidmath;

import java.util.Arrays;

/**
 * This is a port of the public domain Isaac (cryptographic) random number generator to Java, by Bob Jenkins.
 * It is a RandomnessSource here, so it should generally be used to make an RNG, which has more features.
 * Isaac32RNG is slower than the non-cryptographic RNGs in SquidLib, but much faster than cryptographic RNGs
 * that need SecureRandom, and it's compatible with GWT and Android to boot! Isaac32RNG should perform better
 * than IsaacRNG on GWT, or when you specifically need a large amount of int values to be set using
 * {@link #setBlock(int[])}. If you don't need GWT support, then {@link IsaacRNG} will have better properties.
 * Created by Tommy Ettinger on 8/1/2016.
 */
public class Isaac32RNG implements RandomnessSource {
    final static int SIZEL = 8;              /* log of size of results[] and mem[] */
    final static int SIZE = 1 << SIZEL;               /* size of results[] and mem[] */ // 256
    final static int MASK = (SIZE - 1) << 2;            /* for pseudorandom lookup */ // 1020
    int count;                           /* count through the results in results[] */
    int results[];                                /* the results given to the user */
    private int mem[];                                   /* the internal state */
    private int a;                                              /* accumulator */
    private int b;                                          /* the last result */
    private int c;              /* counter, guarantees cycle is at least 2^^40 */


    /* no seed, equivalent to randinit(ctx,FALSE) in C */
    public Isaac32RNG() {
        mem = new int[SIZE];
        results = new int[SIZE];
        init(false);
    }

    /* equivalent to randinit(ctx, TRUE) after putting seed in randctx in C */
    public Isaac32RNG(final int seed[]) {
        mem = new int[SIZE];
        results = new int[SIZE];
        if(seed == null)
            init(false);
        else {
            System.arraycopy(seed, 0, results, 0, Math.min(256, seed.length));
            init(true);
        }
    }
    /**
     * Constructs an IsaacRNG with its state filled by the value of seed, run through the LightRNG algorithm.
     * @param seed any long; will have equal influence on all bits of state
     */
    public Isaac32RNG(long seed) {
        mem = new int[SIZE];
        results = new int[SIZE];
        long z;
        for (int i = 0; i < 256; i++) {
            z = seed += 0x9E3779B97F4A7C15L;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            results[i++] = (int) ((z ^ (z >>> 31)) & 0xffffffffL);
            results[i] = (int) ((z ^ (z >>> 31)) >>> 32);
        }
        init(true);
    }

    /**
     * Constructs an IsaacRNG with its state filled by repeated hashing of seed.
     * @param seed a String that should be exceptionally long to get the best results.
     */
    public Isaac32RNG(String seed) {
        mem = new int[SIZE];
        results = new int[SIZE];
        if(seed == null)
            init(false);
        else {
            char[] chars = seed.toCharArray();
            int slen = chars.length, i = 0;
            for (; i < 256 && i < slen; i++) {
                results[i] = CrossHash.Wisp.hash(chars, i, slen);
            }
            for (; i < 256; i++) {
                results[i] = CrossHash.Wisp.hash(results);
            }
            init(true);
        }
    }

    private Isaac32RNG(Isaac32RNG other)
    {
        this(other.results);
    }


    /**
     * Generates 256 results to be used by later calls to next() or nextLong().
     * This is a fast (not small) implementation.
     */
    public final void regen() {
        int i, j, x, y;

        b += ++c;
        for (i = 0, j = SIZE >>> 1; i < SIZE >>> 1; ) {
            x = mem[i];
            a ^= a << 13;
            a += mem[j++];
            mem[i] = y = mem[(x & MASK) >> 2] + a + b;
            results[i++] = b = mem[((y >> SIZEL) & MASK) >> 2] + x;

            x = mem[i];
            a ^= a >>> 6;
            a += mem[j++];
            mem[i] = y = mem[(x & MASK) >> 2] + a + b;
            results[i++] = b = mem[((y >> SIZEL) & MASK) >> 2] + x;

            x = mem[i];
            a ^= a << 2;
            a += mem[j++];
            mem[i] = y = mem[(x & MASK) >> 2] + a + b;
            results[i++] = b = mem[((y >> SIZEL) & MASK) >> 2] + x;

            x = mem[i];
            a ^= a >>> 16;
            a += mem[j++];
            mem[i] = y = mem[(x & MASK) >> 2] + a + b;
            results[i++] = b = mem[((y >> SIZEL) & MASK) >> 2] + x;
        }

        for (j = 0; j < SIZE >>> 1; ) {
            x = mem[i];
            a ^= a << 13;
            a += mem[j++];
            mem[i] = y = mem[(x & MASK) >> 2] + a + b;
            results[i++] = b = mem[((y >> SIZEL) & MASK) >> 2] + x;

            x = mem[i];
            a ^= a >>> 6;
            a += mem[j++];
            mem[i] = y = mem[(x & MASK) >> 2] + a + b;
            results[i++] = b = mem[((y >> SIZEL) & MASK) >> 2] + x;

            x = mem[i];
            a ^= a << 2;
            a += mem[j++];
            mem[i] = y = mem[(x & MASK) >> 2] + a + b;
            results[i++] = b = mem[((y >> SIZEL) & MASK) >> 2] + x;

            x = mem[i];
            a ^= a >>> 16;
            a += mem[j++];
            mem[i] = y = mem[(x & MASK) >> 2] + a + b;
            results[i++] = b = mem[((y >> SIZEL) & MASK) >> 2] + x;
        }
    }


    /**
     * Initializes this IsaacRNG; typically used from the constructor but can be called externally.
     *
     * @param flag if true, use data from seed; if false, initializes this to unseeded random state
     */
    public final void init(boolean flag) {
        int i;
        int a, b, c, d, e, f, g, h;
        a = b = c = d = e = f = g = h = 0x9e3779b9;                        /* the golden ratio */

        for (i = 0; i < 4; ++i) {
            a ^= b << 11;
            d += a;
            b += c;
            b ^= c >>> 2;
            e += b;
            c += d;
            c ^= d << 8;
            f += c;
            d += e;
            d ^= e >>> 16;
            g += d;
            e += f;
            e ^= f << 10;
            h += e;
            f += g;
            f ^= g >>> 4;
            a += f;
            g += h;
            g ^= h << 8;
            b += g;
            h += a;
            h ^= a >>> 9;
            c += h;
            a += b;
        }

        for (i = 0; i < SIZE; i += 8) {              /* fill in mem[] with messy stuff */
            if (flag) {
                a += results[i];
                b += results[i + 1];
                c += results[i + 2];
                d += results[i + 3];
                e += results[i + 4];
                f += results[i + 5];
                g += results[i + 6];
                h += results[i + 7];
            }
            a ^= b << 11;
            d += a;
            b += c;
            b ^= c >>> 2;
            e += b;
            c += d;
            c ^= d << 8;
            f += c;
            d += e;
            d ^= e >>> 16;
            g += d;
            e += f;
            e ^= f << 10;
            h += e;
            f += g;
            f ^= g >>> 4;
            a += f;
            g += h;
            g ^= h << 8;
            b += g;
            h += a;
            h ^= a >>> 9;
            c += h;
            a += b;
            mem[i] = a;
            mem[i + 1] = b;
            mem[i + 2] = c;
            mem[i + 3] = d;
            mem[i + 4] = e;
            mem[i + 5] = f;
            mem[i + 6] = g;
            mem[i + 7] = h;
        }

        if (flag) {           /* second pass makes all of seed affect all of mem */
            for (i = 0; i < SIZE; i += 8) {
                a += mem[i];
                b += mem[i + 1];
                c += mem[i + 2];
                d += mem[i + 3];
                e += mem[i + 4];
                f += mem[i + 5];
                g += mem[i + 6];
                h += mem[i + 7];
                a ^= b << 11;
                d += a;
                b += c;
                b ^= c >>> 2;
                e += b;
                c += d;
                c ^= d << 8;
                f += c;
                d += e;
                d ^= e >>> 16;
                g += d;
                e += f;
                e ^= f << 10;
                h += e;
                f += g;
                f ^= g >>> 4;
                a += f;
                g += h;
                g ^= h << 8;
                b += g;
                h += a;
                h ^= a >>> 9;
                c += h;
                a += b;
                mem[i] = a;
                mem[i + 1] = b;
                mem[i + 2] = c;
                mem[i + 3] = d;
                mem[i + 4] = e;
                mem[i + 5] = f;
                mem[i + 6] = g;
                mem[i + 7] = h;
            }
        }

        regen();
        count = SIZE;
    }

    public final int nextInt() {
        if (0 == count--) {
            regen();
            count = SIZE - 1;
        }
        return results[count];
    }
    /**
     * Generates and returns a block of 256 pseudo-random int values.
     * @return a freshly-allocated array of 256 pseudo-random ints, with all bits possible
     */
    public final int[] nextBlock()
    {
        regen();
        final int[] block = new int[SIZE];
        System.arraycopy(results, 0, block, 0, SIZE);
        count = 0;
        return block;
    }

    /**
     * Generates enough pseudo-random int values to fill {@code data} and assigns them to it.
     */
    public final void setBlock(final int[] data)
    {
        int len, i;
        if(data == null || (len = data.length) == 0) return;
        for (i = 0; len > 256; i += 256, len -= 256) {
            regen();
            System.arraycopy(results, 0, data, i, 256);
        }
        regen();
        System.arraycopy(results, 0, data, i, len);
        count = len & 255;
    }

    /**
     * Generates enough pseudo-random float values between 0f and 1f to fill {@code data} and assigns them to it.
     * Inclusive on 0f, exclusive on 1f. Intended for cases where you need some large source of randomness to be checked
     * later repeatedly, such as how permutation tables are used in Simplex noise.
     */
    public final void setBlock(final float[] data)
    {
        int len, n;
        if(data == null || (len = data.length) == 0) return;
        for (int i = 0; i < len; i++) {
            data[i] = NumberTools.intBitsToFloat(((n = nextInt()) >>> 9 ^ (n & 0x7fffff)) | 0x3f800000) - 1f;
        }
    }

    /**
     * Generates enough pseudo-random float values between -1f and 1f to fill {@code data} and assigns them to it.
     * Inclusive on -1f, exclusive on 1f. Intended for cases where you need some large source of randomness to be
     * checked later repeatedly, such as how permutation tables are used in Simplex noise.
     */
    public final void setSignedBlock(final float[] data)
    {
        int len, n;
        if(data == null || (len = data.length) == 0) return;
        for (int i = 0; i < len; i++) {
            data[i] = NumberTools.intBitsToFloat(((n = nextInt()) >>> 9 ^ (n & 0x7fffff)) | 0x40000000) - 3f;
        }
    }

    @Override
    public final int next( int bits ) {
        return nextInt() >>> 32 - bits;
    }

    /**
     * Using this method, any algorithm that needs to efficiently generate more
     * than 32 bits of random data can interface with this randomness source.
     * <p>
     * Get a random long between Long.MIN_VALUE and Long.MAX_VALUE (both inclusive).
     *
     * @return a random long between Long.MIN_VALUE and Long.MAX_VALUE (both inclusive)
     */
    @Override
    public long nextLong() {
        return (nextInt() & 0xffffffffL) | (nextInt() & 0xffffffffL) << 32;
    }


    /**
     * Produces another RandomnessSource, but the new one will not produce the same data as this one.
     * This is meant to be a "more-secure" generator, so this helps reduce the ability to guess future
     * results from a given sequence of output.
     * @return another RandomnessSource with the same implementation but no guarantees as to generation
     */
    @Override
    public RandomnessSource copy() {
        return new Isaac32RNG(results);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Isaac32RNG isaac32RNG = (Isaac32RNG) o;

        if (count != isaac32RNG.count) return false;
        if (a != isaac32RNG.a) return false;
        if (b != isaac32RNG.b) return false;
        if (c != isaac32RNG.c) return false;
        if (!Arrays.equals(results, isaac32RNG.results)) return false;
        return Arrays.equals(mem, isaac32RNG.mem);
    }

    @Override
    public int hashCode() {
        int result = count;
        result = 31 * result + CrossHash.Wisp.hash(results);
        result = 31 * result + CrossHash.Wisp.hash(mem);
        result = 31 * result + a;
        result = 31 * result + b;
        result = 31 * result + c;
        return result;
    }

    @Override
    public String toString()
    {
        return "Isaac32RNG with a hidden state (id is " + System.identityHashCode(this) + ')';
    }

}