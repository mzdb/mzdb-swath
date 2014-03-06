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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

import org.systemsbiology.jrap.MSXMLParser;
import org.systemsbiology.jrap.Scan;
import org.systemsbiology.jrap.ScanHeader;


/***
 * This class implements an mzRTree. An mzRTree is a new data structure for storing mass
 * spectometry data which supports efficient range query operations. A mass spectometry 
 * data can be envisioned as a matrix where each row contains a spectrum and each column 
 * an mz value. A range query returns an arbitrary submatrix of the input matrix. Currently
 * the mzRTree can be constructed starting from an mzXML files. The 
 * implementation is explained in the full paper. 
 * 
 * Usage in a few words: create an mzRTree calling MzRTree(String in_file, String out_dir, int res, int strip),
 * then use range_query(int rt_i, int rt_f, float mz_i, float mz_f) for retrieving data. An mzRTree
 * is automatically stored into the disk for future usage and can be efficiently loaded 
 * in memory invoking MzRTree(String input_dir).
 * 
 * @version 1.0
 * 
 * @author Sara Nasso (Department of Information Engineering, University of Padova)
 * @author Francesco Silvestri (Department of Information Engineering, University of Padova)
 * @author Francesco Tisiot
 * 
 * <br/> Contact: <a href="mailto:mzRTree@gmail.com">mzRTree@gmail.com</a>
 */
public class MzRTree {
	public static final int FIXED_SIZE_SWATH_WIDTH = 25;
	public static final float START_SWATH_MZ = 400F;
	//codes that identify how a BB is saved*/
	private final static byte[] SPARSE_MATRIX = { 'S', 'P', 'A', 'R' }; //sparse 
	private final static byte[] DENSE_MATRIX = { 'D', 'E', 'N', 'S' }; //dense

	//codes that identify a node when the r-tree is stored into the disk
	private static final byte[] INTERNAL_NODE = { 'I', 'N', 'T', 'E' }; //internal node
	private static final byte[] LEAF_NODE = { 'L', 'E', 'A', 'F' }; // leaf node
	private static final byte[] NULL_NODE = { 'N', 'U', 'L', 'L' }; //null (not a real node)
	private final int msLevel;
	
	private int swath_number_partial_fake_due_to_ms1 = 1;
	private int swath_number_partial_true_due_to_ms2 = 32;
	private String xml_input_file; // URL of the input mzXML file
	private MSXMLParser parser;
	private String output_dir; // URL of the directory containing the output (strips and r-tree)
	private String output_data_dir; // URL of the directory containing the strips
	private int numberOfData;
	// (it could be different from input_divisions due to rounding)
	private float lowest_mz;	//lower mz values among all of the strips
	private float highest_mz; //higher mz values among all of the strips
	private int spectra_number; //number of spectra
	private int max_spectra_per_strip; //maximum number of spectra per strip
	//	private float density; //density of the matrix ( = non zero intensities/total number of intensities)

	private boolean mzRTree_ready = false; //true if the mzRTree is ready, false otherwise

	//Pointer to the header and tail of the linked list containing all the BBs.
	//They are used only when the mzRTree is built from scratch
	private BBnode header;
	private BBnode tail;
	private int bb_number; 	//number of bb, used only when the mzRTree is built from scratch

	RTreeNode root = new RTreeNode(); //root of the r-tree
	private DBmzRTree mzRTreeDB;
	private String fileSwathSizesPath;


