
A series of Groovy Scripts that make use of the [**Trackmate**]( https://imagej.net/TrackMate ) API.

Inspired/adapted from [**Scripting Trackmate**](https://imagej.net/Scripting_TrackMate)

# ijs-Run-TrackMate

These scripts will **run TrackMate** to detect spots and track them.

## ijs-Run-TrackMate-Settings-from-Input-and-Make-ROIs.groovy

Runs Trackmate on the current image based on the user settings, and exports the detected and tracked objects as ROIs into ImageJ's Roi Manager.

### Input parameters
<img src="https://github.com/BIOP/ijs-TrackMate/blob/master/images/1-Input-settings-to-run-TrackMate.jpg" title="Input settings" width="50%" align="center">

### Outputs
<img src="https://github.com/BIOP/ijs-TrackMate/blob/master/images/1-Results.PNG" title="Results" width="50%" align="center">

## ijs-Run-TrackMate-Settings-from-XmlData-and-Make-ROIs.groovy

Imports a Trackmate file (saved as an XML), and uses the detection and tracking settings from that file to run Trackmate on the current image. Exports the detected and tracked objects as ROIs into ImageJ's Roi Manager.

### Input parameters
User has to select a TrackMate XML file

<img src="https://github.com/BIOP/ijs-TrackMate/blob/master/images/2-SelectXml.jpg" title="Select xml file" width="50%" align="center">

### Outputs
<img src="https://github.com/BIOP/ijs-TrackMate/blob/master/images/1-Results.PNG" title="Results" width="50%" align="center">

## ijs-Run-TrackMate-Using-RoiManager-or-ResultsTable

Will allow you to run TrackMate based on the provided settings via SciJava Parameters, but instead of using the Spot detection algorithms from Trackmate, it will use the contents of the ROI Manager to populate the centroids of spot-like objects. These will then be used for the actual tracking. 
**NOTE:** the ROIs in the RoiManager neeed to have their **position** set so that when this script queries the frame of each ROI, it can do so using (`getPosition()`)[https://imagej.nih.gov/ij/developer/api/ij/gui/Roi.html#getPosition--].
It can also work from a ResultsTable instead of the ROIManager.

The script saves both the Tracks as XML files and the TrackMate project, which can be reopened using `Plugins > Tracking > Load a TrackMate File...`.

### Input Parameters
An open Image stack and an open RoiManager that contains the Rois to track. It is recommended that the image stack be saved to disk before running this script, as TrackMate will store a reference to this image when saving the XML file.
The tracking parameters that can be set are the "Max Frame Gap", the "Link Distance", the "Max Distance" and the "Split Distance", which should be in calibrated units. The "Allow Split" options configures the LAP tracker to allow for split events. 

### Outputs
After running this script in its current state, you will get 
1. An opened TrackMate instance where you can inspect and curate the tracking results
2. The ROIs in the RoiManager renamed to reflect their associated track, frame and philogeny (if "Allow Split" was checked)
3. Two XML files, one with the TrackMate Project and one with the Tracks results, located in the same folder as the original image.
# ijs-Read-TrackMate-from-XmlData  

These scripts will **NOT run TrackMate**, but read data from the xml file to perform various operations.

## ijs-Read-TrackMate-Tracks-from-XmlData-and-Make-ROIs.groovy
 
Reads an existing trackmate XML file and exports the tracked objects as ROIs into ImageJ's Roi Manager.

### Input Parameters
User has to select a TrackMate XML file 
<img src="https://github.com/BIOP/ijs-TrackMate/blob/master/images/2-SelectXml.jpg" title="Select xml file" width="50%" align="center">

### Outputs
<img src="https://github.com/BIOP/ijs-TrackMate/blob/master/images/1-Results.PNG" title="Results" width="50%" align="center">
