package uk.ac.franciscrickinstitute;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;

import java.awt.*;

public class TWOMBLIWindow extends StackWindow {

    private static final double MAX_MAGNIFICATION = 32.0;
    private static final double MIN_MAGNIFICATION = 1/72.0;

    public TWOMBLIWindow(ImagePlus imp) {
        super(imp, new ImageCanvas(imp));
        this.zoomImage();
    }

    private void zoomImage() {
        // Adjust our screen positions + image sizes
        final ImageCanvas canvas = (ImageCanvas) this.getCanvas();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();

        // Zoom in if our image is small
        while ((this.ic.getWidth() < width / 2 || this.ic.getHeight() < height / 2)
                && this.ic.getMagnification() < TWOMBLIWindow.MAX_MAGNIFICATION) {
            final int canvasWidth = this.ic.getWidth();
            this.ic.zoomIn(0, 0);
            if (canvasWidth == this.ic.getWidth()) {
                this.ic.zoomOut(0, 0);
                break;
            }
        }

        // Zoom out if our image is large
        while ((this.ic.getWidth() > 0.75 * width || this.ic.getHeight() > 0.75 * height)
                && this.ic.getMagnification() > TWOMBLIWindow.MIN_MAGNIFICATION) {
            final int canvasWidth = this.ic.getWidth();
            this.ic.zoomOut(0, 0);
            if (canvasWidth == this.ic.getWidth()) {
                this.ic.zoomIn(0, 0);
                break;
            }
        }

        this.setTitle("TWOMBLI");


    }
}
