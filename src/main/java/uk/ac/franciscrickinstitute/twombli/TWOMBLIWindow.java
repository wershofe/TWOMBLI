package uk.ac.franciscrickinstitute.twombli;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.gui.WaitForUserDialog;
import org.scijava.command.CommandModule;

// Logservice integration
public class TWOMBLIWindow extends StackWindow {

    private static final double MAX_MAGNIFICATION = 32.0;
    private static final double MIN_MAGNIFICATION = 1/72.0;

    private final ImagePlus originalImage;
    final TWOMBLIConfigurator plugin;
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
    private final JButton anamorfButton;
    private final JButton infoButton;
    private final JButton selectOutputButton;
    private final JLabel selectedOutputField;
    private final JButton runPreviewButton;
    private final JButton revertPreview;
    private final JButton selectBatchButton;
    private JLabel selectedBatchField;
    private final JButton runButton;

    private String outputDirectory;
    private String anamorfPropertiesFile;
    private String batchPath;

    // Processing
    private HashMap<String, Object> inputs = new HashMap<>();
    private LinkedList<ImagePlus> processQueue = new LinkedList<>();
    private List<CommandModule> finishedFutures = new ArrayList<>();
    private int progressBarCurrent;
    private int progressBarMax;;

    /*
    TODO: UX:
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

        // Layouts
        FlowLayout panelLayout = new FlowLayout();
        panelLayout.setAlignment(FlowLayout.LEFT);

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
        minimumLineWidthPanel.setLayout(panelLayout);
        minimumLineWidthPanel.setToolTipText("Minimum line width in pixels. This should approximate the size of the smallest matrix fibres.");
        minimumLineWidthPanel.add(minimLineWidthInfo);
        minimumLineWidthPanel.add(this.minimumLineWidthField);

        // Maximum line width panel
        JLabel maximumLineWidthInfo = new JLabel("Maximum Line Width (px):");
        this.maximumLineWidthField = new JFormattedTextField(intFormat);
        this.maximumLineWidthField.setValue(5);
        JPanel maximumLineWidthPanel = new JPanel();
        maximumLineWidthPanel.setLayout(panelLayout);
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
        minimumBranchLengthPanel.setLayout(panelLayout);
        minimumBranchLengthPanel.setToolTipText("The minimum length in pixels before a branch can occur.");
        minimumBranchLengthPanel.add(minimumBranchLengthInfo);
        minimumBranchLengthPanel.add(this.minimumBranchLengthField);

        // Minimum curvature
        JLabel minimumCurvatureWindowInfo = new JLabel("Minimum Anamorf Curvature Window:");
        this.minimumCurvatureWindowField = new JFormattedTextField(intFormat);
        this.minimumCurvatureWindowField.setValue(40);
        JPanel minimumCurvatureWindowPanel = new JPanel();
        minimumCurvatureWindowPanel.setLayout(panelLayout);
        minimumCurvatureWindowPanel.setToolTipText("The minimum curvature window for Anamorf.");
        minimumCurvatureWindowPanel.add(minimumCurvatureWindowInfo);
        minimumCurvatureWindowPanel.add(this.minimumCurvatureWindowField);

        // Maximum curvature
        JLabel maximumCurvatureWindowInfo = new JLabel("Maximum Anamorf Curvature Window:");
        this.maximumCurvatureWindowField = new JFormattedTextField(intFormat);
        this.maximumCurvatureWindowField.setValue(40);
        JPanel maximumCurvatureWindowPanel = new JPanel();
        maximumCurvatureWindowPanel.setLayout(panelLayout);
        maximumCurvatureWindowPanel.setToolTipText("The minimum curvature window for Anamorf.");
        maximumCurvatureWindowPanel.add(maximumCurvatureWindowInfo);
        maximumCurvatureWindowPanel.add(this.maximumCurvatureWindowField);

        // Maximum display HDM
        JLabel maximumDisplayHDMInfo = new JLabel("Maximum Display HDM:");
        this.maximumDisplayHDMField = new JFormattedTextField(intFormat);
        this.maximumDisplayHDMField.setValue(40);
        JPanel maximumDisplayHDMPanel = new JPanel();
        maximumDisplayHDMPanel.setLayout(panelLayout);
        maximumDisplayHDMPanel.setToolTipText("The maximum display HDM.");
        maximumDisplayHDMPanel.add(maximumDisplayHDMInfo);
        maximumDisplayHDMPanel.add(this.maximumDisplayHDMField);

        // Curvature step size
        JLabel curvatureStepSizeInfo = new JLabel("Curvature Step Size:");
        this.curvatureStepSizeField = new JFormattedTextField(intFormat);
        this.curvatureStepSizeField.setValue(10);
        JPanel curvatureStepSizePanel = new JPanel();
        curvatureStepSizePanel.setLayout(panelLayout);
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
        contrastSaturationPanel.setLayout(panelLayout);
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
        gapAnalysisDiameterPanel.setLayout(panelLayout);
        gapAnalysisDiameterPanel.setToolTipText("The minimum diameter for gap analysis. 0 finds only 1.");
        gapAnalysisDiameterPanel.add(gapAnalysisDiameterInfo);
        gapAnalysisDiameterPanel.add(this.gapAnalysisDiameterField);

        // Anamorf properties
        this.anamorfButton = new JButton("Add Custom Anamorf Properties File (.xml)");
        this.anamorfButton.setToolTipText("Add a custom anamorf properties file to use for the analysis - if none provided, defaults will be used.");
        ActionListener anamorfListener = e -> this.getAnamorfProperties();
        this.anamorfButton.addActionListener(anamorfListener);

        // Info button
        this.infoButton = new JButton("Information!");
        this.infoButton.setToolTipText("Get information about TWOMBLI.");
        ActionListener infoListener = e -> this.showInfo();
        this.infoButton.addActionListener(infoListener);

        // Select output directory button
        this.selectOutputButton = new JButton("Select Output Directory (Required!)");
        this.selectOutputButton.setToolTipText("Choose a directory to output all the data. This includes preview data!");
        ActionListener selectOutputListener = e -> this.getOutputDirectory();
        this.selectOutputButton.addActionListener(selectOutputListener);
        this.selectedOutputField = new JLabel("No output directory selected.");

        // Run Preview button
        this.runPreviewButton = new JButton("Run Preview");
        this.runPreviewButton.setToolTipText("Run TWOMBLI with the current configuration on the preview image.");
        ActionListener runPreviewListener = e -> this.runPreviewProcess();
        this.runPreviewButton.addActionListener(runPreviewListener);

        // Revert preview button
        this.revertPreview = new JButton("Revert Preview");
        this.revertPreview.setToolTipText("Revert the preview image to the original.");
        ActionListener revertPreviewListener = e -> this.revertPreview();
        this.revertPreview.addActionListener(revertPreviewListener);

        // Select batch button
        this.selectBatchButton = new JButton("Select Batch");
        this.selectBatchButton.setToolTipText("Choose a directory containing multiple images to run.");
        ActionListener selectBatchListener = e -> this.getBatchDirectory();
        this.selectBatchButton.addActionListener(selectBatchListener);
        this.selectedBatchField = new JLabel("No batch directory selected.");

        // Run button
        this.runButton = new JButton("Run Batch");
        this.runButton.setToolTipText("Run TWOMBLI with the current configuration on the entire batch.");
        ActionListener runListener = e -> this.runProcess();
        this.runButton.addActionListener(runListener);

        // Configuration panel
        GridBagLayout configPanelLayout = new GridBagLayout();
        JPanel configPanel = new JPanel();
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        configPanel.setLayout(configPanelLayout);
        GridBagConstraints configPanelConstraints = new GridBagConstraints();
        configPanelConstraints.anchor = GridBagConstraints.NORTH;
        configPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        configPanelConstraints.gridwidth = 1;
        configPanelConstraints.gridheight = 1;
        configPanelConstraints.gridx = 0;
        configPanelConstraints.gridy = 0;
        configPanelConstraints.insets = new Insets(5, 5, 5, 5);

        // Minimum line width
        configPanel.add(minimumLineWidthPanel, configPanelConstraints);

        // Maximum line width
        configPanelConstraints.gridy++;
        configPanel.add(maximumLineWidthPanel, configPanelConstraints);

        // Darklines checkbox
        configPanelConstraints.gridy++;
        configPanel.add(this.darklinesCheckbox, configPanelConstraints);

        // Minimum branch length
        configPanelConstraints.gridy++;
        configPanel.add(minimumBranchLengthPanel, configPanelConstraints);

        // Minimum curvature window
        configPanelConstraints.gridy++;
        configPanel.add(minimumCurvatureWindowPanel, configPanelConstraints);

        // Maximum curvature window
        configPanelConstraints.gridy++;
        configPanel.add(maximumCurvatureWindowPanel, configPanelConstraints);

        // Curvature step size
        configPanelConstraints.gridy++;
        configPanel.add(curvatureStepSizePanel, configPanelConstraints);

        // Anamorf properties
        configPanelConstraints.gridy++;
        configPanel.add(this.anamorfButton, configPanelConstraints);

        // Maximum display HDM
        configPanelConstraints.gridy++;
        configPanel.add(maximumDisplayHDMPanel, configPanelConstraints);

        // Contrast saturation
        configPanelConstraints.gridy++;
        configPanel.add(contrastSaturationPanel, configPanelConstraints);

        // Gap analysis checkbox
        configPanelConstraints.gridy++;
        configPanel.add(this.gapAnalysisCheckbox, configPanelConstraints);

        // Gap analysis diameter
        configPanelConstraints.gridy++;
        configPanel.add(gapAnalysisDiameterPanel, configPanelConstraints);

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

        // Configuration panel
        sidePanel.add(configPanel, sidePanelConstraints);

        // Help button
        sidePanelConstraints.gridy++;
        sidePanel.add(this.infoButton, sidePanelConstraints);

        // Select Output Directory
        sidePanelConstraints.gridy++;
        sidePanel.add(this.selectOutputButton, sidePanelConstraints);

        // Output directory
        sidePanelConstraints.gridy++;
        sidePanel.add(this.selectedOutputField, sidePanelConstraints);

        // Insert run preview button
        sidePanelConstraints.gridy++;
        sidePanel.add(this.runPreviewButton, sidePanelConstraints);

        // Revert preview button
        sidePanelConstraints.gridy++;
        sidePanel.add(this.revertPreview, sidePanelConstraints);

        // Select Batch
        sidePanelConstraints.gridy++;
        sidePanel.add(this.selectBatchButton, sidePanelConstraints);

        // Batch directory
        sidePanelConstraints.gridy++;
        sidePanel.add(this.selectedBatchField, sidePanelConstraints);

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

        // Disable all interactions until we have an output directory
        this.toggleOutputAvailableInteractions(false);

        // Finish up and set our sizes
        this.pack();
        this.setMinimumSize(this.getPreferredSize());
    }

    private void toggleOutputAvailableInteractions(boolean state) {
        this.minimumLineWidthField.setEnabled(state);
        this.maximumLineWidthField.setEnabled(state);
        this.darklinesCheckbox.setEnabled(state);
        this.minimumBranchLengthField.setEnabled(state);
        this.minimumCurvatureWindowField.setEnabled(state);
        this.maximumCurvatureWindowField.setEnabled(state);
        this.maximumDisplayHDMField.setEnabled(state);
        this.curvatureStepSizeField.setEnabled(state);
        this.contrastSaturationField.setEnabled(state);
        this.gapAnalysisCheckbox.setEnabled(state);
        this.gapAnalysisDiameterField.setEnabled(state);
        this.anamorfButton.setEnabled(state);
        this.runPreviewButton.setEnabled(state);
        this.revertPreview.setEnabled(state);
        this.selectBatchButton.setEnabled(state);
        this.toggleRunButton(state);
    }

    private void toggleRunButton(boolean state) {
        if (!state) {
            this.runButton.setEnabled(false);
            return;
        }

        if (this.batchPath == null) {
            this.runButton.setEnabled(false);
            return;
        }

        this.runButton.setEnabled(true);
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
        String info = "TWOMBLI is a tool for the analysis of matrix fibres in images.\n" +
                "It is designed to be used with images of the extracellular matrix.\n" +
                "For more information, please see: {TODO:paper_linl}\n" +
                "For a video on how to use TWOMBLI, please visit: {TODO:video_link}\n" +
                "Please report any issues to: {TODO:github_link}\n" +
                "TWOMBLI utilises various third party tools and libraries, including:\n" +
                " - OrientationJ: {TODO:orientation_link}\n" +
                " - Anamorf: {TODO:anamorf_link}\n" +
                " - IJ-RidgeDetection: {TODO:ij_ridge_link}\n" +
                " - MaxInscribedCircles: {TODO:circles_link}\n" +
                " - Bio-Formats: {TODO:bioformats_link}\n";
        WaitForUserDialog dialog = new WaitForUserDialog("TWOMBLI Information", info);
        dialog.show();
    }

    private void getOutputDirectory() {
        String potential = IJ.getDirectory("Get output directory");
        if (!Files.isDirectory(Paths.get(potential))) {
            this.toggleOutputAvailableInteractions(false);
            return;
        }

        // Ensure the directory is empty
        boolean outcome = FileUtils.verifyOutputDirectoryIsEmpty(potential);
        if (!outcome) {
            return;
        }

        // Set the other actions as enabled
        this.outputDirectory = potential;
        this.selectedOutputField.setText(potential);
        this.toggleOutputAvailableInteractions(true);
    }

    private void getAnamorfProperties() {
        String potential = IJ.getFilePath("Get anamorf properties");
        if (potential == null) {
            this.anamorfPropertiesFile = potential;
            return;
        }

        if (!Files.isRegularFile(Paths.get(potential)) && !potential.endsWith(".xml")) {
            return;
        }

        // Set the other actions as enabled
        this.anamorfPropertiesFile = potential;
    }

    private void getBatchDirectory() {
       String potential = IJ.getDirectory("Get Batch Folder");
       if (potential == null) {
           this.batchPath = potential;
           return;
       }

        if (!Files.isDirectory(Paths.get(potential))) {
            return;
        }

        // Set the other actions as enabled
        this.batchPath = potential;
        this.selectedBatchField.setText(potential);
        this.toggleRunButton(true);
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

        // Empty our output directory (which should only contain previous run data)
        boolean outcome = FileUtils.verifyOutputDirectoryIsEmpty(this.outputDirectory);
        if (!outcome) {
            this.processQueue.clear();
            return;
        }

        // Prepare a progress bar and block user input
        this.progressBarCurrent = 0;
        this.progressBarMax = this.processQueue.size();
        IJ.showMessage("Processing Images. This may take a while. (Press OK to start.)");
        IJ.showProgress(this.progressBarCurrent, this.progressBarMax);

        // Process our first image
        this.processNext(true);
    }

    private void processNext(boolean isPreview) {
        ImagePlus img = this.processQueue.remove();
        this.inputs.put("img", img);
        Future<CommandModule> future = this.plugin.commandService.run(TWOMBLIRunner.class, false, inputs);

//        // Delay our polling to prevent weird race conditions
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        this.plugin.moduleService.waitFor(future);

        Thread t = new Thread(new FuturePoller(this, future, isPreview));
        t.start();
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
        if (!this.processQueue.isEmpty()) {
            this.progressBarCurrent += 1;
            IJ.showProgress(this.progressBarCurrent, this.progressBarMax);
            this.processNext(false);
        }

        // Restore our gui functionality & close progress bars
        else {
            IJ.showProgress(1, 1);
            this.generateSummaries();

            // Reopen us?
            this.setVisible(true);
        }
    }

    private void generateSummaries() {
        // Loop through our results and generate a summary
        Path gapsOutputPath = Paths.get(this.outputDirectory, "gaps_summary.csv");
        Path twombliOutputPath = Paths.get(this.outputDirectory, "twombli_summary.csv");
        for (CommandModule output : this.finishedFutures) {
            // Gather our basic info
            String filePrefix = (String) output.getInput("filePrefix");
            double alignment = (double) output.getOutput("alignment");
            double dimension = (int) output.getOutput("dimension");
            Path anamorfSummaryPath = Paths.get(this.outputDirectory, "masks", filePrefix + "_results.csv");
            Path hdmSummaryPath = Paths.get(this.outputDirectory, "hdm_csvs", filePrefix + "_ResultsHDM.csv");
            Path gapAnalysisPath = Paths.get(this.outputDirectory, "gap_analysis", filePrefix + "_gaps.csv");

            // Write to our twombli summary
            try {
                List<String> anamorfEntries = Files.readAllLines(anamorfSummaryPath);
                String anamorfData = anamorfEntries.get(anamorfEntries.size() - 1);

                List<String> hdmEntries = Files.readAllLines(hdmSummaryPath);
                String[] hdmData = hdmEntries.get(hdmEntries.size() - 1).split(",");

                List<String> lines = new ArrayList<>();
                lines.add(anamorfData + "," + hdmData[hdmData.length - 1] + "," + alignment + "," + dimension);
                Files.write(twombliOutputPath, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Write to our gap analysis summary
            if (this.gapAnalysisCheckbox.isSelected()) {
                try {
                    List<String> lines = Files.readAllLines(gapAnalysisPath);
                    Files.write(gapsOutputPath, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
