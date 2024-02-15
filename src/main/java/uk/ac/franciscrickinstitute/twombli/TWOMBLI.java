package uk.ac.franciscrickinstitute.twombli;

import net.imagej.ImageJ;

public class TWOMBLI {

    private static final boolean DEBUG_FLAG = true;

    // This function ensures that our dependencies are present.
    // Inform the user of a failed environment where possible.
    static {
        // TODO: This is a future feature, for now we just rely on the packaged dependencies
        if (!TWOMBLI.DEBUG_FLAG) {
            boolean outcome = Dependencies.checkRootDependencies();
            if (!outcome) {
                throw new RuntimeException("TWOMBLI failed to start due to missing dependencies.");
            }
        }
    }


    // Entrant for debugging via IDE
    public static void main(final String... args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.launch(args);
        ij.command().run(TWOMBLIConfigurator.class, true);
    }
}
