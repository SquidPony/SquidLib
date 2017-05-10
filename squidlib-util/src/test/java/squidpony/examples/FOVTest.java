package squidpony.examples;

import squidpony.ArrayTools;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.SerpentMapGenerator;
import squidpony.squidmath.*;

import java.util.ArrayList;

/**
 * Created by Tommy Ettinger on 5/13/2016.
 */
public class FOVTest {
    public static int width = 100, height = 100, depth = 16;

    public static void main(String[] args) {
        StatefulRNG rng = new StatefulRNG(0xCAFEBA77L);
        DungeonGenerator dungeonGenerator = new DungeonGenerator(width, height, rng);

        dungeonGenerator.addDoors(15, true);
        dungeonGenerator.addWater(15);
        dungeonGenerator.addGrass(5);
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
        OrderedSet<Coord> points = new OrderedSet<>(20);
        double[][] losMap = ArrayTools.copy(fov.calculateLOSMap(resMap, pt.x, pt.y));
        LFSR tumbler = new LFSR(0x123456789ABCDEF0L);
        for (int i = 0; i < 20; i++) {
            points.add(pt = floors.atFraction(tumbler.nextDouble()));
            double rad = rng.between(5.0, 12.0);
            fovMaps.add(ArrayTools.copy(fov.calculateFOV(resMap, pt.x, pt.y, rad, Radius.CIRCLE)));
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
}
