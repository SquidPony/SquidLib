package squidpony.squidgrid.mapping;

import squidpony.annotation.Beta;
import squidpony.squidgrid.Direction;
import squidpony.squidmath.*;
import squidpony.squidmath.Noise.Noise3D;
import squidpony.squidmath.Noise.Noise4D;

/**
 * Can be used to generate world maps with a wide variety of data, starting with height, temperature and moisture.
 * From there, you can determine biome information in as much detail as your game needs, with a default implementation
 * available that assigns a single biome to each cell based on heat/moisture. The maps this produces are valid
 * for spherical or toroidal world projections, and will wrap from edge to opposite edge seamlessly thanks to a
 * technique from the Accidental Noise Library ( https://www.gamedev.net/blog/33/entry-2138456-seamless-noise/ ) that
 * involves getting a 2D slice of 4D Simplex noise. Because of how Simplex noise works, this also allows extremely high
 * zoom levels as long as certain parameters are within reason. You can access the height map with the
 * {@link #heightData} field, the heat map with the {@link #heatData} field, the moisture map with the
 * {@link #moistureData} field, and a special map that stores ints representing the codes for various ranges of
 * elevation (0 to 8 inclusive, with 0 the deepest ocean and 8 the highest mountains) with {@link #heightCodeData}. The
 * last map should be noted as being the simplest way to find what is land and what is water; any height code 4 or
 * greater is land, and any height code 3 or less is water. This can produce rivers, and keeps that information in a
 * GreasedRegion (alongside a GreasedRegion containing lake positions) instead of in the other map data. This class does
 * not use Coord at all, but if you want maps with width and/or height greater than 256, and you want to use the river
 * or lake data as a Collection of Coord, then you should call {@link squidpony.squidmath.Coord#expandPoolTo(int, int)}
 * with your width and height so the Coords remain safely pooled. If you're fine with keeping rivers and lakes as
 * GreasedRegions and not requesting Coord values from them, then you don't need to do anything with Coord. Certain
 * parts of this class are not necessary to generate, just in case you want river-less maps or something similar;
 * setting {@link #generateRivers} to false will disable river generation (it defaults to true).
 * <br>
 * The main trade-off this makes to obtain better quality is reduced speed; generating a 512x512 map on a circa-2016
 * laptop (i7-6700HQ processor at 2.6 GHz) takes about 1 second (about 1.15 seconds for an un-zoomed map, 0.95 or so
 * seconds to increase zoom at double resolution). If you don't need a 512x512 map, this takes commensurately less time
 * to generate less grid cells, with 64x64 maps generating faster than they can be accurately seen on the same hardware.
 * River positions are produced using a different method, and do not involve the Simplex noise parts other than using
 * the height map to determine flow. Zooming with rivers is tricky, and generally requires starting from the outermost
 * zoom level and progressively enlarging and adding detail to all rivers as zoom increases on specified points.
 */
@Beta
public abstract class WorldMapGenerator {
    public final int width, height;
    public long seed, cachedState;
    public StatefulRNG rng;
    public boolean generateRivers = true;
    public final double[][] heightData, heatData, moistureData;
    public final GreasedRegion landData, riverData, lakeData,
            partialRiverData, partialLakeData;
    protected final GreasedRegion workingData;
    public final int[][] heightCodeData;
    public double waterModifier = 0.0, coolingModifier = 1.0,
            minHeight = Double.POSITIVE_INFINITY, maxHeight = Double.NEGATIVE_INFINITY,
            minHeightActual = Double.POSITIVE_INFINITY, maxHeightActual = Double.NEGATIVE_INFINITY,
            minHeat = Double.POSITIVE_INFINITY, maxHeat = Double.NEGATIVE_INFINITY,
            minWet = Double.POSITIVE_INFINITY, maxWet = Double.NEGATIVE_INFINITY;
    public int zoom = 0;
    protected IntVLA startCacheX = new IntVLA(8), startCacheY = new IntVLA(8);
    public static final double
            deepWaterLower = -1.0, deepWaterUpper = -0.7,        // 0
            mediumWaterLower = -0.7, mediumWaterUpper = -0.3,    // 1
            shallowWaterLower = -0.3, shallowWaterUpper = -0.1,  // 2
            coastalWaterLower = -0.1, coastalWaterUpper = 0.1,   // 3
            sandLower = 0.1, sandUpper = 0.18,                   // 4
            grassLower = 0.18, grassUpper = 0.35,                // 5
            forestLower = 0.35, forestUpper = 0.6,               // 6
            rockLower = 0.6, rockUpper = 0.8,                    // 7
            snowLower = 0.8, snowUpper = 1.0;                    // 8

    public static final double[] lowers = {deepWaterLower, mediumWaterLower, shallowWaterLower, coastalWaterLower,
            sandLower, grassLower, forestLower, rockLower, snowLower};

    /**
     * Constructs a WorldMapGenerator (this class is abstract, so you should typically call this from a subclass or as
     * part of an anonymous class that implements {@link #regenerate(int, int, int, int, double, double, long)}).
     * Always makes a 256x256 map. If you were using {@link WorldMapGenerator#WorldMapGenerator(long, int, int)}, then
     * this would be the same as passing the parameters {@code 0x1337BABE1337D00DL, 256, 256}.
     */
    public WorldMapGenerator()
    {
        this(0x1337BABE1337D00DL, 256, 256);
    }
    /**
     * Constructs a WorldMapGenerator (this class is abstract, so you should typically call this from a subclass or as
     * part of an anonymous class that implements {@link #regenerate(int, int, int, int, double, double, long)}).
     * Takes only the width/height of the map. The initial seed is set to the same large long
     * every time, and it's likely that you would set the seed when you call {@link #generate(long)}. The width and
     * height of the map cannot be changed after the fact, but you can zoom in.
     *
     * @param mapWidth the width of the map(s) to generate; cannot be changed later
     * @param mapHeight the height of the map(s) to generate; cannot be changed later
     */
    public WorldMapGenerator(int mapWidth, int mapHeight)
    {
        this(0x1337BABE1337D00DL, mapWidth, mapHeight);
    }
    /**
     * Constructs a WorldMapGenerator (this class is abstract, so you should typically call this from a subclass or as
     * part of an anonymous class that implements {@link #regenerate(int, int, int, int, double, double, long)}).
     * Takes an initial seed and the width/height of the map. The {@code initialSeed}
     * parameter may or may not be used, since you can specify the seed to use when you call {@link #generate(long)}.
     * The width and height of the map cannot be changed after the fact, but you can zoom in.
     *
     * @param initialSeed the seed for the StatefulRNG this uses; this may also be set per-call to generate
     * @param mapWidth the width of the map(s) to generate; cannot be changed later
     * @param mapHeight the height of the map(s) to generate; cannot be changed later
     */
    public WorldMapGenerator(long initialSeed, int mapWidth, int mapHeight)
    {
        width = mapWidth;
        height = mapHeight;
        seed = initialSeed;
        cachedState = ~initialSeed;
        rng = new StatefulRNG(initialSeed);
        heightData = new double[width][height];
        heatData = new double[width][height];
        moistureData = new double[width][height];
        landData = new GreasedRegion(width, height);
        riverData = new GreasedRegion(width, height);
        lakeData = new GreasedRegion(width, height);
        partialRiverData = new GreasedRegion(width, height);
        partialLakeData = new GreasedRegion(width, height);
        workingData = new GreasedRegion(width, height);
        heightCodeData = new int[width][height];
    }

    /**
     * Generates a world using a random RNG state and all parameters randomized.
     * The worlds this produces will always have width and height as specified in the constructor (default 256x256).
     * You can call {@link #zoomIn(int, int, int)} to double the resolution and center on the specified area, but the width
     * and height of the 2D arrays this changed, such as {@link #heightData} and {@link #moistureData} will be the same.
     */
    public void generate()
    {
        generate(rng.nextLong());
    }

