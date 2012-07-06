import ij.*;
import ij.gui.*;
import ij.process.*;

public class RegImagePair {
	ImagePlus firstImage, secondImage;
	
	// Constructor
	RegImagePair(ImagePlus firstImage, ImagePlus secondImage) {
	      this.firstImage = firstImage;
	      this.secondImage = secondImage;
	}
	
	ImagePlus getFirst() {
		return this.firstImage;
	}
	ImagePlus getSecond() {
		return this.secondImage;
	}

	public ImagePlus calcNDVI (int irChannel, int redChannel){
		int width = this.getFirst().getWidth();
		int height = this.getFirst().getHeight();
		ColorProcessor irImage = (ColorProcessor)this.getFirst().getProcessor();
		ColorProcessor redImage = (ColorProcessor)this.getSecond().getProcessor();
		ImagePlus newImage;
		newImage = NewImage.createFloatImage("ndviImage", width, height, 1, NewImage.FILL_BLACK);
		byte[] irArray = irImage.getChannel(irChannel);
		byte[] redArray = redImage.getChannel(redChannel);
		double outPixel = 0.0;
		for (int y=0; y<height; y++) {
            int offset = y*width;
			for (int x=0; x<width; x++) {
				int pos = offset+x;
				double irPixel = irArray[pos] & 0xff;
				double visPixel = redArray[pos] & 0xff;
				if ((irPixel + visPixel) == 0.0) {
					outPixel = 0.0;
				} else {
				
					outPixel = (irPixel - visPixel)/(irPixel + visPixel);
				}
				newImage.getProcessor().putPixelValue(x, y, outPixel);
			}
		}
		return newImage;		
	}
}

