mzdb-swath
==========

A prototype Java library for the SWATH-MS support in mzDB.


The following libraries are required:

- Java Random Access Parser (JRAP). We tested mzRTree with JRAP 3.0 and 4.0. Available here: 
	
				http://tools.proteomecenter.org/wiki/index.php?title=Software:JRAP
		
 - Jakarta Regexp. We tested mzRTree with version 1.4. Available here: 
 
 				http://jakarta.apache.org/regexp/index.html
 	
 - Xerces2 Java Parser. We tested mzRTree with version 2.6.2. Available here:
 			
 				http://xerces.apache.org/xerces2-j/
 
 
 How does it work in a few words? (More info in MzRTree.java)
 
 1) Create an object MzRTree from an mzXML file using the constructor 
 		
 			MzRTree my_mzrtree = new MzRTree(in_file, out_dir, res, strip),
 	
 	where the String in_file contains the URL of the mzXML file, the String out_dir contains 
 	the URL of the directory where the mzRTree will be stored, the int res is the resolution of 
 	the mz dimension, the int strip is the number of strips in which spectra in the mzXML file 
 	will be partitioned. 
 
 2)	If an mzRTree is stored in a directory, then you can (quickly) create an MzRTree object 
 	using the constructor:
 	
 			MzRTree my_mzrtree = new MzRTree(String in_dir),
 			
 	where the String in_dir contains the URL of the directory containing the mzRTree.
 
 3) For retrieving intensities of mz values (in Da) in (mz_i, mz_f] of spectra whose position 
 	in the mzXML file is k with k in (rt_i, rt_f], use the method   
 
 			float[][] data = my_mzrtree.range_query(rt_i, rt_f, mz_i, mz_f),
 			
 	where rt_i and rt_f are int, mz_i and mz_f float.  
  
