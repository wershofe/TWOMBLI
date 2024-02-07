package uk.ac.franciscrickinstitute;

import javax.swing.*;
import java.util.List;
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
