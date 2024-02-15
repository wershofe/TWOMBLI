package uk.ac.franciscrickinstitute.twombli;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.Macro_Runner;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import loci.plugins.macro.LociFunctions;
import net.calm.anamorf.Batch_Analyser;
import net.calm.anamorf.params.DefaultParams;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>TWOMBLI>Runner")
public class TWOMBLIRunner implements Command {

    @Parameter
    public ImagePlus img;

    @Parameter
    public String outputPath = null;

    @Parameter
    public String filePrefix = null;

    @Parameter
    public int minimumLineWidth = 5;

    @Parameter
    public int maximumLineWidth = 20;

    @Parameter
    public boolean darkLines = false;

    @Parameter
    public int minimumBranchLength = 10;

    @Parameter
    public String anamorfPropertiesFile = null;

    @Parameter
    public int minimumCurvatureWindow = 40;

    @Parameter
    public int curvatureWindowStepSize = 10;

    @Parameter
    public int maximumCurvatureWindow = 40;

    @Parameter
    public int maximumDisplayHDM = 200;

    @Parameter
    public double contrastSaturation = 0.35;

    @Parameter
    public boolean performGapAnalysis = true;

    @Parameter
    public int minimumGapDiameter = 0;

    @Parameter(type=ItemIO.OUTPUT)
    public ImagePlus output;

    @Parameter(type=ItemIO.OUTPUT)
    public double alignment;

    @Parameter(type=ItemIO.OUTPUT)
    public int dimension;

    // Magic number declarations
    private static final double HIGH_CONTRAST_THRESHOLD = 120.0;
    private static final double LOW_CONTRAST_THRESHOLD = 0.0;

