package de.voodoosoft.gameroots.mapgen;

import de.voodoosoft.gameroots.shared.geom.IntPoint;
import de.voodoosoft.gameroots.shared.geom.IntRect;

import java.util.List;

public interface RoomCallback {
	void processRoom(int room, IntRect roomRect, List<IntPoint> tiles);
}
