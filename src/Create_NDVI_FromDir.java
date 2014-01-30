import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.LUT;
import ij.Prefs;
import ij.gui.DialogListener;
import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.TextField;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.util.Vector;

public class Create_NDVI_FromDir implements PlugIn, DialogListener {
	public void run(String arg) {
		String[] outputImageTypes = {"tiff", "jpeg", "gif", "zip", "raw", "avi", "bmp", "fits", "png", "pgm"};
		String[] ndviBands = {"red", "green", "blue"};	
		// Get list of LUTs
		String lutLocation = IJ.getDirectory("luts");
		File lutDirectory = new File(lutLocation);
		String[] lutNames = lutDirectory.list();
		String logName = "log.txt";
		
		ImagePlus inImagePlus = null;
		ImagePlus ndviImage = null;
		String outFileBase = "";
		int redBand, irBand;
		Boolean saveParameters = true;
		Boolean useDefaults = false;
		
		// Initialize variables from IJ.Prefs file
		String fileType = Prefs.get("pm.fromSBDir.fromSBDir.fileType", outputImageTypes[0]);
		Boolean createNDVIColor = Prefs.get("pm.fromSBDir.createNDVIColor", true);
		Boolean createNDVIFloat = Prefs.get("pm.fromSBDir.createNDVIFloat", true);
		Boolean stretchVisible = Prefs.get("pm.fromSBDir.stretchVisible", true);
		Boolean stretchIR = Prefs.get("pm.fromSBDir.stretchIR", true);
		double saturatedPixels = Prefs.get("pm.fromSBDir.saturatedPixels", 2.0);
		double maxColorScale = Prefs.get("pm.fromSBDir.maxColorScale", 1.0);
		double minColorScale = Prefs.get("pm.fromSBDir.minColorScale", -1.0);
		String lutName = Prefs.get("pm.fromSBDir.lutName", lutNames[0]);
		int redBandIndex = (int)Prefs.get("pm.fromSBDir.redBandIndex", 2); 
		int irBandIndex = (int)Prefs.get("pm.fromSBDir.irBandIndex", 0);
		saturatedPixels = Prefs.get("pm.fromSBDir.saturatedPixels", 2.0);
		
		// Create dialog window
		GenericDialog dialog = new GenericDialog("Enter variables");
		dialog.addCheckbox("Load default parameters (click OK below to reload)", false);
		dialog.addMessage("Output image options:");
		dialog.addChoice("Output image type", outputImageTypes, fileType);
		dialog.addCheckbox("Output Color NDVI image?", createNDVIColor);
		dialog.addNumericField("Minimum NDVI value for scaling color NDVI image", minColorScale, 1);
		dialog.addNumericField("Maximum NDVI value for scaling color NDVI image", maxColorScale, 1);
		dialog.addCheckbox("Output floating point NDVI image?", createNDVIFloat);
		dialog.addCheckbox("Stretch the visible band before creating NDVI?", stretchVisible);
		dialog.addCheckbox("Stretch the NIR band before creating NDVI?", stretchIR);
		dialog.addNumericField("Saturation value for stretch", saturatedPixels, 1);
		dialog.addChoice("Channel for Red band to create NDVI", ndviBands, ndviBands[redBandIndex]);
		dialog.addChoice("Channel for IR band to create NDVI", ndviBands, ndviBands[irBandIndex]);
		dialog.addChoice("Select output color table for color NDVI image", lutNames, lutName);
		dialog.addCheckbox("Save parameters for next session", true);
		dialog.addDialogListener(this);
		dialog.showDialog();
		if (dialog.wasCanceled()) {
			return;
		}
		
		useDefaults = dialog.getNextBoolean();
		if (useDefaults) {
			dialog = null;
			// Create dialog window with default values
			dialog = new GenericDialog("Enter variables");
			dialog.addCheckbox("Load default parameters (click OK below to reload)", false);
			dialog.addMessage("Output image options:");
			dialog.addChoice("Output image type", outputImageTypes, outputImageTypes[0]);
			dialog.addCheckbox("Output Color NDVI image?", true);
			dialog.addNumericField("Enter the minimum NDVI value for scaling color NDVI image", -1.0, 1);
			dialog.addNumericField("Enter the maximum NDVI value for scaling color NDVI image", 1.0, 1);
			dialog.addCheckbox("Output floating point NDVI image?", true);
			dialog.addCheckbox("Stretch the visible band before creating NDVI?", true);
			dialog.addCheckbox("Stretch the NIR band before creating NDVI?", true);
			dialog.addNumericField("Enter the saturation value for stretch", 2.0, 1);
			dialog.addChoice("Channel for Red band to create NDVI", ndviBands, ndviBands[2]);
			dialog.addChoice("Channel for IR band to create NDVI", ndviBands, ndviBands[0]);
			dialog.addChoice("Select output color table for color NDVI image", lutNames, lutNames[0]);
			dialog.addCheckbox("Save parameters for next session", false);
			dialog.addDialogListener(this);
			dialog.showDialog();
			if (dialog.wasCanceled()) {
				return;
			}
		}
		
		// Get variables from dialog
		if (useDefaults) { 
			dialog.getNextBoolean();
		}
		fileType = dialog.getNextChoice();
		createNDVIColor = dialog.getNextBoolean();
		minColorScale = dialog.getNextNumber();
		maxColorScale = dialog.getNextNumber();
		createNDVIFloat = dialog.getNextBoolean();
		stretchVisible = dialog.getNextBoolean();
		stretchIR = dialog.getNextBoolean();
		saturatedPixels = dialog.getNextNumber();
		redBand = dialog.getNextChoiceIndex() + 1;
		irBand = dialog.getNextChoiceIndex() + 1;
		lutName  = dialog.getNextChoice();
		saveParameters  = dialog.getNextBoolean();
	
		if (saveParameters) {
			// Set preferences to IJ.Prefs file
			Prefs.set("pm.fromSBDir.fileType", fileType);
			Prefs.set("pm.fromSBDir.createNDVIColor", createNDVIColor);
			Prefs.set("pm.fromSBDir.createNDVIFloat", createNDVIFloat);
			Prefs.set("pm.fromSBDir.stretchVisible", stretchVisible);
			Prefs.set("pm.fromSBDir.stretchIR", stretchIR);
			Prefs.set("pm.fromSBDir.saturatedPixels", saturatedPixels);
			Prefs.set("pm.fromSBDir.maxColorScale", maxColorScale);
			Prefs.set("pm.fromSBDir.minColorScale", minColorScale);
			Prefs.set("pm.fromSBDir.lutName", lutName);
			Prefs.set("pm.fromSBDir.redBandIndex", redBand - 1);
			Prefs.set("pm.fromSBDir.irBandIndex", irBand - 1);
		
			// Save preferences to IJ.Prefs file
			Prefs.savePreferences();
		}
		
		// Dialog for input photo directory
	    DirectoryChooser inDirChoose = new DirectoryChooser("Input image directory");
        String inDir = inDirChoose.getDirectory();
        if (inDir == null) {
       	 IJ.error("Input image directory was not selected");
       	 return;
        }
        File inFolder = new File(inDir);
        File[] inputImages = inFolder.listFiles();
        
     // Dialog for output photos directory and log file name
     	SaveDialog sd = new SaveDialog("Output directory and log file name", "log", ".txt");
     	String outDirectory = sd.getDirectory();
     	logName = sd.getFileName();
     	if (logName==null){
     	   IJ.error("No directory was selected");
     	   return;
     	}
     	
     	try {
	    	BufferedWriter bufWriter = new BufferedWriter(new FileWriter(outDirectory+logName));
	    	// Write parameter settings to log file
	    	bufWriter.write("PARAMETER SETTINGS:\n");
		    bufWriter.write("Output image type: " + fileType + "\n");
		    bufWriter.write("Output Color NDVI image? " + createNDVIColor + "\n");
		    bufWriter.write("Minimum NDVI value for scaling color NDVI image: " + minColorScale + "\n");
		    bufWriter.write("Maximum NDVI value for scaling color NDVI image: " + maxColorScale + "\n");
		    bufWriter.write("Output floating point NDVI image? " + createNDVIFloat + "\n");
		    bufWriter.write("Stretch the visible band before creating NDVI? " + stretchVisible + "\n");
		    bufWriter.write("Stretch the NIR band before creating NDVI? " + stretchIR + "\n");
		    bufWriter.write("Saturation value for stretch: " + saturatedPixels + "\n");
		    bufWriter.write("Channel from visible image to use for Red band to create NDVI: " + redBand + "\n");
		    bufWriter.write("Channel from IR image to use for IR band to create NDVI: " + irBand + "\n");
		    bufWriter.write("Select output color table for color NDVI image: " + lutName + "\n\n");
	    	bufWriter.close();
	    } catch (Exception e) {
	    	IJ.error("Error writing log file", e.getMessage());
	    	return;
	    }

	    // Start processing one image at a time
	    for (File inImage : inputImages) {
	    	// Open image
	    	inImagePlus = new ImagePlus(inImage.getAbsolutePath());
	    	outFileBase = inImagePlus.getTitle().replaceFirst("[.][^.]+$", "");
	    	
	    	// Make sure images are RGB
	    	if (inImagePlus.getType() != ImagePlus.COLOR_RGB) {
	    		IJ.error("Images must be Color RGB");
	    		return;  
	    	}
	    	
	    	inImagePlus.show();
	    	RegImagePair imagePair = new RegImagePair(inImagePlus, inImagePlus);
	    	ndviImage = imagePair.calcNDVI(irBand, redBand, stretchVisible, stretchIR, saturatedPixels);
	    		
	    	if (createNDVIFloat) {
    			IJ.save(ndviImage, outDirectory+outFileBase+"_NDVI_Float."+fileType);
    		}
	    		
	    	if (createNDVIColor) {
    			IndexColorModel cm = null;
    			LUT lut;
    			// Uncomment next line to use default float-to-byte conversion
    			//ImageProcessor colorNDVI = ndviImage.getProcessor().convertToByte(true);
    			ImagePlus colorNDVI;
    			colorNDVI = NewImage.createByteImage("Color NDVI", ndviImage.getWidth(), ndviImage.getHeight(), 1, NewImage.FILL_BLACK);
    			
    			float[] pixels = (float[])ndviImage.getProcessor().getPixels();
    			for (int y=0; y<ndviImage.getHeight(); y++) {
    	            int offset = y*ndviImage.getWidth();
    				for (int x=0; x<ndviImage.getWidth(); x++) {
    					int pos = offset+x;
    					colorNDVI.getProcessor().putPixelValue(x, y, Math.round((pixels[pos] - minColorScale)/((maxColorScale - minColorScale) / 255.0)));
    				}	    						    				
    			}
    			// Get the LUT
    			try {
    			cm = LutLoader.open(lutLocation+lutName);
    			} catch (IOException e) {
    			IJ.error(""+e);
    			}
    		
    			lut = new LUT(cm, 255.0, 0.0);
    			colorNDVI.getProcessor().setLut(lut);
    			colorNDVI.show();
    			IJ.save(colorNDVI, outDirectory+outFileBase+"_NDVI_Color."+fileType);
    		}
	    		IJ.run("Close All");
	    }
	}
	
	// Method to update dialog based on user selections
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
			Checkbox ndviColorCheckbox = (Checkbox)gd.getCheckboxes().get(1);
			Vector<?> numericChoices = gd.getNumericFields();
			Vector<?> choices = gd.getChoices();
			if (ndviColorCheckbox.getState()) {
				((TextField)numericChoices.get(0)).setEnabled(true);
				((TextField)numericChoices.get(1)).setEnabled(true);
				((Choice)choices.get(3)).setEnabled(true);
			} 
			else {
				((TextField)numericChoices.get(0)).setEnabled(false);
				((TextField)numericChoices.get(1)).setEnabled(false);
				((Choice)choices.get(3)).setEnabled(false);
			}			
			return true;
		}
}


