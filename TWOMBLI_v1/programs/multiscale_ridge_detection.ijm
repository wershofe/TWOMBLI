var contrastHigh = 200;
var contrastLow = 100;

min_line_width = 5;
max_line_width = 20;

outputDir = getDirectory("Choose output directory");

setBatchMode(true);

input = getTitle();
setResult("Image Title", nResults, input);
sigma = calcSigma(min_line_width);
lowerThresh = calcLowerThresh(min_line_width, sigma);
upperThresh = calcUpperThresh(min_line_width, sigma);
print("Running ridge detection for line width " + min_line_width);
run("Ridge Detection", "line_width=" + min_line_width + " high_contrast=" + contrastHigh + " low_contrast=" + contrastLow + " extend_line make_binary method_for_overlap_resolution=NONE sigma=" + sigma + " lower_threshold=" + lowerThresh + " upper_threshold=" + upperThresh + " minimum_line_length=0 maximum=0");
rename("lw_" + min_line_width);
result = getTitle();

for(lw = min_line_width + 1; lw <= max_line_width; lw++){
	sigma = calcSigma(lw);
	lowerThresh = calcLowerThresh(lw, sigma);
	upperThresh = calcUpperThresh(lw, sigma);
	selectWindow(input);
	print("Running ridge detection for line width " + lw);
	run("Ridge Detection", "line_width=" + lw + " high_contrast=" + contrastHigh + " low_contrast=" + contrastLow + " extend_line make_binary method_for_overlap_resolution=NONE sigma=" + sigma + " lower_threshold=" + lowerThresh + " upper_threshold=" + upperThresh + " minimum_line_length=0 maximum=0");
	rename("lw_" + lw);
	this_result = getTitle();
	printResult(lw);
	imageCalculator("OR create", result, this_result);
	rename("Composite_" + lw);
	temp = result;
	result = getTitle();
	close(temp);
	selectWindow(this_result);
	saveAs("PNG", outputDir + File.separator + "mask_" + lw);
	close();
}

saveAs("PNG", outputDir + File.separator + "composite_mask");
close();

saveAs("Results", outputDir + File.separator + "Results.csv");
close("Results");

setBatchMode(false);

print("Done");

function calcSigma(lineWidth){
	return lineWidth / (2 * sqrt(3)) + 0.5;
}

function calcLowerThresh(lineWidth, estimatedSigma){
	clow=contrastLow;
	return 0.17 * floor(abs(-2 * clow * (lineWidth / 2.0)
					/ (sqrt(2 * PI) * estimatedSigma * estimatedSigma * estimatedSigma)
					* exp(-((lineWidth / 2.0) * (lineWidth / 2.0)) / (2 * estimatedSigma * estimatedSigma))));
}

function calcUpperThresh(lineWidth, estimatedSigma){
	chigh = contrastHigh;
	return 0.17 * floor(abs(-2 * chigh * (lineWidth / 2.0)
					/ (sqrt(2 * PI) * estimatedSigma * estimatedSigma * estimatedSigma)
					* exp(-((lineWidth / 2.0) * (lineWidth / 2.0)) / (2 * estimatedSigma * estimatedSigma))));
}

function printResult(lineWidth){
	getHistogram(values, counts, 256);
	setResult("Line Width " + lineWidth, nResults-1, counts[255]);
}
