import ij.IJ;
import java.io.*;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;



public class FilePairList extends ArrayList<FilePair>{
	//ArrayList<FilePair> filePairs;

	private static final long serialVersionUID = 7570377225978123977L;

	// Constructor from two File arrays, time offsets and acceptable time difference
	FilePairList(File[] listFirst_Files, File[] listSecondFiles, long offset, double acceptableDifference) {
		//ArrayList<FilePair> filePairs = new ArrayList<FilePair>();
		for (int i = 0; i < listSecondFiles.length; i++) { 
			if (listSecondFiles[i].isFile()) { 
		        long timeDiff;
		        long bestTimeDiff;
		        String matchingFirst = null;
		        // Find closest matching file last modified time
		        for (int k = 0; k < listFirst_Files.length; k++) {
		           bestTimeDiff=999999999;
		        	if (listFirst_Files[k].isFile()) {
		               timeDiff = Math.abs(listSecondFiles[i].lastModified() - (listFirst_Files[k].lastModified() + offset));
		               if (timeDiff < bestTimeDiff) {
		             	  bestTimeDiff = timeDiff;
		             	  matchingFirst = listFirst_Files[k].getAbsolutePath();
		               }
		               
		               if (bestTimeDiff <= acceptableDifference) {
		            	   FilePair filePair = new FilePair(matchingFirst, listSecondFiles[i].getAbsolutePath());
		                  this.add(filePair);
		               }
		            }
		        }
		    }    
		}
		this.trimToSize();
//		this.filePairs = filePairs;
	}
	
	// Constructor from a text file
	FilePairList(String dir, String fileName) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(dir+fileName));
			String line;
	      
			line = br.readLine();
			
			while (line!=null) {
				FilePair filePair = new FilePair(line.split(",")[0], line.split(",")[1]);
				this.add(filePair);
				line = br.readLine();
			}
		} catch (Exception e) {
			IJ.error("Error reading file pairs", e.getMessage());
			return;
		}
		
		this.trimToSize();	
	}
	
	// Methods
//	ArrayList<FilePair> getFilePairs() {
//		return this.filePairs;
//	}
	
//	FilePairList add(FilePair filePair) {
//		this.add(filePair);
//		return this;
//	}
	
	// Write contents of FilePairList to a file
	void writeFilePairs(String dir, String fileName) {      
		try { 
			BufferedWriter bw = new BufferedWriter(new FileWriter(dir+fileName));
	         
			for (FilePair photoPair : this) {
				bw.write(photoPair.getFirst()+", "+photoPair.getSecond()+"\n");
			}

			//bw.write("end");
			bw.close();
			IJ.showStatus("File with matching pairs created");
		} 
		catch (Exception e) {
		IJ.error("Error writing file pairs", e.getMessage());
		return;
		}
	}
}
