package squidpony.squidmath;

/**
 * Various numeric functions that are important to performance but need alternate implementations on GWT to obtain it.
 * Super-sourced on GWT, but most things here are direct calls to JDK methods when on desktop or Android.
 */
public class NumberTools {
    public static long doubleToLongBits(final double value)
    {
        return Double.doubleToLongBits(value);
    }
    public static long doubleToRawLongBits(final double value)
    {
        return Double.doubleToRawLongBits(value);
    }
    public static double longBitsToDouble(final long bits)
    {
        return Double.longBitsToDouble(bits);
    }
    public static int doubleToLowIntBits(final double value)
    {
        return (int)(Double.doubleToLongBits(value) & 0xffffffffL);
    }
    public static int doubleToHighIntBits(final double value)
    {
        return (int)(Double.doubleToLongBits(value) >>> 32);
    }
    public static int doubleToMixedIntBits(final double value)
    {
        final long l = Double.doubleToLongBits(value);
        return (int)l ^ (int)(l >>> 32);
    }

    /**
     * Makes a modified version of value that uses the specified bits (up to 12) for its exponent and sign.
     * Meant for some specific cases, like adjusting the exponent on an unknown double to the 1.0 to 2.0 range (which
     * would pass 0x3ff for exponentBits). If you have a double from 1.0 to 2.0, you can subtract 1.0 from it to get the
     * often-desirable 0.0-1.0 range. Other common cases are 0x400, which adjusts to between 2.0 and 4.0 (subtracting
     * 3.0 from this gives the -1.0 to 1.0 range, useful for noise), and 0xBFF, which adjusts to between -2.0 and -1.0.
     * For the last case, you might think that -0x3ff would work, but sadly it doesn't. You can use
     * {@code exponentBits |= 0x800} to set the sign bit to negative, or {@code exponentBits &= 0x7ff} for positive.
     * @param value a double that will have its sign and exponent set to the specified bits
     * @param exponentBits the bits to use for the sign and exponent section of the returned modification of value
     * @return the double produced by keeping the significand of value but changing its exponent and sign as given
     */
    public static double setExponent(final double value, final int exponentBits)
    {
        return Double.longBitsToDouble((Double.doubleToLongBits(value) & 0xfffffffffffffL) | ((long) exponentBits << 52));
    }

    /**
     * Gets an 8-bit section of the given double {@code value}, using {@code whichByte} to select whether this should
     * return byte 0 (least significant), 1, 2, and so on up to 7 (most significant).
     * @param value a float
     * @param whichByte an int that will be used to select the byte to take from value (any int is allowed, only the bottom 3 bits are used to select)
     * @return the selected byte from the given float
     */
    public static byte getSelectedByte(final double value, final int whichByte)
    {
        return (byte)(Double.doubleToLongBits(value) >>> ((whichByte & 7) << 3));
    }

    /**
     * Like {@link #getSelectedByte(double, int)}, this sets the byte at a selected position in the int representation of
     * a double, then returns the double produced by the bit change. Uses {@code whichByte} to select whether this should
     * set byte 0 (least significant), 1, 2, and so on up to 7 (most significant). {@code newValue} is a byte.
     * @param value a double
     * @param whichByte an int that will be used to select the byte to take from value (any int is allowed, only the bottom 3 bits are used to select)
     * @param newValue a byte that will be placed into the returned double's bits at the selected position
     * @return a double that results from changing the bits at the selected position to match newValue
     */
    public static double setSelectedByte(final double value, final int whichByte, final byte newValue)
    {
        return Double.longBitsToDouble((Double.doubleToLongBits(value) & ~(255 << ((whichByte & 7) << 3)))
                | ((newValue & 255) << ((whichByte & 7) << 3)));
    }

