package squidpony.squidmath;

import squidpony.annotation.Beta;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Simple hashing functions that we can rely on staying the same cross-platform.
 * The static methods of this class (not its inner classes) use the Fowler/Noll/Vo
 * Hash (FNV-1a) algorithm, which is public domain. The hashes this returns are always
 * 0 when given null to hash. Arrays with identical elements of identical types will
 * hash identically. Arrays with identical numerical values but different types will
 * hash differently (only for FNV-1a, not the inner classes). FNV-1a may be somewhat
 * slow if you are running many hashes or hashing very large data; in that case you
 * should consider one of the inner classes, Lightning or Storm.
 * <br>
 * There are several static inner classes in CrossHash, Lightning, Storm, Falcon, Chariot, Wisp,
 * and Mist, that provide different hashing properties, as well as the inner IHasher interface.
 * Of these, Wisp should be the default choice in most cases. If you need a salt to alter the
 * hash function, using one of a large family of such functions instead of a single function like
 * Wisp, then Storm and Mist are good choices (Mist is faster, Storm is more stable API-wise).
 * Falcon and Lightning are mostly superseded by Wisp. Chariot has some limitations, and uses
 * potentially much more memory than the others (which use almost none).
 * <br>
 * IHasher values are provided as static fields, and use Wisp to hash a specific type or fall
 * back to Object.hashCode if given an object with the wrong type. IHasher values are optional
 * parts of OrderedMap and OrderedSet, and allow arrays to be used as keys in those collections
 * while keeping hashing by value instead of the normal hashing by reference for arrays. You
 * probably won't ever need to make a class that implements IHasher yourself; for some cases you
 * may want to look at the {@link Hashers} class for additional functions.
 * <br>
 * The inner classes provide alternate, faster hashing algorithms. Lightning, Wisp, and Falcon
 * have no theoretical basis or grounds in any reason other than empirical testing for why they
 * do what they do, and this seems to be in line with many widely-used hashes (see: The Art of
 * Hashing, http://eternallyconfuzzled.com/tuts/algorithms/jsw_tut_hashing.aspx ). That said, Wisp
 * performs very well, ahead of Arrays.hashCode (10.5 ms instead of 15 ms) for over a million
 * hashes of 16-element long arrays, not including overhead for generating them, while SipHash and
 * FNV-1a take approximately 80 ms and 135-155 ms, respectively, for the same data). Lightning and
 * Falcon perform less-well, with Lightning taking 17 ms instead of 15 ms for Arrays.hashCode, and
 * Falcon taking about 12.3 ms but slowing down somewhat if a 32-bit hash is requested from long
 * data. All of these have good, low, collision rates on Strings and long arrays.
 * <br>
 * Storm is a variant on Lightning with 64 bits for a salt-like modifier as a member variable,
 * which can make 2 to the 64 individual hashing functions from one set of code. Mist is similar,
 * but has 128 bits for some hashes (including any calls to hash64()) and 64 bits for some other
 * hashes (only calls to hash() with data that doesn't involve long or double arrays). Storm and Mist
 * have some properties of a cryptographic hash, but is not recommended it for that usage. It is,
 * however ideal for situations that show up often in game development where end users may be able
 * to see and possibly alter some information that you don't want changed (i.e. save data stored on
 * a device or in the browser's LocalStorage). If you want a way to verify the data is what you
 * think it is, you can store a hash, using one of the many-possible hash functions this can
 * produce, somewhere else and verify that the saved data has the hash it did last time; if the
 * exact hashing function isn't known (or exact functions aren't known) by a tampering user,
 * then it is unlikely they can make the hash match even if they can edit it. Storm is slightly
 * slower than Lightning just like Mist is slightly slower than Wisp, at about 20 ms for a
 * million hashes with Storm instead of Lightning's 17 ms, and at about 18 ms for Mist for the same
 * data instead of Wisp's 10.5, but should never be worse than twice as slow as Arrays.hashCode, and
 * is still about three times faster than the similar SipHash that SquidLib previously had here.
 * <br>
 * All of the hashes used here have about the same rate of collisions on identical data
 * (testing used Arrays.hashCode, all the hashes in here now, and the now-removed SipHash), with
 * any fluctuation within some small margin of error. Lightning has been changed frequently but
 * is considered stable now, but it isn't being considered to replace the FNV-1a algorithm in
 * CrossHash for compatibility reasons. Both Lightning and Wisp seem to meet all the criteria for
 * a good hash function, though, including doing well with a visual test that shows issues in
 * FNV-1a and especially Arrays.hashCode. Wisp is considered stable now, and is not considered yet
 * for replacing FNV-1a for the same reason. Chariot is not recommended. Falcon is only recommended
 * if you need an alternate algorithm for some reason and need only 64-bit hashes; it has issues when
 * generating 32-bit hashes of long arrays or double arrays. Mist is not considered stable yet, but
 * probably will be soon; it offers an improvement on speed and salting over Storm, and its collision
 * rates were very very low in testing.
 * <br>
 * To help find patterns in hash output in a visual way, you can hash an x,y point, take the bottom 24 bits,
 * and use that as an RGB color for the pixel at that x,y point. On a 512x512 grid of points, the patterns
 * in Arrays.hashCode and the default CrossHash algorithm (FNV-1a) are evident, and Sip (implementing SipHash)
 * does approximately as well as Lightning, with no clear patterns visible (Sip has been removed from SquidLib
 * because it needs a lot of code and is slower than Storm and especially Lightning). The idea is from a
 * technical report on visual uses for hashing, http://www.clockandflame.com/media/Goulburn06.pdf .
 * <ul>
 * <li>{@link java.util.Arrays#hashCode(int[])}: http://i.imgur.com/S4Gh1sX.png</li>
 * <li>{@link CrossHash#hash(int[])}: http://i.imgur.com/x8SDqvL.png</li>
 * <li>(Former) CrossHash.Sip.hash(int[]): http://i.imgur.com/keSpIwm.png</li>
 * <li>{@link CrossHash.Lightning#hash(int[])}: http://i.imgur.com/afGJ9cA.png</li>
 * </ul>
 * <br>
 * Note: This class was formerly called StableHash, but since that refers to a specific
 * category of hashing algorithm that this is not, and since the goal is to be cross-
 * platform, the name was changed to CrossHash.
 * Created by Tommy Ettinger on 1/16/2016.
 *
 * @author Glenn Fowler
 * @author Phong Vo
 * @author Landon Curt Noll
 * @author Tommy Ettinger
 */
public class CrossHash {
    public static int hash(final boolean[] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length - 1, o = 0;
        for (int i = 0; i <= len; i++) {
            o |= (data[i]) ? (1 << (i & 7)) : 0;
            if ((i & 7) == 7 || i == len) {
                h ^= o;
                h *= 16777619;
                o = 0;
            }
        }
        return h;
    }

    public static int hash(final byte[] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= data[i];
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final char[] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= data[i] & 0xff;
            h *= 16777619;
            h ^= data[i] >>> 8;
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final short[] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= data[i] & 0xff;
            h *= 16777619;
            h ^= data[i] >>> 8;
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final int[] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= data[i] & 0xff;
            h *= 16777619;
            h ^= (data[i] >>> 8) & 0xff;
            h *= 16777619;
            h ^= (data[i] >>> 16) & 0xff;
            h *= 16777619;
            h ^= data[i] >>> 24;
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final long[] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= (int) (data[i] & 0xff);
            h *= 16777619;
            h ^= (int) ((data[i] >>> 8) & 0xff);
            h *= 16777619;
            h ^= (int) ((data[i] >>> 16) & 0xff);
            h *= 16777619;
            h ^= (int) ((data[i] >>> 24) & 0xff);
            h *= 16777619;
            h ^= (int) ((data[i] >>> 32) & 0xff);
            h *= 16777619;
            h ^= (int) ((data[i] >>> 40) & 0xff);
            h *= 16777619;
            h ^= (int) ((data[i] >>> 48) & 0xff);
            h *= 16777619;
            h ^= (int) (data[i] >>> 56);
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final float[] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length, t;
        for (int i = 0; i < len; i++) {
            t = NumberTools.floatToIntBits(data[i]);
            h ^= t & 0xff;
            h *= 16777619;
            h ^= (t >>> 8) & 0xff;
            h *= 16777619;
            h ^= (t >>> 16) & 0xff;
            h *= 16777619;
            h ^= t >>> 24;
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final double[] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length;
        long t;
        for (int i = 0; i < len; i++) {
            t = NumberTools.doubleToLongBits(data[i]);
            h ^= (int) (t & 0xff);
            h *= 16777619;
            h ^= (int) ((t >>> 8) & 0xff);
            h *= 16777619;
            h ^= (int) ((t >>> 16) & 0xff);
            h *= 16777619;
            h ^= (int) ((t >>> 24) & 0xff);
            h *= 16777619;
            h ^= (int) ((t >>> 32) & 0xff);
            h *= 16777619;
            h ^= (int) ((t >>> 40) & 0xff);
            h *= 16777619;
            h ^= (int) ((t >>> 48) & 0xff);
            h *= 16777619;
            h ^= (int) (t >>> 56);
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final CharSequence data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length();
        for (int i = 0; i < len; i++) {
            h ^= data.charAt(i) & 0xff;
            h *= 16777619;
            h ^= data.charAt(i) >>> 8;
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final CharSequence[] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length, t;
        for (int i = 0; i < len; i++) {
            t = hash(data[i]);
            h ^= t & 0xff;
            h *= 16777619;
            h ^= (t >>> 8) & 0xff;
            h *= 16777619;
            h ^= (t >>> 16) & 0xff;
            h *= 16777619;
            h ^= t >>> 24;
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final char[][] data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length, t;
        for (int i = 0; i < len; i++) {
            t = hash(data[i]);
            h ^= t & 0xff;
            h *= 16777619;
            h ^= (t >>> 8) & 0xff;
            h *= 16777619;
            h ^= (t >>> 16) & 0xff;
            h *= 16777619;
            h ^= t >>> 24;
            h *= 16777619;
        }
        return h;
    }

    public static int hash(final CharSequence[]... data) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length, t;
        for (int i = 0; i < len; i++) {
            t = hash(data[i]);
            h ^= t & 0xff;
            h *= 16777619;
            h ^= (t >>> 8) & 0xff;
            h *= 16777619;
            h ^= (t >>> 16) & 0xff;
            h *= 16777619;
            h ^= t >>> 24;
            h *= 16777619;
        }
        return h;
    }

    /**
     * Hashes only a subsection of the given data, starting at start (inclusive) and ending before end (exclusive).
     *
     * @param data  the char array to hash
     * @param start the start of the section to hash (inclusive)
     * @param end   the end of the section to hash (exclusive)
     * @return
     */
    public static int hash(final char[] data, int start, int end) {
        if (data == null)
            return 0;
        int h = -2128831035, len = data.length;
        start %= len;
        end %= len;
        if (end <= start || start < 0 || end <= 0)
            return 0;
        for (int i = start; i < end; i++) {
            h ^= data[i] & 0xff;
            h *= 16777619;
            h ^= data[i] >>> 8;
            h *= 16777619;
        }
        return h;
    }

    public static long hash64(final boolean[] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length - 1, o = 0;
        for (int i = 0; i <= len; i++) {
            o |= (data[i]) ? (1 << (i & 7)) : 0;
            if ((i & 7) == 7 || i == len) {
                h ^= o;
                h *= 1099511628211L;
                o = 0;
            }
        }
        return h;
    }

    public static long hash64(final byte[] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= data[i];
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final char[] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= data[i] & 0xff;
            h *= 1099511628211L;
            h ^= data[i] >>> 8;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final short[] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= data[i] & 0xff;
            h *= 1099511628211L;
            h ^= data[i] >>> 8;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final int[] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= data[i] & 0xff;
            h *= 1099511628211L;
            h ^= (data[i] >>> 8) & 0xff;
            h *= 1099511628211L;
            h ^= (data[i] >>> 16) & 0xff;
            h *= 1099511628211L;
            h ^= data[i] >>> 24;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final long[] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length;
        for (int i = 0; i < len; i++) {
            h ^= data[i] & 0xff;
            h *= 1099511628211L;
            h ^= (data[i] >>> 8) & 0xff;
            h *= 1099511628211L;
            h ^= (data[i] >>> 16) & 0xff;
            h *= 1099511628211L;
            h ^= (data[i] >>> 24) & 0xff;
            h *= 1099511628211L;
            h ^= (data[i] >>> 32) & 0xff;
            h *= 1099511628211L;
            h ^= (data[i] >>> 40) & 0xff;
            h *= 1099511628211L;
            h ^= (data[i] >>> 48) & 0xff;
            h *= 1099511628211L;
            h ^= data[i] >>> 56;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final float[] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length, t;
        for (int i = 0; i < len; i++) {
            t = NumberTools.floatToIntBits(data[i]);
            h ^= t & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 8) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 16) & 0xff;
            h *= 1099511628211L;
            h ^= t >>> 24;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final double[] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length, t;
        for (int i = 0; i < len; i++) {
            t = NumberTools.doubleToLongBits(data[i]);
            h ^= t & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 8) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 16) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 24) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 32) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 40) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 48) & 0xff;
            h *= 1099511628211L;
            h ^= t >>> 56;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final CharSequence data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length();
        for (int i = 0; i < len; i++) {
            h ^= data.charAt(i) & 0xff;
            h *= 1099511628211L;
            h ^= data.charAt(i) >>> 8;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final CharSequence[] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length, t;
        for (int i = 0; i < len; i++) {
            t = hash64(data[i]);
            h ^= t & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 8) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 16) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 24) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 32) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 40) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 48) & 0xff;
            h *= 1099511628211L;
            h ^= t >>> 56;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final char[][] data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length, t;
        for (int i = 0; i < len; i++) {
            t = hash64(data[i]);
            h ^= t & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 8) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 16) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 24) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 32) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 40) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 48) & 0xff;
            h *= 1099511628211L;
            h ^= t >>> 56;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final Iterable<? extends CharSequence> data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, t;
        for (CharSequence datum : data) {
            t = hash64(datum);
            h ^= t & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 8) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 16) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 24) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 32) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 40) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 48) & 0xff;
            h *= 1099511628211L;
            h ^= t >>> 56;
            h *= 1099511628211L;
        }
        return h;
    }

    public static long hash64(final CharSequence[]... data) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length, t;
        for (int i = 0; i < len; i++) {
            t = hash64(data[i]);
            h ^= t & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 8) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 16) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 24) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 32) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 40) & 0xff;
            h *= 1099511628211L;
            h ^= (t >>> 48) & 0xff;
            h *= 1099511628211L;
            h ^= t >>> 56;
            h *= 1099511628211L;
        }
        return h;
    }

    /**
     * Hashes only a subsection of the given data, starting at start (inclusive) and ending before end (exclusive).
     *
     * @param data  the char array to hash
     * @param start the start of the section to hash (inclusive)
     * @param end   the end of the section to hash (exclusive)
     * @return
     */
    public static long hash64(final char[] data, int start, int end) {
        if (data == null)
            return 0;
        long h = -3750763034362895579L, len = data.length;
        start %= len;
        end %= len;
        if (end <= start || start < 0 || end <= 0)
            return 0;
        for (int i = start; i < end; i++) {
            h ^= data[i] & 0xff;
            h *= 1099511628211L;
            h ^= data[i] >>> 8;
            h *= 1099511628211L;
        }
        return h;
    }

    /**
     * An interface that can be used to move the logic for the hashCode() and equals() methods from a class' methods to
     * an implementation of IHasher that certain collections in SquidLib can use. Primarily useful when the key type is
     * an array, which normally doesn't work as expected in Java hash-based collections, but can if the right collection
     * and IHasher are used. See also {@link Hashers} for additional implementations, some of which need dependencies on
     * things the rest of CrossHash doesn't, like a case-insensitive String hasher/equator that uses RegExodus to handle
     * CharSequence comparison on GWT.
     */
    public interface IHasher extends Serializable {
        /**
         * If data is a type that this IHasher can specifically hash, this method should use that specific hash; in
         * other situations, it should simply delegate to calling {@link Object#hashCode()} on data. The body of an
         * implementation of this method can be very small; for an IHasher that is meant for byte arrays, the body could
         * be: {@code return (data instanceof byte[]) ? CrossHash.Lightning.hash((byte[]) data) : data.hashCode();}
         *
         * @param data the Object to hash; this method should take any type but often has special behavior for one type
         * @return a 32-bit int hash code of data
         */
        int hash(final Object data);

        /**
         * Not all types you might want to use an IHasher on meaningfully implement .equals(), such as array types; in
         * these situations the areEqual method helps quickly check for equality by potentially having special logic for
         * the type this is meant to check. The body of implementations for this method can be fairly small; for byte
         * arrays, it looks like: {@code return left == right
         * || ((left instanceof byte[] && right instanceof byte[])
         * ? Arrays.equals((byte[]) left, (byte[]) right)
         * : Objects.equals(left, right));} , but for multidimensional arrays you should use the
         * {@link #equalityHelper(Object[], Object[], IHasher)} method with an IHasher for the inner arrays that are 1D
         * or otherwise already-hash-able, as can be seen in the body of the implementation for 2D char arrays, where
         * charHasher is an existing IHasher that handles 1D arrays:
         * {@code return left == right
         * || ((left instanceof char[][] && right instanceof char[][])
         * ? equalityHelper((char[][]) left, (char[][]) right, charHasher)
         * : Objects.equals(left, right));}
         *
         * @param left  allowed to be null; most implementations will have special behavior for one type
         * @param right allowed to be null; most implementations will have special behavior for one type
         * @return true if left is equal to right (preferably by value, but reference equality may sometimes be needed)
         */
        boolean areEqual(final Object left, final Object right);
    }

    /**
     * Not a general-purpose method; meant to ease implementation of {@link IHasher#areEqual(Object, Object)}
     * methods when the type being compared is a multi-dimensional array (which normally requires the heavyweight method
     * {@link Arrays#deepEquals(Object[], Object[])} or doing more work yourself; this reduces the work needed to
     * implement fixed-depth equality). As mentioned in the docs for {@link IHasher#areEqual(Object, Object)}, example
     * code that hashes 2D char arrays can be done using an IHasher for 1D char arrays called charHasher:
     * {@code return left == right
     * || ((left instanceof char[][] && right instanceof char[][])
     * ? equalityHelper((char[][]) left, (char[][]) right, charHasher)
     * : Objects.equals(left, right));}
     *
     * @param left
     * @param right
     * @param inner
     * @return
     */
    public static boolean equalityHelper(Object[] left, Object[] right, IHasher inner) {
        if (left == right)
            return true;
        if ((left == null) ^ (right == null))
            return false;
        for (int i = 0; i < left.length && i < right.length; i++) {
            if (!inner.areEqual(left[i], right[i]))
                return false;
        }
        return true;
    }

    private static class BooleanHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        BooleanHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof boolean[]) ? CrossHash.Wisp.hash((boolean[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right || ((left instanceof boolean[] && right instanceof boolean[]) ? Arrays.equals((boolean[]) left, (boolean[]) right) : Objects.equals(left, right));
        }
    }

    public static final IHasher booleanHasher = new BooleanHasher();

    private static class ByteHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        ByteHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof byte[]) ? CrossHash.Wisp.hash((byte[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right
                    || ((left instanceof byte[] && right instanceof byte[])
                    ? Arrays.equals((byte[]) left, (byte[]) right)
                    : Objects.equals(left, right));
        }
    }

    public static final IHasher byteHasher = new ByteHasher();

    private static class ShortHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        ShortHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof short[]) ? CrossHash.Wisp.hash((short[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right || ((left instanceof short[] && right instanceof short[]) ? Arrays.equals((short[]) left, (short[]) right) : Objects.equals(left, right));
        }
    }

    public static final IHasher shortHasher = new ShortHasher();

    private static class CharHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        CharHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof char[]) ? CrossHash.Wisp.hash((char[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right || ((left instanceof char[] && right instanceof char[]) ? Arrays.equals((char[]) left, (char[]) right) : Objects.equals(left, right));
        }
    }

    public static final IHasher charHasher = new CharHasher();

    private static class IntHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        IntHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof int[]) ? CrossHash.Wisp.hash((int[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return (left instanceof int[] && right instanceof int[]) ? Arrays.equals((int[]) left, (int[]) right) : Objects.equals(left, right);
        }
    }

    public static final IHasher intHasher = new IntHasher();

    private static class LongHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        LongHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof long[]) ? CrossHash.Wisp.hash((long[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return (left instanceof long[] && right instanceof long[]) ? Arrays.equals((long[]) left, (long[]) right) : Objects.equals(left, right);
        }
    }

    public static final IHasher longHasher = new LongHasher();

    private static class FloatHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        FloatHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof float[]) ? CrossHash.Wisp.hash((float[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right || ((left instanceof float[] && right instanceof float[]) ? Arrays.equals((float[]) left, (float[]) right) : Objects.equals(left, right));
        }
    }

    public static final IHasher floatHasher = new FloatHasher();

    private static class DoubleHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        DoubleHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof double[]) ? CrossHash.Wisp.hash((double[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right || ((left instanceof double[] && right instanceof double[]) ? Arrays.equals((double[]) left, (double[]) right) : Objects.equals(left, right));
        }
    }

    public static final IHasher doubleHasher = new DoubleHasher();

    private static class Char2DHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        Char2DHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof char[][]) ? CrossHash.Wisp.hash((char[][]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right
                    || ((left instanceof char[][] && right instanceof char[][])
                    ? equalityHelper((char[][]) left, (char[][]) right, charHasher)
                    : Objects.equals(left, right));
        }
    }

    public static final IHasher char2DHasher = new Char2DHasher();

    private static class Int2DHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        Int2DHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof int[][]) ? CrossHash.Wisp.hash((int[][]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right
                    || ((left instanceof int[][] && right instanceof int[][])
                    ? equalityHelper((int[][]) left, (int[][]) right, intHasher)
                    : Objects.equals(left, right));
        }
    }

    public static final IHasher int2DHasher = new Int2DHasher();

    private static class Long2DHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        Long2DHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof long[][]) ? CrossHash.Wisp.hash((long[][]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right
                    || ((left instanceof long[][] && right instanceof long[][])
                    ? equalityHelper((long[][]) left, (long[][]) right, longHasher)
                    : Objects.equals(left, right));
        }
    }

    public static final IHasher long2DHasher = new Long2DHasher();

    private static class StringHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        StringHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof CharSequence) ? CrossHash.Wisp.hash((CharSequence) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return Objects.equals(left, right);
        }
    }

    public static final IHasher stringHasher = new StringHasher();

    private static class StringArrayHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        StringArrayHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof CharSequence[]) ? CrossHash.Wisp.hash((CharSequence[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right || ((left instanceof CharSequence[] && right instanceof CharSequence[]) ? equalityHelper((CharSequence[]) left, (CharSequence[]) right, stringHasher) : Objects.equals(left, right));
        }
    }

    /**
     * Though the name suggests this only hashes String arrays, it can actually hash any CharSequence array as well.
     */
    public static final IHasher stringArrayHasher = new StringArrayHasher();

    private static class ObjectArrayHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        ObjectArrayHasher() {
        }

        @Override
        public int hash(final Object data) {
            return (data instanceof Object[]) ? CrossHash.Wisp.hash((Object[]) data) : data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right || ((left instanceof Object[] && right instanceof Object[]) && Arrays.equals((Object[]) left, (Object[]) right) || Objects.equals(left, right));
        }
    }
    public static final IHasher objectArrayHasher = new ObjectArrayHasher();

    private static class DefaultHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        DefaultHasher() {
        }

        @Override
        public int hash(final Object data) {
            return data.hashCode();
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return Objects.equals(left, right);
        }
    }

    public static final IHasher defaultHasher = new DefaultHasher();

    private static class IdentityHasher implements IHasher, Serializable
    {
        private static final long serialVersionUID = 3L;
        IdentityHasher() { }

        @Override
        public int hash(Object data) {
            return System.identityHashCode(data);
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            return left == right;
        }
    }
    public static final IHasher identityHasher = new IdentityHasher();

    private static class GeneralHasher implements IHasher, Serializable {
        private static final long serialVersionUID = 3L;

        GeneralHasher() {
        }

        @Override
        public int hash(final Object data) {
            return CrossHash.Wisp.hash(data);
        }

        @Override
        public boolean areEqual(Object left, Object right) {
            if(left == right) return true;
            Class l = left.getClass(), r = right.getClass();
            if(l == r)
            {
                if(l.isArray())
                {
                    if(left instanceof int[]) return Arrays.equals((int[]) left, (int[]) right);
                    else if(left instanceof long[]) return Arrays.equals((long[]) left, (long[]) right);
                    else if(left instanceof char[]) return Arrays.equals((char[]) left, (char[]) right);
                    else if(left instanceof double[]) return Arrays.equals((double[]) left, (double[]) right);
                    else if(left instanceof boolean[]) return Arrays.equals((boolean[]) left, (boolean[]) right);
                    else if(left instanceof byte[]) return Arrays.equals((byte[]) left, (byte[]) right);
                    else if(left instanceof float[]) return Arrays.equals((float[]) left, (float[]) right);
                    else if(left instanceof short[]) return Arrays.equals((short[]) left, (short[]) right);
                    else if(left instanceof char[][]) return equalityHelper((char[][]) left, (char[][]) right, charHasher);
                    else if(left instanceof int[][]) return equalityHelper((int[][]) left, (int[][]) right, intHasher);
                    else if(left instanceof long[][]) return equalityHelper((long[][]) left, (long[][]) right, longHasher);
                    else if(left instanceof CharSequence[]) return equalityHelper((CharSequence[]) left, (CharSequence[]) right, stringHasher);
                    else if(left instanceof Object[]) return Arrays.equals((Object[]) left, (Object[]) right);
                }
                return Objects.equals(left, right);
            }
            return false;
        }
    }

    /**
     * This IHasher is the one you should use if you aren't totally certain what types will go in an OrderedMap's keys
     * or an OrderedSet's items, since it can handle mixes of elements.
     */
    public static final IHasher generalHasher = new GeneralHasher();

    /**
     * A quick, simple hashing function that seems to have good results. Like LightRNG, it stores a state that
     * it updates independently of the output, and this starts at a large prime. At each step, it takes the
     * current item in the array being hashed, adds a large non-prime used in LightRNG's generation function
     * (it's 2 to the 64, times the golden ratio phi, and truncated to a signed long), multiplies by a prime
     * called the "state multiplier", adds the result to the state and stores it, multiplies the value of the
     * state by another prime called the "output multiplier", then XORs the current result with that value
     * before moving onto the next item in the array. A finalization step XORs the result with a complex value
     * made by adding the state (left over from the previous step) to what was the output multiplier, adding
     * the last known value for result to the phi-related constant from LightRNG, multiplying that pair, adding
     * the initial state (which turns out to be unusually good for this, despite no particularly special numeric
     * qualities other than being a probable prime) and then bitwise-rotating it left by a seemingly-random
     * number drawn from the highest 6 bits of the state.
     * <br>
     * This all can be done very quickly; a million hashes of a million different 16-element long arrays can be
     * computed in under 18-20 ms (in the benchmark, some amount of that is overhead from generating a new
     * array with LongPeriodRNG, since the benchmark uses that RNG's state for data, and the default
     * Arrays.hashCode implementation is only somewhat faster at under 16 ms). After several tries and tweaks
     * to the constants this uses, it also gets remarkably few hash collisions. On the same 0x100000, or
     * 1048576, RNG states for data, Lightning gets 110 collisions, the JDK Arrays.hashCode method gets 129
     * collisions, Sip (implementing SipHash) gets 145 collisions, and CrossHash (using the FNV-1a algorithm)
     * gets 135 collisions. Storm depends on the salt chosen, but with one initialized with the phi-based
     * constant that shows up in LightRNG and here, Storm gets 116 collisions. Dispersion is not perfect, but
     * at many bit sizes Lightning continues to have less collisions (it disperses better than the other hashes
     * with several quantities of bits, at least on this test data). Lightning also does relatively well, though
     * it isn't clearly ahead of the rest all the time, when hashing Strings, especially ones that use a larger
     * set of letters, it seems (FakeLanguageGen was used to make test data, and languages that used more
     * characters in their alphabet seemed to hash better with this than competing hashes for some reason).
     * <br>
     * There is certainly room for improvement with the specific numbers chosen; earlier versions used the
     * state multiplier "Neely's number", which is a probable prime made by taking the radix-29 number
     * "HARGNALLINSCLOPIOPEEPIO" (a reference to a very unusual TV show), truncating to 64 bits, and rotating
     * right by 42 bits. This version uses "Neely's number" for an initial state and during finalization, and
     * uses a different probable prime as the state multiplier, made with a similar process; it starts with the
     * radix-36 number "EDSGERWDIJKSTRA", then does the same process but rotates right by 37 bits to obtain a
     * different prime. This tweak seems to help with hash collisions. Extensive trial and error was used to
     * find the current output multiplier, which has no real relationship to anything else but has exactly 32 of
     * 64 bits set to 1, has 1 in the least and most significant bit indices (meaning it is negative and odd),
     * and other than that seems to have better results on most inputs for mystifying reasons. Earlier versions
     * applied a Gray code step to alter the output instead of a multiplier that heavily overflows to obfuscate
     * state, but that had a predictable pattern for most of the inputs tried, which seemed less-than-ideal for
     * a hash. Vitally, Lightning avoids predictable collisions that Arrays.hashCode has, like
     * {@code Arrays.hashCode(new long[]{0})==Arrays.hashCode(new long[]{-1})}.
     * <br>
     * The output multiplier is 0xC6BC279692B5CC83L, the state multiplier is 0xD0E89D2D311E289FL, the number
     * added to the state (from LightRNG and code derived from FastUtil, but obtained from the golden ratio
     * phi) is 0x9E3779B97F4A7C15L, and the starting state ("Neely's Number") is 0x632BE59BD9B4E019L.
     * <br>
     * To help find patterns in hash output in a visual way, you can hash an x,y point, take the bottom 24 bits,
     * and use that as an RGB color for the pixel at that x,y point. On a 512x512 grid of points, the patterns
     * in Arrays.hashCode and the default CrossHash algorithm (FNV-1a) are evident, and Sip (implementing
     * SipHash) does approximately as well as Lightning, with no clear patterns visible (Sip has been removed
     * from SquidLib because it needs a lot of code and is slower than Storm and especially Lightning). The
     * idea is from a technical report on visual uses for hashing,
     * http://www.clockandflame.com/media/Goulburn06.pdf .
     * <ul>
     * <li>{@link java.util.Arrays#hashCode(int[])}: http://i.imgur.com/S4Gh1sX.png</li>
     * <li>{@link CrossHash#hash(int[])}: http://i.imgur.com/x8SDqvL.png</li>
     * <li>(Former) CrossHash.Sip.hash(int[]): http://i.imgur.com/keSpIwm.png</li>
     * <li>{@link CrossHash.Lightning#hash(int[])}: http://i.imgur.com/afGJ9cA.png</li>
     * </ul>
     */
    // tested output multipliers
    // 0x DA1A459BD9B4C619L
    // 0x DC1A459879B5C619L
    // 0x DC1A479829B5E639L
    // 0x DC1A479C29B5C639L
    // 0x EA1C479692B5C639L
    // 0x CA1C479692B5C635L // this gets 105 collisions, low
    // 0x CABC479692B5C635L
    // 0x DC1A479C29B5C647L
    // 0x DC1A479C29B5C725L
    // 0x CABC279692B5CB21L
    // 0x C6BC279692B5CC83L // this gets 100 collisions, lowest
    // 0x C6BC279692B4D8A5L
    // 0x C6BC279692B4D345L
    // 0x C6EC273692B4A4B9L
    // 0x C6A3256B52D5B463L
    // 0x C6A3256B52D5B463L
    // 0x C6A3256D52D5B4C9L
    // 0x D8A3256D52D5B619L
    // 0x D96E6AC724658947L
    // 0x D96E6AC724658C2DL
    // 0x CCABF9E32FD684F9L
    // 0x C314163FAF912A01L
    // 0x C3246007A332C12AL
    // 0x CA1C479692B5C6ABL
    // 0x C6B5275692B5CC83 // untested so far
    @SuppressWarnings("NumericOverflow")
    public static final class Lightning {

        public static long hash64(final boolean[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] ? 0x9E3779B97F4A7C94L : 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final byte[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final short[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final char[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final int[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final long[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final float[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (NumberTools.floatToIntBits(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final double[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (NumberTools.doubleToLongBits(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = start; i < end && i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final char[] data, final int start, final int end, final int step) {
            if (data == null || start >= end || step <= 0)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = start; i < end && i < data.length; i += step) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final CharSequence data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length(); i++) {
                result ^= (z += (data.charAt(i) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final char[][] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final long[][] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final CharSequence[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final Iterable<? extends CharSequence> data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (CharSequence datum : data) {
                result ^= (z += (hash64(datum) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final CharSequence[]... data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static long hash64(final Object[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            Object o;
            for (int i = 0; i < data.length; i++) {
                o = data[i];
                result ^= (z += ((o == null ? 0 : o.hashCode()) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public static int hash(final boolean[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] ? 0x9E3779B97F4A7C94L : 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final byte[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final short[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final char[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final int[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;

            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final long[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final float[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (NumberTools.floatToIntBits(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final double[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (NumberTools.doubleToLongBits(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;

            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = start; i < end && i < data.length; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final char[] data, final int start, final int end, final int step) {
            if (data == null || start >= end || step <= 0)
                return 0;

            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = start; i < end && i < data.length; i += step) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final CharSequence data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length(); i++) {
                result ^= (z += (data.charAt(i) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final char[][] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final long[][] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final CharSequence[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final Iterable<? extends CharSequence> data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (CharSequence datum : data) {
                result ^= (z += (hash64(datum) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final CharSequence[]... data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public static int hash(final Object[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            Object o;
            for (int i = 0; i < data.length; i++) {
                o = data[i];
                result ^= (z += ((o == null ? 0 : o.hashCode()) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * 0xC6BC279692B5CC83L;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }
    }

    /**
     * A whole cluster of Lightning-like hash functions that sacrifice a small degree of speed, but can be
     * constructed with a salt value that helps obscure what hashing function is actually being used.
     * <br>
     * The salt field is not serialized, so it is important that the same salt will be given by the
     * program when the same hash results are wanted for some inputs.
     * <br>
     * A group of 24 static, final, pre-initialized Storm members are present in this class, each with the
     * name of a letter in the Greek alphabet (this uses the convention on Wikipedia,
     * https://en.wikipedia.org/wiki/Greek_alphabet#Letters , where lambda is spelled with a 'b'). The whole
     * group of 24 pre-initialized members are also present in a static array called {@code predefined}.
     * These can be useful when, for example, you want to get multiple hashes of a single array or String
     * as part of cuckoo hashing or similar techniques that need multiple hashes for the same inputs.
     */
    public static final class Storm implements Serializable {
        private static final long serialVersionUID = 3152426757973945155L;

        private transient long $alt;

        public Storm() {
            this(0L);
        }

        public Storm(final CharSequence alteration) {
            this(Lightning.hash64(alteration));
        }

        public Storm(final long alteration) {
            long s = alteration + 0x9E3779B97F4A7C15L;
            s = (s ^ (s >>> 30)) * 0xBF58476D1CE4E5B9L;
            s = (s ^ (s >>> 27)) * 0x94D049BB133111EBL;
            s ^= s >>> 31;
            $alt = s += 191 - Long.bitCount(s);
        }

        public static final Storm alpha = new Storm("alpha"), beta = new Storm("beta"), gamma = new Storm("gamma"),
                delta = new Storm("delta"), epsilon = new Storm("epsilon"), zeta = new Storm("zeta"),
                eta = new Storm("eta"), theta = new Storm("theta"), iota = new Storm("iota"),
                kappa = new Storm("kappa"), lambda = new Storm("lambda"), mu = new Storm("mu"),
                nu = new Storm("nu"), xi = new Storm("xi"), omicron = new Storm("omicron"), pi = new Storm("pi"),
                rho = new Storm("rho"), sigma = new Storm("sigma"), tau = new Storm("tau"),
                upsilon = new Storm("upsilon"), phi = new Storm("phi"), chi = new Storm("chi"), psi = new Storm("psi"),
                omega = new Storm("omega");
        public static final Storm[] predefined = new Storm[]{alpha, beta, gamma, delta, epsilon, zeta, eta, theta, iota,
                kappa, lambda, mu, nu, xi, omicron, pi, rho, sigma, tau, upsilon, phi, chi, psi, omega};

        public long hash64(final boolean[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] ? 0x9E3779B97F4A7C15L : 0x789ABCDEFEDCBA98L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final byte[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final short[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final char[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final int[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final long[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final float[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (NumberTools.floatToIntBits(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final double[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (NumberTools.doubleToLongBits(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = start; i < end && i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final char[] data, final int start, final int end, final int step) {
            if (data == null || start >= end || step <= 0)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = start; i < end && i < len; i += step) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final CharSequence data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length();
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data.charAt(i) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58));
        }

        public long hash64(final char[][] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final long[][] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final CharSequence[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final Iterable<? extends CharSequence> data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (CharSequence datum : data) {
                result ^= (z += (hash64(datum) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final CharSequence[]... data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public long hash64(final Object[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            Object o;
            for (int i = 0; i < len; i++) {
                o = data[i];
                result ^= (z += ((o == null ? 0 : o.hashCode()) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;

            }
            return result ^ Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58));
        }

        public int hash(final boolean[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] ? 0x9E3779B97F4A7C15L : 0x789ABCDEFEDCBA98L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final byte[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final short[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final char[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final int[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;

            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final long[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final float[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (NumberTools.floatToIntBits(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final double[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (NumberTools.doubleToLongBits(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;

            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = start; i < end && i < len; i++) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final char[] data, final int start, final int end, final int step) {
            if (data == null || start >= end || step <= 0)
                return 0;

            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = start; i < end && i < len; i += step) {
                result ^= (z += (data[i] + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final CharSequence data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length();
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (data.charAt(i) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final char[][] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final long[][] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final CharSequence[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final Iterable<? extends CharSequence> data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (CharSequence datum : data) {
                result ^= (z += (hash64(datum) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final CharSequence[]... data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            for (int i = 0; i < len; i++) {
                result ^= (z += (hash64(data[i]) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;
            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }

        public int hash(final Object[] data) {
            if (data == null)
                return 0;
            final long chips = $alt << 1 ^ 0xC6BC279692B5CC83L, len = data.length;
            long z = 0x632BE59BD9B4E019L + chips, result = 1L;
            Object o;
            for (int i = 0; i < len; i++) {
                o = data[i];
                result ^= (z += ((o == null ? 0 : o.hashCode()) + 0x9E3779B97F4A7C15L) * 0xD0E89D2D311E289FL) * chips;

            }
            return (int) ((result ^= Long.rotateLeft((z * 0xC6BC279692B5CC83L ^ $alt ^ result * 0x9E3779B97F4A7C15L) + 0x632BE59BD9B4E019L, (int) (chips + z >>> 58))) ^ (result >>> 32));
        }
    }

    /**
     * An alternative hashing function that is slightly faster than CrossHash.Lightning, drastically
     * faster than the default CrossHash methods (not in an inner class), and has good quality on most
     * input, but has issues with certain methods (namely, {@link CrossHash.Falcon#hash(long[])} and
     * {@link CrossHash.Falcon#hash(double[])} disregard the upper 32 bits, at the least, of any items
     * in their input arrays, though the hash64 variants don't have this issue).
     * <br>
     * In most cases Lightning is a safe alternative, and is only slightly slower. If statistical
     * quality or "salting" of the hash is particularly important, you should use {@link CrossHash.Storm}
     * with a variety of salts/alterations.
     * <br>
     * Created by Tommy Ettinger on 1/16/2016.
     */
    @Beta
    public static final class Falcon {
        public static long hash64(final boolean[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= (data[i] ? 0xC6BC279692B5CC83L : 0x789ABCDEFEDCBA98L) * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final byte[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final short[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final char[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final int[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final long[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final float[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= NumberTools.floatToIntBits(data[i]) * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final double[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= NumberTools.doubleToLongBits(data[i]) * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final CharSequence data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length(); i++) {
                result += (z ^= data.charAt(i) * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = start; i < end && i < data.length; i++) {
                result += (z ^= data[i] * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final char[] data, final int start, final int end, final int step) {
            if (data == null || start >= end || step <= 0)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = start; i < end && i < data.length; i += step) {
                result += (z ^= data[i] * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final char[][] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= hash64(data[i]) * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final long[][] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= hash64(data[i]) * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final CharSequence[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= hash64(data[i]) * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final CharSequence[]... data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= hash64(data[i]) * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }

        public static long hash64(final Object[] data) {
            if (data == null)
                return 0;
            long z = 0x632BE59BD9B4E019L, result = 1L;
            Object o;
            for (int i = 0; i < data.length; i++) {
                o = data[i];
                result += (z ^= (o == null ? 0 : o.hashCode()) * 0xD0E89D2D311E289FL) + 0x9E3779B97F4A7C15L;
            }
            return result ^ ((z ^ result) >>> 16) * 0x9E3779B97F4A7C15L;
        }


        public static int hash(final boolean[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= (data[i] ? 0x9E3779B9 : 0x789ABCDEL) * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final byte[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final short[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final char[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final int[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        /**
         * Be aware that this disregards the most-significant 32 bits of each long in data.
         * Its use is discouraged, and if you need 32-bit hashes of long arrays, you should use
         * {@link CrossHash.Lightning#hash(long[])} instead.
         *
         * @param data an array of long; be aware that this disregards a significant amount of data
         * @return a 32-bit int hash of some of data
         * @see CrossHash.Lightning#hash(long[]) You should prefer CrossHash.Lightning for this
         */
        public static int hash(final long[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= data[i] * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final float[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= NumberTools.floatToIntBits(data[i]) * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        /**
         * Be aware that this disregards the most-significant 32 bits of the long
         * representation of each double in data. Its use is discouraged, and if you
         * need 32-bit hashes of double arrays, you should use
         * {@link CrossHash.Lightning#hash(double[])} instead.
         *
         * @param data an array of double; be aware that this disregards a significant amount of data
         * @return a 32-bit int hash of some of data
         * @see CrossHash.Lightning#hash(double[]) You should prefer CrossHash.Lightning for this
         */
        public static int hash(final double[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= NumberTools.doubleToLongBits(data[i]) * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final CharSequence data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length(); i++) {
                result += (z ^= data.charAt(i) * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = start; i < end && i < data.length; i++) {
                result += (z ^= data[i] * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final char[] data, final int start, final int end, final int step) {
            if (data == null || start >= end || step <= 0)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = start; i < end && i < data.length; i += step) {
                result += (z ^= data[i] * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final char[][] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= hash(data[i]) * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final long[][] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= hash(data[i]) * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final CharSequence[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= hash(data[i]) * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final CharSequence[]... data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            for (int i = 0; i < data.length; i++) {
                result += (z ^= hash(data[i]) * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }

        public static int hash(final Object[] data) {
            if (data == null)
                return 0;
            int z = 0x632BE5AB, result = 1;
            Object o;
            for (int i = 0; i < data.length; i++) {
                o = data[i];
                result += (z ^= (o == null ? 0 : o.hashCode()) * 0x85157AF5) + 0x62E2AC0D;
            }
            return result ^ ((z ^ result) >>> 8) * 0x9E3779B9;
        }
    }
    // Nice ints, all probable primes except the last one, for 32-bit hashing
    // 0x62E2AC0D 0x632BE5AB 0x85157AF5 0x9E3779B9
    /**
     * The fastest hash in CrossHash, but no slouch on quality, either. Uses a finely-tuned mix of very few operations
     * for each element, plus a minimal and simple finalization step, and as such obtains superior speed on the standard
     * benchmark SquidLib uses for hashes (hashing one million 16-element long arrays, remaining the best in both 32-bit
     * and 64-bit versions). Specifically, Wisp takes 9.478 ms to generate a million 64-bit hashes on a recent laptop
     * with an i7-6700HQ processor (removing the time the control takes to generate the million arrays), whereas on the
     * same setup the second-fastest hash, Falcon, takes 12.783 ms (also removing generation time). For comparison, the
     * JDK's Arrays.hashCode method takes 13.642 ms on the same workload, though it produces 32-bit hashes. Wisp
     * performs almost exactly as well producing 32-bit hashes as it does 64-bit hashes, where Falcon slows down
     * slightly, and other hashes suffer a larger penalty producing 32-bit. This also avoids the quality issue in
     * Falcon's 32-bit hashes of longs or doubles (all bits are considered here), and passes visual tests where an
     * earlier version of Wisp did not. Collision rates are on-par with all other CrossHash classes and the JDK's
     * Arrays.hashCode method, that is, acceptably low.
     * <br>
     * This version replaces an older version of Wisp that had serious quality issues and wasn't quite as fast. Since
     * the only reason one would use the older version was speed without regard for quality, and it was marked as Beta,
     * a faster version makes sense to replace the slower one, rather than add yet another nested class in CrossHash.
     * <br>
     * Wisp is no longer considered Beta-quality, and due to its speed and apparently very low collision rate in most
     * arrays, it's recommended for usage in more places now. Code that used Lightning should in probably switch to Wisp
     * if GWT is a potential target, since Wisp doesn't rely on having certain JVM optimizations that are probably only
     * available on desktop platforms.
     */
    public static final class Wisp {
        public static long hash64(final boolean[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * (data[i] ? 0xC6BC279692B5CC83L : 0xAEF17502108EF2D9L));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final byte[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]);
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final short[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]);
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final char[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]);
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final int[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]);
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final long[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]);
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final float[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * NumberTools.floatToIntBits(data[i]));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        /**
         * The hashAlt and hash64Alt methods for floating-point number arrays have better visual hashing properties than
         * hash and hash64, but are somewhat slower on desktop. They may be drastically faster than hash and hash64 on
         * GWT, however, because they don't use {@link Double#doubleToLongBits(double)} or its equivalent for Floats,
         * and those methods have much more complex implementations on GWT than on desktop Java.
         * @param data a float array to hash
         * @return a 64-bit hash code of data
         */
        public static long hash64Alt(final float[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            double t;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * ((long) (-0xD0E8.9D2D311E289Fp-25 * (t = data[i]) + t * -0x1.39b4dce80194cp9)));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final double[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * NumberTools.doubleToLongBits(data[i]));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        /**
         * The hashAlt and hash64Alt methods for floating-point number arrays have better visual hashing properties than
         * hash and hash64, but are somewhat slower on desktop. They may be drastically faster than hash and hash64 on
         * GWT, however, because they don't use {@link Double#doubleToLongBits(double)} or its equivalent for Floats,
         * and those methods have much more complex implementations on GWT than on desktop Java.
         * @param data a double array to hash
         * @return a 64-bit hash code of data
         */
        public static long hash64Alt(final double[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            double t;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * ((long) (-0xD0E8.9D2D311E289Fp-25 * (t = data[i]) + t * -0x1.39b4dce80194cp9)));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final CharSequence data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length();
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data.charAt(i));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = end < data.length ? end : data.length;
            for (int i = start; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]);
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final char[][] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data[i]));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final int[][] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data[i]));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final long[][] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data[i]));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final CharSequence[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data[i]));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final CharSequence[]... data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data[i]));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final Iterable<? extends CharSequence> data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            for (CharSequence datum : data) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(datum));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final List<? extends CharSequence> data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.size();
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data.get(i)));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }
        
        public static long hash64(final Object[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            Object o;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * ((o = data[i]) == null ? -1L : o.hashCode()));
            }
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static long hash64(final Object data) {
            if (data == null)
                return 0L;
            long a = 0x632BE59BD9B4E019L ^ 0x8329C6EB9E6AD3E3L * data.hashCode(), result = 0x9E3779B97F4A7C94L + a;
            return result * (a | 1L) ^ (result >>> 27 | result << 37);
        }

        public static int hash(final boolean[] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * (data[i] ? 0x789ABCDE : 0x62E2AC0D));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }


        public static int hash(final byte[] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]);
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final short[] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]);
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final char[] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]);
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }
        public static int hash(final int[] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]);
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final long[] data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]);
            }
            return (int)((result = (result * (a | 1L) ^ (result >>> 27 | result << 37))) ^ (result >>> 32));
        }
        /**
         * This method is reasonable in quality and speed on desktop, though it has some visual hashing artifacts.
         * The hashAlt method for float arrays has better visual hashing properties than this one, but it is somewhat
         * slower on desktop. Because hashAlt turns out to be faster on GWT, super-sourcing is used to replace calls to
         * {@link Wisp#hash(float[])} with calls to {@link Wisp#hashAlt(float[])} on GWT only. This changes the results
         * between desktop/Android and GWT, but GWT was different anyway due to how it handles math being... "special."
         * @param data a float array to hash
         * @return a 32-bit hash code of data
         */
        public static int hash(final float[] data) {
            return NumberTools.hashWisp(data);
        }

        /**
         * This method is identical to {@link Wisp#hash(float[])} on desktop, but unlike that method, it will not be
         * super-sourced on GWT. This makes it slower (sometimes significantly so) on GWT, but using this method on GWT
         * will behave closer to how it behaves on desktop than using {@link Wisp#hash(float[])} on GWT. GWT will
         * probably not produce the same numbers anyway, because its math is "special."
         * @param data a float array to hash
         * @return a 32-bit hash code of data
         */
        public static int hashAlt(final float[] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            double t;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * ((int) (-0xD0E8.9D2D311E289Fp-25f * (t = data[i]) + t * -0x1.39b4dce80194cp9f)));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        /**
         * This method is identical to {@link Wisp#hashAlt(double[])} on desktop, but is super-sourced on GWT, replacing
         * the implementation only on that platform, to avoid math with 64-bit longs. This means this method, though not
         * {@link Wisp#hashAlt(double[])}, will have different results for the same input on desktop/Android versus on
         * GWT. GWT was different anyway, though, due to how it handles math being generally "special."
         * @param data a double array to hash
         * @return a 32-bit hash code of data
         */
        public static int hash(final double[] data) {
            return NumberTools.hashWisp(data);
        }

        /**
         * This method is identical to {@link Wisp#hash(double[])} on desktop, but unlike that method, it will not be
         * super-sourced on GWT. This makes it slower (sometimes significantly so) on GWT, but using this method on GWT
         * will behave closer to how it behaves on desktop than using {@link Wisp#hash(double[])} on GWT. GWT will
         * probably not produce the same numbers anyway, because its math is "special."
         * @param data a double array to hash
         * @return a 32-bit hash code of data
         */
        public static int hashAlt(final double[] data) {
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

        public static int hash(final CharSequence data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length();
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data.charAt(i));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = end < data.length ? end : data.length;
            for (int i = start; i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]);
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final char[][] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data[i]));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final int[][] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data[i]));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final long[][] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data[i]));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final CharSequence[] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data[i]));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final CharSequence[]... data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data[i]));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final Iterable<? extends CharSequence> data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            for (CharSequence datum : data) {
                result += (a ^= 0x85157AF5 * hash(datum));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final List<? extends CharSequence> data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.size();
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data.get(i)));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final Object[] data) {
            if (data == null)
                return 0;
            int result = 0x9E3779B9, a = 0x632BE5AB;
            final int len = data.length;
            Object o;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * ((o = data[i]) == null ? -1 : o.hashCode()));
            }
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }

        public static int hash(final Object data) {
            if (data == null)
                return 0;
            int a = 0x632BE5AB ^ 0x85157AF5 * data.hashCode(), result = 0x9E3779B9 + a;
            return result * (a | 1) ^ (result >>> 11 | result << 21);
        }
    }


    /**
     * Strongly universal hashing based loosely on Daniel Lemire's (Apache-licensed) code and paper, available at
     * https://github.com/lemire/StronglyUniversalStringHashing . This should have among the best statistical qualities
     * of any of these hashes while being fairly fast. It is like {@link Storm} in that you need to instantiate a
     * Chariot object to use its hashing functions, but unlike Storm's single long it uses to salt the hash, these
     * objects have a somewhat-large, expansible cache of random numbers they use to modify every result differently
     * based on its position in the input array. The cache's size is related to the largest array, String, or similar
     * sequence that this has been required to hash, and the cache's size should be about 4 bytes per byte that needs to
     * be hashed in a single input (so hashing only 16-byte arrays, of any number of such arrays, would require 64 bytes
     * of cache). Technically, only slightly more than 2 bytes per byte are required, but this caches more random
     * numbers in advance to speed up expected larger inputs. Only produces 32-bit hashes because of constraints on this
     * strongly universal hash algorithm; you could create two or more of these with different seeds and run them on the
     * same inputs to get more than 32 bits, although that would be rather slow.
     * <br>
     * For 32-bit hash functions where the function can be altered by a salt, CrossHash provides {@link Storm} and now
     * Chariot, and assuming there aren't bugs in Chariot, this class could be preferable because it has about equal
     * performance and the salt is a less-predictable 128 bits instead of 64 bits. If you need 64-bit hashes, you should
     * use Storm instead unless you want to chain together two 32-bit hashes from Chariot (with 256 bits of salt).
     * <br>
     * This previously failed visual testing, but now a finalization step nicely eliminates artifacts on the hashes of
     * similar points or inputs. It is now almost exactly the same speed as Storm (within the margin of error, or 50
     * microseconds of difference per million hashes, and the quality is pretty much indistinguishable (which is good).
     */
    @Beta
    public static final class Chariot implements Serializable {
        private static final long serialVersionUID = 3152426757973945155L;

        private final long $alt0, $alt1;

        private int top;

        private transient long $tate0, $tate1;

        private transient long[] $tore = null;

        public Chariot() {
            this(0L);
        }

        public Chariot(final CharSequence alteration) {
            this(Falcon.hash64(alteration));
        }

        public Chariot(final long alteration0, final long alteration1) {
            $alt0 = alteration0;
            $alt1 = alteration1;
            expand(32);
        }

        public Chariot(final long alteration) {
            long state = alteration + 0x9E3779B97F4A7C15L,
                    z = state;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            z ^= (z >>> 31);
            $alt0 = z + 191 - Long.bitCount(z);
            state += 0x9E3779B97F4A7C15L;
            z = state;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            z ^= (z >>> 31);
            $alt1 = z + 191 - Long.bitCount(z);
            expand(32);
        }

        private void expand(final int amount) {
            if (amount <= 0)
                return;
            int done;
            long z;
            if ($tore == null) {
                top = 32 + amount;
                $tore = new long[top];
                $tate0 = $alt0;
                $tate1 = $alt1;
                done = 0;
            } else {
                done = $tore.length;
                top = done + amount;
                long[] ls = new long[top];
                System.arraycopy($tore, 0, ls, 0, done);
                $tore = ls;
            }
            long s1;
            for (; done < top; done++) {
                final long s0 = $tate0;
                s1 = $tate1;
                $tore[done] = s0 + s1;

                s1 ^= s0;
                $tate0 = Long.rotateLeft(s0, 55) ^ s1 ^ (s1 << 14); // a, b
                $tate1 = Long.rotateLeft(s1, 36); // c
            }
        }

        public static final Chariot alpha = new Chariot("alpha"), beta = new Chariot("beta"), gamma = new Chariot("gamma"),
                delta = new Chariot("delta"), epsilon = new Chariot("epsilon"), zeta = new Chariot("zeta"),
                eta = new Chariot("eta"), theta = new Chariot("theta"), iota = new Chariot("iota"),
                kappa = new Chariot("kappa"), lambda = new Chariot("lambda"), mu = new Chariot("mu"),
                nu = new Chariot("nu"), xi = new Chariot("xi"), omicron = new Chariot("omicron"), pi = new Chariot("pi"),
                rho = new Chariot("rho"), sigma = new Chariot("sigma"), tau = new Chariot("tau"),
                upsilon = new Chariot("upsilon"), phi = new Chariot("phi"), chi = new Chariot("chi"), psi = new Chariot("psi"),
                omega = new Chariot("omega");
        public static final Chariot[] predefined = new Chariot[]{alpha, beta, gamma, delta, epsilon, zeta, eta, theta, iota,
                kappa, lambda, mu, nu, xi, omicron, pi, rho, sigma, tau, upsilon, phi, chi, psi, omega};


        public int hash(final boolean[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if ((limit >> 5) + 3 > top)
                expand(top << 1 < (limit >> 5) + 3 ? (limit >> 5) + 3 : top);
            long sum = $tore[0], t = 0L;
            int i = 0, ii = 0;
            for (; i + 31 < limit; i += 32) {
                sum += (t = (data[i] ? 0x00000001L : 0L) | (data[i + 1] ? 0x00000002L : 0L)
                        | (data[i + 2] ? 0x00000004L : 0L) | (data[i + 3] ? 0x00000008L : 0L)
                        | (data[i + 4] ? 0x00000010L : 0L) | (data[i + 5] ? 0x00000020L : 0L)
                        | (data[i + 6] ? 0x00000040L : 0L) | (data[i + 7] ? 0x00000080L : 0L)
                        | (data[i + 8] ? 0x00000100L : 0L) | (data[i + 9] ? 0x00000200L : 0L)
                        | (data[i + 10] ? 0x00000400L : 0L) | (data[i + 11] ? 0x00000800L : 0L)
                        | (data[i + 12] ? 0x00001000L : 0L) | (data[i + 13] ? 0x00002000L : 0L)
                        | (data[i + 14] ? 0x00004000L : 0L) | (data[i + 15] ? 0x00008000L : 0L)
                        | (data[i + 16] ? 0x00010000L : 0L) | (data[i + 17] ? 0x00020000L : 0L)
                        | (data[i + 18] ? 0x00040000L : 0L) | (data[i + 19] ? 0x00080000L : 0L)
                        | (data[i + 20] ? 0x00100000L : 0L) | (data[i + 21] ? 0x00200000L : 0L)
                        | (data[i + 22] ? 0x00400000L : 0L) | (data[i + 23] ? 0x00800000L : 0L)
                        | (data[i + 24] ? 0x01000000L : 0L) | (data[i + 25] ? 0x02000000L : 0L)
                        | (data[i + 26] ? 0x04000000L : 0L) | (data[i + 27] ? 0x08000000L : 0L)
                        | (data[i + 28] ? 0x10000000L : 0L) | (data[i + 29] ? 0x20000000L : 0L)
                        | (data[i + 30] ? 0x40000000L : 0L) | (data[i + 31] ? 0x80000000L : 0L)
                ) * ($tore[++ii]);
            }
            if ((limit & 31) != 0) {
                t = 0L;
                for (int l = 0; l < (limit & 31); l++) {
                    t |= data[i++] ? 1L << l : 0L;
                }
                if (t == 0) sum += $tore[++ii] ^ 0x632BE59BD9B4E019L;
                else sum += t * ($tore[++ii]);
            } else if (limit > 0 && t == 0)
                sum += $tore[ii] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[ii + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final byte[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if ((limit >> 2) + 3 > top)
                expand(top << 1 < (limit >> 2) + 3 ? (limit >> 2) + 3 : top);
            long sum = $tore[0], t = 0L;
            int i = 0, ii = 0;
            for (; i + 3 < limit; i += 4) {
                sum += (t = (data[i] & 0xFFL) | (data[i + 1] & 0xFFL) << 8
                        | (data[i + 2] & 0xFFL) << 16 | (data[i + 3] & 0xFFL) << 24) * ($tore[++ii]);
            }
            if ((limit & 3) != 0) {
                t = 0L;
                for (int l = 0; l < (limit & 3); l++) {
                    t |= (data[i++] & 0xFFL) << (l << 3);
                }
                if (t == 0) sum += $tore[++ii] ^ 0x632BE59BD9B4E019L;
                else sum += t * ($tore[++ii]);
            } else if (limit > 0 && t == 0)
                sum += $tore[ii] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[ii + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final short[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if ((limit >> 1) + 3 > top)
                expand(top << 1 < (limit >> 1) + 3 ? (limit >> 1) + 3 : top);
            long sum = $tore[0], t = 0L;
            int i = 0, ii = 0;
            for (; i + 1 < limit; i += 2) {
                sum += (t = (data[i] & 0xFFFFL) | (data[i + 1] & 0xFFFFL) << 16) * ($tore[++ii]);
            }
            if ((limit & 1) != 0) {
                t = data[i] & 0xFFFFL;
                if (t == 0) sum += $tore[++ii] ^ 0x632BE59BD9B4E019L;
                else sum += t * ($tore[++ii]);
            } else if (limit > 0 && t == 0)
                sum += $tore[ii] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[ii + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final char[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if ((limit >> 1) + 3 > top)
                expand(top << 1 < (limit >> 1) + 3 ? (limit >> 1) + 3 : top);
            long sum = $tore[0], t = 0L;
            int i = 0, ii = 0;
            for (; i + 1 < limit; i += 2) {
                sum += (t = (data[i] & 0xFFFFL) | (data[i + 1] & 0xFFFFL) << 16) * ($tore[++ii]);
            }
            if ((limit & 1) != 0) {
                t = data[i] & 0xFFFFL;
                if (t == 0) sum += $tore[++ii] ^ 0x632BE59BD9B4E019L;
                else sum += t * ($tore[++ii]);
            } else if (limit > 0 && t == 0)
                sum += $tore[ii] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[ii + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final char[] data, final int start, final int end) {
            if (data == null)
                return 0;
            final int limit = end - start;
            if ((limit >> 1) + 3 > top)
                expand(top << 1 < (limit >> 1) + 3 ? (limit >> 1) + 3 : top);
            long sum = $tore[0], t = 0L;
            int i = start, ii = 0;
            for (; i + 1 < end; i += 2) {
                sum += (t = (data[i] & 0xFFFFL) | (data[i + 1] & 0xFFFFL) << 16) * ($tore[++ii]);
            }
            if ((limit & 1) != 0) {
                t = data[i] & 0xFFFFL;
                if (t == 0) sum += $tore[++ii] ^ 0x632BE59BD9B4E019L;
                else sum += t * ($tore[++ii]);
            } else if (limit > 0 && t == 0)
                sum += $tore[ii] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[ii + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final char[] data, final int start, final int end, final int step) {
            if (data == null)
                return 0;
            final int limit = (end - start + step - 1) / step;
            if ((limit >> 1) + 3 > top)
                expand(top << 1 < (limit >> 1) + 3 ? (limit >> 1) + 3 : top);
            long sum = $tore[0], t = 0L;
            int i = start, ii = 0;
            for (; i + 1 < end; i += step << 1) {
                sum += (t = (data[i] & 0xFFFFL) | (data[i + step] & 0xFFFFL) << 16) * ($tore[++ii]);
            }
            if ((limit & 1) != 0) {
                t = data[i] & 0xFFFFL;
                if (t == 0) sum += $tore[++ii] ^ 0x632BE59BD9B4E019L;
                else sum += t * ($tore[++ii]);
            } else if (limit > 0 && t == 0)
                sum += $tore[ii] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[ii + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final int[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if (limit + 2 > top)
                expand(top << 1 < limit + 2 ? limit + 2 : top);
            long sum = $tore[0];
            for (int i = 0; i < limit; ) {
                sum += (data[i] & 0xFFFFFFFFL) * ($tore[++i]);
            }
            if (limit > 0 && data[limit - 1] == 0)
                sum += $tore[limit] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[limit + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final long[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if ((limit << 1) + 2 > top)
                expand(top << 1 < (limit << 1) + 2 ? (limit << 1) + 2 : top);
            long sum = $tore[0], t = 0;
            for (int i = 0, ii = 1; i < limit; ii += 2) {
                sum += ((t = data[i++]) & 0xFFFFFFFFL) * ($tore[ii]) + (t >>> 32) * ($tore[ii + 1]);
            }
            if (limit > 0 && (t >>> 32) == 0)
                sum += $tore[limit] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[(limit << 1) + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }


        public int hash(final float[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if (limit + 2 > top)
                expand(top << 1 < limit + 2 ? limit + 2 : top);
            long sum = $tore[0];
            for (int i = 0; i < limit; ) {
                sum += (NumberTools.floatToIntBits(data[i]) & 0xFFFFFFFFL) * ($tore[++i]);
            }
            if (limit > 0 && data[limit - 1] == 0)
                sum += $tore[limit] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[limit + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final double[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if ((limit << 1) + 2 > top)
                expand(top << 1 < (limit << 1) + 2 ? (limit << 1) + 2 : top);
            long sum = $tore[0], t = 0;
            for (int i = 0, ii = 1; i < limit; ii += 2) {
                sum += ((t = NumberTools.doubleToLongBits(data[i++])) & 0xFFFFFFFFL) * ($tore[ii]) + (t >>> 32) * ($tore[ii + 1]);
            }
            if (limit > 0 && (t >>> 32) == 0)
                sum += $tore[limit] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[(limit << 1) + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final CharSequence data) {
            if (data == null)
                return 0;
            final int limit = data.length();
            if ((limit >> 1) + 3 > top)
                expand(top << 1 < (limit >> 1) + 3 ? (limit >> 1) + 3 : top);
            long sum = $tore[0], t = 0L;
            int i = 0, ii = 0;
            for (; i + 1 < limit; i += 2) {
                sum += (t = (data.charAt(i) & 0xFFFFL) | (data.charAt(i + 1) & 0xFFFFL) << 16) * ($tore[++ii]);
            }
            if ((limit & 1) != 0) {
                t = data.charAt(i) & 0xFFFFL;
                if (t == 0) sum += $tore[++ii] ^ 0x632BE59BD9B4E019L;
                else sum += t * ($tore[++ii]);
            } else if (limit > 0 && t == 0)
                sum += $tore[ii] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[ii + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final char[][] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if (limit + 2 > top)
                expand(top << 1 < limit + 2 ? limit + 2 : top);
            long sum = $tore[0];
            for (int i = 0; i < limit; ) {
                sum += (hash(data[i]) & 0xFFFFFFFFL) * ($tore[++i]);
            }
            if (limit > 0 && data[limit - 1] == null)
                sum += $tore[limit] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[limit + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final long[][] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if (limit + 2 > top)
                expand(top << 1 < limit + 2 ? limit + 2 : top);
            long sum = $tore[0];
            for (int i = 0; i < limit; ) {
                sum += (hash(data[i]) & 0xFFFFFFFFL) * ($tore[++i]);
            }
            if (limit > 0 && data[limit - 1] == null)
                sum += $tore[limit] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[limit + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final CharSequence[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if (limit + 2 > top)
                expand(top << 1 < limit + 2 ? limit + 2 : top);
            long sum = $tore[0];
            for (int i = 0; i < limit; ) {
                sum += (hash(data[i]) & 0xFFFFFFFFL) * ($tore[++i]);
            }
            if (limit > 0 && data[limit - 1] == null)
                sum += $tore[limit] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[limit + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }

        public int hash(final CharSequence[]... data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if (limit + 2 > top)
                expand(top << 1 < limit + 2 ? limit + 2 : top);
            long sum = $tore[0];
            for (int i = 0; i < limit; ) {
                sum += (hash(data[i]) & 0xFFFFFFFFL) * ($tore[++i]);
            }
            if (limit > 0 && data[limit - 1] == null)
                sum += $tore[limit] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[limit + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }


        public int hash(final Object[] data) {
            if (data == null)
                return 0;
            final int limit = data.length;
            if (limit + 2 > top)
                expand(top << 1 < limit + 2 ? limit + 2 : top);
            Object o;
            long sum = $tore[0];
            for (int i = 0; i < limit; ) {
                o = data[i];
                sum += (o == null ? 0xFFFFFFFFL : (o.hashCode() & 0xFFFFFFFFL)) * ($tore[++i]);
            }
            if (limit > 0 && data[limit - 1] == null)
                sum += $tore[limit] ^ 0x632BE59BD9B4E019L;
            sum ^= $tore[limit + 1] + sum >>> (5 + (sum >>> 59));
            return (int) (((sum *= 0xAEF17502108EF2D9L) >>> 43) ^ sum);
        }
    }

    /**
     * A whole cluster of Wisp-like hash functions that sacrifice a small degree of speed, but can be built with up
     * to 128 bits of salt values that help to obscure what hashing function is actually being used. This class is
     * similar to Storm in how you can construct one (using a CharSequence, one long to use to produce a salt, or in
     * this class two longs to use to produce a salt), but differs from Storm by being somewhat faster, having many
     * more possible salt "states" when using the constructors that take two longs or a CharSequence, and also by using
     * 32-bit math when only 32-bit inputs and output are used (relevant for GWT with its slower 64-bit math).
     * The salt values are mostly a pair of longs, but for the hash() functions that don't take a long array or double
     * array, a different salt value is used, a pair of ints.
     * <br>
     * The salt fields are not serialized, so it is important that the same salt will be given by the
     * program when the same hash results are wanted for some inputs.
     * <br>
     * A group of 24 static, final, pre-initialized Mist members are present in this class, each with the
     * name of a letter in the Greek alphabet (this uses the convention on Wikipedia,
     * https://en.wikipedia.org/wiki/Greek_alphabet#Letters , where lambda is spelled with a 'b'). The whole
     * group of 24 pre-initialized members are also present in a static array called {@code predefined}.
     * These can be useful when, for example, you want to get multiple hashes of a single array or String
     * as part of cuckoo hashing or similar techniques that need multiple hashes for the same inputs.
     */
    @Beta
    public static final class Mist implements Serializable {
        private static final long serialVersionUID = -1275284837479983271L;

        private transient long $l1, $l2;

        private transient int $i1, $i2;

        public Mist() {
            this(0x1234567876543210L, 0xEDCBA98789ABCDEFL);
        }

        public Mist(final CharSequence alteration) {
            this(Wisp.hash64(alteration), Falcon.hash64(alteration));
        }
        private static int permute(final long state)
        {
            int s = (int)state ^ 0xD0E89D2D;
            s = (s >>> 19 | s << 13);
            s ^= state >>> (5 + (state >>> 59));
            return ((s *= 277803737) >>> 22) ^ s;
        }

        @SuppressWarnings("NumericOverflow")
        public Mist(final long alteration) {
            $i1 = permute(alteration);
            $l1 = alteration + $i1;
            $l1 = ($l1 ^ ($l1 >>> 30)) * 0xBF58476D1CE4E5B9L;
            $l1 = ($l1 ^ ($l1 >>> 27)) * 0x94D049BB133111EBL;
            $l1 ^= $l1 >>> 31;

            $i2 = permute($l1 + 0x9E3779B97F4A7C15L);
            $l2 = alteration + 6 * 0x9E3779B97F4A7C15L;
            $l2 = ($l2 ^ ($l2 >>> 30)) * 0xBF58476D1CE4E5B9L;
            $l2 = ($l2 ^ ($l2 >>> 27)) * 0x94D049BB133111EBL;
            $l2 ^= $l2 >>> 31;
        }

        @SuppressWarnings("NumericOverflow")
        public Mist(final long alteration1, long alteration2) {
            $i1 = permute(alteration1);
            $l1 = alteration1 + $i1;
            $i2 = permute(alteration2 + $i1);
            $l2 = alteration2 + $i2;
        }

        /**
         * Alters all of the salt values in a pseudo-random way based on the previous salt value.
         * This will effectively make this Mist object a different, incompatible hashing functor.
         * Meant for use in Cuckoo Hashing, which can need the hash function to be updated or changed.
         * An alternative is to select a different Mist object from {@link #predefined}, or to simply
         * construct a new Mist with a different parameter or set of parameters.
         */
        @SuppressWarnings("NumericOverflow")
        public void randomize()
        {
            $i1 = permute($l2 + 3 * 0x9E3779B97F4A7C15L);
            $l1 = $l2 + $i1;
            $l1 = ($l1 ^ ($l1 >>> 30)) * 0xBF58476D1CE4E5B9L;
            $l1 = ($l1 ^ ($l1 >>> 27)) * 0x94D049BB133111EBL;
            $l1 ^= $l1 >>> 31;

            $i2 = permute($l1 + 5 * 0x9E3779B97F4A7C15L);
            $l2 = $l1 + 6 * 0x9E3779B97F4A7C15L;
            $l2 = ($l2 ^ ($l2 >>> 30)) * 0xBF58476D1CE4E5B9L;
            $l2 = ($l2 ^ ($l2 >>> 27)) * 0x94D049BB133111EBL;
            $l2 ^= $l2 >>> 31;

        }

        public static final Mist alpha = new Mist("alpha"), beta = new Mist("beta"), gamma = new Mist("gamma"),
                delta = new Mist("delta"), epsilon = new Mist("epsilon"), zeta = new Mist("zeta"),
                eta = new Mist("eta"), theta = new Mist("theta"), iota = new Mist("iota"),
                kappa = new Mist("kappa"), lambda = new Mist("lambda"), mu = new Mist("mu"),
                nu = new Mist("nu"), xi = new Mist("xi"), omicron = new Mist("omicron"), pi = new Mist("pi"),
                rho = new Mist("rho"), sigma = new Mist("sigma"), tau = new Mist("tau"),
                upsilon = new Mist("upsilon"), phi = new Mist("phi"), chi = new Mist("chi"), psi = new Mist("psi"),
                omega = new Mist("omega");
        public static final Mist[] predefined = new Mist[]{alpha, beta, gamma, delta, epsilon, zeta, eta, theta, iota,
                kappa, lambda, mu, nu, xi, omicron, pi, rho, sigma, tau, upsilon, phi, chi, psi, omega};

        public long hash64(final boolean[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * (data[i] ? 0x9E3779B97F4A7C15L : 0x789ABCDEFEDCBA98L)) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }


        public long hash64(final byte[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final short[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final char[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final int[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final long[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }


        public long hash64(final float[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * NumberTools.floatToIntBits(data[i])) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final double[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * NumberTools.doubleToLongBits(data[i])) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = start; i < end && i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final char[] data, final int start, final int end, final int step) {
            if (data == null || start >= end || step <= 0)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = start; i < end && i < len; i += step) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final CharSequence data) {
            if (data == null)
                return 0;
            final int len = data.length();
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data.charAt(i)) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final char[][] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data[i])) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final long[][] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data[i])) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final CharSequence[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data[i])) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final Iterable<? extends CharSequence> data) {
            if (data == null)
                return 0;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (CharSequence datum : data) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(datum)) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final CharSequence[]... data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * hash64(data[i])) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public long hash64(final Object[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            Object o;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * ((o = data[i]) == null ? -1 : o.hashCode())) ^ $l2 * a + $l1;
            }
            return result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37);
        }

        public int hash(final boolean[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * (data[i] ? 0x789ABCDE : 0x62E2AC0D)) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final byte[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final short[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final char[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final int[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final long[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * data[i]) ^ $l2 * a + $l1;
            }
            return (int)((result = (result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37))) ^ (result >>> 32));
        }


        public int hash(final float[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * NumberTools.floatToIntBits(data[i])) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final double[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            long result = 0x9E3779B97F4A7C94L + $l2, a = 0x632BE59BD9B4E019L;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x8329C6EB9E6AD3E3L * NumberTools.doubleToLongBits(data[i])) ^ $l2 * a + $l1;
            }
            return (int)((result = (result * (a * $l1 | 1L) ^ (result >>> 27 | result << 37))) ^ (result >>> 32));
        }

        public int hash(final char[] data, final int start, final int end) {
            if (data == null || start >= end)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = start; i < end && i < len; i++) {
                result += (a ^= 0x85157AF5 * data[i]) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final char[] data, final int start, final int end, final int step) {
            if (data == null || start >= end || step <= 0)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = start; i < end && i < len; i += step) {
                result += (a ^= 0x85157AF5 * data[i]) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final CharSequence data) {
            if (data == null)
                return 0;
            final int len = data.length();
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * data.charAt(i)) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final char[][] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data[i])) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final long[][] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data[i])) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final CharSequence[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data[i])) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final Iterable<? extends CharSequence> data) {

            if (data == null)
                return 0;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (CharSequence datum : data) {
                result += (a ^= 0x85157AF5 * hash(datum)) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final CharSequence[]... data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data[i])) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final Object[] data) {
            if (data == null)
                return 0;
            final int len = data.length;
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            Object o;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * ((o = data[i]) == null ? -1 : o.hashCode())) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }
        public int hash(final List<? extends CharSequence> data) {
            if (data == null)
                return 0;
            final int len = data.size();
            int result = 0x9E3779B9 + $i2, a = 0x632BE5AB;
            for (int i = 0; i < len; i++) {
                result += (a ^= 0x85157AF5 * hash(data.get(i))) ^ $i2 * a + $i1;
            }
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

        public int hash(final Object data) {
            if (data == null)
                return 0;
            int a = 0x632BE5AB ^ 0x85157AF5 * data.hashCode(), result = 0x9E3779B9 + $i2 + (a ^ $i2 * a + $i1);
            return result * (a * $i1 | 1) ^ (result >>> 11 | result << 21);
        }

    }
}
