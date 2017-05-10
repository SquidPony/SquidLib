package squidpony.squidmath;

import org.junit.Test;
import squidpony.ArrayTools;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidgrid.mapping.DungeonUtility;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;


/**
 * Created by Tommy Ettinger on 10/1/2015.
 */
public class GreasedRegionTest {

    public static GreasedRegion dataCross = new GreasedRegion(64, 64).insertRectangle(25, 2, 14, 60).insertRectangle(2, 25, 60, 14);
    public static GreasedRegion dataCross2 = new GreasedRegion(128, 128).insertRectangle(24+32, 2 + 32, 16, 60).insertRectangle(2 + 32, 24 + 32, 60, 16);
    public static GreasedRegion box = new GreasedRegion(64, 64).insertRectangle(24, 24, 16, 16);
    public static GreasedRegion box2 = new GreasedRegion(120, 120).insertRectangle(24+32, 24+32, 16, 16);
    public static GreasedRegion box3 = new GreasedRegion(240, 240).insertRectangle(30, 30, 180, 180);
    public static StatefulRNG srng = new StatefulRNG(0x1337BEEF);
    public static DungeonGenerator dungeonGen = new DungeonGenerator(64, 64, srng);
    public static char[][] dungeon = dungeonGen.generate();
    public static GreasedRegion dataDungeon = new GreasedRegion(dungeon, '.');
    public static final char[] letters = ArrayTools.letterSpan(256);
    static {
        //printRegion(dataCross);
        //printRegion(dataCross2);
    }
    public static final boolean PRINTING = false;
    public static void print2D(int[][] data)
    {
        if(!PRINTING)
            return;
        if(data == null || data.length <= 0 || data[0] == null || data[0].length <= 0)
            System.out.println("null");
        else
        {
            int d = 0;
            for (int y = 0; y < data[0].length; y++) {
                for (int x = 0; x < data.length; x++) {
                    d = data[x][y]& 255;
                    System.out.print(letters[d]);
                }
                System.out.println();
            }
        }
    }
    @Test
    public void testBasics() {
        //printPacked(dataCross, 64, 64);
        GreasedRegion singleNegative = new GreasedRegion(dataCross).not(),
                doubleNegative = new GreasedRegion(dataCross).not().not();
        assertTrue(dataCross.equals(doubleNegative));
        GreasedRegion gr = new GreasedRegion(box2);
        printRegion(gr);
        GreasedRegion gr2 = new GreasedRegion(120, 120);
        gr2.insertRectangle(24 + 32, 24 + 32, 16, 16);
        assertTrue(gr.equals(gr2));
        if(false) {
            srng.setState(0x123456789ABCDEFL);
            printRegion(new GreasedRegion(120, 120).insertSeveral(gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng),
                    gr.singleRandom(srng), gr.singleRandom(srng)));
            System.out.println("\nSOBOL:");
            int dcs = dataCross.copy().not().size() + dataCross.size() / 20;
            printRegion(gr = dataCross.copy().not().insertSeveral(dataCross.separatedPortion(0.05)));
            System.out.println("expected size: " + (dcs) + ", actual size " + gr.size());
            System.out.println("\nVDC_2:");
            printRegion(gr2 = dataCross.copy().not().insertSeveral(dataCross.quasiRandomSeparated(0.05)));
            System.out.println("expected size: " + (dcs) + ", actual size " + gr2.size());
            System.out.println();
            printRegion(dataDungeon);
            System.out.println("\nSOBOL:");
            printRegion(gr = dataDungeon.copy().empty().insertSeveral(dataDungeon.separatedPortion(0.05)));
            System.out.println("expected size: " + (dataDungeon.size() * 3 / 50) + ", actual size " + gr.size());
            System.out.println("\nVDC_2:");
            printRegion(gr2 = dataDungeon.copy().empty().insertSeveral(dataDungeon.quasiRandomSeparated(0.05)));
            System.out.println("expected size: " + (dataDungeon.size() * 3 / 50) + ", actual size " + gr2.size());
        }
        GreasedRegion g = new GreasedRegion(box);
        GreasedRegion g2 = new GreasedRegion(64, 64);
        g2.insertRectangle(24, 24, 16, 16);
        assertTrue(g.equals(g2));
        GreasedRegion grr = new GreasedRegion(box3);
        GreasedRegion grr2 = new GreasedRegion(240, 240);
        grr2.insertRectangle(30, 30, 180, 180);
        assertTrue(grr.equals(grr2));
//        GreasedRegion gri = new GreasedRegion(grr).insertRectangle(24, 52, 16, 16);
//        GreasedRegion gri2 = new GreasedRegion(grr).insert(0, 28, box);
//        printRegion(box);
//        printRegion(gri);
//        printRegion(gri2);
//        assertTrue(gri.equals(gri2));
        GreasedRegion bigOn = new GreasedRegion(152, 152).not();
        GreasedRegion gri = new GreasedRegion(150, 150).insertRectangle(3, 4, 147, 146);
        GreasedRegion gri2 = new GreasedRegion(150, 150).insert(3, 4, bigOn);
        printRegion(box);
        printRegion(gri);
        printRegion(gri2);
        assertTrue(gri.equals(gri2));

    }

    public static int FOV_RANGE = 12;
    public static Radius RADIUS = Radius.SQUARE;

    public static void printRegion(GreasedRegion r) {
        if (PRINTING) {
            DungeonUtility.debugPrint(r.toChars());
            System.out.println();
        }
    }

    public void printBits16(int n) {
        for (int i = 0x8000; i > 0; i >>= 1)
            System.out.print((n & i) > 0 ? 1 : 0);
    }

    public void printBits32(int n) {
        for (int i = 1 << 31; i != 0; i >>>= 1)
            System.out.print((n & i) != 0 ? 1 : 0);
    }

    public long arrayMemoryUsage(int length, long bytesPerItem)
    {
        return (((bytesPerItem * length + 12 - 1) / 8) + 1) * 8L;
    }
    public long arrayMemoryUsage2D(int xSize, int ySize, long bytesPerItem)
    {
        return arrayMemoryUsage(xSize, (((bytesPerItem * ySize + 12 - 1) / 8) + 1) * 8L);
    }
    public int arrayMemoryUsageJagged(short[][] arr)
    {
        int ctr = 0;
        for (int i = 0; i < arr.length; i++) {
            ctr += arrayMemoryUsage(arr[i].length, 2);
        }
        return (((ctr + 12 - 1) / 8) + 1) * 8;
    }


    @Test
    public void testTranslate() {
        GreasedRegion crossZeroTranslated = new GreasedRegion(dataCross).translate(0, 0);
        GreasedRegion crossTranslated = new GreasedRegion(dataCross).translate(1, 1);
        GreasedRegion crossUnTranslated = new GreasedRegion(crossTranslated).translate(-1, -1);

        assertTrue(dataCross.equals(crossZeroTranslated));
        assertTrue(dataCross.equals(crossUnTranslated));

        printRegion(dataCross);
        GreasedRegion crossBox = new GreasedRegion(dataCross).translate(25, 25).translate(-50, -50);
        printRegion(crossBox);
        assertTrue(crossBox.equals(new GreasedRegion(CoordPacker.unpack(CoordPacker.rectangle(14, 14), 64, 64))));

        GreasedRegion big = new GreasedRegion(150, 150).not().translate(100, 100);
        GreasedRegion big2 = new GreasedRegion(150, 150).insertRectangle(100, 100, 50, 50);
        printRegion(big);
        printRegion(big2);
        assertTrue(big.equals(big2));
    }

    @Test
    public void testUnion() {
        GreasedRegion box = new GreasedRegion(dataCross);
        //printRegion(box);
        box.translate(25, 25);
        //printRegion(box);
        box.translate(-50, -50);
        //printRegion(box);
        box.translate(25, 25);
        GreasedRegion alter = new GreasedRegion(CoordPacker.unpack(CoordPacker.rectangle(25, 2, 14, 60), 64, 64))
                .and(new GreasedRegion(CoordPacker.unpack(CoordPacker.rectangle(2, 25, 60, 14), 64, 64)));
        //printRegion(box);
        //printRegion(alter);
        assertTrue(box.equals(alter));
        GreasedRegion minus = new GreasedRegion(dataCross).andNot(box);
        GreasedRegion xr = new GreasedRegion(CoordPacker.unpack(CoordPacker.rectangle(25, 2, 14, 60), 64, 64))
                .xor(new GreasedRegion(CoordPacker.unpack(CoordPacker.rectangle(2, 25, 60, 14), 64, 64)));
        printRegion(minus);
        printRegion(xr);
        assertTrue(minus.equals(xr));

    }
    @Test
    public void testExpanding() {

        GreasedRegion edge = new GreasedRegion(dataCross).fringe();
        printRegion(edge);
        GreasedRegion bonus = new GreasedRegion(dataCross).expand();
        printRegion(bonus);
        assertTrue(new GreasedRegion(bonus).andNot(edge).equals(dataCross));

        GreasedRegion edge2 = new GreasedRegion(dataCross2).fringe8way();
        printRegion(edge2);
        GreasedRegion bonus2 = new GreasedRegion(dataCross2).expand8way();
        printRegion(bonus2);
        assertTrue(new GreasedRegion(bonus2).andNot(edge2).equals(dataCross2));


        GreasedRegion flooded = new GreasedRegion(Coord.get(26, 2), 64, 64).flood(dataCross, 2);
        GreasedRegion manual = new GreasedRegion(64, 64, Coord.get(25, 2), Coord.get(26, 2), Coord.get(27, 2), Coord.get(28, 2),
                Coord.get(25, 3), Coord.get(26, 3), Coord.get(27, 3),
                Coord.get(26, 4));
        printRegion(flooded);
        printRegion(manual);
        assertTrue(flooded.equals(manual));

    }
    @Test
    public void testRetracting() {
        GreasedRegion surf = new GreasedRegion(dataCross).surface();
        printRegion(surf);
        GreasedRegion shrunk = new GreasedRegion(dataCross).retract();
        printRegion(shrunk);
        assertTrue(new GreasedRegion(shrunk).or(surf).equals(dataCross));

        GreasedRegion surf2 = new GreasedRegion(dataCross2).surface8way();
        printRegion(surf2);
        GreasedRegion shrunk2 = new GreasedRegion(dataCross2).retract8way();
        printRegion(shrunk2);
        assertTrue(new GreasedRegion(shrunk2).or(surf2).equals(dataCross2));
        System.out.println();
        GreasedRegion littleBox = new GreasedRegion(32, 32).insertRectangle(12, 12, 8, 8);
        printRegion(littleBox);
        System.out.println();
        ArrayList<GreasedRegion> toLimit = littleBox.fringeSeriesToLimit8way();
        for(GreasedRegion gr : toLimit)
            printRegion(gr);
    }
    @Test
    public void testFitting()
    {
        GreasedRegion wrecked = new GreasedRegion(dataCross);
        int[][] numbers = GreasedRegion.sum(new GreasedRegion(64, 64).not().retractSeries8way(30));
        print2D(numbers);
        System.out.println();
        printRegion(wrecked);
        System.out.println();
        print2D(wrecked.fit(numbers, 0));
    }
    @Test
    public void testRandom()
    {
        StatefulRNG rng = new StatefulRNG(0x1337BEEF);
        GreasedRegion wrecked = dataCross.copy().disperse();
        printRegion(wrecked);
        wrecked.disperse8way();
        printRegion(wrecked);
        wrecked.remake(dataCross).disperseRandom(rng);
        printRegion(wrecked);
    }

    @Test
    public void testCorners() {
        GreasedRegion beveled = new GreasedRegion(dataCross).removeCorners();
        printRegion(beveled);
        GreasedRegion other = new GreasedRegion(dataCross).expand().retract8way();
        printRegion(other);
        assertTrue(beveled.equals(other));
        beveled.fill(true).removeCorners();
        printRegion(beveled);
    }

    @Test
    public void testZoom() {
        GreasedRegion crossCopy = new GreasedRegion(dataCross);
        printRegion(crossCopy);
        crossCopy.zoom(13, 13);
        printRegion(crossCopy);
        GreasedRegion midCross = new GreasedRegion(71, 71).insertRectangle(27, 2, 17, 60).insertRectangle(2, 27, 60, 17);
        printRegion(midCross);
        midCross.zoom(30, 33);
        printRegion(midCross);
    }
}