    /**
     * Very limited-use. Takes any double and produces a double in the -1.0 to 1.0 range, with similar inputs producing
     * close to a consistent rate of up and down through the range. This is meant for noise, where it may be useful to
     * limit the amount of change between nearby points' noise values and prevent sudden "jumps" in noise value.
     * @param value any double
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static double bounce(final double value)
    {
        long s = Double.doubleToLongBits(value) & 0xfffffffffffffL;
        return Double.longBitsToDouble(((s ^ -((s & 0x8000000000000L)>>51)) & 0xfffffffffffffL)
                | 0x4010000000000000L) - 5.0;
    }

    /**
     * Very limited-use. Takes any double and produces a double in the -1.0 to 1.0 range, with similar inputs producing
     * close to a consistent rate of up and down through the range. This is meant for noise, where it may be useful to
     * limit the amount of change between nearby points' noise values and prevent sudden "jumps" in noise value.
     * @param value any double
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static float bounce(final float value)
    {
        int s = Float.floatToIntBits(value) & 0x007fffff;
        return Float.intBitsToFloat(((s ^ -((s & 0x00400000)>>22)) & 0x007fffff)
                | 0x40800000) - 5f;
    }
    /**
     * Very limited-use. Takes the significand bits of a double, represented as a long of which this uses 52 bits, and
     * produces a double in the -1.0 to 1.0 range, with similar inputs producing close to a consistent rate of up and
     * down through the range. This is meant for noise, where it may be useful to limit the amount of change between
     * nearby points' noise values and prevent sudden "jumps" in noise value.
     * @param value any long; only the lower 52 bits will be used
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static double bounce(final long value)
    {
        long s = value & 0xfffffffffffffL;
        return Double.longBitsToDouble(((s ^ -((s & 0x8000000000000L)>>51)) & 0xfffffffffffffL)
                | 0x4010000000000000L) - 5.0;
    }
    /**
     * Very limited-use. Takes the significand bits of a double, represented as a pair of ints {@code valueLow} and
     * {@code valueHigh}, using all bits in valueLow and the least-significant 20 bits of valueHigh, and
     * produces a double in the -1.0 to 1.0 range, with similar inputs producing close to a consistent rate of up and
     * down through the range. This is meant for noise, where it may be useful to limit the amount of change between
     * nearby points' noise values and prevent sudden "jumps" in noise value.
     * @param valueLow any int; all bits will be used as the less-significant bits of the significand
     * @param valueHigh any int; only the bottom 20 bits will be used as the more-significant bits of the significand
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */

    public static double bounce(final int valueLow, final int valueHigh)
    {
        long s = (((long) valueHigh) << 32 | valueLow) & 0xfffffffffffffL;
        return Double.longBitsToDouble(((s ^ -((s & 0x8000000000000L)>>51)) & 0xfffffffffffffL)
                | 0x4010000000000000L) - 5.0;
    }

    public static int floatToIntBits(final float value)
    {
        return Float.floatToIntBits(value);
    }
    public static int floatToRawIntBits(final float value)
    {
        return Float.floatToRawIntBits(value);
    }
    public static float intBitsToFloat(final int bits)
    {
        return Float.intBitsToFloat(bits);
    }

    /**
     * Gets an 8-bit section of the given float {@code value}, using {@code whichByte} to select whether this should
     * return byte 0 (least significant), 1, 2, or 3 (most significant).
     * @param value a float
     * @param whichByte an int that will be used to select the byte to take from value (any int is allowed, only the bottom 2 bits are used to select)
     * @return the selected byte from the given float
     */
    public static byte getSelectedByte(final float value, final int whichByte)
    {
        return (byte)(Float.floatToIntBits(value) >>> ((whichByte & 3) << 3));
    }

    /**
     * Like {@link #getSelectedByte(float, int)}, this sets the byte at a selected position in the int representation of
     * a float, then returns the float produced by the bit change. Uses {@code whichByte} to select whether this should
     * set byte 0 (least significant), 1, 2, or 3 (most significant). {@code newValue} is a byte.
     * @param value a float
     * @param whichByte an int that will be used to select the byte to take from value (any int is allowed, only the bottom 2 bits are used to select)
     * @param newValue a byte that will be placed into the returned float's bits at the selected position
     * @return a float that results from changing the bits at the selected position to match newValue
     */
    public static float setSelectedByte(final float value, final int whichByte, final byte newValue)
    {
        return Float.intBitsToFloat((Float.floatToIntBits(value) & ~(255 << ((whichByte & 3) << 3)))
                | ((newValue & 255) << ((whichByte & 3) << 3)));
    }

