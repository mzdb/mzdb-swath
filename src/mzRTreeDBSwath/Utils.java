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
 * This class contains some static and generic methods that are used in mzRTree.
 * 
 * @version 1.0
 * 
 * @author Sara Nasso (Department of Information Engineering, University of Padova)
 * @author Francesco Silvestri (Department of Information Engineering, University of Padova)
 * @author Francesco Tisiot
 * 
 * <br/> Contact: <a href="mailto:mzRTree@gmail.com">mzRTree@gmail.com</a>
 */
class Utils {

	/*Below some constants that are used in the code. Do not change them! If you want to change 
	them, you have to rebuild all the mzRTree that you have used so far!!!!*/
	
	//mzRTree ignore all spectra in the input mzXML file with level bigger than MAX_MS_LEVEL
	protected static final int MAX_MS_LEVEL = 1; 
	//Maximum number of BB in files strip_X_Y.dat. 
	protected static final int MAX_BB_PER_FILE = 1000;
	//If the density of a BB is smaller than DENSITY, it is stored into disk as a sparse matrix,
	//as a complete matrix otherwise.
	protected static final float DENSITY = (float) 0.5;
	//Maximum size among the mz dimension of a BB. The maximum size among the rt dimension
	//is function of the number of strips required when the mzRTree is built.
	protected static final int SIZE_MZ_BB = 5;
	//Max number of BBs in a leaf.
	protected static final int BB_PER_LEAF = 200;

	/*Some constants for printing during debug*/
	
	protected static final int PRINT_ALWAYS = 0;//Print also in the non debug mode.
	protected static final int PRINT_DEBUG = 1; //Print only in the debug mode
	protected static int PRINT_MODE = PRINT_DEBUG; //Print mode

	/**
	 * Convert an int into four bytes.
	 * @param value		the int to convert
	 * 
	 * @return			a byte array containing the four bytes  
	 * */
	protected static byte[] intToBytes(int value){
		byte[] bytes = new byte[4];
		bytes[0] =(byte)(( value >>> 24 ) & 0xFF);
		bytes[1] =(byte)(( value >>> 16 ) & 0xFF);
		bytes[2] =(byte)(( value >>>  8 ) & 0xFF);
		bytes[3] =(byte)(( value >>>  0 ) & 0xFF);
		return bytes;
	}
	
	
	/** Convert the first four bytes of array bytes into an int.
	 * 
	 * @param bytes		the input array
	 * @return			the int contained in the first four bytes of array bytes
	 * */
	protected static int bytesToInt(byte bytes[]){
		return ((((bytes[0]<<24)&0xFF000000) |((bytes[1]<<16)&0x00FF0000) |((bytes[2]<<8)&0x0000FF00) |(bytes[3]&0x000000FF)));
	}

	/**
	 * Convert an int into four bytes which are stored in mbyte[i:i+3].
	 * @param value		the int to convert
	 * @param bytes		the array where to save the four bytes 
	 * @param offset	the position where to save the four bytes
	 * */
	protected static void intToBytes(int value, byte[] bytes, int offset){
		bytes[offset+0] =(byte)(( value >>> 24 ) & 0xFF);
		bytes[offset+1] =(byte)(( value >>> 16 ) & 0xFF);
		bytes[offset+2] =(byte)(( value >>>  8 ) & 0xFF);
		bytes[offset+3] =(byte)(( value >>>  0 ) & 0xFF);
	}	

	/** Convert four consecutive bytes of array bytes starting from bytes[offset[ into an int.
	 * 
	 * @param bytes		the input array
	 * @param offset	the position where the four bytes starts.
	 * @return			the int contained in the first four bytes of array bytes
	 * */
	protected static int bytesToInt(byte[] bytes,int offset){
		return ((((bytes[offset+0]<<24)&0xFF000000) |((bytes[offset+1]<<16)&0x00FF0000) |((bytes[offset+2]<<8)&0x0000FF00) |(bytes[offset+3]&0x000000FF)));
	}

	
	/**
	 * Print a string in the standard output and terminate the line. 
	 * Works only if mode<-PRINT_MODE.
	 * 
	 * @param string	the string to print
	 * @param mode 		can only assume values PRINT_ALWAYS or PRINT_DEBUG.
	 * */
	protected static void println(String string, int mode) {
		if(mode<=PRINT_MODE) System.out.println(string);
	}

	/**
	 * Print a string in the standard output. 
	 * Works only if mode<-PRINT_MODE.
	 * 
	 * @param string	the string to print
	 * @param mode 		can only assume values PRINT_ALWAYS or PRINT_DEBUG.
	*/
	protected static void print(String string, int mode) {
		if(mode<=PRINT_MODE) System.out.print(string);
	}

	/**
	 * Compare two byte arrays of length 4.
	 * 
	 *  @param a	the first array
	 *  @param b	the second array
	 *  
	 *  @return		true if and only if a[i]==b[i] for each 0<=i<4; false otherwise.
	 * */
	protected static boolean equals_size4(byte[] a, byte[] b){
		return ((a[0]^b[0]) | (a[1]^b[1]) | (a[2]^b[2]) | (a[3]^b[3]))==0; 
	}
		
	/**
	 * Compare two int arrays of length 2.
	 * 
	 *  @param a	the first array
	 *  @param b	the second array
	 *  
	 *  @return		true if and only if a[0]==b[0] and a[1]==b[1]; false otherwise.
	 * */
	protected static boolean equals_size2(int[] a, int[] b){
		return ((a[0]^b[0]) | (a[1]^b[1]))==0; 
	}
	
