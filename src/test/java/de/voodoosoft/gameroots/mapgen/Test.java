package de.voodoosoft.gameroots.mapgen;

import de.voodoosoft.gameroots.mapgen.bsp.BspMapCreator;



public class Test {
	public static void main(String[] args) {
		BspMapCreator bspMapCreator = new BspMapCreator();
		bspMapCreator.setMinRoomSize(5);
		bspMapCreator.setMaxIterations(6);
		bspMapCreator.setMapDimension(100, 60);
		bspMapCreator.setOut(System.out);
		bspMapCreator.createMap();
	}
}
