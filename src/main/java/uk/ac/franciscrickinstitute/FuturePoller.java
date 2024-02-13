package uk.ac.franciscrickinstitute;

import javax.swing.*;
import java.util.concurrent.ExecutionException;
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

        // Get the outcome and check if it's an exception
        CommandModule possibleException;
        try {
            possibleException = this.future.get();
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
            throw new RuntimeException(e);
        }

//        this.window.plugin.moduleService.waitFor(this.future);

        // Apparently isDone isn't enough, we need to check the future itself... (I deleted the real comment here because it was very rude)
//        CommandModule module = null;
//        while (module == null) {
//            try {
//                module = this.future.get();
//            } catch (Exception e) {
//                e.printStackTrace();
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        }

        // Inject our preview image
        SwingUtilities.invokeLater(() -> window.handleFutureComplete(future, isPreview));
    }
}
