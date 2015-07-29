import ij.plugin.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import java.awt.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;

import org.apache.sanselan.common.ImageMetadata;
import org.apache.sanselan.common.RationalNumber;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.*;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.*;



public class WriteEXIF {
	ImagePlus inImage = null;
	File outImage = null;
	double[] latLon = null;
	// Constructor
	WriteEXIF(ImagePlus inImage, File outImage, double[] latLon) {
		this.outImage = outImage;
		this.inImage = inImage;
		this.latLon = latLon;
	}



    void writeJPEG_WithEXIF() {
    	OutputStream os = null;
    	TiffOutputSet outputSet = null;
    	boolean canThrow = false;
    	outputSet = new TiffOutputSet();
    	String fileName = inImage.getOriginalFileInfo().fileName;
    	String fileDir = inImage.getOriginalFileInfo().directory;
    	File inImageFile = new File(fileDir+fileName);
    	
   

    	
    	try {
    		outputSet.setGPSInDegrees(latLon[1], latLon[0]);
    		os = new FileOutputStream(outImage);
    		os = new BufferedOutputStream(os);
    		
    		 new ExifRewriter().updateExifMetadataLossless(inImageFile, os, outputSet);
    	}
    	catch (ImageWriteException e) {
			e.printStackTrace();
			IJ.error("Error adding GPS metadata to file \n" + this.outImage.getAbsolutePath());
		}
    	catch(FileNotFoundException e) {
			e.printStackTrace();
			IJ.error("Error adding GPS metadata to file \n" + this.outImage.getAbsolutePath());
    	}
    	catch(IOException e) {
			e.printStackTrace();
			IJ.error("Error adding GPS metadata to file \n" + this.outImage.getAbsolutePath());
    	}
    	catch(ImageReadException e) {
			e.printStackTrace();
			IJ.error("Error adding GPS metadata to file \n" + this.outImage.getAbsolutePath());
    	}
    	

    }


}