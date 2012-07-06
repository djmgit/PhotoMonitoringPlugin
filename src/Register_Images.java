import java.io.*;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.image.*;

public class Register_Images implements PlugIn {
	static final int ADD=1, SUBTRACT=2, MULTIPLY=3, DIVIDE=4;
	public void run(String arg) {
		ImagePlus rawSourceImage = null;
		ImagePlus rawTargetImage = null;
		ImagePlus processSourceImage = null;
		ImagePlus processTargetImage = null;
		ImagePlus regSource = null;
		RegImagePair regImages = null;
		ImagePlus ndviImage = null;
		String regMethod;
		String transformation;
		String preprocessingMethod;
		Boolean createNGR;
		Boolean createNDVIColor;
		Boolean createNDVIFloat;
		Boolean outputClipTwo;
		String fileType = "";
		String lutName = "";
		String logName = "log.txt";
		String outFileBase = "";
		boolean continueProcessing = true;
//		String redBandString = ""; 
//		String irBandString = "";
		int redBand, irBand;
		
		// Dialog variables
		String[] regMethodTypes = {"Try SIFT", "SIFT", "bUnwarpJ"};
		String[] transformationTypes = {"Rigid", "Affine"};
		String[] preprocessingType = {"nir (g+b) vis (g-b)", "none"}; 
		String[] ndviBands = {"1", "2", "3"};
		String[] outputImageTypes = {"tiff", "jpeg", "gif", "zip", "raw", "avi", "bmp", "fits", "png", "pgm"};
		// Get list of LUTs
		String lutLocation = IJ.getDirectory("luts");
		File lutDirectory = new File(lutLocation);
		String[] lutNames = lutDirectory.list();
		
		// Create dialog window
		GenericDialog dialog = new GenericDialog("Enter variables");
		dialog.addChoice("Select registration method", regMethodTypes, regMethodTypes[0]);
		dialog.addChoice("Select transformation type if using SIFT", transformationTypes, transformationTypes[0]);
		dialog.addChoice("Method to improve point selection", preprocessingType, preprocessingType[0]);
		dialog.addCheckbox("Create NGR image?", true);
		dialog.addCheckbox("Create Color NDVI image?", true);
		dialog.addCheckbox("Create floating point NDVI image?", true);
		dialog.addCheckbox("Output clipped visible image?", true);
		dialog.addChoice("Output image type", outputImageTypes, outputImageTypes[0]);
		dialog.addChoice("Channel (1-3) from visible image to use for Red band to create NDVI", ndviBands, ndviBands[2]);
		dialog.addChoice("Channel (1-3) from IR image to use for IR band to create NDVI", ndviBands, ndviBands[2]);
		dialog.addChoice("Select output color table for color NDVI image", lutNames, lutNames[0]);
		dialog.showDialog();
		if (dialog.wasCanceled()) {
			return;
		}
		
		// Get variables from dialog
		regMethod = dialog.getNextChoice();
		transformation = dialog.getNextChoice();
		preprocessingMethod = dialog.getNextChoice();
		createNGR = dialog.getNextBoolean();
		createNDVIColor = dialog.getNextBoolean();
		createNDVIFloat = dialog.getNextBoolean();
		outputClipTwo = dialog.getNextBoolean();
		fileType = dialog.getNextChoice();
		//redBandString = dialog.getNextChoice();
		//irBandString = dialog.getNextChoice();
		redBand = Integer.parseInt(dialog.getNextChoice());
		irBand = Integer.parseInt(dialog.getNextChoice());
		lutName  = dialog.getNextChoice();
		
		
		// Dialog for photo pair list file
		OpenDialog od = new OpenDialog("Photo pair file", arg);
	    String pairDirectory = od.getDirectory();
	    String pairFileName = od.getFileName();
	    if (pairFileName==null) {
	    	IJ.error("No file was selected");
	    	return;
	    }
		
		// Dialog for output photos directory and log file name
		SaveDialog sd = new SaveDialog("Output directory", "log", ".txt");
	    String outDirectory = sd.getDirectory();
	    logName = sd.getFileName();
	    if (logName==null){
	    	IJ.error("No directory was selected");
	    	return;
	    }
	    
	    // Get photoPairs
	    FilePairList photoPairs = new FilePairList(pairDirectory, pairFileName);
	    boolean noPoints = true;
	    
	    // Start processing one image pair at a time
	    try {
	    	BufferedWriter bufWriter = new BufferedWriter(new FileWriter(outDirectory+logName));
	    
	    	for (FilePair filePair : photoPairs) {
	    		// Open image pairs
	    		continueProcessing = true;
	    	
	    		rawSourceImage = new ImagePlus(filePair.getFirst().trim());
	    		rawTargetImage = new ImagePlus(filePair.getSecond().trim());
	    		outFileBase = rawTargetImage.getTitle().replaceFirst("[.][^.]+$", "");
	    	
	    		// Make sure images are RGB
	    		if (rawSourceImage.getType() != ImagePlus.COLOR_RGB | rawTargetImage.getType() != ImagePlus.COLOR_RGB) {
	    			IJ.error("Images must be Color RGB");
	    			return;  
	    		}
	    	
	    		rawSourceImage.show();
	    		rawTargetImage.show();
	
	    		if (regMethod == "SIFT" || regMethod == "Try SIFT") {
	    			IJ.log("Processing "+rawTargetImage.getTitle()+" and "+rawSourceImage.getTitle()+" using SIFT and landmark correspondences");
	    			int trys = 1;
	    			noPoints = true;
	    			
	    			// Do pre-processing if requested
	    			if (preprocessingMethod == "nir (g+b) vis (g-b)") {
	    				processSourceImage = channelMath((ColorProcessor)rawSourceImage.getProcessor(), 2, 3, ADD);
	    				processTargetImage = channelMath((ColorProcessor)rawTargetImage.getProcessor(), 2, 3, SUBTRACT);				
	    			} 
	    			else if (preprocessingMethod == "none") {
	    				processSourceImage = rawSourceImage;
	    				processTargetImage = rawTargetImage;
	    			}
	    			processSourceImage.show();
	    			processTargetImage.show();
	    	
	    			processSourceImage.setTitle("sourceProcessed");
	    			processTargetImage.setTitle("targetProcessed");
	    	
	    			// Try 5 times to get a set of match points for the transformation
	    			while (trys <= 5 && noPoints) {
	    				IJ.log("Number of times trying SIFT: "+trys);
	    				// Get match points using SIFT
	    				IJ.run("Extract SIFT Correspondences", "source_image="+processTargetImage.getTitle()+" target_image="+
	    						processSourceImage.getTitle()+" initial_gaussian_blur=1.60    " +
	    								"steps_per_scale_octave=3 minimum_image_size=64 maximum_image_size=1024 " +
	    								"feature_descriptor_size=4 feature_descriptor_orientation_bins=8 closest/   " +
	    								"next_closest_ratio=0.92 filter maximal_alignment_error=25 " +
	    								"minimal_inlier_ratio=0.05 minimal_number_of_inliers=7 expected_transformation="+
	    								transformation);
	    				if (processSourceImage.getRoi() != null) {
	    					if (processTargetImage.getRoi().getType() == Roi.POINT) {
	    						noPoints = false;
	    					}
	    				}
	    				trys = trys + 1;
	    			}
	    	
	    			rawSourceImage.setRoi(processSourceImage.getRoi(), false);
	    			rawTargetImage.setRoi(processTargetImage.getRoi(), false);
	    			processSourceImage.close();
	    			processTargetImage.close();
	    			rawSourceImage.show();
	    			rawTargetImage.show();
	    	
	    			IJ.run("Landmark Correspondences", "source_image="+rawSourceImage.getTitle()+" template_image="+
	    					rawTargetImage.getTitle()+" transformation_method=[Moving Least Squares (non-linear)] " +
	    							"alpha=1 mesh_resolution=32 transformation_class="+transformation+
	    							" interpolate");
	    			regSource = WindowManager.getImage("Transformed"+rawSourceImage.getTitle());
	    			bufWriter.write("Images "+rawTargetImage.getTitle()+" and "+rawSourceImage.getTitle()+" " +
	    					"registered using SIFT and landmark correspondences with "+transformation+" transformation\n");
	    		}
	    		else if (regMethod == "Try SIFT") {
	    			noPoints = true;
	    			IJ.log("No points generated for landmark correspondence - trying bUnwarpJ");
	    		}
	    		else {
	    			bufWriter.write("SIFT was not able to create points for image pair "+rawTargetImage.getTitle()+" and "+
	    					rawSourceImage.getTitle()+" - rerun using bUnwarpJ option\n");
	    			IJ.log("SIFT was not able to create points for image pair "+rawTargetImage.getTitle()+" and "+
	    					rawSourceImage.getTitle()+" - rerun using bUnwarpJ option");
	    		}
	    
	    		if (regMethod == "bUnwarpJ" || (regMethod == "Try SIFT" && noPoints)) {
	    			IJ.log("Processing "+rawSourceImage+" and "+rawTargetImage+" using bUnwarpJ");
	    			IJ.run("bUnwarpJ", "source_image="+rawSourceImage.getTitle()+" target_image="+rawTargetImage.getTitle()+
	    					" registration=Fast image_subsample_factor=0 initial_deformation=[Very Coarse] " +
	    					"inal_deformation=Fine divergence_weight=0 curl_weight=0 landmark_weight=0 image_weight=1 " +
	    					"consistency_weight=10 stop_threshold=0.01");
	    			regSource = new ImagePlus("newSourceImage", WindowManager.getImage("Registered Source Image").getStack().getProcessor(1));
	    			bufWriter.write("Images "+rawTargetImage.getTitle()+" and "+rawSourceImage.getTitle()+
	    					" registered using bUnwarpJ\n");
	    		}
	    		bufWriter.flush();
	    
	    		if (continueProcessing) {
	    			regImages = clipPair(regSource, rawTargetImage);
	    			if (createNDVIFloat | createNDVIColor) {
	    				ndviImage = regImages.calcNDVI(redBand, irBand);
	    			}
	    			
	    				if (createNDVIFloat) {
	    					IJ.save(ndviImage, outDirectory+outFileBase+"_NDVI_Float."+fileType);
	    				}
	    				if (createNDVIColor) {
	    					IndexColorModel cm = null;
	    					LUT lut; 
	    					ImageProcessor colorNDVI = ndviImage.getProcessor().convertToByte(true);
	    					// Get the LUT
	    					try {
	    						cm = LutLoader.open(lutLocation+lutName);
	    					} catch (IOException e) {
	    						IJ.error(""+e);
	    					}
	    					lut = new LUT(cm, 0.0, 255.0);
	    					colorNDVI.setLut(lut);
	    					ImagePlus colorNDVI_Image = new ImagePlus("Color NDVI", colorNDVI);
	    					IJ.save(colorNDVI_Image, outDirectory+outFileBase+"_NDVI_Color."+fileType);
	    				}
	    	
	    				if (outputClipTwo) {
	    					IJ.save(regImages.getSecond(), outDirectory+outFileBase+"_Clipped."+fileType);
	    				}
	    	
	    				if (createNGR) {
	    					ColorProcessor firstCP = (ColorProcessor)regImages.getFirst().getProcessor();
	    					ColorProcessor secondCP = (ColorProcessor)regImages.getSecond().getProcessor();
	    					ColorProcessor colorNGR = new ColorProcessor(regImages.getFirst().getWidth(), regImages.getSecond().getHeight());
	    					colorNGR.setChannel(1, firstCP.getChannel(1, null));
	    					colorNGR.setChannel(2, secondCP.getChannel(1, null));
	    					colorNGR.setChannel(3, secondCP.getChannel(2, null));
	    					ImagePlus ngrImage = new ImagePlus("NGR Image", colorNGR);
	    					IJ.save(ngrImage, outDirectory+outFileBase+"_NGR."+fileType);
	    				}
	    	
	    		}
	    		IJ.run("Close All");
	    	}
	    	bufWriter.close();
	    } catch (Exception e) {
	    	IJ.error("Error writing log file", e.getMessage());
	    	return;
	    }
	}
	
	
	// Method to perform channel math
	public ImagePlus channelMath (ColorProcessor image, int chanA, int chanB, int operator) {
		int width = image.getWidth();
		int height = image.getHeight();
		ImagePlus newImage;
		newImage = NewImage.createFloatImage("resultImage", width, height, 1, NewImage.FILL_BLACK);
		byte[] array1 = image.getChannel(chanA);
		byte[] array2 = image.getChannel(chanB);
		double outPixel = 0.0;

		for (int y=0; y<height; y++) {
            int offset = y*width;
			for (int x=0; x<width; x++) {
				int pos = offset+x;
				double pixel1 = array1[pos] & 0xff;
				double pixel2 = array2[pos] & 0xff;
                switch (operator) {
                	case SUBTRACT: outPixel = pixel1 - pixel2; break;
                	case ADD: outPixel = pixel1 + pixel2; break;
                	case MULTIPLY: outPixel = pixel1 * pixel2; break;
                	case DIVIDE: outPixel = pixel2!=0.0?pixel1/pixel2:0.0; break;
                }
                	newImage.getProcessor().putPixelValue(x, y, outPixel);
            }
		}
		return newImage;
	}
	
