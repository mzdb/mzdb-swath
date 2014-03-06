/* 
 * Copyright 2014, Sara Nasso, David Bouyssie, Marc Dubois
 * 
 * This file is part of mzDB.
 *
 * mzDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mzDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with mzDb.  If not, see <http://www.gnu.org/licenses/>.
*/

package mzRTreeDBSwath;

public class MzIntensity implements Comparable<Object>{
	private Float mz;
	private Float intensity;

	public MzIntensity(Float mz, Float intensity){
		this.mz = mz;
		this.intensity = intensity;
	}

	@Override
	public int compareTo(Object other) {
		if(other instanceof Float){
			return this.getMz().compareTo((Float)other);
		} else if (other instanceof MzIntensity){
			return this.getMz().compareTo(((MzIntensity) other).getMz());
		}
		return -1;
	}

	public Float getMz() {
		return mz;
	}

	public Float getIntensity() {
		return intensity;
	}
}

