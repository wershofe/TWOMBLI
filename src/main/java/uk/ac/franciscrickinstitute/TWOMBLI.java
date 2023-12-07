package uk.ac.franciscrickinstitute;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = InteractiveCommand.class, menuPath = "Plugins>TWOMBLI")
public class TWOMBLI<T extends RealType<T>> extends InteractiveCommand {

    public static void main(final String... args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Load a dummy dataset
        final File file = ij.ui().chooseFile(null, "open");
        if (file == null) {
            return;
        }

        // Load the dataset
        final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());
        ij.ui().show(dataset);
        ij.command().run(TWOMBLI.class, true);
    }

    @Override
    public void run() {
        // TODO: Do something!
    }
}