    /**
     * Generates a world using the specified RNG state as a long. Other parameters will be randomized, using the same
     * RNG state to start with.
     * The worlds this produces will always have width and height as specified in the constructor (default 256x256).
     * You can call {@link #zoomIn(int, int, int)} to double the resolution and center on the specified area, but the width
     * and height of the 2D arrays this changed, such as {@link #heightData} and {@link #moistureData} will be the same.
     * @param state the state to give this generator's RNG; if the same as the last call, this will reuse data
     */
    public void generate(long state) {
        generate(-1.0, -1.0, state);
    }

    /**
     * Generates a world using the specified RNG state as a long, with specific water and cooling modifiers that affect
     * the land-water ratio and the average temperature, respectively.
     * The worlds this produces will always have width and height as specified in the constructor (default 256x256).
     * You can call {@link #zoomIn(int, int, int)} to double the resolution and center on the specified area, but the width
     * and height of the 2D arrays this changed, such as {@link #heightData} and {@link #moistureData} will be the same.
     * @param waterMod should be between 0.85 and 1.2; a random value will be used if this is negative
     * @param coolMod should be between 0.85 and 1.4; a random value will be used if this is negative
     * @param state the state to give this generator's RNG; if the same as the last call, this will reuse data
     */
    public void generate(double waterMod, double coolMod, long state)
    {
        if(cachedState != state || waterMod != waterModifier || coolMod != coolingModifier)
        {
            seed = state;
            zoom = 0;
            startCacheX.clear();
            startCacheY.clear();
            startCacheX.add(0);
            startCacheY.add(0);

        }
        regenerate(startCacheX.peek(), startCacheY.peek(),
                (width >> zoom), (height >> zoom), waterMod, coolMod, state);
    }

    /**
     * Halves the resolution of the map and doubles the area it covers; the 2D arrays this uses keep their sizes. This
     * version of zoomOut always zooms out from the center of the currently used area.
     * <br>
     * Only has an effect if you have previously zoomed in using {@link #zoomIn(int, int, int)} or its overload.
     */
    public void zoomOut()
    {
        zoomOut(1, width >> 1, height >> 1);
    }
    /**
     * Halves the resolution of the map and doubles the area it covers repeatedly, halving {@code zoomAmount} times; the
     * 2D arrays this uses keep their sizes. This version of zoomOut allows you to specify where the zoom should be
     * centered, using the current coordinates (if the map size is 256x256, then coordinates should be between 0 and
     * 255, and will refer to the currently used area and not necessarily the full world size).
     * <br>
     * Only has an effect if you have previously zoomed in using {@link #zoomIn(int, int, int)} or its overload.
     * @param zoomCenterX the center X position to zoom out from; if too close to an edge, this will stop moving before it would extend past an edge
     * @param zoomCenterY the center Y position to zoom out from; if too close to an edge, this will stop moving before it would extend past an edge
     */
    public void zoomOut(int zoomAmount, int zoomCenterX, int zoomCenterY)
    {
        if(zoomAmount == 0) return;
        if(zoomAmount < 0) {
            zoomIn(-zoomAmount, zoomCenterX, zoomCenterY);
            return;
        }
        if(zoom > 0)
        {
            if(seed != cachedState)
            {
                generate(rng.nextLong());
            }

            zoom -= zoomAmount;
            startCacheX.pop();
            startCacheY.pop();
            startCacheX.add(Math.min(Math.max(startCacheX.pop() + (zoomCenterX >> zoom + 1) - (width >> zoom + 2),
                    0), width - (width >> zoom)));
            startCacheY.add(Math.min(Math.max(startCacheY.pop() + (zoomCenterY >> zoom + 1) - (height >> zoom + 2),
                    0), height - (height >> zoom)));
            regenerate(startCacheX.peek(), startCacheY.peek(),width >> zoom, height >> zoom,
                    waterModifier, coolingModifier, cachedState);
            rng.setState(cachedState);
        }

    }
    /**
     * Doubles the resolution of the map and halves the area it covers; the 2D arrays this uses keep their sizes. This
     * version of zoomIn always zooms in to the center of the currently used area.
     * <br>
     * Although there is no technical restriction on maximum zoom, zooming in more than 5 times (64x scale or greater)
     * will make the map appear somewhat less realistic due to rounded shapes appearing more bubble-like and less like a
     * normal landscape.
     */
    public void zoomIn()
    {
        zoomIn(1, width >> 1, height >> 1);
    }
    /**
     * Doubles the resolution of the map and halves the area it covers repeatedly, doubling {@code zoomAmount} times;
     * the 2D arrays this uses keep their sizes. This version of zoomIn allows you to specify where the zoom should be
     * centered, using the current coordinates (if the map size is 256x256, then coordinates should be between 0 and
     * 255, and will refer to the currently used area and not necessarily the full world size).
     * <br>
     * Although there is no technical restriction on maximum zoom, zooming in more than 5 times (64x scale or greater)
     * will make the map appear somewhat less realistic due to rounded shapes appearing more bubble-like and less like a
     * normal landscape.
     * @param zoomCenterX the center X position to zoom in to; if too close to an edge, this will stop moving before it would extend past an edge
     * @param zoomCenterY the center Y position to zoom in to; if too close to an edge, this will stop moving before it would extend past an edge
     */
    public void zoomIn(int zoomAmount, int zoomCenterX, int zoomCenterY)
    {
        if(zoomAmount == 0) return;
        if(zoomAmount < 0)
        {
            zoomOut(-zoomAmount, zoomCenterX, zoomCenterY);
            return;
        }
        if(seed != cachedState)
        {
            generate(rng.nextLong());
        }
        zoom += zoomAmount;
        if(startCacheX.isEmpty())
        {
            startCacheX.add(0);
            startCacheY.add(0);
        }
        else {
            startCacheX.add(Math.min(Math.max(startCacheX.peek() + (zoomCenterX >> zoom - 1) - (width >> zoom + 1),
                    0), width - (width >> zoom)));
            startCacheY.add(Math.min(Math.max(startCacheY.peek() + (zoomCenterY >> zoom - 1) - (height >> zoom + 1),
                    0), height - (height >> zoom)));
        }
        regenerate(startCacheX.peek(), startCacheY.peek(),width >> zoom, height >> zoom,
                waterModifier, coolingModifier, cachedState);
        rng.setState(cachedState);
    }

    protected abstract void regenerate(int startX, int startY, int usedWidth, int usedHeight,
                              double waterMod, double coolMod, long state);

    public int codeHeight(final double high)
    {
        if(high < deepWaterUpper)
            return 0;
        if(high < mediumWaterUpper)
            return 1;
        if(high < shallowWaterUpper)
            return 2;
        if(high < coastalWaterUpper)
            return 3;
        if(high < sandUpper)
            return 4;
        if(high < grassUpper)
            return 5;
        if(high < forestUpper)
            return 6;
        if(high < rockUpper)
            return 7;
        return 8;
    }
    protected final int decodeX(final int coded)
    {
        return coded % width;
    }
    protected final int decodeY(final int coded)
    {
        return coded / width;
    }
    protected int wrapX(final int x)  {
        return (x + width) % width;
    }
    protected int wrapY(final int y)  {
        return (y + height) % height;
    }
    private static final Direction[] reuse = new Direction[6];
    private void appendDirToShuffle(RNG rng) {
        rng.randomPortion(Direction.CARDINALS, reuse);
        reuse[rng.next(2)] = Direction.DIAGONALS[rng.next(2)];
        reuse[4] = Direction.DIAGONALS[rng.next(2)];
        reuse[5] = Direction.OUTWARDS[rng.next(3)];
    }

