package de.voodoosoft.gameroots.shared.geom;

public class IntPoint {
	public int x, y;

	public IntPoint() {
	}

	public IntPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		IntPoint intPoint = (IntPoint)o;

		if (x != intPoint.x)
			return false;
		return y == intPoint.y;
	}

	@Override
	public int hashCode() {
		int result = x;
		result = 31 * result + y;
		return result;
	}
}
