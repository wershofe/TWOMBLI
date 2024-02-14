package uk.ac.franciscrickinstitute;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import ij.IJ;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = true, menuPath = "TWOMBLI>Batch Runner")
public class TWOMBLIBatchRunner implements Command {

    @Parameter
    private String inputPath;

    @Parameter
    private String outputPath;

    @Parameter
    private int minimumLineWidth = 5;

    @Parameter
    private int maximumLineWidth = 20;

    @Parameter
    private boolean darkLines = false;

    @Parameter
    private int minimumBranchLength = 10;

    @Parameter
    private String anamorfPropertiesFile = null;

    @Parameter
    private int minimumCurvatureWindow = 40;

    @Parameter
    private int curvatureWindowStepSize = 10;

    @Parameter
    private int maximumCurvatureWindow = 40;

    @Parameter
    private int maximumDisplayHDM = 200;

    @Parameter
    private double contrastSaturation = 0.35;

    @Parameter
    private boolean performGapAnalysis = true;

    @Parameter
    private int minimumGapDiameter = 0;

    // Processing
    private int progressBarCurrent;
    private int progressBarMax;;

    private List<TWOMBLIRunner> runners = new ArrayList<>();

    @Override
    public void run() {
        // Identify our batch targets
        File sourceDirectory = new File(this.inputPath);
        File[] files = sourceDirectory.listFiles((dir, name) -> {
            for (String suffix : TWOMBLIConfigurator.EXTENSIONS) {
                if (name.endsWith(suffix)) {
                    return true;
                }
            }

            return false;
        });

        // Ensure we have files
        if (files == null || files.length == 0) {
            return;
        }

        // Empty our output directory (which should only contain previous run data)
        boolean outcome = FileUtils.verifyOutputDirectoryIsEmpty(this.outputPath);
        if (!outcome) {
            return;
        }

        // Prepare a progress bar and block user input
        this.progressBarCurrent = 0;
        this.progressBarMax = files.length;
        IJ.showMessage("Processing Images. This may take a while. (Press OK to start.)");
        IJ.showProgress(this.progressBarCurrent, this.progressBarMax);

        // Loop through our files, load their images, add to queue
        for (File file : files) {
            ImagePlus img = IJ.openImage(file.getAbsolutePath());

            // Set up our runner
            TWOMBLIRunner runner = new TWOMBLIRunner();
            this.runners.add(runner);
            runner.img = img;
            runner.outputPath = this.outputPath;
            runner.minimumLineWidth = this.minimumLineWidth;
            runner.maximumLineWidth = this.maximumLineWidth;
            runner.darkLines = this.darkLines;
            runner.minimumBranchLength = this.minimumBranchLength;
            runner.anamorfPropertiesFile = this.anamorfPropertiesFile;
            runner.minimumCurvatureWindow = this.minimumCurvatureWindow;
            runner.curvatureWindowStepSize = this.curvatureWindowStepSize;
            runner.maximumCurvatureWindow = this.maximumCurvatureWindow;
            runner.maximumDisplayHDM = this.maximumDisplayHDM;
            runner.contrastSaturation = this.contrastSaturation;
            runner.performGapAnalysis = this.performGapAnalysis;
            runner.minimumGapDiameter = this.minimumGapDiameter;

            // Run our TWOMBLIRunner directly
            runner.run();
        }

        // Finish up by generating our summaries
        this.generateSummaries();
    }

    private void generateSummaries() {
        // Loop through our results and generate a summary
        Path gapsOutputPath = Paths.get(this.outputPath, "gaps_summary.csv");
        Path twombliOutputPath = Paths.get(this.outputPath, "twombli_summary.csv");
        for (TWOMBLIRunner runner : this.runners) {
            // Gather our basic info
            String filePrefix = runner.filePrefix;
            double alignment = runner.alignment;
            double dimension = runner.dimension;
            Path anamorfSummaryPath = Paths.get(this.outputPath, "masks", filePrefix + "_results.csv");
            Path hdmSummaryPath = Paths.get(this.outputPath, "hdm_csvs", filePrefix + "_ResultsHDM.csv");
            Path gapAnalysisPath = Paths.get(this.outputPath, "gap_analysis", filePrefix + "_gaps.csv");

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
            if (this.performGapAnalysis) {
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
