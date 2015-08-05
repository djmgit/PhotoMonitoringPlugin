import ij.*;
import java.io.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.sanselan.*;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.common.*;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.*;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

public class WriteEXIF {
	File outImageFile = null;
	File originalFile = null;
	//ImagePlus inImage = null;
	File tempImageFile = null;
	//double[] latLon = null;
	// Constructor
	WriteEXIF(File originalFile, File outImageFile, File tempImageFile) {
		this.originalFile = originalFile;
		this.outImageFile = outImageFile;
		this.tempImageFile = tempImageFile;
		//this.latLon = latLon;
		//this.inImage = inImage;
	}



    void copyEXIF() {
    	OutputStream os = null;
    	TiffOutputSet outputSet = null;
    	//outputSet = new TiffOutputSet();    
    	JpegImageMetadata jpegMetadata = null;
    	try {
    		final IImageMetadata metadata = Sanselan.getMetadata(originalFile);
    		if (metadata instanceof JpegImageMetadata) {
    			jpegMetadata = (JpegImageMetadata) metadata;
    		}
            //final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                final TiffImageMetadata exif = jpegMetadata.getExif();
                if (null != exif) {
                    outputSet = exif.getOutputSet();
                }
/*            }
    		
            if (null == outputSet) {
                outputSet = new TiffOutputSet();
            }*/
            
            //outputSet.setGPSInDegrees(latLon[1], latLon[0]);
            
            //BufferedImage bufImage = ImageIO.read(outImageFile);
        	//WritableRaster raster = bufImage .getRaster();
            //DataBufferByte data   = (DataBufferByte) raster.getDataBuffer();
            
            
            //File newFile = new File(outImageFile);
    		os = new FileOutputStream(outImageFile);
    		os = new BufferedOutputStream(os);
    		
    		//ByteArrayOutputStream bos = new ByteArrayOutputStream();
    		new ExifRewriter().updateExifMetadataLossless(tempImageFile, os, outputSet);
    		tempImageFile.delete();
    		
           }
    		
            if (null == outputSet) {
                tempImageFile.renameTo(outImageFile);
            }
    		//byte[] byteData = data.getData();
    		//new ExifRewriter().updateExifMetadataLossless(byteData, bos, outputSet);
    		//BufferedImage outBufImage = new BufferedImage(inImage.getWidth(), inImage.getHeight(), BufferedImage.TYPE_INT_RGB);
    		//ImageIO.write(outBufImage, "jpg", bos);
    	}
    	catch (ImageWriteException e) {
			e.printStackTrace();
			IJ.error("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath());
		}
    	catch(FileNotFoundException e) {
			e.printStackTrace();
			IJ.error("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath());
    	}
    	catch(IOException e) {
			e.printStackTrace();
			IJ.error("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath());
    	}
    	catch(ImageReadException e) {
			e.printStackTrace();
			IJ.error("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath());
    	}
    }
}