	/**
	 * This method sorts a linked list of BBnodes using quicksort. Observe that the bb stored
	 * in from is not used, the first node of interests is from.next.
	 * 
	 * @param from			the head of the linked list 
	 * @param to			the tail of the linked list
	 * @param size			the number of BBnodes
	 * @param sort_type		specifies how to sort BBnodes. It can assume values 
	 * 						SORT_INCREASING_MIN_RT, SORT_INCREASING_MIN_MZ, SORT_INCREASING_MAX_RT, 
	 * 						SORT_INCREASING_MAX_MZ, SORT_DECREASING_MIN_RT, SORT_DECREASING_MIN_MZ,
	 * 						SORT_DECREASING_MAX_RT, SORT_DECREASING_MAX_MZ.
	 * 
	 * @return				the new tail of the sorted linked list
	 * */
//	protected static BBnode sort(BBnode from, BBnode to, int size, int sort_type){
//		if(size<=1){			
//			return to;
//		}
//		else{
//			//remember to which BBnode the last node point. to.next is not sorted
//			BBnode old_to_next = to.next;
//		
//			//compute the pivot
//			BBnode pivot = set_pivot(from.next, to, sort_type);
//			
//			//list of nodes smaller than pivot
//			BBnode start_small = new BBnode();
//			BBnode end_small = start_small;
//			int small = 0;
//
//			//list of nodes equal to pivot
//			BBnode start_equal = new BBnode();
//			BBnode end_equal = start_equal;
//			int equal = 0;
//			
//			//list of nodes bigger than pivot
//			BBnode start_big = new BBnode();
//			BBnode end_big = start_big;
//			int big = 0;
//
//			BBnode cursor = from.next;
//
//			int tmp;
//			
//			//partition the linked list
//			while(cursor!=to.next){
//				tmp = cursor.compareTo(pivot, sort_type);
//				if(tmp<0){
//					end_small.next = cursor;
//					end_small = end_small.next;
//					small++;
//				} else if (tmp>0){
//					end_big.next = cursor;
//					end_big = end_big.next;
//					big++;
//				} else{
//					end_equal.next = cursor;
//					end_equal = end_equal.next;
//					equal++;
//				}
//				cursor = cursor.next;
//			}
//			
//			end_small.next = null;
//			end_equal.next = null;
//			end_big.next = null;
//
//			//sort recursivelly the linked lists of smaller and bigger BBnodes
//			end_small = sort(start_small, end_small, small, sort_type);
//			end_big = sort(start_big, end_big, big, sort_type);
//			
//			//merge results
//			end_equal.next = start_big.next;
//			end_small.next = start_equal.next;			
//			from.next = start_small.next;
//			
//			//return the last node (observe that the some of the
//			// three lists (smaller, equal, bigger BBs) may be empty.  
//			if(big!=0){
//				end_big.next = old_to_next;
//				return end_big;
//			} else if(equal!=0){
//				end_equal.next = old_to_next;
//				return end_equal;
//			} else{
//				end_small.next = old_to_next;
//				return end_small;	
//			}
//		}        
//	}

	/**
	 * Compute the pivot of a linked list headed by from. The pivot is the average 
	 * of an extreme (among min_mz, min_rt, max_mz, max_rt) in the BBs of from and to; 
	 * the used extreme depends on the sort_type. Observe that in the returned pivot, only
	 * the extreme of interest is set, while the others assume the default value (-1)
	 * 
	 * @param from			the head of the linked list 
	 * @param to			the tail of the linked list
	 * @param sort_type		specifies how to sort BBnodes. It can assume values 
	 * 						SORT_INCREASING_MIN_RT, SORT_INCREASING_MIN_MZ, SORT_INCREASING_MAX_RT, 
	 * 						SORT_INCREASING_MAX_MZ, SORT_DECREASING_MIN_RT, SORT_DECREASING_MIN_MZ,
	 * 						SORT_DECREASING_MAX_RT, SORT_DECREASING_MAX_MZ.
	 * 
	 * @return 				the BBnode representing the pivot.
	 * */
	private static BBnode set_pivot(BBnode from, BBnode to, int sort_type) {
		BBnode out = new BBnode();		
		switch(sort_type){
		case BBnode.SORT_INCREASING_MIN_MZ:
			out.min_mz = (from.min_mz+to.min_mz)/2;
			break;		
		case BBnode.SORT_INCREASING_MIN_RT:
			out.min_rt = (from.min_rt+to.min_rt)/2;
			break;
		case BBnode.SORT_DECREASING_MAX_RT:
			out.max_rt = (from.max_rt+to.max_rt)/2;
			break;
		case BBnode.SORT_DECREASING_MAX_MZ:
			out.max_mz = (from.max_mz + to.max_mz)/2;
			break;
		case BBnode.SORT_INCREASING_MAX_RT:
			out.max_rt = (from.max_rt+to.max_rt)/2;
			break;
		case BBnode.SORT_INCREASING_MAX_MZ:
			out.max_mz = (from.max_mz + to.max_mz)/2;
			break;
		case BBnode.SORT_DECREASING_MIN_RT:
			out.min_rt = (from.min_rt+to.min_rt)/2;
			break;
		case BBnode.SORT_DECREASING_MIN_MZ:
			out.min_mz = (from.min_mz+to.min_mz)/2;
			break;
		default:
			out=null;
		}		
		return out;
	}
}