/*
	20-11-19
	Written by Esther Wershof
*/

/*
 ------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
	Functions
-------------------------------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
*/

function cropFolder(input) {
		list = getFileList(input);
		list = Array.sort(list);
		for (i = 0; i < list.length; i++) {
			if(File.isDirectory(input + File.separator + list[i]))
	{
				cropFolder(input + File.separator + list[i]);
			//if(endsWith(list[i], suffix))
			} else {
				cropFile(input, list[i]);
			}
		}
	}
	
	function cropFile(input, file) {
		reopenROI();
		open(input + File.separator + file);
		selectRegions(input, file);
	}


// Closing stuff
function reopenROI() {
	run("ROI Manager...");
	selectWindow("ROI Manager");
	     run("Close");
	run("Close All");
	run("ROI Manager...");
}

function selectRegions(input, file){
	regionsToKeep=true;
	if(getBoolean("Are you selecting regions to keep in this image? (Select no, if selecting regions to discard)")){
		print("Open your image. Draw regions of interest and add to ROI manager");
		print("Once you have drawn all regions of interest and added them to ROI manager, click OK");
		waitForUser("Once you have drawn all regions of interest and added them to ROI manager, click OK");
		// roiManager("Add");

		saveIndividual= getBoolean("Do you want to save these ROIs as separate images? (Select no to have a single image with blacked out areas)");

		if(saveIndividual==false)
		{
			rename("Cropped");
			roiManager("Fill");
	
			//run("Duplicate...","Cropped");
			open(input + File.separator + file);
			rename("Original");
			
			
			print("Here 1");
			wait(3000);
	
			imageCalculator("Subtract create", "Original","Cropped");
			rename("Result");
	
			print("Here 2");
			wait(3000);
			
			saveAs("PNG", outputCroppedFolder + File.separator + "cropped_" + file );
		}
		else {
			for (i=0; i<roiManager("count"); ++i) {
		    	open(input + File.separator + file);
				rename("Original");
		    	run("Duplicate...", " ");
				copy = getImageID();
		    	roiManager("Select", i);
		    	run("Crop");
		        saveAs("Tiff", outputCroppedFolder + File.separator + "cropped_region_" + i + "_" + file );
		    }
		}

	} else{
		regionsToKeep=false;
		print("Open your image. Draw regions you want to remove and add to ROI manager");
		print("Once you have drawn all regions you want to remove, click OK");
		waitForUser("Once you have drawn all regions you want to remove, click OK");
		// roiManager("Add");
		
		rename("Result");
		roiManager("Fill");
		saveAs("PNG", outputCroppedFolder + File.separator + "cropped_" + file );

		print("Here 2");
		wait(3000);
	}
}
//---------------------------------------------------------------------------


/*
 ------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
	Script
-------------------------------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
 ------------------------------------------------------------------------------------------------------------------------------
*/

// Choose input and output folders

if (isOpen("Log")) { 
	 selectWindow("Log"); 
	 run("Close"); 
	} 

print("Maximimise the log then click OK"); // ask user to maximimse log
waitForUser("Maximimise the log then click OK");
if (isOpen("Log")) { 				// reposition in top left corner
     selectWindow("Log"); 
     setLocation(0, 0); 
  } 

close("*");
print("Select raw images folder");
inputRawFolder = getDirectory("Select raw images folder");
print("Select where output cropped images will be saved");
outputCroppedFolder = getDirectory("Select where output cropped images will be saved");

cropFolder(inputRawFolder);
close("*");
print("Finished!");