	/**
	 * Build an mzRTree from an input mzXML file and save data into the
	 * directory pointed by out_dir. This directory will contain: the directory
	 * <i>data</i> containing the strips (the X-th strip is stored in <i>n</i>
	 * files <i>strip_X_Y.dat</i> where Y is a number in <i>[0, n)</i>; the file
	 * <i>strip_X.BB</i> contains the list of the BB in X), the file <i>
	 * index.ind </i> that contains the r-tree data structures, the file
	 * <i>retention.txt</i> that contains the list of retention time values, the
	 * file <i>header.mzXML</i> which contains the header of the input mzXML
	 * file, the file <i>info.txt</i> that contains some information on the
	 * mzRTree.
	 * 
	 * @param in_file 		the URL of the input mzXML file
	 * @param out_dir 		the URL of the output directory
	 * @param res  the 		resolution of the mz dimension which will be used for building the mzRTree
	 * @param strip 		the number of strip in which spectra will be divided (the actual number of
	 * 						strips may be smaller due to rounding and will be stored in info.txt)
	 * 
	 * @throws MzRTreeException if a problem arises when building the mzRTree.
	 * @throws IOException 
	 * */


	public MzRTree(DBmzRTree mzRTreeDBin,String filePathDB,String in_file, String out_dir, int max_spectra_per_strip, float lowestmz, float highestmz, int msLevel, String fileSwathSizesPath) throws MzRTreeException, SQLException, ClassNotFoundException, IOException {
		this.xml_input_file = in_file;
		this.parser = new MSXMLParser(xml_input_file);
		this.output_dir = out_dir + File.separator;
		this.max_spectra_per_strip = max_spectra_per_strip;
		this.lowest_mz = lowestmz;
		this.highest_mz = highestmz;
		this.msLevel = msLevel;
		this.bb_number = 0;
		this.fileSwathSizesPath = fileSwathSizesPath;
		if (mzRTreeDBin == null){
			mzRTreeDB = new DBmzRTree(filePathDB,false);
		}else{
			mzRTreeDB=mzRTreeDBin;
		}
		
		create();
		
		mzRTree_ready = true;
	}
	
	private void create() throws MzRTreeException, SQLException, ClassNotFoundException, IOException{
		if(fileSwathSizesPath!=null){ 
			importSwathSizeInfo();
		} 
		else{
			setDefaultSwathSizeInfo();
		}

		for(int currentSwath = 1; currentSwath <= getTotSwathNumber(); currentSwath++){
			save_strips(parser, currentSwath);
//			java.lang.Runtime.getRuntime().exec("purge");
			System.out.println("SWATH # " + currentSwath + " DONE! ");
		}
	}

	private void setDefaultSwathSizeInfo() throws SQLException {

		Connection connToMzRTreeDB=mzRTreeDB.getConnection();
		connToMzRTreeDB.setAutoCommit(false);
		PreparedStatement prepStatSwathInfo = connToMzRTreeDB.prepareStatement("INSERT INTO SWATHS VALUES (?,?)");
		Float inMz = 0F;
		Float finMz = (float) FIXED_SIZE_SWATH_WIDTH; //fake for MS1 swath
		mzRTreeDB.insertSwathInfo( prepStatSwathInfo,  inMz, finMz);

		for(int currentSwath = 1; currentSwath <= getTotSwathNumber()-1; currentSwath++) {
			inMz = START_SWATH_MZ + FIXED_SIZE_SWATH_WIDTH * (currentSwath -1);
			finMz = inMz + FIXED_SIZE_SWATH_WIDTH;
			mzRTreeDB.insertSwathInfo( prepStatSwathInfo,  inMz, finMz);
		}
		prepStatSwathInfo.executeBatch();
		connToMzRTreeDB.commit();
		prepStatSwathInfo.clearBatch();
		prepStatSwathInfo.close();
	}

