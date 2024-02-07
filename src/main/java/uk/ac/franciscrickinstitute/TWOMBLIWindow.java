package uk.ac.franciscrickinstitute;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import org.scijava.command.CommandModule;

// Logservice integration
public class TWOMBLIWindow extends StackWindow {

    private static final double MAX_MAGNIFICATION = 32.0;
    private static final double MIN_MAGNIFICATION = 1/72.0;

    private final ImagePlus originalImage;
    private final TWOMBLIConfigurator plugin;
    private final JFormattedTextField minimumLineWidthField;
    private final JFormattedTextField maximumLineWidthField;
    private final JCheckBox darklinesCheckbox;
    private final JFormattedTextField minimumBranchLengthField;
    private final JFormattedTextField minimumCurvatureWindowField;
    private final JFormattedTextField maximumCurvatureWindowField;
    private final JFormattedTextField curvatureStepSizeField;
    private final JFormattedTextField maximumDisplayHDMField;
    private final JFormattedTextField contrastSaturationField;
    private final JCheckBox gapAnalysisCheckbox;
    private final JFormattedTextField gapAnalysisDiameterField;
    private final JButton runPreviewButton;
    private final JButton selectBatchButton;
    private final JButton runButton;

    private String outputDirectory;
    private String anamorfPropertiesFile;
    private String batchPath;

    // Processing
    private HashMap<String, Object> inputs = new HashMap<>();
    private LinkedList<ImagePlus> processQueue = new LinkedList<>();
    private List<CommandModule> finishedFutures = new ArrayList<>();
    private int progressBarCurrent;
    private int progressBarMax;

    /*
    TODO: UX:
    - Add a progress bar / status update to the actions
    - Think about how to handle preview outputs.
    - Think about a button to restore the preview?
    - Think about previewing each step?
     */

