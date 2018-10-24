package de.voodoosoft.gameroots.mapgen;

import de.voodoosoft.gameroots.shared.geom.IntPoint;

import java.util.Map;



public interface CharLevelMapCreator {
	char[][] createMap();

	Map<IntPoint, Integer> getRoomsByTile();

	void processRooms(RoomCallback roomCallback);
}
