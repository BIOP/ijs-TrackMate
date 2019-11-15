
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



# ijs-Read-TrackMate-from-XmlData  

These scripts will **NOT run TrackMate**, but read data from the xml file to perform various operations.

## ijs-Read-TrackMate-Tracks-from-XmlData-and-Make-ROIs.groovy
 
Reads an existing trackmate XML file and exports the tracked objects as ROIs into ImageJ's Roi Manager.

### Input Parameters
User has to select a TrackMate XML file 
<img src="https://github.com/BIOP/ijs-TrackMate/blob/master/images/2-SelectXml.jpg" title="Select xml file" width="50%" align="center">

### Outputs
<img src="https://github.com/BIOP/ijs-TrackMate/blob/master/images/1-Results.PNG" title="Results" width="50%" align="center">