    /**
     * Generates a pseudo-random double between 0.0 (inclusive) and 1.0 (exclusive) using the given int seed, passing it
     * twice through the (very high-quality and rather fast) {@link PintRNG} algorithm, derived from PCG-Random. This
     * produces a pair of random ints, which this produces a double from using the equivalent of
     * {@link #longBitsToDouble(long)} or something functionally equivalent on GWT.
     * <br>
     * Consider calling this with {@code NumberTools.randomDouble(seed += 0x3C6EF372)} for an optimal period of 2 to the
     * 31 when repeatedly called, but {@code NumberTools.randomDouble(++seed)} will also work just fine.
     * @param seed any int to be used as a seed
     * @return a pseudo-random double from 0.0 (inclusive) to 1.0 (exclusive)
     */
    public static double randomDouble(int seed)
    {
        seed ^= seed >>> (4 + (seed >>> 28));
        long bits = ((seed *= 0x108EF2D9) >>> 22 ^ seed) & 0xffffffffL;
        seed += 0x9E3779B9;
        seed ^= seed >>> (4 + (seed >>> 28));
        bits |= ((((seed *= 0x108EF2D9) >>> 22 ^ seed) & 0xfffffL) << 32 | 0x3ff0000000000000L);
        return Double.longBitsToDouble(bits) - 1.0;
    }
    /**
     * Generates a pseudo-random float between 0.0f (inclusive) and 1.0f (exclusive) using the given int seed, passing
     * it once through the (very high-quality and rather fast) {@link PintRNG} algorithm, derived from PCG-Random. This
     * produces a random int, which this produces a float from using {@link #intBitsToFloat(int)} (long)} or something
     * functionally equivalent on GWT.
     * <br>
     * Consider calling this with {@code NumberTools.randomSignedFloat(seed += 0x9E3779B9)} for an optimal period of 2
     * to the 32 when repeatedly called, but {@code NumberTools.randomSignedFloat(++seed)} will also work just fine.
     * @param seed any int to be used as a seed
     * @return a pseudo-random float from -1.0f (exclusive) to 1.0f (exclusive)
     */
    public static float randomFloat(int seed)
    {
        seed ^= seed >>> (4 + (seed >>> 28));
        return (Float.intBitsToFloat((((seed *= 0x108EF2D9) >>> 22 ^ seed) >>> 9) | 0x3f800000) - 1f);
    }
    /**
     * Generates a pseudo-random float between -1.0f (exclusive) and 1.0f (exclusive) using the given int seed, passing
     * it once through the (very high-quality and rather fast) {@link PintRNG} algorithm, derived from PCG-Random. This
     * produces a random int, which this produces a float from using {@link #intBitsToFloat(int)} (long)} or something
     * functionally equivalent on GWT. The sign bit of the result is determined by data that is not used by the float
     * otherwise, and keeps the results almost linear in distribution between -1.0 and 1.0, exclusive for both (0 shows
     * up twice as often as any single other result, but this shouldn't affect the odds very strongly; it's about a 1 in
     * 8 million chance of exactly 0 occurring vs. a 1 in 16 million of any other specific float this can produce).
     * <br>
     * Consider calling this with {@code NumberTools.randomSignedFloat(seed += 0x9E3779B9)} for an optimal period of 2
     * to the 32 when repeatedly called, but {@code NumberTools.randomSignedFloat(++seed)} will also work just fine.
     * @param seed any int to be used as a seed
     * @return a pseudo-random float from -1.0f (exclusive) to 1.0f (exclusive)
     */
    public static float randomSignedFloat(int seed)
    {
        seed ^= seed >>> (4 + (seed >>> 28));
        return (Float.intBitsToFloat((((seed *= 0x108EF2D9) >>> 22 ^ seed) >>> 9) | 0x3f800000) - 1f) * (seed >> 31 | 1);
    }

    /**
     * Generates a pseudo-random double between -1.0 (exclusive) and 1.0 (exclusive) with a distribution that has a
     * strong central bias (around 0.0). Uses the given int seed, passing it twice through the (very high-quality and
     * rather fast) {@link PintRNG} algorithm, derived from PCG-Random. This produces a pair of random ints, which this
     * uses to generate a pair of floats between 0.0 (inclusive)and 1.0 (exclusive) using the equivalent of
     * {@link #intBitsToFloat(int)} or something functionally equivalent on GWT, multiplies the floats, and sets the
     * sign pseudo-randomly based on an unused bit from earlier.
     * <br>
     * Consider calling this with {@code NumberTools.randomDoubleCurved(seed += 0x3C6EF372)} for an optimal period of 2
     * to the 31 when repeatedly called, but {@code NumberTools.randomDoubleCurved(++seed)} will also work just fine.
     * @param seed any int to be used as a seed
     * @return a pseudo-random double from -1.0 (exclusive) to 1.0 (exclusive), distributed on a curve centered on 0.0
     */
    public static double randomDoubleCurved(int seed)
    {
        seed ^= seed >>> (4 + (seed >>> 28));
        float a = Float.intBitsToFloat((((seed *= 0x108EF2D9) >>> 22 ^ seed) >>> 9) | 0x3f800000);
        seed += 0x9E3779B9;
        seed ^= seed >>> (4 + (seed >>> 28));
        return  (a - 1.0) * (Float.intBitsToFloat((((seed *= 0x108EF2D9) >>> 22 ^ seed) >>> 9) | 0x3f800000) - 1.0) * (seed >> 31 | 1);
    }

    static int hashWisp(final float[] data)
    {
        if (data == null)
            return 0;
        int result = 0x9E3779B9, a = 0x632BE5AB;
        final int len = data.length;
        for (int i = 0; i < len; i++) {
            result += (a ^= 0x85157AF5 * Float.floatToIntBits(data[i]));
        }
        return result * (a | 1) ^ (result >>> 11 | result << 21);
    }

    static int hashWisp(final double[] data)
    {
        if (data == null)
            return 0;
        long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
        final int len = data.length;
        double t;
        for (int i = 0; i < len; i++) {
            result += (a ^= 0x8329C6EB9E6AD3E3L * ((long) (-0xD0E8.9D2D311E289Fp-25 * (t = data[i]) + t * -0x1.39b4dce80194cp9)));
        }
        return (int)((result = (result * (a | 1L) ^ (result >>> 27 | result << 37))) ^ (result >>> 32));
    }
}
