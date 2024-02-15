package uk.ac.franciscrickinstitute.twombli;

import java.io.File;
import java.util.Objects;

import ij.IJ;
import ij.Macro;
import ij.gui.WaitForUserDialog;

public class Dependencies {

    public static boolean checkRootDependencies() {
        try {
            String directory_path = IJ.getDirectory("imagej");
            // Check if we are in fiji and our expected plugins are present
            if (!directory_path.contains("null") && directory_path.toLowerCase().contains("fiji")) {
                File plugins_directory = new File(directory_path + "/plugins");
                return Dependencies.checkDependencies(plugins_directory);
            }

            // Should be in a dev environment?
            else {
                return true;
            }
        }

        catch (Exception error) {
            // TODO: Print this to the IJ environment and disable self
            System.out.println("Could not validate correct installation. Please ensure you are in a FIJI environment with the dependencies pre-installed");
            System.out.println(error.getMessage());
            return false;
        }
    }

    public static boolean checkDependencies(File plugins_folder) {
        StringBuilder installationInfo = new StringBuilder();
        if (!Dependencies.pluginPresent(plugins_folder, "OrientationJ_")) {
            System.out.println("OrientationJ_ plugin not found. Please install it from the update site.");
            installationInfo.append("Enable the BIG-EPFL (http://sites.imagej.net/BIG-EPFL/) update site.\n");
        }

        if (!Dependencies.pluginPresent(plugins_folder, "anamorf")) {
            System.out.println("ANAMORF plugin not found. Please install it from the update site.");
            installationInfo.append("Enable the CALM (http://sites.imagej.net/CALM/) update site.\n");
        }

        if (!Dependencies.pluginPresent(plugins_folder, "ij_ridge_detect")) {
            System.out.println("ij_ridge_detect plugin not found. Please install it from the update site.");
            installationInfo.append("Enable the Biomedgroup (http://sites.imagej.net/Biomedgroup/) update site.\n");
        }

        if (!Dependencies.pluginPresent(plugins_folder, "MaxInscribedCircles")) {
            System.out.println("MaxInscribedCircles plugin not found. Please install it from the update site.");
            installationInfo.append("Enable the PTBIOP (http://biop.epfl.ch/Fiji-Update/) update site.\n");
        }

        if (!Dependencies.pluginPresent(plugins_folder, "bio-formats_plugins")) {
            System.out.println("BioFormates plugin not found. Please use FIJI!!!");
            installationInfo.append("Only FIJI is supported!\n");
        }

        if (installationInfo.length() > 0) {
            String suffix = "To add an update site see https://imagej.net/update-sites/following. (Help > Update... > Manage update sites > BIG-EPFL > Tick and close the window ).";
            WaitForUserDialog dialog = new WaitForUserDialog("TWOMBLI Installation", "A required plugin is missing. " + installationInfo + suffix);
            dialog.show();
            return false;
        }

        else {
            return true;
        }
    }

    private static boolean pluginPresent(File plugins_folder, String plugin_name) {
        return Objects.requireNonNull(plugins_folder.list((dir, name) -> name.contains(plugin_name))).length > 0;
    }
}
