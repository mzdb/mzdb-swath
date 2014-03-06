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

/**
 * This class represents a node of the r-tree built by MzRtree.build_rtree(...). An internal
 * node contains at least N>Utils.MAX_BB_PER_FILE and represents a rectangule delimited
 * by the BBnode bb and its 6 children (contained in the child array)partition the 
 * rectangle in six subrectangles of equal size Math.ceil(N/6) (at most for the last node)
 * as follows:
 * 
 * 
 * <pre> 
 * 
 *  |--------------------------|
 *  |          TOP (0)         |
 *  |--------------------------|
 *  |     |      |      |      |     
 *  |     |      |      |      |
 *  |     |      |      |      |  
 *  |     |CENTER|CENTER| RIGHT|
 *  |LEFT | RIGHT| LEFT |  (3) |
 *  | (1) |  (4) |  (5) |      |
 *  |     |--------------------|
 *  |     |     BOTTOM         |
 *  |     |       (2)          |
 *  |-----|--------------------|
 *  
 * </pre>
 * 
 * Each leaf contains at most Utils.MAX_BB_PER_FILE BBs, which are contained in array leafs.
 * 
 * @version 1.0
 * 
 * @author Sara Nasso (Department of Information Engineering, University of Padova)
 * @author Francesco Silvestri (Department of Information Engineering, University of Padova)
 * @author Francesco Tisiot
 * 
 * <br/> Contact: <a href="mailto:mzRTree@gmail.com">mzRTree@gmail.com</a>
 */
class RTreeNode {	
	//Type of internal nodes
	protected static final int TOP = 0;
	protected static final int LEFT = 1;
	protected static final int BOTTOM = 2;
	protected static final int RIGHT = 3;
	protected static final int CENTER_LEFT = 4;
	protected static final int CENTER_RIGHT = 5;
	
	//children in the case, this is an internal node. If child!=null, then list_bb==null
	protected RTreeNode[] child= null;
    //number of bb contained in the tree rooted at this node
    protected int num_bb = 0;
	// bb delimiting the rectangle that include all the num_bb BBs stored in the tree rooted 
    // at this node (or the node itself, if it is a leaf) 
    protected BBnode range = null;
    //if the node is a leaf, this array contains all the BB assigned to this leaf. If list_bb!=null,
    //then child==null and list_bb.length==range.
    protected BBnode[] list_bb = null;
    //next is used only when an RTreeNode is stored in a linked list. Used only in MzRTree.build_rtree().
    protected RTreeNode next = null;
}