	//Method to clip image pair
	public RegImagePair clipPair (ImagePlus clipSourceImage, ImagePlus otherImage) {
		if ((clipSourceImage.getHeight() != otherImage.getHeight()) | (clipSourceImage.getWidth() != otherImage.getWidth())) {
			return null;
		}
		ImageProcessor byteImage = clipSourceImage.getProcessor().convertToByte(true);
		ImagePlus croppedSource = null;
		ImagePlus croppedOther = null;
		int height = clipSourceImage.getHeight();
		int width = clipSourceImage.getWidth();
		int xmin = 0;
		int ymin = 0;
		int xmax = width - 1;
		int ymax = height - 1;
		boolean topHasNoData = true;
		boolean rightHasNoData = true;
		boolean bottomHasNoData = true;
		boolean leftHasNoData = true;
	    int sidesOK = 0;
		
	    while (sidesOK < 4) {
	    	double proportionNoData = 0.0;
	    	String moveSide = "";
	    	int numNoData = 0;
	    	
	    	// For each side count the number of no-data pixel in the line or column then calculate percent no-data
	         if (topHasNoData) {
	            numNoData = 0;
	            for (int x=xmin; x<=xmax; x++) {
	               if (byteImage.getPixel(x, ymin) == 0) {
	                   numNoData++;
	               }
	            }
	            if ((numNoData/(double)(xmax+1.0)) > proportionNoData) {
	               proportionNoData = numNoData/(double)(xmax+1.0);
	               moveSide = "top";
	            } else if (numNoData == 0) {
	              topHasNoData = false;
	              sidesOK = sidesOK + 1;
	            }
	         }
	         if (rightHasNoData) {
	            numNoData = 0;
	           for (int y=ymin; y<=ymax; y++) {
	               if (byteImage.getPixel(xmax, y) == 0) {
	                   numNoData++;
	               }
	            }
	            if ((numNoData/(double)(ymax+1.0)) > proportionNoData) {
	               proportionNoData = numNoData/(double)(ymax+1.0);
	               moveSide = "right";
	            } else if (numNoData == 0) {
	              rightHasNoData = false;
	              sidesOK = sidesOK + 1;
	            }
	         }
	         if (bottomHasNoData) {
	            numNoData = 0;
	            for (int x=xmin; x<=xmax; x++) {
	               if (byteImage.getPixel(x, ymax) == 0) {
	                   numNoData++;
	               }
	            }
	            if ((numNoData/(double)(xmax+1.0)) > proportionNoData) {
	               proportionNoData = numNoData/(double)(xmax+1.0);
	               moveSide = "bottom";
	            } else if (numNoData == 0) {
	              bottomHasNoData = false;
	              sidesOK = sidesOK + 1;
	            }
	         }
	         if (leftHasNoData) {
	            numNoData = 0;
	            for (int y=ymin; y<=ymax; y++) {
	               if (byteImage.getPixel(xmin, y) == 0) {
	                   numNoData++;
	               }
	            }
	            if ((numNoData/(double)(ymax+1.0)) > proportionNoData) {
	               proportionNoData = numNoData/(double)(ymax+1.0);
	               moveSide = "left";
	            } else if (numNoData == 0) {
	              rightHasNoData = false;
	              sidesOK = sidesOK + 1;
	            }
	         }
	         // Move the side that has the highest proportion of no-data pixels
	         if (moveSide == "top") {
	            ymin = ymin + 1;
	         }
	         if (moveSide == "right") {
	            xmax = xmax - 1;
	         }
	         if (moveSide == "bottom") {
	            ymax = ymax - 1;
	         }
	         if (moveSide == "left") {
	            xmin = xmin + 1;
	         }
	    }
	      // Calculate selection rectangle
	    clipSourceImage.setRoi(xmin,ymin, xmax - xmin + 1, ymax - ymin + 1);
	    otherImage.setRoi(xmin,ymin, xmax - xmin + 1, ymax - ymin + 1);
	    clipSourceImage.show();
	    otherImage.show();
	    croppedSource = new ImagePlus("croppedFirst", clipSourceImage.getProcessor().crop());
	    croppedOther = new ImagePlus("croppedSecond", otherImage.getProcessor().crop());
	    RegImagePair outPair = new RegImagePair(croppedSource, croppedOther);
	    return outPair;
	}
}


