package de.voodoosoft.gameroots.shared.geom;




public class IntRect {
	public int x, y, width, height;

	public IntRect() {
	}

	public IntRect(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public IntRect(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public IntRect(IntRect source) {
		this.x = source.x;
		this.y = source.y;
		this.width = source.width;
		this.height = source.height;
	}

	public boolean contains(int px, int py) {
		int w = this.width;
		int h = this.height;
		if ((w | h) < 0) {
			return false;
		}

		int x = this.x;
		int y = this.y;
		if (px < x || py < y) {
			return false;
		}

		w += x;
		h += y;

		return ((w < x || w > px) && (h < y || h > py));
	}

	public boolean intersects(IntRect r) {
		int tw = this.width;
		int th = this.height;
		int rw = r.width;
		int rh = r.height;
		if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
			return false;
		}
		int tx = this.x;
		int ty = this.y;
		int rx = r.x;
		int ry = r.y;
		rw += rx;
		rh += ry;
		tw += tx;
		th += ty;
		//      overflow || intersect
		return ((rw < rx || rw > tx) &&
		  (rh < ry || rh > ty) &&
		  (tw < tx || tw > rx) &&
		  (th < ty || th > ry));
	}

	public void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		IntRect intRect = (IntRect)o;

		if (x != intRect.x)
			return false;
		if (y != intRect.y)
			return false;
		if (width != intRect.width)
			return false;
		return height == intRect.height;
	}

	@Override
	public int hashCode() {
		int result = x;
		result = 31 * result + y;
		result = 31 * result + width;
		result = 31 * result + height;
		return result;
	}

	@Override
	public String toString() {
		return x + ":" + y + ":" + width + ":" + height;
	}
}