    protected void addRivers()
    {
        landData.refill(heightCodeData, 4, 999);
        long rebuildState = rng.nextLong();
        //workingData.allOn();
                //.empty().insertRectangle(8, 8, width - 16, height - 16);
        riverData.empty().refill(heightCodeData, 6, 100);
        riverData.quasiRandomRegion(0.0066);
        int[] starts = riverData.asTightEncoded();
        int len = starts.length, currentPos, choice, adjX, adjY, currX, currY, tcx, tcy, stx, sty, sbx, sby;
        riverData.clear();
        lakeData.clear();
        PER_RIVER:
        for (int i = 0; i < len; i++) {
            workingData.clear();
            currentPos = starts[i];
            stx = tcx = currX = decodeX(currentPos);
            sty = tcy = currY = decodeY(currentPos);
            while (true) {

                double best = 999999;
                choice = -1;
                appendDirToShuffle(rng);

                for (int d = 0; d < 5; d++) {
                    adjX = wrapX(currX + reuse[d].deltaX);
                    /*
                    if (adjX < 0 || adjX >= width)
                    {
                        if(rng.next(4) == 0)
                            riverData.or(workingData);
                        continue PER_RIVER;
                    }*/
                    adjY = wrapY(currY + reuse[d].deltaY);
                    if (heightData[adjX][adjY] < best && !workingData.contains(adjX, adjY)) {
                        best = heightData[adjX][adjY];
                        choice = d;
                        tcx = adjX;
                        tcy = adjY;
                    }
                }
                currX = tcx;
                currY = tcy;
                if (best >= heightData[stx][sty]) {
                    tcx = rng.next(2);
                    adjX = wrapX(currX + ((tcx & 1) << 1) - 1);
                    adjY = wrapY(currY + (tcx & 2) - 1);
                    lakeData.insert(currX, currY);
                    lakeData.insert(wrapX(currX+1), currY);
                    lakeData.insert(wrapX(currX-1), currY);
                    lakeData.insert(currX, wrapY(currY+1));
                    lakeData.insert(currX, wrapY(currY-1));

                    if(heightCodeData[adjX][adjY] <= 3) {
                        riverData.or(workingData);
                        continue PER_RIVER;
                    }
                    else if((heightData[adjX][adjY] -= 0.0002) < 0.0) {
                        if (rng.next(3) == 0)
                            riverData.or(workingData);
                        continue PER_RIVER;
                    }
                    tcx = rng.next(2);
                    adjX = wrapX(currX + ((tcx & 1) << 1) - 1);
                    adjY = wrapY(currY + (tcx & 2) - 1);
                    if(heightCodeData[adjX][adjY] <= 3) {
                        riverData.or(workingData);
                        continue PER_RIVER;
                    }
                    else if((heightData[adjX][adjY] -= 0.0002) < 0.0) {
                        if (rng.next(3) == 0)
                            riverData.or(workingData);
                        continue PER_RIVER;
                    }
                }
                if(choice != -1 && reuse[choice].isDiagonal())
                {
                    tcx = wrapX(currX - reuse[choice].deltaX);
                    tcy = wrapY(currY - reuse[choice].deltaY);
                    if(heightData[tcx][currY] <= heightData[currX][tcy] && !workingData.contains(tcx, currY))
                    {
                        if(heightCodeData[tcx][currY] < 3 || riverData.contains(tcx, currY))
                        {
                            riverData.or(workingData);
                            continue PER_RIVER;
                        }
                        workingData.insert(tcx, currY);
                    }
                    else if(!workingData.contains(currX, tcy))
                    {
                        if(heightCodeData[currX][tcy] < 3 || riverData.contains(currX, tcy))
                        {
                            riverData.or(workingData);
                            continue PER_RIVER;
                        }
                        workingData.insert(currX, tcy);

                    }
                }
                if(heightCodeData[currX][currY] < 3 || riverData.contains(currX, currY))
                {
                    riverData.or(workingData);
                    continue PER_RIVER;
                }
                workingData.insert(currX, currY);
            }
        }

        GreasedRegion tempData = new GreasedRegion(width, height);
        int riverCount = riverData.size() >> 4, currentMax = riverCount >> 3, idx = 0, prevChoice;
        for (int h = 5; h < 9; h++) { //, currentMax += riverCount / 18
            workingData.empty().refill(heightCodeData, h).and(riverData);
            RIVER:
            for (int j = 0; j < currentMax && idx < riverCount; j++) {
                double vdc = VanDerCorputQRNG.weakDetermine(idx++), best = -999999;
                currentPos = workingData.atFractionTight(vdc);
                if(currentPos < 0)
                    break;
                stx = sbx = tcx = currX = decodeX(currentPos);
                sty = sby = tcy = currY = decodeY(currentPos);
                appendDirToShuffle(rng);
                choice = -1;
                prevChoice = -1;
                for (int d = 0; d < 5; d++) {
                    adjX = wrapX(currX + reuse[d].deltaX);
                    adjY = wrapY(currY + reuse[d].deltaY);
                    if (heightData[adjX][adjY] > best) {
                        best = heightData[adjX][adjY];
                        prevChoice = choice;
                        choice = d;
                        sbx = tcx;
                        sby = tcy;
                        tcx = adjX;
                        tcy = adjY;
                    }
                }
                currX = sbx;
                currY = sby;
                if (prevChoice != -1 && heightCodeData[currX][currY] >= 4) {
                    if (reuse[prevChoice].isDiagonal()) {
                        tcx = wrapX(currX - reuse[prevChoice].deltaX);
                        tcy = wrapY(currY - reuse[prevChoice].deltaY);
                        if (heightData[tcx][currY] <= heightData[currX][tcy]) {
                            if(heightCodeData[tcx][currY] < 3)
                            {
                                riverData.or(tempData);
                                continue;
                            }
                            tempData.insert(tcx, currY);
                        }
                        else
                        {
                            if(heightCodeData[currX][tcy] < 3)
                            {
                                riverData.or(tempData);
                                continue;
                            }
                            tempData.insert(currX, tcy);
                        }
                    }
                    if(heightCodeData[currX][currY] < 3)
                    {
                        riverData.or(tempData);
                        continue;
                    }
                    tempData.insert(currX, currY);
                }

                while (true) {
                    best = -999999;
                    appendDirToShuffle(rng);
                    choice = -1;
                    for (int d = 0; d < 6; d++) {
                        adjX = wrapX(currX + reuse[d].deltaX);
                        adjY = wrapY(currY + reuse[d].deltaY);
                        if (heightData[adjX][adjY] > best && !riverData.contains(adjX, adjY)) {
                            best = heightData[adjX][adjY];
                            choice = d;
                            sbx = adjX;
                            sby = adjY;
                        }
                    }
                    currX = sbx;
                    currY = sby;
                    if (choice != -1) {
                        if (reuse[choice].isDiagonal()) {
                            tcx = wrapX(currX - reuse[choice].deltaX);
                            tcy = wrapY(currY - reuse[choice].deltaY);
                            if (heightData[tcx][currY] <= heightData[currX][tcy]) {
                                if(heightCodeData[tcx][currY] < 3)
                                {
                                    riverData.or(tempData);
                                    continue RIVER;
                                }
                                tempData.insert(tcx, currY);
                            }
                            else
                            {
                                if(heightCodeData[currX][tcy] < 3)
                                {
                                    riverData.or(tempData);
                                    continue RIVER;
                                }
                                tempData.insert(currX, tcy);
                            }
                        }
                        if(heightCodeData[currX][currY] < 3)
                        {
                            riverData.or(tempData);
                            continue RIVER;
                        }
                        tempData.insert(currX, currY);
                    }
                    else
                    {
                        riverData.or(tempData);
                        tempData.clear();
                        continue RIVER;
                    }
                    if (best <= heightData[stx][sty] || heightData[currX][currY] > rng.nextDouble(280.0)) {
                        riverData.or(tempData);
                        tempData.clear();
                        if(heightCodeData[currX][currY] < 3)
                            continue RIVER;
                        lakeData.insert(currX, currY);
                        sbx = rng.next(8);
                        sbx &= sbx >>> 4;
                        if ((sbx & 1) == 0)
                            lakeData.insert(wrapX(currX + 1), currY);
                        if ((sbx & 2) == 0)
                            lakeData.insert(wrapX(currX - 1), currY);
                        if ((sbx & 4) == 0)
                            lakeData.insert(currX, wrapY(currY + 1));
                        if ((sbx & 8) == 0)
                            lakeData.insert(currX, wrapY(currY - 1));
                        sbx = rng.next(2);
                        lakeData.insert(wrapX(currX + (-(sbx & 1) | 1)), wrapY(currY + ((sbx & 2) - 1))); // random diagonal
                        lakeData.insert(currX, wrapY(currY + ((sbx & 2) - 1))); // ortho next to random diagonal
                        lakeData.insert(wrapX(currX + (-(sbx & 1) | 1)), currY); // ortho next to random diagonal

                        continue RIVER;
                    }
                }
            }

        }

        rng.setState(rebuildState);
    }

