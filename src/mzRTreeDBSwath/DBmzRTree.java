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

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.SynchronousMode;

public class DBmzRTree{

	private static final byte[] SPARSE_MATRIX = { 'S', 'P', 'A', 'R' };
	private String filePath;
	private SQLiteConfig config;
	private Connection connection;

	public DBmzRTree(String filePath, boolean createDB) throws SQLException, ClassNotFoundException {		
		this.filePath = filePath;
		this.config = new SQLiteConfig();
		this.config.setSynchronous(SynchronousMode.OFF);
		if (createDB){
			try {
				this.createDB();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else{
			connection = this.getConnection();
		}
	}

	/**
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		if (connection == null){
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			connection = DriverManager.getConnection("jdbc:sqlite:" + this.filePath);
			// SQLite optimization (not good to do it at every connection call!!)
			connection.createStatement().execute("PRAGMA journal_mode=WAL");
			connection.createStatement().execute("PRAGMA synchronous=OFF");
			connection.setAutoCommit(false); // only required if autocommit state not known
		}
		return connection;
	}

	public void createDB() throws ClassNotFoundException, SQLException{
		Connection connection = this.getConnection();
		Statement stat = connection.createStatement();

		stat.executeUpdate("CREATE VIRTUAL TABLE BBs USING rtree (" +
				"ID  INTEGER NOT NULL," +
				"minScan  INTEGER NOT NULL," +
				"maxScan  INTEGER NOT NULL," +
				"minMz  FLOAT NOT NULL," +
				"maxMz  FLOAT NOT NULL," +
				"minMzPrec  FLOAT NOT NULL," +
				"maxMzPrec  FLOAT NOT NULL)");

		stat.executeUpdate("CREATE TABLE DATA (" +
				"BBs_ID  INTEGER NOT NULL," +
				"BBsData  BLOB NOT NULL," +
				"PRIMARY KEY  (BBs_ID))");

		stat.executeUpdate("CREATE TABLE METADATA (" +
				"lowest_mz  FLOAT NOT NULL,"+
				"highest_mz  FLOAT NOT NULL,"+
				"maxScanNumber INT NOT NULL)");

		stat.executeUpdate("CREATE TABLE SCAN_RT (" +
				"scanNumberAllLevels  INT NOT NULL," +
				"retTime  FLOAT NOT NULL," +
				"PRIMARY KEY  (scanNumberAllLevels))");

		stat.executeUpdate("CREATE TABLE SWATHS (" +
				"minMz  FLOAT NOT NULL," +
				"maxMz  FLOAT NOT NULL," +
				"PRIMARY KEY  (minMz,maxMz))");

		stat.close();
		connection.commit();
	}



	public void insertData(int currentScan, float currentMz, int bb_number, float currentIonCounts, PreparedStatement prepStat) throws SQLException{
		prepStat.setInt(1, currentScan);
		prepStat.setFloat(2, currentMz);
		prepStat.setInt(3, bb_number);
		prepStat.setFloat(4, currentIonCounts);
		prepStat.addBatch();
	}

	public void insertBB(int bb_number, int min_rt, int max_rt, float min_mz, float max_mz, float min_prec, float max_prec, PreparedStatement prepStat) throws SQLException {
		prepStat.setInt(1, bb_number);
		prepStat.setInt(2, min_rt);
		prepStat.setInt(3, max_rt);
		prepStat.setFloat(4, min_mz);
		prepStat.setFloat(5, max_mz);
		prepStat.setFloat(6, min_prec);
		prepStat.setFloat(7, max_prec);
		prepStat.addBatch();
	}

	public void insertBBsToSpectrumList(int currentScan, int bb_number,PreparedStatement prepStat) throws SQLException {
		prepStat.setInt(1, currentScan);
		prepStat.setInt(2, bb_number);
		prepStat.addBatch();
	}


	public void insertMapScanToRT(int scanAbs, float retTime, PreparedStatement prepStat) throws SQLException {
		prepStat.setInt(1, scanAbs);
		prepStat.setFloat(2, retTime);
		prepStat.addBatch();
	}

	public void insertSwathInfo(PreparedStatement prepStat, Float inMz, Float finMz) throws SQLException {
		prepStat.setFloat(1, inMz);
		prepStat.setFloat(2, finMz);
		prepStat.addBatch();		
	}

	public void insertBBsData(PreparedStatement prepStat, int BB_ID, ByteArrayOutputStream binaryBBsData) throws SQLException {
		prepStat.setInt(1, BB_ID);
		prepStat.setBytes(2, binaryBBsData.toByteArray());	
		prepStat.addBatch();
	}

	public void setMetadata(float lowest_mz, float highest_mz, int spectra_number) throws SQLException {
		Connection connTomzRTreeDB=this.getConnection();
		Statement stat=connTomzRTreeDB.createStatement();
		stat.executeUpdate("INSERT INTO METADATA VALUES (" +lowest_mz+ "," +highest_mz+"," +spectra_number+")");
		connTomzRTreeDB.commit();
		stat.close();
	}

	public int getMaxScanNumber() throws SQLException {
		return runQuery("SELECT maxScanNumber FROM METADATA").getInt("maxScanNumber");
	}

	public float getLowestMz() throws SQLException {
		return runQuery("SELECT lowest_mz FROM METADATA").getFloat("lowest_mz");
	}

	public float getHighestMz() throws SQLException {
		return runQuery("SELECT highest_mz FROM METADATA").getFloat("highest_mz");
	}

	public int getMzResolution() throws SQLException {
		return runQuery("SELECT mzResolution FROM METADATA").getInt("mzResolution");
	}

	public int getSwathNumber() throws SQLException {
		return runQuery("SELECT COUNT(*) FROM SWATHS").getInt(1);
	}


	public float[] getSwathRange(float swathPrecursor) throws SQLException {
		float[] range = new float[]{0F,25F};
		if(swathPrecursor != -1){
			ResultSet res = runQuery("SELECT * FROM SWATHS " +
					" WHERE minMz <= "+ swathPrecursor +
					" AND maxMz >= "+ swathPrecursor);
			res.next();
			range[0] = res.getFloat("minMz");
			range[1] = res.getFloat("maxMz");
			res.close();
		}
		return range;
	}

	public float getPrecMax(float swathPrecursor) throws SQLException{
		float[] range = getSwathRange(swathPrecursor);
		return range[1];
	}

	public int getcurrentSwathNumber(float swathPrecursor) throws SQLException{
		return runQuery("SELECT COUNT(*) FROM SWATHS"+
				" WHERE minMz <= "+ swathPrecursor).getInt(1);
	}

	public ResultSet getBBsList(int scan_i, int scan_f, float mz_i, float mz_f, float mzPrecursor) throws SQLException {
		return runQuery("SELECT minScan, maxScan, minMz, maxMz, BBsData FROM BBs, DATA"+
				" WHERE BBs_ID = ID "+
				" AND minScan <=" +scan_f+" AND maxScan>="+ scan_i+" AND minMz<="+ mz_f+" AND maxMz>=" + mz_i + " AND minMzPrec<="+ mzPrecursor+" AND maxMzPrec>" + mzPrecursor + " ORDER BY minMz"); // + " ORDER BY BBs_ID"); // sorting not needed because of the data mapped to the matrix
	}

	public int[] getScanfromRT(float rt_i, float rt_f, int swathNumber) throws SQLException {
		String query= "SELECT MIN(scanNumberAllLevels) AS scan_i, MAX(scanNumberAllLevels) AS scan_f FROM SCAN_RT"+
				" WHERE retTime >=" + rt_i + " AND retTime <=" +rt_f + 
				" AND ((scanNumberAllLevels - " + swathNumber + ") % " + getSwathNumber() + " =0 ) " +
				"ORDER BY scanNumberAllLevels";
		ResultSet res=runQuery(query);
		System.out.println(query);
		int[] absScanRange = new int[2];
		res.next();
		absScanRange[0]=res.getInt("scan_i");
		absScanRange[1]=res.getInt("scan_f");
		System.out.println(absScanRange[0]);
		System.out.println(absScanRange[1]);
		return absScanRange;	
	}

	public float[] getRTsInRange(float rt_i, float rt_f, float mzPrecursor) throws SQLException {
		float precMax= getPrecMax(mzPrecursor);
		int currentSwathNumber= getcurrentSwathNumber(mzPrecursor);
		String query= "SELECT retTime FROM SCAN_RT"+
				" WHERE retTime >=" + rt_i + " AND retTime <=" +rt_f + 
				" AND (scanNumberAllLevels % " + getSwathNumber() + " == " + currentSwathNumber + " ) " +
				" ORDER BY retTime";
		ResultSet res=runQuery(query);
		ArrayList<Float> rTs=new ArrayList<Float>();
		while (res.next()){
			rTs.add(res.getFloat("retTime"));
		}
		return ArrayUtils.toPrimitive(rTs.toArray(new Float[rTs.size()]));
	}

	public ResultSet runQuery(String query) throws SQLException{
		Statement stat = this.getConnection().createStatement();
		return stat.executeQuery(query); 
	}

	private ResultSet runQuery(String query, int resultSetType, int resultSetConcurrency) throws SQLException{
		Statement stat = null;
		try{
			stat=getConnection().createStatement(resultSetType, resultSetConcurrency);
			return stat.executeQuery(query);
		} catch(SQLException ex){
			throw ex;
		}
	}

	public ArrayList<MzIntList> range_query(float rt_i, float rt_f, float mz_i, float mz_f, float mzPrecursor) throws MzRTreeException, SQLException {
		// access through retention times
		float precMax= getPrecMax(mzPrecursor);
		int currentSwathNumber= getcurrentSwathNumber(mzPrecursor);

		int[] scans = getScanfromRT( rt_i,  rt_f,currentSwathNumber);
		if (scans[0] <= 0 || scans[1] <= 0)
			throw(new MzRTreeException("Scan # is <=0 : " + "scans[0] = " + scans[0] + "scans[1] = " +scans[1] ));

		return range_query(scans[0]-1, scans[1]-1, mz_i, mz_f, mzPrecursor);
	}


	public ArrayList<MzIntList> range_query(int scan_i, int scan_f, float mzi, float mzf, float mzPrecursor) throws MzRTreeException, SQLException {
		// access through scan numbers (relative to the MS level) 

		ResultSet listBBs = this.getBBsList( scan_i,  scan_f,  mzi, mzf, mzPrecursor);

		int buffer_pointer = 0;

		ArrayList<DBmzRTree.MzIntList> matrix = new ArrayList<DBmzRTree.MzIntList>(scan_f - scan_i+1);
		for (int i=0; i<(scan_f - scan_i+1); i++){
			matrix.add(new MzIntList());
		}

		float data;

		// some variables for SPARSE case
		int tmp2 = 0, num_byte, col, row;
		float rel_mz_idx, curr_mz;
		// some variables for DENSE case
		int num_col_mzs, start_scan, end_scan, start_mz, end_mz, skipped_mzs, skipped_scans, idx_rel_mzi, idx_rel_mzf, idx_rel_scan_i, idx_rel_scan_f,min_scan_BB,max_scan_BB, min_mz_BB, max_mz_BB, num_BB;
		// General variables
		int i, j, r ; //, idx_curr_byte;

		int counter_iter = 0;
		int writtenValues = 0;


		while (listBBs.next()) {
			counter_iter +=1;
			//			System.out.println("just read BB # " + counter_iter);
			// read the BB's coordinates
			min_scan_BB=listBBs.getInt("minScan");
			max_scan_BB=listBBs.getInt("maxScan");
			min_mz_BB = (int) listBBs.getFloat("minMz"); //last
			max_mz_BB = (int) listBBs.getFloat("maxMz"); //last
			num_BB = (int) listBBs.getRow();
			//load the BB into the buffer
			byte[] buffer=listBBs.getBytes("BBsData");
			buffer_pointer = 0;
			int idx_curr_byte = 0;

			// how is the BB saved?
			if (Utils.equals_size4(buffer, SPARSE_MATRIX)) { 
				//				System.out.println("SPARSE!!!!");

				buffer_pointer += 4; //MzRTree.SPARSE_MATRIX has size 4 bytes 

				//from which rt do we start?
				start_scan = scan_i >= min_scan_BB ? scan_i : min_scan_BB; 
				//in which rt do we finish?
				end_scan = scan_f <= max_scan_BB ? scan_f: max_scan_BB;

				//the first ignore_rt rows of the BB are not of interest
				skipped_scans = start_scan - min_scan_BB;

				//in which row of the output matrix do we start to copy the current (part of) BB?
				idx_rel_scan_i = start_scan - scan_i;
				//in which row of the output matrix do we finish to copy the current (part of) BB?
				idx_rel_scan_f = end_scan - scan_i;

				i = 0;
				//jump the first bb_rti rows since they do not contain interesting data
				while (i < skipped_scans) {
					buffer_pointer += 4 + Utils.bytesToInt(buffer, buffer_pointer); // length of a scan (spectrum)
					i++;
				}

				//only scan_f-scan_i rows of the BB are of interest
				for (row = idx_rel_scan_i; row <= idx_rel_scan_f; row++) {
					//					System.out.println(" welcome @scan# " + (start_scan+row) + " c/o BB# " + num_BB);
					num_byte = Utils.bytesToInt(buffer, buffer_pointer);
					buffer_pointer += 4;
					tmp2 = buffer_pointer + num_byte;
					while (buffer_pointer < tmp2) {
						curr_mz = Float.intBitsToFloat(Utils.bytesToInt(buffer, buffer_pointer));
						buffer_pointer += 4;
						data = Float.intBitsToFloat(Utils.bytesToInt(buffer, buffer_pointer));
						buffer_pointer += 4;
						rel_mz_idx = curr_mz - mzi;
						//copy only intensities of the mz values in the range of interest
						if (rel_mz_idx > 0 && curr_mz <= mzf) {
							matrix.get(row).mzs.add((float) curr_mz);
							matrix.get(row).intensities.add((float) data);

						}
					}
				}
			}

		}
		return matrix;
	}

	public class MzIntList {

		private final ArrayList<Float> mzs;
		private final ArrayList<Float> intensities;

		public MzIntList(){
			mzs= new ArrayList<Float>();
			intensities= new ArrayList<Float>();
		}

		public Float[] getMzs() {
			Float[] mzsArray = new Float[mzs.size()];
			return mzs.toArray(mzsArray);
		}

		public Float[] getIntensities() {
			Float[] intsArray = new Float[intensities.size()];
			return intensities.toArray(intsArray);
		}

	}

}

