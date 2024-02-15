package uk.ac.franciscrickinstitute.twombli;

import javax.swing.*;
import java.util.concurrent.Future;

import org.scijava.command.CommandModule;

public class FuturePoller implements Runnable {

    private final TWOMBLIWindow window;
    private final Future<CommandModule> future;
    private final boolean isPreview;

    public FuturePoller(TWOMBLIWindow window, Future<CommandModule> future, boolean isPreview) {
        this.window = window;
        this.future = future;
        this.isPreview = isPreview;
    }

    @Override
    public void run() {
        // This whole thing is just one massive race condition. If you do it on the GUI thread, everything hangs.
        // If you do it on a separate thread there's something funky going on with IJs event dispatcher, scijava threads, awt threads, and this thread.

        // Ensure our preview image has returned
        while (!this.future.isDone()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Inject our preview image
        SwingUtilities.invokeLater(() -> window.handleFutureComplete(future, isPreview));
    }
}
