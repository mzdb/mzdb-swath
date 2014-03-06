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

import java.util.Arrays;

import org.systemsbiology.jrap.MSXMLParser;
import org.systemsbiology.jrap.Scan;


public class TestMzXML {

		private static final String INPUT_XML_FILE = "/Users/saran/Desktop/napedro_L120417_006_SW_human_oldMsConvert.mzXML";
		
		private static final String DB_PATH = "/Users/saran/work/swath/swathFullFile.db";


		
		private static final int STRIP_SPECTRA_NUMBER = 86;
		private static final int MSLEVEL = 1;


		private static final float MZI = (float)400;
		private static final float MZF = (float)1200;
		private static final int SWATH_NUMBER = 32;
		private static final int SWATH_WIDTH = 25;

		public static void main(String args[]) throws Exception {
			Runtime.getRuntime().exec("purge");

		
			long totTime = 0;
			long tmp=0;
			long startTime = 0;
			long endTime = 0;
			int iteraz = 10;
			long totTimeLoadmzRTreeDB = 0;
			long totTimemzRTreeDB60rt50ppm = 0;
			long totTimemzRTreeDB100rt50ppm = 0;
			long totTimeLoadmzXML = 0;
			long totTimeMzXML60rt50ppm = 0;
			long totTimeMzXML100rt50ppm = 0;
			DBmzRTree mzRTreedb = new DBmzRTree(DB_PATH, false);
			int currentSwathNumber = 0;
			int totalNumberOfSwaths = mzRTreedb.getSwathNumber();

			try{

				System.gc();
				startTime = System.nanoTime();
				int RT =  2300;		
				float mzPrecursor=400; //TODO
				endTime = System.nanoTime();
				totTimeLoadmzRTreeDB += (endTime-startTime);

//				Runtime.getRuntime().exec("purge");

				System.gc();
				startTime = System.nanoTime();
				MSXMLParser parser = new MSXMLParser(INPUT_XML_FILE);
				System.out.println("numero di scan totale " + parser.getScanCount());
				endTime = System.nanoTime();
				totTimeLoadmzXML = (endTime-startTime);

				for(int j=0;j<iteraz;j++){

					float mzi = MZI+50;
					float mzf = MZF-50;
					float error = 0;
					int ppm = 50;
					int scani=10;
					int scanf=RT-10;
					int offsetMz = 100;
					int offsetRt = 100;
				

					////////////ACCESS 200RTx50ppm (XIC BASED ACCESS)//////////////////
					for(int k=0;k<SWATH_NUMBER;k++){
						for(int i=0;i<10;i++){

							mzi= (float) 600+i*offsetMz;
							error=mzi/((1/ppm)*10^6);
							mzf= mzi + error;
							scani=100+i*offsetRt;
							scanf=300+i*offsetRt;
							mzPrecursor = 405 + k*SWATH_WIDTH;
							currentSwathNumber = mzRTreedb.getcurrentSwathNumber(mzPrecursor);

							System.gc();
							startTime = System.nanoTime();
							xmlAccess(parser,scani, scanf, mzi, mzf,currentSwathNumber,totalNumberOfSwaths);
							endTime = System.nanoTime();
							tmp=endTime-startTime;
							totTimeMzXML100rt50ppm = (totTimeMzXML100rt50ppm + tmp);
							System.gc();
//							Runtime.getRuntime().exec("purge");
						}
					}

					////////////ACCESS 60RTx50ppm //////////////////

					for(int k=0;k<SWATH_NUMBER;k++){
						for(int i=0;i<10;i++){

							mzi= (float) 600+i*offsetMz;
							error=mzi/((1/ppm)*10^6);
							mzf= mzi + error;
							scani=100+i*offsetRt;
							scanf=160+i*offsetRt;
							mzPrecursor = 405 + k*SWATH_WIDTH;
							currentSwathNumber = mzRTreedb.getcurrentSwathNumber(mzPrecursor);
							

							System.gc();
							startTime = System.nanoTime();
							xmlAccess(parser,scani, scanf, mzi, mzf,currentSwathNumber,totalNumberOfSwaths);
							endTime = System.nanoTime();
							tmp=endTime-startTime;
							totTimeMzXML60rt50ppm = (totTimeMzXML60rt50ppm + tmp);
							System.gc();
//							Runtime.getRuntime().exec("purge");
						}
					}
				}
				
				System.out.println("Time Load mzXML = "
						+ (totTimeLoadmzXML/iteraz) / Math.pow(10, 9));

				System.out.println("Time mzXML  (60Rtx5Da-Chroms) = "
						+ (totTimeMzXML60rt50ppm/iteraz) / Math.pow(10, 9));

				System.out.println("Time mzXML  (200Rtx5Da-Chroms) = "
						+ (totTimeMzXML100rt50ppm/iteraz) / Math.pow(10, 9));
				System.gc();
				Runtime.getRuntime().exec("purge");
				mzRTreedb.getConnection().close();


			} catch(Exception ex){
				ex.printStackTrace();
			}
			finally{
				mzRTreedb.getConnection().close();
				Runtime.getRuntime().exec("purge");
			}
		}

		public static void xmlAccess(MSXMLParser parser, int rti, int rtf,
				float mzi, float mzf, int swathNumber, int totalNumberOfSwaths) {
			int swathXMLScan = 0;			
			for(int i=rti;i<rtf;i++){
				swathXMLScan=swathNumber+(i-1)*totalNumberOfSwaths;
				Scan current_scan = parser.rap(swathXMLScan);
				float[] massList = current_scan.getMassIntensityList()[0];
				int mziIndex = Arrays.binarySearch(massList, mzi);
				int mzfIndex = Arrays.binarySearch(massList, mzf);
				for( int j = mziIndex; j <= mzfIndex; j++) {
					float currentMz = massList[j];
				}
			}
		}
	}
