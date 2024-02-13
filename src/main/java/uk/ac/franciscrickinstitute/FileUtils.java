package uk.ac.franciscrickinstitute;

import java.io.File;

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
}
