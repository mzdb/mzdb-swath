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
 * This class represents a bounding box and contains a method for compare two BBs.
 * 
 * @version 1.0
 * 
 * @author Sara Nasso (Department of Information Engineering, University of Padova)
 * @author Francesco Silvestri (Department of Information Engineering, University of Padova)
 * @author Francesco Tisiot
 * 
 * <br/> Contact: <a href="mailto:mzRTree@gmail.com">mzRTree@gmail.com</a>
 */
class BBnode {
				
	/*A bounding box can be sorted in 8 different ways. The variable current_sort_type
	 indicates which is in use. */
	protected static final int SORT_INCREASING_MIN_RT = 0;
	protected static final int SORT_INCREASING_MIN_MZ = 1;
	protected static final int SORT_INCREASING_MAX_RT = 2;
	protected static final int SORT_INCREASING_MAX_MZ = 3;
	protected static final int SORT_DECREASING_MIN_RT = 4;
	protected static final int SORT_DECREASING_MIN_MZ = 5;
	protected static final int SORT_DECREASING_MAX_RT = 6;
	protected static final int SORT_DECREASING_MAX_MZ = 7;
	
	protected static final int STRIP_NUM = 0;
	protected static final int STRIP_SUB_NUM = 1;
	
	//upper-left corner of the BB (min_rt, min_mz)
    protected int min_rt = -1;
    protected float min_mz = -1;
	//lower-right corner of the BB (min_rt, min_mz)
    protected int max_rt = -1;
    protected float max_mz = -1;
    
    protected int non_zero_values;  
    
    //is the bounding box sparse? that is: is its density smaller or equal than Utils.DENSITY?
    protected boolean is_sparse = false;
    
    //name of the file strip_X_Y.dat that contains the current BB.
    //We have: file_name[Utils.STRIP_NUM]=X and file_name[Utils.STRIP_SUB_NUM]=Y. 
    protected int[] file_name = {-1, -1};
    //position in the file which contains the first byte of the BB.  
    protected int start_byte = -1;
    //position in the file which contains the last byte of the BB.
    protected int end_byte = -1;
    
    //the next BBnode in the linked list
    protected BBnode next;
    
	/**
	 * Method for comparing two BBnodes: this and bb which is the first parameter. 
	 * The second argument specifies how to compare the two BBnodes: 
	 *   if sort_type==Utils.SORT_INCREASING_MIN_MZ, this<bb if this.min_mz<bb.min_mz;
	 *   if sort_type==Utils.SORT_INCREASING_MIN_RT, this<bb if this.min_rt<bb.min_rt;
	 *   if sort_type==Utils.SORT_INCREASING_MAX_MZ, this<bb if this.max_mz<bb.max_mz;
	 *   if sort_type==Utils.SORT_INCREASING_MAX_RT, this<bb if this.max_rt<bb.max_mz;
	 *   if sort_type==Utils.SORT_DECREASING_MIN_MZ, this<bb if this.min_mz>bb.min_mz;
	 *   if sort_type==Utils.SORT_DECREASING_MIN_RT, this<bb if this.min_rt>bb.min_rt;
	 *   if sort_type==Utils.SORT_DECREASING_MAX_MZ, this<bb if this.max_mz>bb.max_mz;
	 *   if sort_type==Utils.SORT_DECREASING_MAX_RT, this<bb if this.max_rt>bb.max_mz;
	 *   otherwise this==bb.
	 * 
	 * @param bb 		the BBnode to be compared.
	 * @param sort_type	the int that specifies how to compare the BBnode.
	 * 
	 * @return the value 0 if this BBnode is equal to the argument BBnode; a value less than
     * 		0 if this BBnode is less than the argument BBnode; and a value greater 
     * 		than 0 if this <code>Integer</code> is greater than the argument BBnode.
	 * */
   
}

