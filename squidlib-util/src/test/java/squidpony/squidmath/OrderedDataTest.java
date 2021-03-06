package squidpony.squidmath;

import org.junit.Assert;
import org.junit.Test;
import squidpony.examples.TestConfiguration;

import static squidpony.examples.TestConfiguration.PRINTING;

/**
 * Created by Tommy Ettinger on 6/19/2017.
 */
public class OrderedDataTest {
    @Test
    public void testGeneral() {
        OrderedMap<Coord, String> om = new OrderedMap<>(10);
        LightRNG rng = new LightRNG(0x89ABCDEF);
        RNG rn = new RNG(rng);
        Coord r;
        final int COUNT = 100;
        for (int i = 0; i < COUNT; i++) {
            do {
                r = rn.nextCoord(60, 60);
            }while (om.containsKey(r));
            om.put(r, i + ":" + r);
        }
        for (int i = 0; i < COUNT; i++) {
            TestConfiguration.println(om.keyAt(i) + " => " + om.getAt(i));
        }
        TestConfiguration.println("\n");

        om.putAt(om.keyAt(1), om.removeAt(1), 1);
        for (int i = 0; i < COUNT; i++) {
            TestConfiguration.println(om.keyAt(i) + " => " + om.getAt(i));
        }
        TestConfiguration.println("");

        om.putAt(om.keyAt(1), om.removeAt(1), 1);
        om.putAt(om.keyAt(2), om.removeAt(2), 2);
        for (int i = 0; i < COUNT; i++) {
            TestConfiguration.println(om.keyAt(i) + " => " + om.getAt(i));
        }
        TestConfiguration.println("");

        for (int n = 0; n < 4; n++) {
            TestConfiguration.println(om.order.size);
            for (int i = 0; i < COUNT; i++) {
                int pos = om.order.get(i);
                Assert.assertEquals("DUPLICATE! RUN " + n + ", I " + i, om.order.indexOf(pos), om.order.lastIndexOf(pos));
            }
            for (int i = 0; i < COUNT; i++) {
                r = om.keyAt(i);
                while(true)
                {
                    String s = om.alterAtCarefully(i, r.translateCapped(rn.nextIntHasty(3) - 1, rn.nextIntHasty(3) - 1, 100, 100));
                    if(s == null)
                        TestConfiguration.println("CORRECTLY BLOCKED REPLACEMENT BY DUPLICATE IN RUN " + n + " AT i " + i);
                    else
                        break;
                }
                // Try commenting out the above line and uncommenting below to see the difference.
                //om.alterAtCarefully(i, r.translateCapped(rn.nextIntHasty(3) - 1, rn.nextIntHasty(3) - 1, 100, 100));
                //om.alterAt(i, r.translateCapped(rn.nextIntHasty(3) - 1, rn.nextIntHasty(3) - 1, 100, 100));
            }
            for (int i = 0; i < om.size(); i++) {
                TestConfiguration.println(om.keyAt(i) + " => " + om.getAt(i));
            }
            TestConfiguration.println("");
        }
    }
    @Test
    public void testSmallMap()
    {
        if(!PRINTING)
            return;
        OrderedMap<Coord, Integer> small = OrderedMap.makeMap(Coord.get(28, 28), 0);
        System.out.println(small);
        small.alterAtCarefully(0, Coord.get(27, 29));
        System.out.println(small);
        small.alterAtCarefully(0, null);
        System.out.println(small);
        small.alterAtCarefully(0, Coord.get(27, 28));
        System.out.println(small);
        small.put(Coord.get(1, 1), 2);
        small.alterAtCarefully(0, Coord.get(28, 27));
        System.out.println(small);
    }
    @Test
    public void testSmallSet()
    {
        if(!PRINTING)
            return;
        OrderedSet<Coord> small = new OrderedSet<>(1);
        small.add(Coord.get(28, 28));
        System.out.println(small);
        small.alterAt(0, Coord.get(27, 29));
        System.out.println(small);
        small.alterAt(0, null);
        System.out.println(small);
        small.alterAt(0, Coord.get(27, 28));
        System.out.println(small);
        small.add(Coord.get(1, 1));
        small.alterAt(0, Coord.get(28, 27));
        System.out.println(small);
    }
}
