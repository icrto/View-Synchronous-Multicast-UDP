package util;

import java.io.Serializable;

public class Tuple<X, Y> implements Serializable{ 
	/**
	 * 
	 */
	private static final long serialVersionUID = 4574143716778128052L;
	public final X x; 
	public final Y y; 
	public X getX() {
		return x;
	}
	public Y getY() {
		return y;
	}
	public Tuple(X x, Y y) { 
		this.x = x; 
		this.y = y; 
	} 
	@Override
	public String toString() {
		return "[senderId=" + x + ", seqN=" + y + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((x == null) ? 0 : x.hashCode());
		result = prime * result + ((y == null) ? 0 : y.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Tuple)) {
			return false;
		}
		Tuple other = (Tuple) obj;
		if (x == null) {
			if (other.x != null) {
				return false;
			}
		} else if (!x.equals(other.x)) {
			return false;
		}
		if (y == null) {
			if (other.y != null) {
				return false;
			}
		} else if (!y.equals(other.y)) {
			return false;
		}
		return true;
	}
	
	
} 