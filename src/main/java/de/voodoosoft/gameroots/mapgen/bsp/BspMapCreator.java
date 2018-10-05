package de.voodoosoft.gameroots.mapgen.bsp;


import de.voodoosoft.gameroots.shared.geom.IntPoint;
import de.voodoosoft.gameroots.shared.geom.IntRect;
import de.voodoosoft.gameroots.mapgen.CharLevelMapCreator;
import de.voodoosoft.gameroots.mapgen.TileChar;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Dungeon generator using a BSP tree.
 */
public class BspMapCreator implements CharLevelMapCreator {
	private static final Logger log = Logger.getLogger(BspMapCreator.class.getName());

	static Random rnd = new Random();
	static int cellPad = 12;
	static int roomPad = 2;
	private int mapWidth;
	private int mapHeight;
	private int maxIterations;
	private int minRoomSize;
	private Map<IntPoint, Integer> roomsByTile;
	private List<IntRect> rooms;
	private long seed;
	private PrintStream out;

	public BspMapCreator(int width, int height) {
		this();
		this.mapWidth = width;
		this.mapHeight = height;
	}

	public BspMapCreator() {
		roomsByTile = new HashMap<>();
		rooms = new ArrayList<>();
	}

	public void setOut(PrintStream out) {
		this.out = out;
	}

	public static void setCellPad(int cellPad) {
		BspMapCreator.cellPad = cellPad;
	}

	public static void setRoomPad(int roomPad) {
		BspMapCreator.roomPad = roomPad;
	}

	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public void setMinRoomSize(int minRoomSize) {
		if (minRoomSize < 5) {
			throw new RuntimeException("min room size is 5");
		}
		this.minRoomSize = minRoomSize;
	}

	public void setMapDimension(int mapWidth, int mapHeight) {
		this.mapWidth = mapWidth;
		this.mapHeight = mapHeight;
	}

	@Override
	public char[][] createMap() {
		if (seed == 0) {
			seed = new Date().getTime();
		}
		rnd = new Random(seed);

		// generate BSP tree
		CellNode root = new CellNode(0, 0, mapWidth, mapHeight);
		root.width = mapWidth;
		root.height = mapHeight;
		splitCell(root, maxIterations);
		insertRooms(root);
		connectRooms(root);

		// render map characters
		char[][] map = initMap();
		renderCorridors(map, root);
		renderRooms(map, root);
		renderWalls(map);
		renderDoors(map);

		// make sure room floor tiles are properly set
		// and collect rooms by tile
		for (int i = 0; i < rooms.size(); i++) {
			IntRect room = rooms.get(i);
			floodFill(map, room, i);
		}

		// convert empty tiles into floor
		convertVoid(map);

		if (log.isLoggable(Level.FINE)) {
			renderCellBorders(map, root);
		}

		if (out != null) {
			print(map, mapWidth, mapHeight);
		}

		return map;
	}

	@Override
	public Map<IntPoint, Integer> getRoomsByTile() {
		return roomsByTile;
	}

	private void splitCell(CellNode parent, int maxDepth) {
		if (parent.depth < maxDepth && parent.width > 2 * cellPad && parent.height > 2 * cellPad) {
			int depth = parent.depth + 1;
			parent.horizontal = rnd.nextBoolean();

			if (parent.horizontal) {
				int split = cellPad + rnd.nextInt(parent.width - (2 * cellPad));
				parent.left = new CellNode(parent.x, parent.y, split + 1, parent.height);
				parent.right = new CellNode(parent.x + split, parent.y, parent.width - split, parent.height);
			}
			else {
				int split = cellPad + rnd.nextInt(parent.height - (2 * cellPad));
				parent.left = new CellNode(parent.x, parent.y, parent.width, split + 1);
				parent.right = new CellNode(parent.x, parent.y + split, parent.width, parent.height - split);
			}

			parent.right.depth = depth;
			parent.left.depth = depth;

			splitCell(parent.left, maxDepth);
			splitCell(parent.right, maxDepth);
		}
	}

