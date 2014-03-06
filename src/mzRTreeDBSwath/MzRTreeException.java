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

/**
 * Generic class error used in mzRTree.
 * 
 * @version 1.0
 * 
 * @author Sara Nasso (Department of Information Engineering, University of Padova)
 * @author Francesco Silvestri (Department of Information Engineering, University of Padova)
 * @author Francesco Tisiot
 * 
 * <br/> Contact: <a href="mailto:mzRTree@gmail.com">mzRTree@gmail.com</a>
 */

package mzRTreeDBSwath;

public class MzRTreeException extends Exception {

	private static final long serialVersionUID = 1L;

	protected MzRTreeException(String info){
		super(info);
	}
}
