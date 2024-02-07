package uk.ac.franciscrickinstitute;

import ij.ImagePlus;
import ij.ImageStack;

public class ImageUtils {

    public static ImagePlus duplicateImage(final ImagePlus inputImage) {
        ImageStack copyImageStack = inputImage.getImageStack().duplicate();
        ImagePlus dataImage = new ImagePlus(inputImage.getTitle(), copyImageStack);
        dataImage.setFileInfo(inputImage.getOriginalFileInfo());
        dataImage.setSlice(inputImage.getCurrentSlice());
        return dataImage;
    }
}
