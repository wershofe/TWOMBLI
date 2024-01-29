package uk.ac.franciscrickinstitute;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.InteractiveCommand;

import javax.swing.*;
import java.io.File;
import java.util.Objects;

//@Plugin(type = InteractiveCommand.class, menuPath = "Plugins>TWOMBLI")
//public class TWOMBLI<T extends RealType<T>> extends InteractiveCommand {
public class TWOMBLI implements PlugIn {

    private static final boolean DEBUG_FLAG = true;

    // This function ensures that our dependencies are present.
    // Inform the user of a failed environment where possible.
    static {
        // TODO: This is a future feature, for now we just rely on the packaged dependencies
        if (!TWOMBLI.DEBUG_FLAG) {
            try {
                String directory_path = IJ.getDirectory("imagej");
                // Check if we are in fiji and our expected plugins are present
                if (!directory_path.contains("null") && directory_path.toLowerCase().contains("fiji")) {
                    File plugins_directory = new File(directory_path + "/plugins");
                    checkDependencies(plugins_directory);
                }
            }

            catch (Exception error) {
                // TODO: Print this to the IJ environment and disable self
                System.out.println("Could not validate correct installation. Please ensure you are in a FIJI environment with the dependencies pre-installed");
                System.out.println(error.getMessage());
            }
        }
    }

    private static void checkDependencies(File plugins_folder) {
        if (!TWOMBLI.pluginPresent(plugins_folder, "OrientationJ_")) {
            throw new RuntimeException("Missing plugin OrientationJ");
        }

        if (!TWOMBLI.pluginPresent(plugins_folder, "ANAMORF")) {
            throw new RuntimeException("Missing plugin ANAMORF");
        }

        if (!TWOMBLI.pluginPresent(plugins_folder, "ij_ridge_detect")) {
            throw new RuntimeException("Missing plugin IJ-Ridgedetection");
        }

        if (!TWOMBLI.pluginPresent(plugins_folder, "MaxInscribedCircles")) {
            throw new RuntimeException("Missing plugin MaxInscribedCircles");
        }
    }

    private static boolean pluginPresent(File plugins_folder, String plugin_name) {
        return Objects.requireNonNull(plugins_folder.list((dir, name) -> name.contains(plugin_name))).length > 0;
    }

//    public static void main(final String... args) throws Exception {
//        final ImageJ ij = new ImageJ();
//        ij.ui().showUI();
//
//        // Load a dummy dataset
//        final File file = ij.ui().chooseFile(null, "open");
//        if (file == null) {
//            return;
//        }
//
//        // Load the dataset
//        final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());
//        ij.ui().show(dataset);
//        ij.command().run(TWOMBLI.class, true);
//    }

//    @Override
//    public void run() {
//        // TODO: Do something!
//    }

    @Override
    public void run(String s) {
        // Grab our current image or 'invite' the user to open one!
        ImagePlus initialImage = WindowManager.getCurrentImage();
        if (initialImage == null) {
            // Inform the user?
            initialImage = IJ.openImage();
            if (initialImage == null) {
                return;
            }
        }

        ImageStack twombliImageStack = initialImage.getImageStack().duplicate();
        ImagePlus displayImage = new ImagePlus(initialImage.getTitle(), twombliImageStack);
        displayImage.setTitle("TWOMBLI");
        displayImage.setSlice(initialImage.getCurrentSlice());

        // Hide the old image
        initialImage.getWindow().setVisible(false);

        // TODO: Evaluate if we need to exclude 3d images
        // TODO: Exclude images?
        // TODO: Bulk/batch

        // Display our GUI for this plugin
        SwingUtilities.invokeLater(
                () -> {
                    TWOMBLIWindow window = new TWOMBLIWindow(displayImage);
                    window.pack();
                }
        );
    }
}
