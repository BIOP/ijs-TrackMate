#@ImagePlus( label="Selected Image" ) imp
#@File( label="Select a TrackMate xml File" ) xmlFile_path
#@String( label="ROI Name ", value="myFavROI" ) roi_name
#@Float( label="ROI radius (in pixel)",value=10 ) roi_radius
#@Boolean( label="Verbose", value=true ) verbose
#@RoiManager rm

/* Adapted from https://imagej.net/Scripting_TrackMate
 * and scripts by olivier.burri 
 *
 * = CODE DESCRIPTION =
 * Gets settings from a TrackMate xml file, 
 * run trackmate
 * get spots from tracks
 * adds corresponding rois (of a size roi_radius ) to the roiManager
 * named rois after roi_name and Frame number
 * 
 * == INPUTS ==
 * a TrackMate xml file
 * an open image 
 * 
 * == OUTPUTS ==
 * rois in the roiManager
 * 
 * = DEPENDENCIES =
 * TrackMate
 * 
 * = INSTALLATION = 
 * open script in FIJI and run
 * 
 * = AUTHOR INFORMATION =
 * Code written by Romain Guiet, Olivier Burri , EPFL - SV -PTECH - BIOP 
 * November 2019
 * 
 * = COPYRIGHT =
 * © All rights reserved. ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP), 2018
 * 
 * Licensed under the BSD-3-Clause License:
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided 
 * that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
 *    in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products 
 *     derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/ 


import ij.*
import ij.gui.OvalRoi

import fiji.plugin.trackmate.*
import fiji.plugin.trackmate.io.TmXmlReader
import fiji.plugin.trackmate.providers.*



rm.reset()

//-------------------
// Instantiate reader
//-------------------
def reader = new TmXmlReader( xmlFile_path )

if ( !reader.isReadingOk() ) sys.exit( reader.getErrorMessage() )

//-----------------
// Start new model
//-----------------

def model = new Model()
if ( verbose ) println( model )

// We start by creating an empty settings object
def settings = new Settings()
  
// Then we create all the providers, and point them to the target model:
def detectorProvider        = new DetectorProvider()
def trackerProvider         = new TrackerProvider()
def spotAnalyzerProvider    = new SpotAnalyzerProvider()
def edgeAnalyzerProvider    = new EdgeAnalyzerProvider()
def trackAnalyzerProvider   = new TrackAnalyzerProvider()
  
// Ouf! now we can flesh out our settings object:
reader.readSettings( settings, detectorProvider, trackerProvider, spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider )

// And we have to assign the current image, for the calibration
settings.setFrom( imp )

//------------------------
// Trackmate, run
//------------------------
println ( "Start of : Analysis" )
def trackmate = new TrackMate( model, settings ) 

if ( trackmate.checkInput() ) {
	println( "Doing: TrackMate" )
	trackmate.process()

	
	//-----------------
	// Export each track as ROIs
	//-----------------
	exportTrackAsROIs( imp , model, roi_name , roi_radius )
	println ( "End of :  Analysis" );
	
} else {
	println ( trackmate.getErrorMessage() ) 

}


//-----------------
// Helpers
//-----------------

def void exportTrackAsROIs( ImagePlus imp , Model model, String roi_name, Float roi_radius ){
	int c_index = 1
	int z_index = 1 
	
	exportTrackAsROIs( imp,  model, roi_name, roi_radius , c_index , z_index )
}

def void exportTrackAsROIs( ImagePlus imp, Model model, String roi_name, Float roi_radius , int c_index , int z_index ){
	def cal = imp.getCalibration()
	def width =  2 * cal.getRawX( roi_radius )

	// iterates tracks
	model.getTrackModel().trackIDs(true).eachWithIndex{ id , cntr ->
		def trackSpots = model.getTrackModel().trackSpots( id )
		
		// order track's spots by frame 
		// ( to add corresponding ROIs in order to the roiManager)
		def sorted = new ArrayList< Spot >( trackSpots );		
		def comparator = Spot.frameComparator;
       	Collections.sort( sorted, comparator );
       	
		sorted.each{
			// Get the position in XY T
			//println(it.getFeatures())
			def x = cal.getRawX( it.getFeature( "POSITION_X" ) )
			def y = cal.getRawY( it.getFeature( "POSITION_Y" ) )
			// no getRawT so has to do it like this ! 
			int t = ( Math.round( it.getFeature( "POSITION_T" ) / cal.frameInterval ) + 1 ) as int
			
			//Make the roi and add to manager
			def roi = new OvalRoi( x - width/2, y - width/2, width, width )
			def czt_position = imp.getStackIndex( c_index, z_index,  t)
			roi.setPosition( czt_position )
			roi.setName( roi_name + "#" +IJ.pad( (id+1) ,3 )+ ":frame " + IJ.pad( t,3 ) )
			rm.addRoi( roi )
		}
	}
}