    @Override
    public void run() {
        // Validate our runtime environment
        boolean outcome = Dependencies.checkRootDependencies();
        if (!outcome) {
            return;
        }

        // TODO: Validate 3d stack behavior
        // If our output path is none, build up our prerequisites
        if (this.outputPath == null || this.filePrefix == null) {
            FileInfo fileInfo = this.img.getOriginalFileInfo();
            this.filePrefix = fileInfo.fileName.substring(0, fileInfo.fileName.lastIndexOf("."));
        }

        // Create our required folders
        // note, other processes could be doing the same, so we don't bail if something goes wrong.
        String masksDirectory = this.outputPath + File.separator + "masks";
        String maskImageDirectoryPath = masksDirectory + File.separator + this.filePrefix;
        String hdmDirectory = this.outputPath + File.separator + "hdm";
        String hdmResultsDirectory = this.outputPath + File.separator + "hdm_csvs";
        String gapAnalysisDirectory = this.outputPath + File.separator + "gap_analysis";
        try {
            Files.createDirectories(Paths.get(maskImageDirectoryPath));
            Files.createDirectories(Paths.get(hdmDirectory));
            Files.createDirectories(Paths.get(hdmResultsDirectory));
            Files.createDirectories(Paths.get(gapAnalysisDirectory));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Run HDM FIRST as something about anamorf or its dependencies alters our classpath... (I think)
        this.runHDM(hdmDirectory, hdmResultsDirectory, this.img);

        // Actual image handles
        String tmpMaskImagePath = maskImageDirectoryPath + File.separator + this.filePrefix + "_masks.png";

        // Detect ridges
        this.detectRidges(this.img, tmpMaskImagePath);

        // Run anamorf
        this.runAnamorf(maskImageDirectoryPath);

        // Move all files from the mask image directory to the masks directory
        File maskImageDirectory = new File(maskImageDirectoryPath);
        File[] files = maskImageDirectory.listFiles();
        assert files != null;
        try {
            for (File file : files) {
                // Deals with the anamorf folder
                if (file.isDirectory()) {
                    File[] anamorfFiles = file.listFiles();
                    assert anamorfFiles != null;
                    for (File anamorfFile : anamorfFiles) {
                        Files.move(Paths.get(anamorfFile.getAbsolutePath()), Paths.get(masksDirectory + File.separator + this.filePrefix + "_" + anamorfFile.getName()), StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                else {
                    Files.move(Paths.get(file.getAbsolutePath()), Paths.get(masksDirectory + File.separator + file.getName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // Delete the contents of the source directory
            FileUtils.deleteDirectoryContents(maskImageDirectory);
            maskImageDirectory.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Process our masks to obtain some statistics
        String finalMaskImagePath = masksDirectory + File.separator + this.filePrefix + "_masks.png";
        ImagePlus maskImage = IJ.openImage(new File(finalMaskImagePath).getAbsolutePath());
        this.runOrientationJ(maskImage);

        // Close non-essential windows
//        this.closeNonImages();

        // Gap analysis
        maskImage = IJ.openImage(new File(finalMaskImagePath).getAbsolutePath());
        this.performGapAnalysis(gapAnalysisDirectory, maskImage);

        // Output handling
//        this.closeNonImages();
        this.output = maskImage;
    }

    private void detectRidges(ImagePlus inputImage, String maskImage) {
        ImagePlus ridgeDetectionBaseImage = ImageUtils.duplicateImage(inputImage);

        // Enhance contrast
        IJ.run(ridgeDetectionBaseImage, "Enhance Contrast", "saturated=" + this.contrastSaturation);

        // Convert to 8 bit
        IJ.run(ridgeDetectionBaseImage, "8-bit", "");

        // Multiscale ridge detection
        ImagePlus output = this.multiScaleRidgeDetection(ridgeDetectionBaseImage);

        // Invert LUTs
        IJ.run(output, "Invert LUT", "");

        // Handle outputs
        IJ.saveAs(output, "png", maskImage);
        output.close();
    }

    private ImagePlus multiScaleRidgeDetection(ImagePlus inputImage) {
        // Run ridge detection prelim
        ImagePlus currentImage = singleScaleRidgeDetection(inputImage, this.minimumLineWidth);

        // Run ridge detection in while loop where we compare each ridge detection to pick the optimal.
        int currentLineWidth = this.minimumLineWidth + 1;
        while (currentLineWidth <= this.maximumLineWidth) {
            ImagePlus tmpImage = singleScaleRidgeDetection(inputImage, currentLineWidth);
            currentImage = ImageCalculator.run(currentImage, tmpImage, "OR create");
            currentLineWidth += 1;
        }

        return currentImage;
    }

    private ImagePlus singleScaleRidgeDetection(ImagePlus inputImage, int lineWidth) {
        // Copy our input image so that it doesn't mutate it
        ImagePlus dataImage = ImageUtils.duplicateImage(inputImage);

        // Calculate our necessary values
        double sigma = this.calculateSigma(lineWidth);
        double lowerThreshold = this.calculateLowerThreshold(lineWidth, sigma);
        double upperThreshold = this.calculateUpperThreshold(lineWidth, sigma);

        // Build our IJ run command
        StringBuilder commandOptions = new StringBuilder();
        commandOptions.append("line_width=").append(lineWidth);
        commandOptions.append(" high_contrast=").append(TWOMBLIRunner.HIGH_CONTRAST_THRESHOLD);
        commandOptions.append(" low_contrast=").append(TWOMBLIRunner.LOW_CONTRAST_THRESHOLD);
        if (this.darkLines) {
            commandOptions.append(" darkline");
        }
        commandOptions.append(" extend_line");
        commandOptions.append(" make_binary");
        commandOptions.append(" method_for_overlap_resolution=NONE");
        commandOptions.append(" sigma=").append(sigma);
        commandOptions.append(" lower_threshold=").append(lowerThreshold);
        commandOptions.append(" upper_threshold=").append(upperThreshold);
        commandOptions.append(" minimum_line_length=").append(this.minimumBranchLength);
        commandOptions.append(" maximum=0");

        // Run the command
        IJ.run(dataImage, "Ridge Detection", commandOptions.toString());

        // Grab and hide our make binary outcome
        ImagePlus binaryImage = IJ.getImage();
        binaryImage.hide();
        return binaryImage;

        /*
        This is an attempt to call the class contents directly - but I think the maintenance overhead isn't worth it?

        // Preprocessing steps

        // Run ridge detection
        LineDetector lineDetector = new LineDetector();
        lineDetector.detectLines(this.img, sigma, upperThreshold, lowerThreshold, this.minimumBranchLength, 0, this.darkLines, false, false, true, OverlapOption.NONE);

        // Final steps
         */
    }

    private double calculateSigma(int lineWidth) {
        return lineWidth / (2 * Math.sqrt(3.0)) + 0.5;
    }

    private double calculateLowerThreshold(int lineWidth, double sigma) {
        double lowerThresholdLimit = this.darkLines ? 255.0 - TWOMBLIRunner.HIGH_CONTRAST_THRESHOLD : TWOMBLIRunner.LOW_CONTRAST_THRESHOLD;
        return this.calculateThreshold(lineWidth, sigma, lowerThresholdLimit);
    }

    private double calculateUpperThreshold(int lineWidth, double sigma) {
        double upperThresholdLimit = this.darkLines ? 255.0 - TWOMBLIRunner.LOW_CONTRAST_THRESHOLD : TWOMBLIRunner.HIGH_CONTRAST_THRESHOLD;
        return this.calculateThreshold(lineWidth, sigma, upperThresholdLimit);
    }

    private double calculateThreshold(int lineWidth, double sigma, double thresholdLimit) {
        double halfLineWidth = lineWidth / 2.0;
        return 0.17 * Math.floor(Math.abs(-2 * thresholdLimit * halfLineWidth / (Math.sqrt(2 * Math.PI) * sigma * sigma * sigma) * Math.exp(-(halfLineWidth * halfLineWidth) / (2 * sigma * sigma))));
    }

    private void runAnamorf(String maskImageDirectory) {
        DefaultParams props = new DefaultParams();
        try {
            // Default params
            if (this.anamorfPropertiesFile == null) {
                ClassLoader classLoader = getClass().getClassLoader();
                InputStream inputStream = classLoader.getResourceAsStream("AnamorfProperties.xml");
                props.loadFromXML(inputStream);
            }

            // Custom params
            else {
                File propsFile = new File(this.anamorfPropertiesFile);
                props.loadFromXML(Files.newInputStream(propsFile.toPath()));
            }
        }

        catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        // Have to dodge around some of the usual invocation methods here...
        Batch_Analyser batchAnalyser = new Batch_Analyser(true, new File(maskImageDirectory), props);
        Batch_Analyser.getProps().setProperty(DefaultParams.CURVE_LABEL, "true");
        Batch_Analyser.getProps().setProperty(DefaultParams.CURVE_WIN_LABEL, String.valueOf(this.minimumCurvatureWindow));
        Batch_Analyser.getProps().setProperty(DefaultParams.MIN_BRANCH_LABEL, String.valueOf(this.minimumBranchLength));
        batchAnalyser.run(null);

        // Alter our curvature windows - by default this does nothing???
        for (int curvature = this.minimumCurvatureWindow + this.curvatureWindowStepSize; curvature <= this.maximumCurvatureWindow; curvature += this.curvatureWindowStepSize) {
            // Reset
            Batch_Analyser.getProps().setProperty(DefaultParams.BOX_COUNT_LABEL, "false");
            Batch_Analyser.getProps().setProperty(DefaultParams.CIRC_LABEL, "false");
            Batch_Analyser.getProps().setProperty(DefaultParams.CURVE_LABEL, "false");
            Batch_Analyser.getProps().setProperty(DefaultParams.FOURIER_FRAC_LABEL, "false");
            Batch_Analyser.getProps().setProperty(DefaultParams.LAC_LABEL, "false");
            Batch_Analyser.getProps().setProperty(DefaultParams.MEAN_BRANCH_LABEL, "false");
            Batch_Analyser.getProps().setProperty(DefaultParams.NUM_BRANCH_LABEL, "false");
            Batch_Analyser.getProps().setProperty(DefaultParams.NUM_END_LABEL, "false");
            Batch_Analyser.getProps().setProperty(DefaultParams.PROJ_AREA_LABEL, "false");
            Batch_Analyser.getProps().setProperty(DefaultParams.TOT_LENGTH_LABEL, "false");

            // TODO: We don't set the CURVE_LABEL again?
            Batch_Analyser.getProps().setProperty(DefaultParams.CURVE_LABEL, "true");
            Batch_Analyser.getProps().setProperty(DefaultParams.CURVE_WIN_LABEL, String.valueOf(curvature));
            batchAnalyser.run(null);
        }
    }

    private void runHDM(String hdmDirectory, String hdmResultsDirectory, ImagePlus inputImage) {
        ImagePlus hdmBaseImage = ImageUtils.duplicateImage(inputImage);

        // Make 8 Bit
        IJ.run(hdmBaseImage, "8-bit", "");

        // Invert if the lines are dark
        if (!this.darkLines) {
            IJ.run(hdmBaseImage, "Invert", "");
        }

        // Set our min/max
        IJ.setMinAndMax(hdmBaseImage, 0, this.maximumDisplayHDM);

        // Apply LUT and invert
        IJ.run(hdmBaseImage, "Apply LUT", "");
        IJ.run(hdmBaseImage, "Invert", "");

        // Enhance the contrast
        IJ.run(hdmBaseImage, "Enhance Contrast", "saturated=" + this.contrastSaturation);

        // Save the HDM base file
        String filaPath = hdmDirectory + File.separator + this.filePrefix + "_hdm.png";
        IJ.saveAs(hdmBaseImage, "png", filaPath);

        // Find our HDM Macro and run it. Note, the macro has an undeclared dependency on bioformats
        new LociFunctions();
        IJ.runMacro("run(\"Bio-Formats Macro Extensions\")");
        Macro_Runner.runMacroFromJar("Quant_Black_Space.ijm", filaPath);
        ResultsTable resultsTable = Analyzer.getResultsTable();
        try {
            resultsTable.saveAs(hdmResultsDirectory + File.separator + this.filePrefix + "_ResultsHDM.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runOrientationJ(ImagePlus maskImage) {
        IJ.runPlugIn(maskImage, "OrientationJ_Dominant_Direction", "");
        IJ.renameResults("Dominant Direction of " + maskImage.getTitle(), "Results");

        // Obtain our alignment vector
        ResultsTable resultsTable = Analyzer.getResultsTable();
        this.alignment = resultsTable.getValue("Coherency [%]", 0);
        this.dimension = maskImage.getHeight() * maskImage.getWidth();

        // Close everything down
        IJ.run("Clear Results");
    }

    private void performGapAnalysis(String gapAnalysisDirectory, ImagePlus maskImage) {
        if (!this.performGapAnalysis) {
            return;
        }

        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager != null) {
            roiManager.reset();
        }

        // Wrap everything in a try catch because we are writing to the file line by line
        int width = maskImage.getWidth();
        int height = maskImage.getHeight();
        maskImage.setRoi(new Roi(1, 1, width - 2, height - 2));
        IJ.run(maskImage, "Crop", "");
        IJ.run(maskImage, "Canvas Size...", "width=" + width + " height=" + height + " position=Center zero");
        IJ.run(maskImage, "Max Inscribed Circles", "minimum_disk=" + this.minimumGapDiameter + " minimum_similarity=0.50 closeness=5");

        if (roiManager == null) {
            roiManager = RoiManager.getInstance();
        }

        // ROIs
        int nROIs = RoiManager.getInstance().getCount();

        // Gap Image
        ImagePlus duplicateImage = ImageUtils.duplicateImage(maskImage);
        WindowManager.setTempCurrentImage(duplicateImage);
        IJ.run(duplicateImage, "RGB Color", "");
        IJ.setForegroundColor(255, 0, 0);
        for (int i = 0; i < nROIs; i++) {
            roiManager.select(duplicateImage, i);
            Roi roi = roiManager.getRoi(i);
            roi.setStrokeWidth(3);
            roiManager.runCommand("Draw");
        }
        IJ.saveAs(duplicateImage, "png", gapAnalysisDirectory + File.separator + this.filePrefix + "_gap.png");
        duplicateImage.close();
        WindowManager.setTempCurrentImage(null);

        // Perform our measurements
        WindowManager.setTempCurrentImage(maskImage);
        Analyzer.setMeasurement(Analyzer.AREA, true);
        roiManager.runCommand("measure");
        ResultsTable resultsTable = ResultsTable.getResultsTable();
        double[] areas = resultsTable.getColumn("Area");
        Arrays.sort(areas);

        // Calc mean
        double sum = 0;
        for (double area : areas) {
            sum += area;
        }
        double mean = sum / areas.length;

        // Standard Deviation
        double sumOfSquares = 0;
        for (double area : areas) {
            sumOfSquares += Math.pow(area - mean, 2);
        }
        double standardDeviation = Math.sqrt(sumOfSquares / areas.length);
        double fivePercentile = this.percentile(areas, 5);
        double fiftyPercentile = this.percentile(areas, 50);
        double ninetyFivePercentile = this.percentile(areas, 95);

        // Write this to an individual file for later aggregation
        String individualGapAnalysisFilePath = gapAnalysisDirectory + File.separator + this.filePrefix + "_gaps.csv";
        File individualGapAnalysisFile = new File(individualGapAnalysisFilePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(individualGapAnalysisFile))) {
            bw.write(this.filePrefix + " " + mean + " " + standardDeviation + " " + fivePercentile + " " + fiftyPercentile + " " + ninetyFivePercentile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Write the array to file
        String individualGapAnalysisArrayFilePath = gapAnalysisDirectory + File.separator +  this.filePrefix + "_area_arrays.csv";
        File individualGapAnalysisArrayFile = new File(individualGapAnalysisArrayFilePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(individualGapAnalysisArrayFile))) {
            for (double area : areas) {
                bw.write(area + "\n");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        WindowManager.setTempCurrentImage(null);
    }

    private double percentile(double[] values, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * values.length);
        return values[index - 1];
    }

    private void closeNonImages() {
        Frame[] frames = WindowManager.getNonImageWindows();
        for (Frame frame : frames) {
            frame.dispose();
        }
    }
}
