package squidpony.examples;

import org.junit.Ignore;
import org.junit.Test;
import squidpony.ArrayTools;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.SerpentMapGenerator;
import squidpony.squidmath.Coord;
import squidpony.squidmath.GreasedRegion;
import squidpony.squidmath.OrderedSet;
import squidpony.squidmath.StatefulRNG;

import java.util.ArrayList;

/**
 * Created by Tommy Ettinger on 5/13/2016.
 */
public class FOVTest {
    public static int width = 40, height = 40;

    @Test
    @Ignore
    public void testAngles() {
        StatefulRNG rng = new StatefulRNG(0xCAFEBA77L);
        DungeonGenerator dungeonGenerator = new DungeonGenerator(width, height, rng);
//        dungeonGenerator.addDoors(15, true);
//        dungeonGenerator.addWater(15);
//        dungeonGenerator.addGrass(5);
        dungeonGenerator.addBoulders(5);
        SerpentMapGenerator organic = new SerpentMapGenerator(width, height, rng, 0.2);
        organic.putWalledBoxRoomCarvers(4);
        organic.putCaveCarvers(3);
        dungeonGenerator.generate(organic.generate());
        char[][] dungeon = dungeonGenerator.getDungeon();
        char[][] deco = DungeonUtility.doubleWidth(DungeonUtility.hashesToLines(dungeon, true));
        //char[][] bare = dungeonGenerator.getBareDungeon();
        dungeon[dungeonGenerator.stairsUp.x][dungeonGenerator.stairsUp.y] = '<';
        dungeon[dungeonGenerator.stairsDown.x][dungeonGenerator.stairsDown.y] = '>';

        double[][] resMap = DungeonUtility.generateResistances(dungeon);
        FOV fov = new FOV(FOV.SHADOW);
        ArrayList<double[][]> fovMaps = new ArrayList<>(8);
        GreasedRegion floors = new GreasedRegion(dungeon, '.');
        Coord pt = floors.singleRandom(rng);
        Coord start = pt;
        OrderedSet<Coord> points = new OrderedSet<>(floors.quasiRandomSeparated(0.1, 20));
        double[][] losMap = ArrayTools.copy(fov.calculateLOSMap(resMap, pt.x, pt.y));
        for (int i = 0; i < 20; i++) {
            pt = points.getAt(i);
            fovMaps.add(FOV.reuseFOV(resMap, new double[width][height], pt.x, pt.y, 6, Radius.CIRCLE, i * 40, 60.0));
//            fovMaps.add(FOV.reuseFOVSymmetrical(resMap, new double[width][height], pt.x, pt.y, 6, Radius.CIRCLE));
//            fovMaps.add(FOV.bouncingLine(resMap, new double[width][height], pt.x, pt.y, rng.between(25.0, 40.0),
//                    rng.nextDouble(360)));
        }
        double[][] result = FOV.mixVisibleFOVs(losMap, fovMaps);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if(dungeon[x][y] == '#')
                {
                    System.out.print(deco[x * 2][y]);
                    System.out.print(deco[x * 2 + 1][y]);
                }
                else if (start.x == x && start.y == y)
                {
                    System.out.print("!!");
                }
                else if(points.contains(Coord.get(x, y)))
                {
                    System.out.print("**");
                }
                else
                {
                    System.out.print(' ');
                    System.out.print(Math.round(result[x][y] * 9.4999));
                }
            }
            System.out.println();
        }
        System.out.println("\n");

        result = FOV.addFOVs(fovMaps);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if(dungeon[x][y] == '#')
                {
                    System.out.print(deco[x * 2][y]);
                    System.out.print(deco[x * 2 + 1][y]);
                }
                else if (start.x == x && start.y == y)
                {
                    System.out.print("!!");
                }
                else if(points.contains(Coord.get(x, y)))
                {
                    System.out.print("**");
                }
                else
                {
                    System.out.print(' ');
                    System.out.print(Math.round(result[x][y] * 9.4999));
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    @Test
    @Ignore
    public void testSymmetry() {
        StatefulRNG rng = new StatefulRNG(0xCAFEBA77L);
        DungeonGenerator dungeonGenerator = new DungeonGenerator(width, height, rng);
        dungeonGenerator.addBoulders(5);
        SerpentMapGenerator organic = new SerpentMapGenerator(width, height, rng, 0.2);
        organic.putWalledBoxRoomCarvers(4);
        organic.putCaveCarvers(3);
        dungeonGenerator.generate(organic.generate());
        char[][] dungeon = dungeonGenerator.getDungeon();
        char[][] deco = DungeonUtility.doubleWidth(DungeonUtility.hashesToLines(dungeon, true));
        double[][] resMap = DungeonUtility.generateResistances(dungeon);
        GreasedRegion gr = new GreasedRegion(dungeon, '.');
        System.out.println(gr.toString());
        Coord[] floors = gr.asCoords();
        double[][][] allVision = new double[floors.length][width][height];
        for (int i = 0; i < floors.length; i++) {
            Coord c = floors[i];
            FOV.reuseFOVSymmetrical(resMap, allVision[i], c.x, c.y, 6, Radius.CIRCLE);
        }
        int bad = 0, total = 0;
        for (int i = 0; i < floors.length; i++) {
            Coord outer = floors[i];
            for (int j = 0; j < floors.length; j++) {
                Coord inner = floors[j];
                if((allVision[i][inner.x][inner.y] == 0.0) != (allVision[j][outer.x][outer.y] == 0.0))
                {
                    bad++;
                }
                total++;
            }
        }
        System.out.println("Bad matchups? " + bad + "/" + total + " matchups were bad.");

        int i = 42, j = i + 3;
        Coord outer = floors[i], inner = floors[j];
        System.out.println("From position " + outer + " which holds a " + dungeon[outer.x][outer.y] +
                ", looking at the point " + inner + " gives " + allVision[i][inner.x][inner.y]);
        System.out.println("From position " + inner + " which holds a " + dungeon[inner.x][inner.y] +
                ", looking at the point " + outer + " gives " + allVision[j][outer.x][outer.y]);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if(dungeon[x][y] == '#')
                {
                    System.out.print("# ");
                }
                else if (outer.x == x && outer.y == y)
                {
                    System.out.print("!!");
                }
                else if (inner.x == x && inner.y == y)
                {
                    System.out.print("**");
                }
                else
                {                     
                    System.out.print(Math.round(allVision[i][x][y] * 9.4999));
                    System.out.print(Math.round(allVision[j][x][y] * 9.4999));
                }
            }
            System.out.println();
        }

        System.out.println();
    }
}