	private void insertRooms(CellNode node) {
		if (node == null) {
			return;
		}

		if (node.left == null && node.right == null) {
			int maxRoomWidth = node.width - (2 * roomPad);
			int maxRoomHeight = node.height - (2 * roomPad);
			if (maxRoomWidth >= minRoomSize && maxRoomHeight >= minRoomSize) {
				int roomWidth = minRoomSize + rnd.nextInt(maxRoomWidth - minRoomSize);
				int roomHeight = minRoomSize + rnd.nextInt(maxRoomHeight - minRoomSize);

				// let room start at cell center to make sure corridors hit
				int hx = node.x + node.width / 2;
				int hy = node.y + node.height / 2;

				int x = hx - roomWidth / 2;
				int y = hy - roomHeight / 2;
				node.room = new IntRect(x, y, roomWidth, roomHeight);
				rooms.add(node.room);
			}
		}

		insertRooms(node.left);
		insertRooms(node.right);
	}

	private void connectRooms(CellNode node) {
		if (node == null) {
			return;
		}

		if (node.left != null && node.right != null) {
			int x1 = node.left.x + node.left.width / 2;
			int y1 = node.left.y + node.left.height / 2;
			int x2 = node.right.x + node.right.width / 2;
			int y2 = node.right.y + node.right.height / 2;
			node.corridor = new IntRect(x1 - 1, y1 - 1, x2 - x1 + 2, y2 - y1 + 2);
		}

		connectRooms(node.left);
		connectRooms(node.right);
	}

	private void renderWalls(char[][] map) {
		// room walls
		for (int y = 1; y < mapHeight - 1; y++) {
			for (int x = 1; x < mapWidth - 1; x++) {
				if (map[y][x] == TileChar.charTemp) {
					if (map[y][x - 1] == TileChar.charVoid) {
						map[y][x] = TileChar.charWall;
					}
					if (map[y][x + 1] == TileChar.charVoid) {
						map[y][x] = TileChar.charWall;
					}
					if (map[y - 1][x] == TileChar.charVoid) {
						map[y][x] = TileChar.charWall;
					}
					if (map[y + 1][x] == TileChar.charVoid) {
						map[y][x] = TileChar.charWall;
					}
				}
			}
		}

		// corridor walls
		for (int y = 1; y < mapHeight - 1; y++) {
			for (int x = 1; x < mapWidth - 1; x++) {
				if (map[y][x] == TileChar.charFloor) {
					if (map[y][x - 1] == TileChar.charVoid) {
						map[y][x - 1] = TileChar.charWall;
					}
					if (map[y][x + 1] == TileChar.charVoid) {
						map[y][x + 1] = TileChar.charWall;
					}
					if (map[y - 1][x] == TileChar.charVoid) {
						map[y - 1][x] = TileChar.charWall;
					}
					if (map[y + 1][x] == TileChar.charVoid) {
						map[y + 1][x] = TileChar.charWall;
					}
				}
			}
		}
	}

	private void renderDoors(char[][] map) {
		// char sequence indicating door placement
		String roomToken = new StringBuilder().append(TileChar.charWall).append(TileChar.charTemp).append(TileChar.charTemp).append(TileChar.charWall).toString();

		for (int y = 1; y < mapHeight - 1; y++) {
			for (int x = 1; x < mapWidth - 1; x++) {
				if (isHSeq(map, x, y, roomToken)) {
					map[y][x + 1] = TileChar.charDoorV;
					map[y][x + 2] = TileChar.charDoorV;
				}
				else if (isVSeq(map, x, y, roomToken)) {
					map[y + 1][x] = TileChar.charDoorH;
					map[y + 2][x] = TileChar.charDoorH;
				}
			}
		}
	}

