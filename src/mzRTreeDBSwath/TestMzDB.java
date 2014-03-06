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

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;

import mzRTreeDBSwath.DBmzRTree.MzIntList;

import org.sqlite.SQLiteJDBCLoader;
import org.systemsbiology.jrap.MSXMLParser;


public class TestMzDB {

	private static final String OUTPUT_NAME_DB_MEM = "/Users/saran/work/swath/swathFullVarWinSizes2";
	private static final String INPUT_XML_FILE = "/Users/saran/Desktop/napedro_L120417_006_SW_human_oldMsConvert.mzXML";
	private static final int SWATH_NUMBER = 32;
	private static final int SWATH_WIDTH = 25;

	public static void main(String args[]) throws Exception {
		Runtime.getRuntime().exec("purge");
		long tmp=0;
		long startTime = 0;
		long endTime = 0;
		int iteraz = 3;
		long totTimeLoadmzRTreeDB = 0;
		long totTimemzRTreeDB60rt50ppm = 0;
		long totTimemzRTreeDB100rt50ppm = 0;

		DBmzRTree mzRTreeDB = null;


		System.out.println(String.format("running in %s mode", SQLiteJDBCLoader.isNativeMode() ? "native" : "pure-java"));
		File dbFile=new File(OUTPUT_NAME_DB_MEM);

		try{

			// LOAD /////////////////////////////////////

			System.gc();
			startTime = System.nanoTime();
			mzRTreeDB=new DBmzRTree(OUTPUT_NAME_DB_MEM, false);
			float MZI = mzRTreeDB.getLowestMz();
			float MZF = mzRTreeDB.getHighestMz();
			int RT =  2300;//mzRTreeDB.getMaxScanNumber();		
			float mzPrecursor=400; //TODO
			endTime = System.nanoTime();
			totTimeLoadmzRTreeDB += (endTime-startTime);

			System.gc();
			startTime = System.nanoTime();
			MSXMLParser parser = new MSXMLParser(INPUT_XML_FILE);
			endTime = System.nanoTime();

			for(int j=0;j<iteraz;j++){

				float mzi = MZI+50;
				float mzf = MZF-50;
				float error = 0;
				int ppm = 50;
				int scani=10;
				int scanf=RT-10;
				int offsetMz = 100;
				int offsetRt = 500;


				for(int k=0;k<SWATH_NUMBER;k++){
					for(int i=0;i<10;i++){

						mzi= (float) 600+i*offsetMz;
						error=mzi/((1/ppm)*10^6);
						mzf= mzi + error;
						scani=1000+i*offsetRt;
						scanf=1200+i*offsetRt;
						mzPrecursor = 405 + k*SWATH_WIDTH;

						System.gc();
						startTime = System.nanoTime();
						dbAccess(mzRTreeDB, scani, scanf, mzi, mzf,mzPrecursor);
						endTime = System.nanoTime();
						tmp=endTime-startTime;
						totTimemzRTreeDB100rt50ppm = (totTimemzRTreeDB100rt50ppm + tmp);
						System.gc();
					}
				}

				for(int k=0;k<SWATH_NUMBER;k++){
					for(int i=0;i<10;i++){

						mzi= (float) 600+i*offsetMz;
						error=mzi/((1/ppm)*10^6);
						mzf= mzi + error;
						scani=1000+i*offsetRt;
						scanf=1060+i*offsetRt;
						mzPrecursor = 405 + k*SWATH_WIDTH;

						System.gc();
						startTime = System.nanoTime();
						dbAccess(mzRTreeDB, scani, scanf, mzi, mzf,mzPrecursor);
						endTime = System.nanoTime();
						tmp=endTime-startTime;
						totTimemzRTreeDB60rt50ppm = (totTimemzRTreeDB60rt50ppm + tmp);
						System.gc();
					}
				}
			}
			System.out.println("Time Load mzRTreeDB = "
					+ (totTimeLoadmzRTreeDB/iteraz) / Math.pow(10, 9));

			System.out.println("Time mzRTreeDB  (60Rtx5Da-Chroms) = "
					+ (totTimemzRTreeDB60rt50ppm/iteraz) / Math.pow(10, 9));

			System.out.println("Time mzRTreeDB  (200Rtx5Da-Chroms) = "
					+ (totTimemzRTreeDB100rt50ppm/iteraz) / Math.pow(10, 9));

			System.gc();
			Runtime.getRuntime().exec("purge");
		} catch(Exception ex){
			ex.printStackTrace();
		}
		finally{
			if (mzRTreeDB !=null)
				mzRTreeDB.getConnection().close();
			Runtime.getRuntime().exec("purge");
		}
	}

	public static ArrayList<MzIntList> dbAccess(DBmzRTree mzRTreeDB, int rti, int rtf,float mzi, float mzf,	float mzPrecursor) throws MzRTreeException, SQLException, ClassNotFoundException {			
		return mzRTreeDB.range_query(rti, rtf, mzi, mzf, mzPrecursor);
	}

}