    public TWOMBLIWindow(TWOMBLIConfigurator plugin, ImagePlus previewImage, ImagePlus originalImage) {
        super(previewImage, new ImageCanvas(previewImage));
        this.plugin = plugin;
        this.originalImage = originalImage;
        this.zoomImage();
        this.setTitle("TWOMBLI");

        // Image display
        final ImageCanvas canvas = this.getCanvas();

        // Info button
        JButton infoButton = new JButton("Information!");
        infoButton.setToolTipText("Get information about TWOMBLI.");
        ActionListener infoListener = e -> this.showInfo();
        infoButton.addActionListener(infoListener);

        // Select output directory button
        JButton selectOutputButton = new JButton("Select Output Directory (Required!)");
        selectOutputButton.setToolTipText("Choose a directory to output all the data. This includes preview data!");
        ActionListener selectOutputListener = e -> this.getOutputDirectory();
        selectOutputButton.addActionListener(selectOutputListener);

        // Minimum line width panel
        JLabel minimLineWidthInfo = new JLabel("Minimum Line Width (px):");
        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        NumberFormatter intFormatter = new NumberFormatter(intFormat);
        intFormatter.setValueClass(Integer.class);
        intFormatter.setMinimum(1);
        intFormatter.setMaximum(100);
        intFormatter.setAllowsInvalid(false);
        this.minimumLineWidthField = new JFormattedTextField(intFormat);
        this.minimumLineWidthField.setValue(5);
        JPanel minimumLineWidthPanel = new JPanel();
        minimumLineWidthPanel.setLayout(new FlowLayout());
        minimumLineWidthPanel.setToolTipText("Minimum line width in pixels. This should approximate the size of the smallest matrix fibres.");
        minimumLineWidthPanel.add(minimLineWidthInfo);
        minimumLineWidthPanel.add(this.minimumLineWidthField);

        // Maximum line width panel
        JLabel maximumLineWidthInfo = new JLabel("Maximum Line Width (px):");
        this.maximumLineWidthField = new JFormattedTextField(intFormat);
        this.maximumLineWidthField.setValue(5);
        JPanel maximumLineWidthPanel = new JPanel();
        maximumLineWidthPanel.setLayout(new FlowLayout());
        maximumLineWidthPanel.setToolTipText("Maximum line width in pixels. This should approximate the size of the largest matrix fibres.");
        maximumLineWidthPanel.add(maximumLineWidthInfo);
        maximumLineWidthPanel.add(this.maximumLineWidthField);

        // Darklines checkbox
        this.darklinesCheckbox = new JCheckBox("Dark Lines");
        this.darklinesCheckbox.setToolTipText("Check this box if the lines are darker as opposed to light.");

        // Minimum branch length
        JLabel minimumBranchLengthInfo = new JLabel("Maximum Branch Length (px):");
        this.minimumBranchLengthField = new JFormattedTextField(intFormat);
        this.minimumBranchLengthField.setValue(10);
        JPanel minimumBranchLengthPanel = new JPanel();
        minimumBranchLengthPanel.setLayout(new FlowLayout());
        minimumBranchLengthPanel.setToolTipText("The minimum length in pixels before a branch can occur.");
        minimumBranchLengthPanel.add(minimumBranchLengthInfo);
        minimumBranchLengthPanel.add(this.minimumBranchLengthField);

        // Minimum curvature
        JLabel minimumCurvatureWindowInfo = new JLabel("Minimum Anamorf Curvature Window:");
        this.minimumCurvatureWindowField = new JFormattedTextField(intFormat);
        this.minimumCurvatureWindowField.setValue(40);
        JPanel minimumCurvatureWindowPanel = new JPanel();
        minimumCurvatureWindowPanel.setLayout(new FlowLayout());
        minimumCurvatureWindowPanel.setToolTipText("The minimum curvature window for Anamorf.");
        minimumCurvatureWindowPanel.add(minimumCurvatureWindowInfo);
        minimumCurvatureWindowPanel.add(this.minimumCurvatureWindowField);

        // Maximum curvature
        JLabel maximumCurvatureWindowInfo = new JLabel("Maximum Anamorf Curvature Window:");
        this.maximumCurvatureWindowField = new JFormattedTextField(intFormat);
        this.maximumCurvatureWindowField.setValue(40);
        JPanel maximumCurvatureWindowPanel = new JPanel();
        maximumCurvatureWindowPanel.setLayout(new FlowLayout());
        maximumCurvatureWindowPanel.setToolTipText("The minimum curvature window for Anamorf.");
        maximumCurvatureWindowPanel.add(maximumCurvatureWindowInfo);
        maximumCurvatureWindowPanel.add(this.maximumCurvatureWindowField);

        // Maximum display HDM
        JLabel maximumDisplayHDMInfo = new JLabel("Maximum Display HDM:");
        this.maximumDisplayHDMField = new JFormattedTextField(intFormat);
        this.maximumDisplayHDMField.setValue(40);
        JPanel maximumDisplayHDMPanel = new JPanel();
        maximumDisplayHDMPanel.setLayout(new FlowLayout());
        maximumDisplayHDMPanel.setToolTipText("The maximum display HDM.");
        maximumDisplayHDMPanel.add(maximumDisplayHDMInfo);
        maximumDisplayHDMPanel.add(this.maximumDisplayHDMField);

        // Curvature step size
        JLabel curvatureStepSizeInfo = new JLabel("Curvature Step Size:");
        this.curvatureStepSizeField = new JFormattedTextField(intFormat);
        this.curvatureStepSizeField.setValue(10);
        JPanel curvatureStepSizePanel = new JPanel();
        curvatureStepSizePanel.setLayout(new FlowLayout());
        curvatureStepSizePanel.setToolTipText("The step size for the curvature window.");
        curvatureStepSizePanel.add(curvatureStepSizeInfo);
        curvatureStepSizePanel.add(this.curvatureStepSizeField);

        // Contrast Saturation
        JLabel contrastSaturationInfo = new JLabel("Contrast Saturation:");
        NumberFormat longFormat = NumberFormat.getNumberInstance();
        NumberFormatter longFormatter = new NumberFormatter(longFormat);
        intFormatter.setValueClass(Float.class);
        intFormatter.setMinimum(0.0);
        intFormatter.setMaximum(1.0);
        intFormatter.setAllowsInvalid(false);
        this.contrastSaturationField = new JFormattedTextField(longFormatter);
        this.contrastSaturationField.setValue(0.35);
        JPanel contrastSaturationPanel = new JPanel();
        contrastSaturationPanel.setLayout(new FlowLayout());
        contrastSaturationPanel.setToolTipText("The contrast saturation. (Between 0 and 1)");
        contrastSaturationPanel.add(contrastSaturationInfo);
        contrastSaturationPanel.add(this.contrastSaturationField);

        // Gap analysis checkbox
        this.gapAnalysisCheckbox = new JCheckBox("Perform Gap Analysis");
        this.gapAnalysisCheckbox.setToolTipText("Check this box to perform gap analysis.");

        // Gap analysis diameter
        JLabel gapAnalysisDiameterInfo = new JLabel("Minimum Gap Analysis Diameter:");
        this.gapAnalysisDiameterField = new JFormattedTextField(intFormat);
        this.gapAnalysisDiameterField.setValue(0);
        JPanel gapAnalysisDiameterPanel = new JPanel();
        gapAnalysisDiameterPanel.setLayout(new FlowLayout());
        gapAnalysisDiameterPanel.setToolTipText("The minimum diameter for gap analysis. 0 finds only 1.");
        gapAnalysisDiameterPanel.add(gapAnalysisDiameterInfo);
        gapAnalysisDiameterPanel.add(this.gapAnalysisDiameterField);

        // Anamorf properties
        JButton anamorfButton = new JButton("Add Custom Anamorf Properties File (.xml)");
        anamorfButton.setToolTipText("Add a custom anamorf properties file to use for the analysis - if none provided, defaults will be used.");
        ActionListener anamorfListener = e -> this.getAnamorfProperties();
        anamorfButton.addActionListener(anamorfListener);

        // Run Preview button
        this.runPreviewButton = new JButton("Run Preview");
        this.runPreviewButton.setToolTipText("Run TWOMBLI with the current configuration on the preview image.");
        ActionListener runPreviewListener = e -> this.runPreviewProcess();
        this.runPreviewButton.addActionListener(runPreviewListener);
        this.runPreviewButton.setEnabled(false);

        // Revert preview button
        JButton revertPreview = new JButton("Revert Preview");
        revertPreview.setToolTipText("Revert the preview image to the original.");
        ActionListener revertPreviewListener = e -> this.revertPreview();
        revertPreview.addActionListener(revertPreviewListener);
        revertPreview.setEnabled(false);

        // Select batch button
        this.selectBatchButton = new JButton("Select Batch");
        this.selectBatchButton.setToolTipText("Choose a directory containing multiple images to run.");
        ActionListener selectBatchListener = e -> this.getBatchDirectory();
        this.selectBatchButton.addActionListener(selectBatchListener);
        this.selectBatchButton.setEnabled(false);

        // Run button
        this.runButton = new JButton("Run Batch");
        this.runButton.setToolTipText("Run TWOMBLI with the current configuration on the entire batch.");
        ActionListener runListener = e -> this.runProcess();
        this.runButton.addActionListener(runListener);
        this.runButton.setEnabled(false);

        // Sidebar panel
        GridBagLayout sidePanelLayout = new GridBagLayout();
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(sidePanelLayout);
        GridBagConstraints sidePanelConstraints = new GridBagConstraints();
        sidePanelConstraints.anchor = GridBagConstraints.NORTH;
        sidePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        sidePanelConstraints.gridwidth = 1;
        sidePanelConstraints.gridheight = 1;
        sidePanelConstraints.gridx = 0;
        sidePanelConstraints.gridy = 0;
        sidePanelConstraints.insets = new Insets(5, 5, 5, 5);

        // Help button
        sidePanel.add(infoButton, sidePanelConstraints);

        // Insert batch button
        sidePanelConstraints.gridy++;
        sidePanel.add(selectOutputButton, sidePanelConstraints);

        // Minimum line width
        sidePanelConstraints.gridy++;
        sidePanel.add(minimumLineWidthPanel, sidePanelConstraints);

        // Maximum line width
        sidePanelConstraints.gridy++;
        sidePanel.add(maximumLineWidthPanel, sidePanelConstraints);

        // Darklines checkbox
        sidePanelConstraints.gridy++;
        sidePanel.add(this.darklinesCheckbox, sidePanelConstraints);

        // Minimum branch length
        sidePanelConstraints.gridy++;
        sidePanel.add(minimumBranchLengthPanel, sidePanelConstraints);

        // Minimum curvature window
        sidePanelConstraints.gridy++;
        sidePanel.add(minimumCurvatureWindowPanel, sidePanelConstraints);

        // Maximum curvature window
        sidePanelConstraints.gridy++;
        sidePanel.add(maximumCurvatureWindowPanel, sidePanelConstraints);

        // Curvature step size
        sidePanelConstraints.gridy++;
        sidePanel.add(curvatureStepSizePanel, sidePanelConstraints);

        // Anamorf properties
        sidePanelConstraints.gridy++;
        sidePanel.add(anamorfButton, sidePanelConstraints);

        // Maximum display HDM
        sidePanelConstraints.gridy++;
        sidePanel.add(maximumDisplayHDMPanel, sidePanelConstraints);

        // Contrast saturation
        sidePanelConstraints.gridy++;
        sidePanel.add(contrastSaturationPanel, sidePanelConstraints);

        // Gap analysis checkbox
        sidePanelConstraints.gridy++;
        sidePanel.add(this.gapAnalysisCheckbox, sidePanelConstraints);

        // Gap analysis diameter
        sidePanelConstraints.gridy++;
        sidePanel.add(gapAnalysisDiameterPanel, sidePanelConstraints);

        // Insert run preview button
        sidePanelConstraints.gridy++;
        sidePanel.add(this.runPreviewButton, sidePanelConstraints);

        // Insert output button
        sidePanelConstraints.gridy++;
        sidePanel.add(selectBatchButton, sidePanelConstraints);

        // Insert run button
        sidePanelConstraints.gridy++;
        sidePanel.add(this.runButton, sidePanelConstraints);

        // Spacer so we valign
        sidePanelConstraints.gridy++;
        sidePanelConstraints.weighty = 1;
        sidePanel.add(Box.createVerticalBox(), sidePanelConstraints);

        // Content panel
        GridBagLayout contentPanelLayout = new GridBagLayout();
        Panel contentPanel = new Panel();
        contentPanel.setLayout(contentPanelLayout);
        GridBagConstraints contentPanelConstraints = new GridBagConstraints();
        contentPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        contentPanelConstraints.fill = GridBagConstraints.BOTH;
        contentPanelConstraints.gridwidth = 1;
        contentPanelConstraints.gridheight = 1;
        contentPanelConstraints.gridx = 0;
        contentPanelConstraints.gridy = 0;
        contentPanelConstraints.weightx = 1;
        contentPanelConstraints.weighty = 1;

        // Insert canvas first
        contentPanel.add(canvas, contentPanelConstraints);

        // Side panel for controls
        contentPanelConstraints.gridx++;
        contentPanelConstraints.weightx = 0;
        contentPanelConstraints.weighty = 0;
        contentPanel.add(sidePanel, contentPanelConstraints);

        // Core window layout properties
        GridBagLayout windowLayout = new GridBagLayout();
        GridBagConstraints windowConstraints = new GridBagConstraints();
        windowConstraints.anchor = GridBagConstraints.NORTHWEST;
        windowConstraints.fill = GridBagConstraints.NORTH;
        windowConstraints.weightx = 1;
        windowConstraints.weighty = 1;
        this.setLayout(windowLayout);
        this.add(contentPanel, windowConstraints);

        // Finish up and set our sizes
        this.pack();
        this.setMinimumSize(this.getPreferredSize());
    }