	private boolean isHSeq(char[][] map, int x, int y, String seq) {
		for (int i = 0; i < seq.length(); i++) {
			if (map[y][x + i] != seq.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	private boolean isVSeq(char[][] map, int x, int y, String seq) {
		for (int i = 0; i < seq.length(); i++) {
			if (map[y + i][x] != seq.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	private void floodFill(char[][] map, IntRect room, int roomNr) {
		ArrayDeque<IntPoint> todo = new ArrayDeque<>();
		todo.push(new IntPoint(room.x + 1, room.y + 1));
		while(!todo.isEmpty()) {
			IntPoint tile = todo.pop();
			if (isTempFloor(map, tile.x, tile.y)) {
				map[tile.y][tile.x] = TileChar.charRoomFloor;
				roomsByTile.put(tile, roomNr);
			}
			if (isTempFloor(map, tile.x, tile.y - 1) && !isDoor(map, tile.x, tile.y - 1)) {
				todo.push(new IntPoint(tile.x, tile.y - 1));
			}
			if (isTempFloor(map, tile.x, tile.y + 1) && !isDoor(map, tile.x, tile.y + 1)) {
				todo.push(new IntPoint(tile.x, tile.y + 1));
			}
			if (isTempFloor(map, tile.x - 1, tile.y) && !isDoor(map, tile.x - 1, tile.y)) {
				todo.push(new IntPoint(tile.x - 1, tile.y));
			}
			if (isTempFloor(map, tile.x + 1, tile.y) && !isDoor(map, tile.x + 1, tile.y)) {
				todo.push(new IntPoint(tile.x + 1, tile.y));
			}
		}
	}

	private boolean isTempFloor(char[][] map, int x, int y) {
		return map[y][x] == TileChar.charTemp || map[y][x] == TileChar.charFloor;
	}

	private boolean isDoor(char[][] map, int x, int y) {
		return map[y][x] == TileChar.charDoorH || map[y][x] == TileChar.charDoorV;
	}

	private void renderCellBorders(char[][] map, CellNode node) {
		if (node == null) {
			return;
		}

		// render cell borders
		for (int x = node.x; x < node.x + node.width; x++) {
			map[node.y][x] = TileChar.charCell;
			map[node.y + node.height - 1][x] = TileChar.charCell;
		}

		for (int y = node.y; y < node.y + node.height; y++) {
			map[y][node.x] = TileChar.charCell;
			map[y][node.x + node.width - 1] = TileChar.charCell;
		}

		renderCellBorders(map, node.left);
		renderCellBorders(map, node.right);
	}

	private void renderCorridors(char[][] map, CellNode node) {
		if (node == null) {
			return;
		}

		// render corridors
		IntRect corridor = node.corridor;
		if (corridor != null) {
			for (int y = corridor.y; y < corridor.y + corridor.height; y++) {
				for (int x = corridor.x; x < corridor.x + corridor.width; x++) {
					map[y][x] = TileChar.charFloor;
				}
			}
		}

		renderCorridors(map, node.left);
		renderCorridors(map, node.right);
	}

	private void renderRooms(char[][] map, CellNode node) {
		if (node == null) {
			return;
		}

		// render room
		IntRect room = node.room;
		if (room != null) {
			for (int x = room.x; x < room.x + room.width; x++) {
				for (int y = room.y; y < room.y + room.height; y++) {
					if (!isDoor(map, x, y)) {
						map[y][x] = TileChar.charTemp;
					}
				}
			}
		}

		renderRooms(map, node.left);
		renderRooms(map, node.right);
	}

	private void convertVoid(char[][] map) {
		for (int y = 0; y < mapHeight; y++) {
			for (int x = 0; x < mapWidth; x++) {
				if (map[y][x] == TileChar.charVoid) {
					map[y][x] = TileChar.charOuterFloor;
				}
			}
		}
	}

	private char[][] initMap() {
		char[][] map = new char[mapHeight][mapWidth];
		for (int y = 0; y < mapHeight; y++) {
			for (int x = 0; x < mapWidth; x++) {
				map[y][x] = TileChar.charVoid;
			}
		}
		return map;
	}

	public void print(char[][] map, int width, int height) {
		log.info("seed: " + seed);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				out.print(map[y][x]);
			}

			out.println();
		}
	}
}
