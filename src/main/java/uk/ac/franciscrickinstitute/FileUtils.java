package uk.ac.franciscrickinstitute;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import ij.IJ;
import ij.gui.YesNoCancelDialog;

public class FileUtils {
    public static void deleteDirectoryContents(File sourceDirectory) {
        File[] files = sourceDirectory.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectoryContents(file);
                file.delete();
            } else {
                file.delete();
            }
        }
    }

    public static boolean verifyOutputDirectoryIsEmpty(String potential) {
        // Output must be an empty directory
        boolean foundFiles = false;
        try (Stream<Path> entries = Files.list(Paths.get(potential))) {
            if (entries.findFirst().isPresent()) {
                foundFiles = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return if there were no files present
        if (!foundFiles) {
            return true;
        }

        YesNoCancelDialog dialog = new YesNoCancelDialog(
                IJ.getInstance(),
                "Output Directory Not Empty",
                "The output directory is not empty. Continue and overwrite?",
                "Delete Contents", "Cancel");
        if (dialog.cancelPressed()) {
            return false;
        }

        FileUtils.deleteDirectoryContents(new File(potential));
        return true;
    }
}