    /**
     * A way to get biome information for the cells on a map when you only need a single value to describe a biome, such
     * as "Grassland" or "TropicalRainforest".
     * <br>
     * To use: 1, Construct a SimpleBiomeMapper (constructor takes no arguments). 2, call
     * {@link #makeBiomes(WorldMapGenerator)} with a WorldMapGenerator that has already produced at least one world map.
     * 3, get biome codes from the {@link #biomeCodeData} field, where a code is an int that can be used as an index
     * into the {@link #biomeTable} static field to get a String name for a biome type, or used with an alternate biome
     * table of your design. Biome tables in this case are 54-element arrays organized into groups of 6 elements. Each
     * group goes from the coldest temperature first to the warmest temperature last in the group. The first group of 6
     * contains the dryest biomes, the next 6 are medium-dry, the next are slightly-dry, the next slightly-wet, then
     * medium-wet, then wettest. After this first block of dry-to-wet groups, there is a group of 6 for coastlines, a
     * group of 6 for rivers, and lastly a group for lakes. This also assigns moisture codes and heat codes from 0 to 5
     * for each cell, which may be useful to simplify logic that deals with those factors.
     */
    public static class SimpleBiomeMapper
    {
        /**
         * The heat codes for the analyzed map, from 0 to 5 inclusive, with 0 coldest and 5 hottest.
         */
        public int[][] heatCodeData,
        /**
         * The moisture codes for the analyzed map, from 0 to 5 inclusive, with 0 driest and 5 wettest.
         */
        moistureCodeData,
        /**
         * The biome codes for the analyzed map, from 0 to 53 inclusive. You can use {@link #biomeTable} to look up
         * String names for biomes, or construct your own table as you see fit (see docs in {@link SimpleBiomeMapper}).
         */
        biomeCodeData;

        public static final double
                coldestValueLower = 0.0,   coldestValueUpper = 0.15, // 0
                colderValueLower = 0.15,   colderValueUpper = 0.31,  // 1
                coldValueLower = 0.31,     coldValueUpper = 0.5,     // 2
                warmValueLower = 0.5,      warmValueUpper = 0.69,    // 3
                warmerValueLower = 0.69,   warmerValueUpper = 0.85,  // 4
                warmestValueLower = 0.85,  warmestValueUpper = 1.0,  // 5
        
                driestValueLower = 0.0,    driestValueUpper  = 0.27, // 0
                drierValueLower = 0.27,    drierValueUpper   = 0.4,  // 1
                dryValueLower = 0.4,       dryValueUpper     = 0.6,  // 2
                wetValueLower = 0.6,       wetValueUpper     = 0.8,  // 3
                wetterValueLower = 0.8,    wetterValueUpper  = 0.9,  // 4
                wettestValueLower = 0.9,   wettestValueUpper = 1.0;  // 5

        /**
         * The default biome table to use with biome codes from {@link #biomeCodeData}. Biomes are assigned based on
         * heat and moisture for the first 36 of 54 elements (coldest to warmest for each group of 6, with the first
         * group as the dryest and the last group the wettest), then the next 6 are for coastlines (coldest to warmest),
         * then rivers (coldest to warmest), then lakes (coldest to warmest).
         */
        public static final String[] biomeTable = {
                //COLDEST //COLDER        //COLD            //HOT                  //HOTTER              //HOTTEST
                "Ice",    "Ice",          "Grassland",      "Desert",              "Desert",             "Desert",             //DRYEST
                "Ice",    "Tundra",       "Grassland",      "Grassland",           "Desert",             "Desert",             //DRYER
                "Ice",    "Tundra",       "Woodland",       "Woodland",            "Savanna",            "Desert",             //DRY
                "Ice",    "Tundra",       "SeasonalForest", "SeasonalForest",      "Savanna",            "Savanna",            //WET
                "Ice",    "Tundra",       "BorealForest",   "TemperateRainforest", "TropicalRainforest", "Savanna",            //WETTER
                "Ice",    "BorealForest", "BorealForest",   "TemperateRainforest", "TropicalRainforest", "TropicalRainforest", //WETTEST
                "Rocky",  "Rocky",        "Beach",          "Beach",               "Beach",              "Beach",              //COASTS
                "Ice",    "River",        "River",          "River",               "River",              "River",              //RIVERS
                "Ice",    "River",        "River",          "River",               "River",              "River",              //LAKES
        };

        /**
         * Simple constructor; pretty much does nothing. Make sure to call {@link #makeBiomes(WorldMapGenerator)} before
         * using fields like {@link #biomeCodeData}.
         */
        public SimpleBiomeMapper()
        {
            heatCodeData = null;
            moistureCodeData = null;
            biomeCodeData = null;
        }

        /**
         * Analyzes the last world produced by the given WorldMapGenerator and uses all of its generated information to
         * assign biome codes for each cell (along with heat and moisture codes). After calling this, biome codes can be
         * taken from {@link #biomeCodeData} and used as indices into {@link #biomeTable} or a custom biome table.
         * @param world a WorldMapGenerator that should have generated at least one map; it may be at any zoom
         */
        public void makeBiomes(WorldMapGenerator world) {
            if(world == null || world.width <= 0 || world.height <= 0)
                return;
            if(heatCodeData == null || (heatCodeData.length != world.width || heatCodeData[0].length != world.height))
                heatCodeData = new int[world.width][world.height];
            if(moistureCodeData == null || (moistureCodeData.length != world.width || moistureCodeData[0].length != world.height))
                moistureCodeData = new int[world.width][world.height];
            if(biomeCodeData == null || (biomeCodeData.length != world.width || biomeCodeData[0].length != world.height))
                biomeCodeData = new int[world.width][world.height];
            final double i_hot = (world.maxHeat == world.minHeat) ? 1.0 : 1.0 / (world.maxHeat - world.minHeat);
            for (int x = 0; x < world.width; x++) {
                for (int y = 0; y < world.height; y++) {
                    final double hot = (world.heatData[x][y] - world.minHeat) * i_hot, moist = world.moistureData[x][y];
                    final int heightCode = world.heightCodeData[x][y];
                    int hc, mc;
                    boolean isLake = world.generateRivers && world.partialLakeData.contains(x, y) && heightCode >= 4,
                            isRiver = world.generateRivers && world.partialRiverData.contains(x, y) && heightCode >= 4;
                    if (moist > wetterValueUpper) {
                        mc = 5;
                    } else if (moist > wetValueUpper) {
                        mc = 4;
                    } else if (moist > dryValueUpper) {
                        mc = 3;
                    } else if (moist > drierValueUpper) {
                        mc = 2;
                    } else if (moist > driestValueUpper) {
                        mc = 1;
                    } else {
                        mc = 0;
                    }

                    if (hot > warmerValueUpper) {
                        hc = 5;
                    } else if (hot > warmValueUpper) {
                        hc = 4;
                    } else if (hot > coldValueUpper) {
                        hc = 3;
                    } else if (hot > colderValueUpper) {
                        hc = 2;
                    } else if (hot > coldestValueUpper) {
                        hc = 1;
                    } else {
                        hc = 0;
                    }

                    heatCodeData[x][y] = hc;
                    moistureCodeData[x][y] = mc;
                    biomeCodeData[x][y] = isLake ? hc + 48 : (isRiver ? hc + 42 : ((heightCode == 4) ? hc + 36 : hc + mc * 6));
                }
            }
        }
    }