	private void importSwathSizeInfo() throws IOException, SQLException {
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(this.fileSwathSizesPath));
			String line = null;
			br.readLine(); // jumping over first row where the swath # is saved
			Connection connToMzRTreeDB=mzRTreeDB.getConnection();
			connToMzRTreeDB.setAutoCommit(false);
			PreparedStatement prepStatSwathInfo = connToMzRTreeDB.prepareStatement("INSERT INTO SWATHS VALUES (?,?)");
			mzRTreeDB.insertSwathInfo( prepStatSwathInfo,  0F, 25F); // fake swath for MS1
			int swathNumber = 0;
			try{
				while ((line = br.readLine()) != null) {
					StringTokenizer st = new StringTokenizer(line);
					Float inMz = Float.parseFloat(st.nextToken());
					Float finMz = Float.parseFloat(st.nextToken());
					swathNumber++;
					mzRTreeDB.insertSwathInfo( prepStatSwathInfo,  inMz, finMz);
				}
			}catch(NumberFormatException ex){
				System.out.println("Swath win size input file shows a non standard format for swath number : " + swathNumber);

				throw ex;
			}
			prepStatSwathInfo.executeBatch();
			connToMzRTreeDB.commit();
			prepStatSwathInfo.clearBatch();
			prepStatSwathInfo.close();
			this.swath_number_partial_true_due_to_ms2 = swathNumber;	
		}	
		finally{
			br.close();
		}
	}

	private int getTotSwathNumber(){
		return swath_number_partial_fake_due_to_ms1 + swath_number_partial_true_due_to_ms2;  
	}
	/**
	 * Load a mzRTree that was created in the past and stored in the directory
	 * pointed by in_dir
	 * 
	 * @param in_dir 		the URL of the directory containing the mzRTree.
	 * 
	 * @throws MzRTreeException if a problem arises when building the mzRTree.
	 * */
	public MzRTree(String in_dir, int msLevel) throws MzRTreeException {
		output_dir = in_dir;
		mzRTree_ready = true;
		this.msLevel = msLevel;
	}

	
	/***
	 * This method converts compute the bounding boxes (BBs) and save the strips
	 * into the directory <i> data </i>. Remember that the input can be
	 * envisioned as a matrix of size [spectra_number][mz_number], where
	 * spectra_number is the number of spectra and mz_number is the number of mz
	 * values at the current resolution. In the code rt or spectrum have the
	 * same meaning.
	 * @param parser 
	 * 
	 * @throws MzRTreeException if something bad happens!
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 * */
	private void save_strips(MSXMLParser parser, int currentSwath) throws MzRTreeException, SQLException, ClassNotFoundException {
		try {
			// header (resp., tail) will contain the first (resp., tail) node
			// of the list
			// containing all the BBs (not the real data!).
			header = new BBnode();
			tail = header;
			
			Utils.println("Saving strips.", Utils.PRINT_ALWAYS);

			// only for JRAP3
//			MSXMLParser parser = new MSXMLParser(xml_input_file);
			//only for JRAP4 MSInstrumentInfo instrumentInfo =
			//			 parser.rapFileHeader().getInstrumentInfo(); 
			//			 MSInstrumentInfo instrumentInfo = parser.getHeaderInfo().getInstrumentInfo();


			float rT = 0; // RT contains the real rt values of spectra
			
			int num_nonzero = 0; // total number of non zero entries in the "virtual" input matrix. Used for computing density
			ArrayList<Float> mzValues = new ArrayList<Float>();
			// let's create the matrix where a strip of scans will be stored
			ArrayList<ArrayList<MzIntensity>> matrix = new ArrayList<ArrayList<MzIntensity>>(max_spectra_per_strip);
			int workDone = 0;// advancing in saving spectra
			int strip_number = 0;// current strip number
			int pos_in_strip = 0;// current spectrum number in the current strip
			int current_spectrum = currentSwath; // current spectra number as in the XML file
			int saved_spectra = 0; // number of spectra with MS level equal to msLevel;
			int absScanNumber = 0;
			int groupNumber = 0;
			float swathPrecursor = 0;
			Connection connToMzRTreeDB=mzRTreeDB.getConnection();
			connToMzRTreeDB.setAutoCommit(false);
			PreparedStatement prepStatMapScanToRT = connToMzRTreeDB.prepareStatement("INSERT INTO SCAN_RT VALUES (?,?)");					

			// We now save each spectrum in the respective strip.
				while (current_spectrum <= parser.getScanCount()) {
					Scan scanParser = parser.rap(current_spectrum);
					// if the MS level is not equal to msLevel, then save the strip!
					ScanHeader scanHeader = scanParser.getHeader();
					swathPrecursor = scanHeader.getPrecursorMz();

						absScanNumber = scanHeader.getNum();
						// print some info on the work done so far.
						String retentionTime = scanHeader.getRetentionTime();
						rT= Float.parseFloat(retentionTime
								.substring(2, retentionTime.length() - 1));

						if (scanHeader.getPeaksCount() != -1) {
							// Only for JRAP 3
													float[][] peakList = scanParser.getMassIntensityList();
							//Only for JRAP 4 double [][] peakList =
//							double[][] peakList = scanParser.getMassIntensityList();

							for (int k = 0; k < peakList[0].length; k++) {
								float mz = (float) peakList[0][k];
								float intensity = (float) peakList[1][k];
								if (mz >= lowest_mz && mz <= highest_mz && intensity > 0) {
									int row = pos_in_strip;

									if(matrix.size() <= row || (matrix.size() > row && matrix.get(row) == null)){
										matrix.add(row, new ArrayList<MzIntensity>());
									}
									matrix.get(row).add(new MzIntensity(mz, intensity));
									num_nonzero++;
								}
							}
						}
						pos_in_strip++;
						saved_spectra++;
						mzRTreeDB.insertMapScanToRT(absScanNumber, rT, prepStatMapScanToRT); 
						// if a strip is ready then store it!
						if (pos_in_strip >= max_spectra_per_strip) {
							System.gc();
							long startTime = System.nanoTime();
							save_single_strip(matrix, max_spectra_per_strip, strip_number,swathPrecursor);
							long endTime = System.nanoTime();
							long totTime=endTime-startTime;
							System.gc();
//								java.lang.Runtime.getRuntime().exec("purge");
							strip_number++;
							pos_in_strip = 0;
							mzValues.clear();
							matrix.clear();
						}
//					}
					groupNumber++;
					current_spectrum=currentSwath+groupNumber*getTotSwathNumber();
				}

				mzRTreeDB.setMetadata(lowest_mz,highest_mz,spectra_number);
				// Save the last strip even if it is not full.
				if (pos_in_strip > 0) {
					System.gc();
					long startTime = System.nanoTime();
					save_single_strip(matrix, max_spectra_per_strip, strip_number,swathPrecursor);
					long endTime = System.nanoTime();
					long totTime=endTime-startTime;
					mzValues.clear();
					matrix.clear();
					System.gc();
//						java.lang.Runtime.getRuntime().exec("purge");
				}

				prepStatMapScanToRT.executeBatch();
				connToMzRTreeDB.commit();
				prepStatMapScanToRT.clearBatch();
				prepStatMapScanToRT.close();

		} catch (IOException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			throw new MzRTreeException("Some I/O problems! "
					+ System.getProperty("line.separator") + "---BEGIN---"
					+ System.getProperty("line.separator") + sw.toString()
					+ System.getProperty("line.separator") + "---END---");
		}
		Utils.println("Strips saved!", Utils.PRINT_ALWAYS);

	}


	/***
	 * Save a strip in one or more files containing at most
	 * Utils.MAX_BB_PER_FILE BBs. Data are saved in strip_X_Y.dat where X is the
	 * strip number and Y a number for distinguishing between the files
	 * containing the same strip.
	 * 
	 * @param matrix 		matrix containing the strip
	 * @param num_spectra 	the number of spectra in the strip (stored in the first num_spectra rows of matrix
	 * @param strip_number 	the strip number (or ID)
	 * 
	 * @throws IOException if some problem while writing the file
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 * */
	private void save_single_strip(ArrayList<ArrayList<MzIntensity>> matrix, int num_spectra,
			int strip_number, float swathPrecursor) throws IOException, SQLException, ClassNotFoundException {
		Utils.print("Saving strip " + strip_number + ". ", Utils.PRINT_DEBUG);

		BBnode cursor = tail;

		int old_bb_num = bb_number;
		/*
		 * compute_BB(..) appends to the linked list pointed by tail the BBs
		 * contained in the current strip. In save_single_strip(..) we store in
		 * the disk the new BBs, that is the BBs contained in the linked list
		 * starting from cursor and ending in tail.
		 */
		
		float minMz = Float.MAX_VALUE;
		float maxMz = Float.MIN_VALUE;
		for(ArrayList<MzIntensity> spectrum : matrix ){
			MzIntensity minPair = Collections.min(spectrum);
			MzIntensity maxPair = Collections.max(spectrum);
			minMz = minPair.getMz() < minMz ? minPair.getMz() : minMz;
			maxMz = maxPair.getMz() > maxMz ? maxPair.getMz() : maxMz;
		}
		
		compute_BB(matrix, minMz, maxMz, num_spectra, strip_number);

		/*
		 * The first node of the list pointed by cursor does not contain a BB of
		 * the current strip; just ignore it. The first BB of the strip is in
		 * the node pointed by cursor (if any!).
		 */
		if (cursor.next == null) {
			Utils.println("No new BBs in this strip", Utils.PRINT_DEBUG);
			return;
		}
		cursor = cursor.next;

		/*
		 * Number of files that have been used for storing the current strip.
		 * Remember: no more that Utils.MAX_BB_PER_FILE BBs in each file
		 */
		int num_file = 0;
		/* Number of BBs that have been stored */
		int num_BB = 0;
		/* First byte position of the current BB in the output file */
		int start_byte_BB = 0;
		/* Last byte of the current BB in the output file */
		int end_byte_BB = 0;

		int count_byte = 0; // the number of bytes required by a BB
		int k, kk, j; // support variables
		// an output buffer. 4 is the size in byte of a float. 2 is required by
		// the sparse case (one float is the value, the other the mz value
		/*
		 * For each strip X we save the file strip_X.BB containing info on each
		 * BB: margins, the name of the file containing it, its position in the
		 * output file... strip_X.BB is a useless file. We need it only for
		 * debugging.
		 */
		ByteArrayOutputStream binaryBBsData=new ByteArrayOutputStream();
		Connection connToMzRTreeDB=mzRTreeDB.getConnection();
		connToMzRTreeDB.setAutoCommit(false);
		PreparedStatement prepStatBBs = connToMzRTreeDB.prepareStatement("INSERT INTO BBs VALUES (?,?,?,?,?,?,?)");
		PreparedStatement prepStatData = connToMzRTreeDB.prepareStatement ("INSERT INTO DATA VALUES(?,?)");

		while (cursor != null) { 

			end_byte_BB = 0;

			start_byte_BB = end_byte_BB;

			if (cursor.is_sparse == true) {
				/* If the BB is sparse, save only the non zero values */
				binaryBBsData.write(SPARSE_MATRIX);
				end_byte_BB += 4;
				byte[] out_buffer = new byte[2 * 4 * (cursor.non_zero_values)];
				k = cursor.min_rt - strip_number * num_spectra;
				
				for (kk = cursor.min_rt; kk <= cursor.max_rt; k++, kk++) {
					count_byte = 0;
					ArrayList<MzIntensity> spectrum = matrix.get(k);
					int mzIdxMin = getMzIdx(cursor.min_mz, spectrum);
					int mzIdxMax = getMzIdxMax(cursor.max_mz, spectrum);

					
					for (j = mzIdxMin; j <= mzIdxMax; j++) {
						try{
							float curr_mz=spectrum.get(j).getMz();
//							
							Utils.intToBytes(Float.floatToIntBits(spectrum.get(j).getMz()), out_buffer, count_byte);

						}catch(ArrayIndexOutOfBoundsException ex){
							System.out.println("j is " + j);
							System.out.println("mzIdxMax is " + mzIdxMax);
							System.out.println("count_byte is " + count_byte);
							System.out.println("out_buffer size is " + out_buffer.length);
							Utils.intToBytes(Float.floatToIntBits(spectrum.get(j).getMz()), out_buffer, count_byte);
						}

						Utils.intToBytes(Float.floatToIntBits(spectrum.get(j).getIntensity()), out_buffer, count_byte + 4);
						count_byte += 8;
					}
					binaryBBsData.write(Utils.intToBytes(count_byte));
					if (count_byte > 0){
						binaryBBsData.write(out_buffer, 0, count_byte);
					}

					end_byte_BB += 4 + count_byte;// size of SPARSE_MATRIX is already in, count_byte as written in 4 lines above, and all data
				}
				mzRTreeDB.insertBBsData(prepStatData,old_bb_num+num_BB,binaryBBsData);
			}

			
			float[] range = mzRTreeDB.getSwathRange(swathPrecursor);
			float precMin = range[0];
			float precMax = range[1];
//			System.out.println("prec " + swathPrecursor + " is in swath: "  + precMin + " - " + precMax);
			mzRTreeDB.insertBB(old_bb_num+num_BB, cursor.min_rt, cursor.max_rt, cursor.min_mz, cursor.max_mz, precMin, precMax, prepStatBBs);
			
			num_BB++;
			/*
			 * Let us add a new BB to the list pointed by header and tail
			 * (global variables) and save the info about the file containing
			 * the BB
			 */
			cursor.file_name[BBnode.STRIP_NUM] = strip_number;
			cursor.file_name[BBnode.STRIP_SUB_NUM] = num_file - 1;
			cursor.start_byte = start_byte_BB;
			cursor.end_byte = end_byte_BB-1;
			cursor = cursor.next;

			binaryBBsData.reset();
		}
		binaryBBsData.close();
		Utils.println("Added " + (bb_number - old_bb_num) + " new BBs",Utils.PRINT_DEBUG);
		prepStatData.executeBatch();
		connToMzRTreeDB.commit();
		prepStatData.clearBatch();
		prepStatData.close();

		prepStatBBs.executeBatch();
		connToMzRTreeDB.commit();
		prepStatBBs.clearBatch();
		prepStatBBs.close();
	}
	
	private synchronized int getMzIdx(float mz, ArrayList<MzIntensity> spectrum) {
		int mzIdx = Collections.binarySearch(spectrum, new Float(mz));
		if(mzIdx<0) mzIdx = Math.abs(mzIdx)-1;
		return mzIdx >= spectrum.size() ? spectrum.size() -1 : mzIdx;
	}

	private synchronized int getMzIdxMax(float mz, ArrayList<MzIntensity> spectrum) {
		int mzIdx = Collections.binarySearch(spectrum, new Float(mz));
		if(mzIdx<0) mzIdx = Math.abs(mzIdx)-1-1; // additional -1 to get 1 position before the insertion point.
		return mzIdx;		
	}
	/***
	 * This methods compute the BBs within a strip.
	 * 
	 * @param matrix 		matrix containing the strip
	 * @param minMz 			column index containing the initial mz value
	 * @param maxMz 			column index containing the final mz value
	 * @param num_spectra 	number of spectra in the strip
	 * @param strip_number 	the strip number (or ID)
	 * */
	private void compute_BB(ArrayList<ArrayList<MzIntensity>> matrix, float minMz, float maxMz, int num_spectra, int strip_number) {
		/* Base case: a BB has size at most Utils.SIZE_MZ_BB. */
		if (maxMz - minMz > Utils.SIZE_MZ_BB) {// Recursive base case
			compute_BB(matrix, minMz, (maxMz + minMz) / 2, num_spectra, strip_number);
			compute_BB(matrix, (maxMz + minMz) / 2, maxMz, num_spectra, strip_number);
		} else {
			/*
			 * Let's find the BB extremes: top-left corner (min_rt, min_mz) and
			 * bottom-right corner (max_rt, max_mz
			 */
			int min_rt = Integer.MAX_VALUE;
			float min_mz = Float.MAX_VALUE;
			int max_rt = Integer.MIN_VALUE;
			float max_mz = Float.MIN_VALUE;
			int non_zero_values = 0;
			int rt_abs = strip_number * num_spectra;
			
			for(int i = 0; i < matrix.size(); i++, rt_abs++){
				ArrayList<MzIntensity> spectrum = matrix.get(i);
				int mzIdxMin = getMzIdx(minMz, spectrum);
				int mzIdxMax = getMzIdx(maxMz, spectrum)-1;
				for (int j = mzIdxMin; j <= mzIdxMax; j++) {
					non_zero_values++;
					if (min_rt > rt_abs) min_rt = rt_abs;
					if (max_rt < rt_abs) max_rt = rt_abs;
					float currentMz = spectrum.get(j).getMz();
					if (min_mz > currentMz) min_mz = currentMz;
					if (max_mz < currentMz) max_mz = currentMz;
				}
			}
		
			// If the BB is not empty append it to the list pointed by tail
			if (non_zero_values > 0) {
				float density = (float) non_zero_values / (float) ((max_mz + 1 - min_mz) * (max_rt + 1 - min_rt));
				bb_number++;
				tail.next = new BBnode();
				tail = tail.next;
				tail.min_mz = min_mz;
				tail.max_mz = max_mz;
				tail.min_rt = min_rt;
				tail.max_rt = max_rt;
				tail.is_sparse = true;
				tail.non_zero_values = non_zero_values;
			}
		}
	}

	/**
	 * Check if the mzRTree is ready. The other public methods cannot be called if
	 * the mzRTree is not ready.
	 * 
	 * @return true if the mzRTree is ready, false otherwise. 
	 * */
	public boolean ready() {
		return mzRTree_ready;
	}

	/**
	 * @return URL of the input mzXML file
	 * 
	 * @throws MzRTreeException if the mzRTree is not ready 
	 * */
	public String xml_input_file() throws MzRTreeException{
		if (mzRTree_ready == false) throw new MzRTreeException("Index not ready!");
		return xml_input_file;
	}

	/**
	 * @return URL of the directory containing the output (strips and r-tree)
	 * 
	 * @throws MzRTreeException if the mzRTree is not ready 
	 * */
	public String output_dir() throws MzRTreeException{
		if (mzRTree_ready == false) throw new MzRTreeException("Index not ready!");
		return output_dir;
	}

	/**
	 * @return URL of the directory containing the strips
	 * 
	 * @throws MzRTreeException if the mzRTree is not ready 
	 * */ 
	public String output_data_dir() throws MzRTreeException{
		if (mzRTree_ready == false) throw new MzRTreeException("Index not ready!");
		return output_data_dir;
	}


	/**
	 * @return lower mz values among all of the strips
	 * 
	 * @throws MzRTreeException if the mzRTree is not ready 
	 * */
	public float lowest_mz() throws MzRTreeException{
		if (mzRTree_ready == false) throw new MzRTreeException("Index not ready!");
		return lowest_mz;
	}

	/**
	 * @return higher mz values among all of the strips
	 * 
	 * @throws MzRTreeException if the mzRTree is not ready 
	 * */
	public float highest_mz() throws MzRTreeException{
		if (mzRTree_ready == false) throw new MzRTreeException("Index not ready!");
		return highest_mz;
	}

	/**
	 * @return number of spectra
	 * 
	 * @throws MzRTreeException if the mzRTree is not ready 
	 * */
	public int spectra_number() throws MzRTreeException{
		if (mzRTree_ready == false) throw new MzRTreeException("Index not ready!");
		return spectra_number;
	}

	/**
	 * @return maximum number of spectra per strip
	 * 
	 * @throws MzRTreeException if the mzRTree is not ready 
	 * */
	public int max_spectra_per_strip()  throws MzRTreeException{
		if (mzRTree_ready == false) throw new MzRTreeException("Index not ready!");
		return max_spectra_per_strip;
	}
}