    private void zoomImage() {
        // Adjust our screen positions + image sizes
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();

        // Zoom in if our image is small
        while ((this.ic.getWidth() < width / 2 || this.ic.getHeight() < height / 2)
                && this.ic.getMagnification() < TWOMBLIWindow.MAX_MAGNIFICATION) {
            final int canvasWidth = this.ic.getWidth();
            this.ic.zoomIn(0, 0);
            if (canvasWidth == this.ic.getWidth()) {
                this.ic.zoomOut(0, 0);
                break;
            }
        }

        // Zoom out if our image is large
        while ((this.ic.getWidth() > 0.75 * width || this.ic.getHeight() > 0.75 * height)
                && this.ic.getMagnification() > TWOMBLIWindow.MIN_MAGNIFICATION) {
            final int canvasWidth = this.ic.getWidth();
            this.ic.zoomOut(0, 0);
            if (canvasWidth == this.ic.getWidth()) {
                this.ic.zoomIn(0, 0);
                break;
            }
        }
    }

    private void showInfo() {
        return;
    }

    private void getOutputDirectory() {
        String potential = IJ.getDirectory("Get output directory");
        if (!Files.isDirectory(Paths.get(potential))) {
            this.runPreviewButton.setEnabled(false);
            this.selectBatchButton.setEnabled(false);
            this.runButton.setEnabled(false);
            return;
        }

        // Output must be an empty directory
        try (Stream<Path> entries = Files.list(Paths.get(potential))) {
            if (entries.findFirst().isPresent()) {
                this.plugin.statusService.warn("Output directory must be empty");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set the other actions as enabled
        this.outputDirectory = potential;
        this.runPreviewButton.setEnabled(true);
        this.selectBatchButton.setEnabled(true);
        this.runButton.setEnabled(true);
    }

    private void getAnamorfProperties() {
        String potential = IJ.getFilePath("Get anamorf properties");
        if (!Files.isRegularFile(Paths.get(potential)) && !potential.endsWith(".xml")) {
            return;
        }

        // Set the other actions as enabled
        this.anamorfPropertiesFile = potential;
    }

    private void getBatchDirectory() {
       String potential = IJ.getDirectory("Get Batch Folder");
        if (!Files.isDirectory(Paths.get(potential))) {
            return;
        }

        // Set the other actions as enabled
        this.batchPath = potential;
    }

    private HashMap<String, Object> getInputs() {
        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("outputPath", this.outputDirectory);
        inputs.put("minimumLineWidth", Integer.valueOf(this.minimumLineWidthField.getText()));
        inputs.put("maximumLineWidth", Integer.valueOf(this.maximumLineWidthField.getText()));
        inputs.put("darkLines", this.darklinesCheckbox.isSelected());
        inputs.put("minimumBranchLength", Integer.valueOf(this.minimumBranchLengthField.getText()));
        inputs.put("minimumCurvatureWindow", Integer.valueOf(this.minimumCurvatureWindowField.getText()));
        inputs.put("maximumCurvatureWindow", Integer.valueOf(this.maximumCurvatureWindowField.getText()));
        inputs.put("curvatureWindowStepSize", Integer.valueOf(this.curvatureStepSizeField.getText()));
        inputs.put("maximumDisplayHDM", Integer.valueOf(this.maximumDisplayHDMField.getText()));
        inputs.put("contrastSaturation", Float.valueOf(this.contrastSaturationField.getText()));
        inputs.put("performGapAnalysis", this.gapAnalysisCheckbox.isSelected());
        inputs.put("minimumGapDiameter", Integer.valueOf(this.gapAnalysisDiameterField.getText()));

        if (this.anamorfPropertiesFile != null) {
            inputs.put("anamorfPropertiesFile", this.anamorfPropertiesFile);
        }

        return inputs;
    }

    private void runPreviewProcess() {
        this.preparePreview();
        this.startProcessing();
    }

    private void preparePreview() {
        // TODO: Block input
        // TODO: Progress bars

        // Just copy
        ImagePlus preview;
        if (this.originalImage.getImageStackSize() == 1) {
            preview = ImageUtils.duplicateImage(this.originalImage);
        }

        // Only take the 'currently selected' file for our preview.
        else {
            preview = this.originalImage.crop();
        }


        // Run command and poll for our outputs
        this.processQueue.add(preview);
    }

    private void revertPreview() {
        // Just copy
        ImagePlus preview;
        if (this.originalImage.getImageStackSize() == 1) {
            preview = ImageUtils.duplicateImage(this.originalImage);
        }

        // Only take the 'currently selected' file for our preview.
        else {
            preview = this.originalImage.crop();
        }

        this.setImage(preview);
    }

    private void runProcess() {
        this.preparePreview();

        // Skip if we don't have a batch path
        if (this.batchPath == null) {
            return;
        }

        // Identify our batch targets
        File sourceDirectory = new File(this.batchPath);
        File[] files = sourceDirectory.listFiles((dir, name) -> {
            for (String suffix : TWOMBLIConfigurator.EXTENSIONS) {
                if (name.endsWith(suffix)) {
                    return true;
                }
            }

            return false;
        });

        // Loop through our files, load their images, add to queue
        assert files != null;
        for (File file : files) {
            ImagePlus img = IJ.openImage(file.getAbsolutePath());
            this.processQueue.add(img);
        }

        this.startProcessing();
    }

    private void startProcessing() {
        this.inputs = this.getInputs();

        // Prepare a progress bar and block user input
        this.progressBarCurrent = 0;
        this.progressBarMax = this.processQueue.size();
        this.plugin.statusService.showStatus(this.progressBarCurrent, this.progressBarMax, "Processing images");

        this.processNext(true);
    }

    private void processNext(boolean isPreview) {
        ImagePlus img = this.processQueue.remove();
        this.inputs.put("img", img);
        Future<CommandModule> future = this.plugin.commandService.run(TWOMBLIRunner.class, false, inputs);
        new Thread(new FuturePoller(this, future, isPreview)).start();
    }

    public void handleFutureComplete(Future<CommandModule> future, boolean isPreview) {
        try {
            // Update our preview image with our output
            if (isPreview) {
                CommandModule output = future.get();
                this.setImage((ImagePlus) output.getOutput("output"));
                this.imp.setTitle("TWOMBLI Preview");
            }

            // Store our output for collation
            else {
                CommandModule output = future.get();
                this.finishedFutures.add(output);
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // Check if we have more to process.
        if (this.processQueue.size() > 0) {
            this.progressBarCurrent += 1;
            this.plugin.statusService.showStatus(this.progressBarCurrent, this.progressBarMax, "Processing images");
            this.processNext(false);
        }

        // Restore our gui functionality & close progress bars
        else {
            this.plugin.statusService.clearStatus();
        }

        // Reopen us?
        this.setVisible(true);
    }
}