    /**
     * A concrete implementation of {@link WorldMapGenerator} that tiles both east-to-west and north-to-south. It tends
     * to not appear distorted like {@link SphereMap} does in some areas, even though this is inaccurate for a
     * rectangular projection of a spherical world (that inaccuracy is likely what players expect in a map, though).
     * <a href="http://squidpony.github.io/SquidLib/DetailedWorldMapRiverDemo.png" >Example map</a>.
     */
    public static class TilingMap extends WorldMapGenerator {
        protected static final double terrainFreq = 1.75, terrainRidgedFreq = 1.1, heatFreq = 5.05, moistureFreq = 5.2, otherFreq = 5.5;
        private double minHeat0 = Double.POSITIVE_INFINITY, maxHeat0 = Double.NEGATIVE_INFINITY,
                minHeat1 = Double.POSITIVE_INFINITY, maxHeat1 = Double.NEGATIVE_INFINITY,
                minWet0 = Double.POSITIVE_INFINITY, maxWet0 = Double.NEGATIVE_INFINITY;

        public final Noise4D terrain, terrainRidged, heat, moisture, otherRidged;

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used as a tiling, wrapping east-to-west as well
         * as north-to-south. Always makes a 256x256 map.
         * Uses SeededNoise as its noise generator, with 1.0 as the octave multiplier affecting detail.
         * If you were using {@link squidpony.squidgrid.mapping.WorldMapGenerator.TilingMap#TilingMap(long, int, int, squidpony.squidmath.Noise.Noise4D, double)}, then this would be the
         * same as passing the parameters {@code 0x1337BABE1337D00DL, 256, 256, SeededNoise.instance, 1.0}.
         */
        public TilingMap() {
            this(0x1337BABE1337D00DL, 256, 256, SeededNoise.instance, 1.0);
        }

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used as a tiling, wrapping east-to-west as well
         * as north-to-south.
         * Takes only the width/height of the map. The initial seed is set to the same large long
         * every time, and it's likely that you would set the seed when you call {@link #generate(long)}. The width and
         * height of the map cannot be changed after the fact, but you can zoom in.
         * Uses SeededNoise as its noise generator, with 1.0 as the octave multiplier affecting detail.
         *
         * @param mapWidth  the width of the map(s) to generate; cannot be changed later
         * @param mapHeight the height of the map(s) to generate; cannot be changed later
         */
        public TilingMap(int mapWidth, int mapHeight) {
            this(0x1337BABE1337D00DL, mapWidth, mapHeight, SeededNoise.instance, 1.0);
        }

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used as a tiling, wrapping east-to-west as well
         * as north-to-south.
         * Takes an initial seed and the width/height of the map. The {@code initialSeed}
         * parameter may or may not be used, since you can specify the seed to use when you call {@link #generate(long)}.
         * The width and height of the map cannot be changed after the fact, but you can zoom in.
         * Uses SeededNoise as its noise generator, with 1.0 as the octave multiplier affecting detail.
         *
         * @param initialSeed the seed for the StatefulRNG this uses; this may also be set per-call to generate
         * @param mapWidth    the width of the map(s) to generate; cannot be changed later
         * @param mapHeight   the height of the map(s) to generate; cannot be changed later
         */
        public TilingMap(long initialSeed, int mapWidth, int mapHeight) {
            this(initialSeed, mapWidth, mapHeight, SeededNoise.instance, 1.0);
        }

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used as a tiling, wrapping east-to-west as well
         * as north-to-south. Takes an initial seed, the width/height of the map, and a noise generator (a
         * {@link Noise4D} implementation, which is usually {@link SeededNoise#instance}. The {@code initialSeed}
         * parameter may or may not be used, since you can specify the seed to use when you call
         * {@link #generate(long)}. The width and height of the map cannot be changed after the fact, but you can zoom
         * in. Currently only SeededNoise makes sense to use as the value for {@code noiseGenerator}, and the seed it's
         * constructed with doesn't matter because it will change the seed several times at different scales of noise
         * (it's fine to use the static {@link SeededNoise#instance} because it has no changing state between runs of
         * the program; it's effectively a constant). The detail level, which is the {@code octaveMultiplier} parameter
         * that can be passed to another constructor, is always 1.0 with this constructor.
         *
         * @param initialSeed      the seed for the StatefulRNG this uses; this may also be set per-call to generate
         * @param mapWidth         the width of the map(s) to generate; cannot be changed later
         * @param mapHeight        the height of the map(s) to generate; cannot be changed later
         * @param noiseGenerator   an instance of a noise generator capable of 4D noise, almost always {@link SeededNoise}
         */
        public TilingMap(long initialSeed, int mapWidth, int mapHeight, final Noise4D noiseGenerator) {
            this(initialSeed, mapWidth, mapHeight, noiseGenerator, 1.0);
        }

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used as a tiling, wrapping east-to-west as well
         * as north-to-south. Takes an initial seed, the width/height of the map, and parameters for noise
         * generation (a {@link Noise4D} implementation, which is usually {@link SeededNoise#instance}, and a
         * multiplier on how many octaves of noise to use, with 1.0 being normal (high) detail and higher multipliers
         * producing even more detailed noise when zoomed-in). The {@code initialSeed} parameter may or may not be used,
         * since you can specify the seed to use when you call {@link #generate(long)}. The width and height of the map
         * cannot be changed after the fact, but you can zoom in. Currently only SeededNoise makes sense to use as the
         * value for {@code noiseGenerator}, and the seed it's constructed with doesn't matter because it will change the
         * seed several times at different scales of noise (it's fine to use the static {@link SeededNoise#instance} because
         * it has no changing state between runs of the program; it's effectively a constant). The {@code octaveMultiplier}
         * parameter should probably be no lower than 0.5, but can be arbitrarily high if you're willing to spend much more
         * time on generating detail only noticeable at very high zoom; normally 1.0 is fine and may even be too high for
         * maps that don't require zooming.
         * @param initialSeed the seed for the StatefulRNG this uses; this may also be set per-call to generate
         * @param mapWidth the width of the map(s) to generate; cannot be changed later
         * @param mapHeight the height of the map(s) to generate; cannot be changed later
         * @param noiseGenerator an instance of a noise generator capable of 4D noise, almost always {@link SeededNoise}
         * @param octaveMultiplier used to adjust the level of detail, with 0.5 at the bare-minimum detail and 1.0 normal
         */
        public TilingMap(long initialSeed, int mapWidth, int mapHeight, final Noise4D noiseGenerator, double octaveMultiplier) {
            super(initialSeed, mapWidth, mapHeight);
            terrain = new Noise.Layered4D(noiseGenerator, (int) (0.5 + octaveMultiplier * 8), terrainFreq);
            terrainRidged = new Noise.Ridged4D(noiseGenerator, (int) (0.5 + octaveMultiplier * 10), terrainRidgedFreq);
            heat = new Noise.Layered4D(noiseGenerator, (int) (0.5 + octaveMultiplier * 3), heatFreq);
            moisture = new Noise.Layered4D(noiseGenerator, (int) (0.5 + octaveMultiplier * 4), moistureFreq);
            otherRidged = new Noise.Ridged4D(noiseGenerator, (int) (0.5 + octaveMultiplier * 6), otherFreq);
        }

        protected void regenerate(int startX, int startY, int usedWidth, int usedHeight,
                                  double waterMod, double coolMod, long state)
        {
            boolean fresh = false;
            if(cachedState != state || waterMod != waterModifier || coolMod != coolingModifier)
            {
                minHeight = Double.POSITIVE_INFINITY;
                maxHeight = Double.NEGATIVE_INFINITY;
                minHeat0 = Double.POSITIVE_INFINITY;
                maxHeat0 = Double.NEGATIVE_INFINITY;
                minHeat1 = Double.POSITIVE_INFINITY;
                maxHeat1 = Double.NEGATIVE_INFINITY;
                minHeat = Double.POSITIVE_INFINITY;
                maxHeat = Double.NEGATIVE_INFINITY;
                minWet0 = Double.POSITIVE_INFINITY;
                maxWet0 = Double.NEGATIVE_INFINITY;
                minWet = Double.POSITIVE_INFINITY;
                maxWet = Double.NEGATIVE_INFINITY;
                cachedState = state;
                fresh = true;
            }
            rng.setState(state);
            int seedA = rng.nextInt(), seedB = rng.nextInt(), seedC = rng.nextInt(), t;

            waterModifier = (waterMod <= 0) ? rng.nextDouble(0.25) + 0.89 : waterMod;
            coolingModifier = (coolMod <= 0) ? rng.nextDouble(0.45) * (rng.nextDouble()-0.5) + 1.1 : coolMod;

            double p, q,
                    ps, pc,
                    qs, qc,
                    h, temp,
                    i_w = 6.283185307179586 / width, i_h = 6.283185307179586 / height,
                    xPos = startX, yPos = startY, i_uw = usedWidth / (double)width, i_uh = usedHeight / (double)height;
            double[] trigTable = new double[width << 1];
            for (int x = 0; x < width; x++, xPos += i_uw) {
                p = xPos * i_w;
                trigTable[x<<1]   = Math.sin(p);
                trigTable[x<<1|1] = Math.cos(p);
            }
            for (int y = 0; y < height; y++, yPos += i_uh) {
                q = yPos * i_h;
                qs = Math.sin(q);
                qc = Math.cos(q);
                for (int x = 0, xt = 0; x < width; x++) {
                    ps = trigTable[xt++];//Math.sin(p);
                    pc = trigTable[xt++];//Math.cos(p);
                    h = terrain.getNoiseWithSeed(pc +
                                    terrainRidged.getNoiseWithSeed(pc, ps, qc, qs, seedA + seedB),
                            ps, qc, qs, seedA);
                    h *= waterModifier;
                    heightData[x][y] = h;
                    heatData[x][y] = (p = heat.getNoiseWithSeed(pc, ps, qc
                                    + otherRidged.getNoiseWithSeed(pc, ps, qc, qs, seedB + seedC)
                            , qs, seedB));
                    moistureData[x][y] = (temp = moisture.getNoiseWithSeed(pc, ps, qc, qs
                                    + otherRidged.getNoiseWithSeed(pc, ps, qc, qs, seedC + seedA)
                            , seedC));
                    minHeightActual = Math.min(minHeightActual, h);
                    maxHeightActual = Math.max(maxHeightActual, h);
                    if(fresh) {
                        minHeight = Math.min(minHeight, h);
                        maxHeight = Math.max(maxHeight, h);

                        minHeat0 = Math.min(minHeat0, p);
                        maxHeat0 = Math.max(maxHeat0, p);

                        minWet0 = Math.min(minWet0, temp);
                        maxWet0 = Math.max(maxWet0, temp);

                    }
                }
                minHeightActual = Math.min(minHeightActual, minHeight);
                maxHeightActual = Math.max(maxHeightActual, maxHeight);

            }
            double heightDiff = 2.0 / (maxHeightActual - minHeightActual),
                    heatDiff = 0.8 / (maxHeat0 - minHeat0),
                    wetDiff = 1.0 / (maxWet0 - minWet0),
                    hMod,
                    halfHeight = (height - 1) * 0.5, i_half = 1.0 / halfHeight;
            double minHeightActual0 = minHeightActual;
            double maxHeightActual0 = maxHeightActual;
            yPos = startY;
            ps = Double.POSITIVE_INFINITY;
            pc = Double.NEGATIVE_INFINITY;

            for (int y = 0; y < height; y++, yPos += i_uh) {
                temp = Math.abs(yPos - halfHeight) * i_half;
                temp *= (2.4 - temp);
                temp = 2.2 - temp;
                for (int x = 0; x < width; x++) {
                    heightData[x][y] = (h = (heightData[x][y] - minHeightActual) * heightDiff - 1.0);
                    minHeightActual0 = Math.min(minHeightActual0, h);
                    maxHeightActual0 = Math.max(maxHeightActual0, h);
                    heightCodeData[x][y] = (t = codeHeight(h));
                    hMod = 1.0;
                    switch (t) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                            h = 0.4;
                            hMod = 0.2;
                            break;
                        case 6:
                            h = -0.1 * (h - forestLower - 0.08);
                            break;
                        case 7:
                            h *= -0.25;
                            break;
                        case 8:
                            h *= -0.4;
                            break;
                        default:
                            h *= 0.05;
                    }
                    heatData[x][y] = (h = (((heatData[x][y] - minHeat0) * heatDiff * hMod) + h + 0.6) * temp);
                    if (fresh) {
                        ps = Math.min(ps, h); //minHeat0
                        pc = Math.max(pc, h); //maxHeat0
                    }
                }
            }
            if(fresh)
            {
                minHeat1 = ps;
                maxHeat1 = pc;
            }
            heatDiff = coolingModifier / (maxHeat1 - minHeat1);
            qs = Double.POSITIVE_INFINITY;
            qc = Double.NEGATIVE_INFINITY;
            ps = Double.POSITIVE_INFINITY;
            pc = Double.NEGATIVE_INFINITY;


            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    heatData[x][y] = (h = ((heatData[x][y] - minHeat1) * heatDiff));
                    moistureData[x][y] = (temp = (moistureData[x][y] - minWet0) * wetDiff);
                    if (fresh) {
                        qs = Math.min(qs, h);
                        qc = Math.max(qc, h);
                        ps = Math.min(ps, temp);
                        pc = Math.max(pc, temp);
                    }
                }
            }
            if(fresh)
            {
                minHeat = qs;
                maxHeat = qc;
                minWet = ps;
                maxWet = pc;
            }
            landData.refill(heightCodeData, 4, 999);
            if(generateRivers) {
                if (fresh) {
                    addRivers();
                    riverData.connect8way().thin().thin();
                    lakeData.connect8way().thin();
                    partialRiverData.remake(riverData);
                    partialLakeData.remake(lakeData);
                } else {
                    partialRiverData.remake(riverData);
                    partialLakeData.remake(lakeData);
                    for (int i = 1; i <= zoom; i++) {
                        int stx = (startCacheX.get(i) - startCacheX.get(i - 1)) << (i - 1),
                                sty = (startCacheY.get(i) - startCacheY.get(i - 1)) << (i - 1);
                        if ((i & 3) == 3) {
                            partialRiverData.zoom(stx, sty).connect8way();
                            partialRiverData.or(workingData.remake(partialRiverData).fringe().quasiRandomRegion(0.4));
                            partialLakeData.zoom(stx, sty).connect8way();
                            partialLakeData.or(workingData.remake(partialLakeData).fringe().quasiRandomRegion(0.55));
                        } else {
                            partialRiverData.zoom(stx, sty).connect8way().thin();
                            partialRiverData.or(workingData.remake(partialRiverData).fringe().quasiRandomRegion(0.5));
                            partialLakeData.zoom(stx, sty).connect8way().thin();
                            partialLakeData.or(workingData.remake(partialLakeData).fringe().quasiRandomRegion(0.7));
                        }
                    }
                }
            }
        }
    }

    /**
     * A concrete implementation of {@link WorldMapGenerator} that distorts the map as it nears the poles, expanding the
     * smaller-diameter latitude lines in extreme north and south regions so they take up the same space as the equator.
     * This is ideal for projecting onto a 3D sphere, which could squash the poles to counteract the stretch this does.
     * You might also want to produce an oval map that more-accurately represents the changes in the diameter of a
     * latitude line on a spherical world; this could be done by using one of the maps this class makes and removing a
     * portion of each non-equator row, arranging the removal so if the map is n units wide at the equator, the height
     * should be n divided by {@link Math#PI}, and progressively more cells are removed from rows as you move away from
     * the equator (down to empty space or 1 cell left at the poles).
     * <a href="http://i.imgur.com/wth01QD.png" >Example map, showing distortion</a>
     */
    public static class SphereMap extends WorldMapGenerator {
        protected static final double terrainFreq = 2.5, terrainRidgedFreq = 1.3, heatFreq = 5.05, moistureFreq = 5.2, otherFreq = 5.5;
        private double minHeat0 = Double.POSITIVE_INFINITY, maxHeat0 = Double.NEGATIVE_INFINITY,
                minHeat1 = Double.POSITIVE_INFINITY, maxHeat1 = Double.NEGATIVE_INFINITY,
                minWet0 = Double.POSITIVE_INFINITY, maxWet0 = Double.NEGATIVE_INFINITY;

        public final Noise3D terrain, terrainRidged, heat, moisture, otherRidged;

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used to wrap a sphere (as with a texture on a
         * 3D model), with seamless east-west wrapping, no north-south wrapping, and distortion that causes the poles to
         * have significantly-exaggerated-in-size features while the equator is not distorted.
         * Always makes a 256x256 map.
         * Uses SeededNoise as its noise generator, with 1.0 as the octave multiplier affecting detail.
         * If you were using {@link WorldMapGenerator.SphereMap#SphereMap(long, int, int, Noise3D, double)}, then this would be the
         * same as passing the parameters {@code 0x1337BABE1337D00DL, 256, 256, SeededNoise.instance, 1.0}.
         */
        public SphereMap() {
            this(0x1337BABE1337D00DL, 256, 256, SeededNoise.instance, 1.0);
        }

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used to wrap a sphere (as with a texture on a
         * 3D model), with seamless east-west wrapping, no north-south wrapping, and distortion that causes the poles to
         * have significantly-exaggerated-in-size features while the equator is not distorted.
         * Takes only the width/height of the map. The initial seed is set to the same large long
         * every time, and it's likely that you would set the seed when you call {@link #generate(long)}. The width and
         * height of the map cannot be changed after the fact, but you can zoom in.
         * Uses SeededNoise as its noise generator, with 1.0 as the octave multiplier affecting detail.
         *
         * @param mapWidth  the width of the map(s) to generate; cannot be changed later
         * @param mapHeight the height of the map(s) to generate; cannot be changed later
         */
        public SphereMap(int mapWidth, int mapHeight) {
            this(0x1337BABE1337D00DL, mapWidth, mapHeight, SeededNoise.instance, 1.0);
        }

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used to wrap a sphere (as with a texture on a
         * 3D model), with seamless east-west wrapping, no north-south wrapping, and distortion that causes the poles to
         * have significantly-exaggerated-in-size features while the equator is not distorted.
         * Takes an initial seed and the width/height of the map. The {@code initialSeed}
         * parameter may or may not be used, since you can specify the seed to use when you call {@link #generate(long)}.
         * The width and height of the map cannot be changed after the fact, but you can zoom in.
         * Uses SeededNoise as its noise generator, with 1.0 as the octave multiplier affecting detail.
         *
         * @param initialSeed the seed for the StatefulRNG this uses; this may also be set per-call to generate
         * @param mapWidth    the width of the map(s) to generate; cannot be changed later
         * @param mapHeight   the height of the map(s) to generate; cannot be changed later
         */
        public SphereMap(long initialSeed, int mapWidth, int mapHeight) {
            this(initialSeed, mapWidth, mapHeight, SeededNoise.instance, 1.0);
        }

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used to wrap a sphere (as with a texture on a
         * 3D model), with seamless east-west wrapping, no north-south wrapping, and distortion that causes the poles to
         * have significantly-exaggerated-in-size features while the equator is not distorted.
         * Takes an initial seed, the width/height of the map, and a noise generator (a
         * {@link Noise3D} implementation, which is usually {@link SeededNoise#instance}. The {@code initialSeed}
         * parameter may or may not be used, since you can specify the seed to use when you call
         * {@link #generate(long)}. The width and height of the map cannot be changed after the fact, but you can zoom
         * in. Currently only SeededNoise makes sense to use as the value for {@code noiseGenerator}, and the seed it's
         * constructed with doesn't matter because it will change the seed several times at different scales of noise
         * (it's fine to use the static {@link SeededNoise#instance} because it has no changing state between runs of
         * the program; it's effectively a constant). The detail level, which is the {@code octaveMultiplier} parameter
         * that can be passed to another constructor, is always 1.0 with this constructor.
         *
         * @param initialSeed      the seed for the StatefulRNG this uses; this may also be set per-call to generate
         * @param mapWidth         the width of the map(s) to generate; cannot be changed later
         * @param mapHeight        the height of the map(s) to generate; cannot be changed later
         * @param noiseGenerator   an instance of a noise generator capable of 3D noise, almost always {@link SeededNoise}
         */
        public SphereMap(long initialSeed, int mapWidth, int mapHeight, final Noise3D noiseGenerator) {
            this(initialSeed, mapWidth, mapHeight, noiseGenerator, 1.0);
        }

        /**
         * Constructs a concrete WorldMapGenerator for a map that can be used to wrap a sphere (as with a texture on a
         * 3D model), with seamless east-west wrapping, no north-south wrapping, and distortion that causes the poles to
         * have significantly-exaggerated-in-size features while the equator is not distorted.
         * Takes an initial seed, the width/height of the map, and parameters for noise
         * generation (a {@link Noise3D} implementation, which is usually {@link SeededNoise#instance}, and a
         * multiplier on how many octaves of noise to use, with 1.0 being normal (high) detail and higher multipliers
         * producing even more detailed noise when zoomed-in). The {@code initialSeed} parameter may or may not be used,
         * since you can specify the seed to use when you call {@link #generate(long)}. The width and height of the map
         * cannot be changed after the fact, but you can zoom in. Currently only SeededNoise makes sense to use as the
         * value for {@code noiseGenerator}, and the seed it's constructed with doesn't matter because it will change the
         * seed several times at different scales of noise (it's fine to use the static {@link SeededNoise#instance} because
         * it has no changing state between runs of the program; it's effectively a constant). The {@code octaveMultiplier}
         * parameter should probably be no lower than 0.5, but can be arbitrarily high if you're willing to spend much more
         * time on generating detail only noticeable at very high zoom; normally 1.0 is fine and may even be too high for
         * maps that don't require zooming.
         * @param initialSeed the seed for the StatefulRNG this uses; this may also be set per-call to generate
         * @param mapWidth the width of the map(s) to generate; cannot be changed later
         * @param mapHeight the height of the map(s) to generate; cannot be changed later
         * @param noiseGenerator an instance of a noise generator capable of 3D noise, almost always {@link SeededNoise}
         * @param octaveMultiplier used to adjust the level of detail, with 0.5 at the bare-minimum detail and 1.0 normal
         */
        public SphereMap(long initialSeed, int mapWidth, int mapHeight, final Noise3D noiseGenerator, double octaveMultiplier) {
            super(initialSeed, mapWidth, mapHeight);
            terrain = new Noise.Layered3D(noiseGenerator, (int) (0.5 + octaveMultiplier * 8), terrainFreq);
            terrainRidged = new Noise.Ridged3D(noiseGenerator, (int) (0.5 + octaveMultiplier * 10), terrainRidgedFreq);
            heat = new Noise.Layered3D(noiseGenerator, (int) (0.5 + octaveMultiplier * 3), heatFreq);
            moisture = new Noise.Layered3D(noiseGenerator, (int) (0.5 + octaveMultiplier * 4), moistureFreq);
            otherRidged = new Noise.Ridged3D(noiseGenerator, (int) (0.5 + octaveMultiplier * 6), otherFreq);
        }
        protected int wrapY(final int y)  {
            return Math.max(0, Math.min(y, height - 1));
        }

        protected void regenerate(int startX, int startY, int usedWidth, int usedHeight,
                                  double waterMod, double coolMod, long state)
        {
            boolean fresh = false;
            if(cachedState != state || waterMod != waterModifier || coolMod != coolingModifier)
            {
                minHeight = Double.POSITIVE_INFINITY;
                maxHeight = Double.NEGATIVE_INFINITY;
                minHeat0 = Double.POSITIVE_INFINITY;
                maxHeat0 = Double.NEGATIVE_INFINITY;
                minHeat1 = Double.POSITIVE_INFINITY;
                maxHeat1 = Double.NEGATIVE_INFINITY;
                minHeat = Double.POSITIVE_INFINITY;
                maxHeat = Double.NEGATIVE_INFINITY;
                minWet0 = Double.POSITIVE_INFINITY;
                maxWet0 = Double.NEGATIVE_INFINITY;
                minWet = Double.POSITIVE_INFINITY;
                maxWet = Double.NEGATIVE_INFINITY;
                cachedState = state;
                fresh = true;
            }
            rng.setState(state);
            int seedA = rng.nextInt(), seedB = rng.nextInt(), seedC = rng.nextInt(), t;

            waterModifier = (waterMod <= 0) ? rng.nextDouble(0.25) + 0.89 : waterMod;
            coolingModifier = (coolMod <= 0) ? rng.nextDouble(0.45) * (rng.nextDouble()-0.5) + 1.1 : coolMod;

            double p, q,
                    ps, pc,
                    qs, qc,
                    h, temp,
                    i_w = 6.283185307179586 / width, i_h = (3.141592653589793) / (height+2.0),
                    xPos = startX, yPos, i_uw = usedWidth / (double)width, i_uh = usedHeight / (height+2.0);
            final double[] trigTable = new double[width << 1];
            for (int x = 0; x < width; x++, xPos += i_uw) {
                p = xPos * i_w;
                trigTable[x<<1]   = Math.sin(p);
                trigTable[x<<1|1] = Math.cos(p);
            }
            yPos = startY + i_uh;
            for (int y = 0; y < height; y++, yPos += i_uh) {
                qs = -1.5707963267948966 + yPos * i_h;
                qc = Math.cos(qs);
                qs = Math.sin(qs);
                //qs = Math.sin(qs);
                for (int x = 0, xt = 0; x < width; x++) {
                    ps = trigTable[xt++] * qc;//Math.sin(p);
                    pc = trigTable[xt++] * qc;//Math.cos(p);
                    h = terrain.getNoiseWithSeed(pc +
                                    terrainRidged.getNoiseWithSeed(pc, ps, qs,seedA + seedB),
                            ps, qs, seedA);
                    h *= waterModifier;
                    heightData[x][y] = h;
                    heatData[x][y] = (p = heat.getNoiseWithSeed(pc, ps
                                    + otherRidged.getNoiseWithSeed(pc, ps, qs,seedB + seedC)
                            , qs, seedB));
                    moistureData[x][y] = (temp = moisture.getNoiseWithSeed(pc, ps, qs
                            + otherRidged.getNoiseWithSeed(pc, ps, qs, seedC + seedA)
                            , seedC));
                    minHeightActual = Math.min(minHeightActual, h);
                    maxHeightActual = Math.max(maxHeightActual, h);
                    if(fresh) {
                        minHeight = Math.min(minHeight, h);
                        maxHeight = Math.max(maxHeight, h);

                        minHeat0 = Math.min(minHeat0, p);
                        maxHeat0 = Math.max(maxHeat0, p);

                        minWet0 = Math.min(minWet0, temp);
                        maxWet0 = Math.max(maxWet0, temp);
                    }
                }
                minHeightActual = Math.min(minHeightActual, minHeight);
                maxHeightActual = Math.max(maxHeightActual, maxHeight);

            }
            double heightDiff = 2.0 / (maxHeightActual - minHeightActual),
                    heatDiff = 0.8 / (maxHeat0 - minHeat0),
                    wetDiff = 1.0 / (maxWet0 - minWet0),
                    hMod,
                    halfHeight = (height - 1) * 0.5, i_half = 1.0 / halfHeight;
            double minHeightActual0 = minHeightActual;
            double maxHeightActual0 = maxHeightActual;
            yPos = startY + i_uh;
            ps = Double.POSITIVE_INFINITY;
            pc = Double.NEGATIVE_INFINITY;

            for (int y = 0; y < height; y++, yPos += i_uh) {
                temp = Math.abs(yPos - halfHeight) * i_half;
                temp *= (2.4 - temp);
                temp = 2.2 - temp;
                for (int x = 0; x < width; x++) {
                    heightData[x][y] = (h = (heightData[x][y] - minHeightActual) * heightDiff - 1.0);
                    minHeightActual0 = Math.min(minHeightActual0, h);
                    maxHeightActual0 = Math.max(maxHeightActual0, h);
                    heightCodeData[x][y] = (t = codeHeight(h));
                    hMod = 1.0;
                    switch (t) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                            h = 0.4;
                            hMod = 0.2;
                            break;
                        case 6:
                            h = -0.1 * (h - forestLower - 0.08);
                            break;
                        case 7:
                            h *= -0.25;
                            break;
                        case 8:
                            h *= -0.4;
                            break;
                        default:
                            h *= 0.05;
                    }
                    heatData[x][y] = (h = (((heatData[x][y] - minHeat0) * heatDiff * hMod) + h + 0.6) * temp);
                    if (fresh) {
                        ps = Math.min(ps, h); //minHeat0
                        pc = Math.max(pc, h); //maxHeat0
                    }
                }
            }
            if(fresh)
            {
                minHeat1 = ps;
                maxHeat1 = pc;
            }
            heatDiff = coolingModifier / (maxHeat1 - minHeat1);
            qs = Double.POSITIVE_INFINITY;
            qc = Double.NEGATIVE_INFINITY;
            ps = Double.POSITIVE_INFINITY;
            pc = Double.NEGATIVE_INFINITY;


            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    heatData[x][y] = (h = ((heatData[x][y] - minHeat1) * heatDiff));
                    moistureData[x][y] = (temp = (moistureData[x][y] - minWet0) * wetDiff);
                    if (fresh) {
                        qs = Math.min(qs, h);
                        qc = Math.max(qc, h);
                        ps = Math.min(ps, temp);
                        pc = Math.max(pc, temp);
                    }
                }
            }
            if(fresh)
            {
                minHeat = qs;
                maxHeat = qc;
                minWet = ps;
                maxWet = pc;
            }
            landData.refill(heightCodeData, 4, 999);
            if(generateRivers) {
                if (fresh) {
                    addRivers();
                    riverData.connect8way().thin().thin();
                    lakeData.connect8way().thin();
                    partialRiverData.remake(riverData);
                    partialLakeData.remake(lakeData);
                } else {
                    partialRiverData.remake(riverData);
                    partialLakeData.remake(lakeData);
                    for (int i = 1; i <= zoom; i++) {
                        int stx = (startCacheX.get(i) - startCacheX.get(i - 1)) << (i - 1),
                                sty = (startCacheY.get(i) - startCacheY.get(i - 1)) << (i - 1);
                        if ((i & 3) == 3) {
                            partialRiverData.zoom(stx, sty).connect8way();
                            partialRiverData.or(workingData.remake(partialRiverData).fringe().quasiRandomRegion(0.4));
                            partialLakeData.zoom(stx, sty).connect8way();
                            partialLakeData.or(workingData.remake(partialLakeData).fringe().quasiRandomRegion(0.55));
                        } else {
                            partialRiverData.zoom(stx, sty).connect8way().thin();
                            partialRiverData.or(workingData.remake(partialRiverData).fringe().quasiRandomRegion(0.5));
                            partialLakeData.zoom(stx, sty).connect8way().thin();
                            partialLakeData.or(workingData.remake(partialLakeData).fringe().quasiRandomRegion(0.7));
                        }
                    }
                }
            }
        }
    }
}
