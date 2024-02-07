package uk.ac.franciscrickinstitute;

import java.io.File;
import java.util.Objects;

//import ij.IJ;
import ij.IJ;
import net.imagej.ImageJ;

public class TWOMBLI {

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


    // Entrant for debugging via IDE
    public static void main(final String... args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.launch(args);
        ij.command().run(TWOMBLIConfigurator.class, true);
    }
}
