package squidpony.squidgrid.mapping.styled;

import squidpony.tileset.*;

/**
 * An enumeration of all the kinds of dungeon that DungeonBoneGen
 * knows how to draw already. Each value has a Javadoc description.
 * Created by Tommy Ettinger on 3/10/2015.
 */
public enum TilesetType {
    /**
     * A generally useful kind of dungeon for ruins or underground manufactured areas.
     */
    DEFAULT_DUNGEON,
    /**
     * A good general kind of cave, with long distances between merging paths.
     */
    CAVES_LIMIT_CONNECTIVITY,
    /**
     * Only usable if using Chebyshev distances; many connections are diagonal-only.
     */
    CAVES_TINY_CORRIDORS,
    /**
     * Most parts of the cave are large and open, but tiny corridors link them, providing hiding places.
     */
    CORNER_CAVES,
    /**
     * Very basic demo dungeon.
     */
    HORIZONTAL_CORRIDORS_A,
    /**
     * Slightly less basic demo dungeon.
     */
    HORIZONTAL_CORRIDORS_B,
    /**
     * A bit more complexity in this demo dungeon.
     */
    HORIZONTAL_CORRIDORS_C,
    /**
     * A reference implementation of where you can place walls; mostly floor.
     */
    LIMIT_CONNECTIVITY_FAT,
    /**
     * A reference implementation of where you can place walls; mostly wall.
     */
    LIMITED_CONNECTIVITY,
    /**
     * A generally good maze; MAZE_A and MAZE_B should both be interchangeable, but not on the same map.
     */
    MAZE_A,
    /**
     * A generally good maze; MAZE_A and MAZE_B should both be interchangeable, but not on the same map.
     */
    MAZE_B,
    /**
     * A map that's less dungeon-like than the others, with lots of open space.
     */
    OPEN_AREAS,
    /**
     * An excellent natural cave style that looks worn down haphazardly, as by burrowing creatures or deep rivers.
     */
    REFERENCE_CAVES,
    /**
     * Mostly open, kinda weird.
     */
    ROOMS_AND_CORRIDORS_A,
    /**
     * Mostly open, but with long corridors that should be a good fit for ranged combat.
     */
    ROOMS_AND_CORRIDORS_B,
    /**
     * A nice old-school roguelike map, with thin corridors and rectangular rooms.
     */
    ROOMS_LIMIT_CONNECTIVITY,
    /**
     * A thing of beauty. Good for maps that need to seem otherworldly or unusual, like heavenly planes.
     */
    ROUND_ROOMS_DIAGONAL_CORRIDORS,
    /**
     * A more open cave, but portions of this may seem artificial. Consider alternating with REFERENCE_CAVES .
     */
    SIMPLE_CAVES,
    /**
     * Kinda... not the best map. Very predictable.
     */
    SQUARE_ROOMS_WITH_RANDOM_RECTS;

	/**
	 * @return The {@link Tileset} corresponding to this type.
	 */
	public Tileset getTileset() {
		switch (this) {
		case CAVES_LIMIT_CONNECTIVITY:
			return CavesLimitConnectivity.INSTANCE;
		case CAVES_TINY_CORRIDORS:
			return CavesTinyCorridors.INSTANCE;
		case CORNER_CAVES:
			return CornerCaves.INSTANCE;
		case DEFAULT_DUNGEON:
			return DefaultDungeon.INSTANCE;
		case HORIZONTAL_CORRIDORS_A:
			return HorizontalCorridorsV1.INSTANCE;
		case HORIZONTAL_CORRIDORS_B:
			return HorizontalCorridorsV2.INSTANCE;
		case HORIZONTAL_CORRIDORS_C:
			return HorizontalCorridorsV3.INSTANCE;
		case LIMITED_CONNECTIVITY:
			return LimitedConnectivity.INSTANCE;
		case LIMIT_CONNECTIVITY_FAT:
			return LimitConnectivityFat.INSTANCE;
		case MAZE_A:
			return Maze2Wide.INSTANCE;
		case MAZE_B:
			return MazePlus2Wide.INSTANCE;
		case OPEN_AREAS:
			return OpenAreas.INSTANCE;
		case REFERENCE_CAVES:
			return OpenAreas.INSTANCE;
		case ROOMS_AND_CORRIDORS_A:
			return RoomsAndCorridors.INSTANCE;
		case ROOMS_AND_CORRIDORS_B:
			return RoomsAndCorridors2WideDiagonalBias.INSTANCE;
		case ROOMS_LIMIT_CONNECTIVITY:
			return RoomsLimitConnectivity.INSTANCE;
		case ROUND_ROOMS_DIAGONAL_CORRIDORS:
			return RoundRoomsDiagonalCorridors.INSTANCE;
		case SIMPLE_CAVES:
			return SimpleCaves2Wide.INSTANCE;
		case SQUARE_ROOMS_WITH_RANDOM_RECTS:
			return SquareRoomsWithRandomRects.INSTANCE;
		}
		throw new IllegalStateException("Unmatched: " + this);
	}

    /**
     * Returns MixedGenerator.CAVE_FLOOR (which is 3) or MixedGenerator.ROOM_FLOOR (which is 1) based on whether this
     * TilesetType predominantly generates caves or rooms. This is relevant for certain feature placement.
     * @return 3 if this produces mostly caves, or 1 if it produces mostly rooms
     */
	public int environment() {
        int caves = 3, rooms = 1;
        switch (this) {
            case CAVES_LIMIT_CONNECTIVITY:
                return caves;
            case CAVES_TINY_CORRIDORS:
                return caves;
            case CORNER_CAVES:
                return caves;
            case DEFAULT_DUNGEON:
                return rooms;
            case HORIZONTAL_CORRIDORS_A:
                return rooms;
            case HORIZONTAL_CORRIDORS_B:
                return rooms;
            case HORIZONTAL_CORRIDORS_C:
                return rooms;
            case LIMITED_CONNECTIVITY:
                return rooms;
            case LIMIT_CONNECTIVITY_FAT:
                return rooms;
            case MAZE_A:
                return rooms;
            case MAZE_B:
                return rooms;
            case OPEN_AREAS:
                return caves;
            case REFERENCE_CAVES:
                return caves;
            case ROOMS_AND_CORRIDORS_A:
                return rooms;
            case ROOMS_AND_CORRIDORS_B:
                return rooms;
            case ROOMS_LIMIT_CONNECTIVITY:
                return rooms;
            case ROUND_ROOMS_DIAGONAL_CORRIDORS:
                return rooms;
            case SIMPLE_CAVES:
                return caves;
            case SQUARE_ROOMS_WITH_RANDOM_RECTS:
                return rooms;
        }
        throw new IllegalStateException("Unmatched: " + this);


    }
}

