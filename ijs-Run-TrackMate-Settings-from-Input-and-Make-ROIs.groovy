#@ImagePlus(label="Selected Image") imp
#@String(label="ROI name ", value="myFavROI") roi_name
#@String (visibility=MESSAGE, value="Some TrackMate Detection parameters", required=false) msg1
#@Integer(label="spotRadius (in pixel)",value=10 ) spotRadius
#@Float(label="spot Threshold",value=1.0 ) spotThr
#@Float(label="spot Quality",value=0.1 ) spotQ
#@String (visibility=MESSAGE, value="Some TrackMate LAPTracker parameters", required=false) msg2
#@Integer(label="frameGap ",value=15 ) frameGap
#@Integer(label="gapClosing",value=15 ) gapClosing
#@Boolean(label="Verbose", value=true) verbose
#@RoiManager rm

roi_radius = spotRadius

// Adapted from https://imagej.net/Scripting_TrackMate
// and scripts by olivier.burri 

// = CODE DESCRIPTION =
// Runs TrackMate to detect and track
// then gets spots from tracks, 
// adds corresponding rois (of a size roi_radius ) to the roiManager
// named rois after roi_name and Frame number
// 
// == INPUTS ==
// an open image 
// defined some parameters for detection and Tracking
// 
// == OUTPUTS ==
// rois in the roiManager
// 
// = DEPENDENCIES =
// TrackMate
// 
// = INSTALLATION = 
// open script in FIJI and run
// 
// = AUTHOR INFORMATION =
// Code written by Romain Guiet, Olivier Burri , EPFL - SV -PTECH - BIOP 
// November 2019
// 
// = COPYRIGHT =
// © All rights reserved. ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP), 2018
// 
// Licensed under the BSD-3-Clause License:
// Redistribution and use in source and binary forms, with or without modification, are permitted provided 
// that the following conditions are met:
// 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
//    in the documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products 
//     derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
// BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
// IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import ij.*
import ij.IJ

import fiji.plugin.trackmate.Model
import fiji.plugin.trackmate.Settings

import fiji.plugin.trackmate.detection.LogDetectorFactory

import fiji.plugin.trackmate.features.FeatureFilter
import fiji.plugin.trackmate.tracking.sparselap.SparseLAPTrackerFactory
import fiji.plugin.trackmate.tracking.LAPUtils
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer

import fiji.plugin.trackmate.TrackMate
import fiji.plugin.trackmate.Spot

import ij.gui.OvalRoi
import ij.plugin.frame.RoiManager

rm.reset()


//------------------------
// Prepare model object
//------------------------
def model = new Model()

//------------------------
// Prepare settings objects
//------------------------
 def settings = new Settings()
settings.setFrom(imp)

// Configure detector - We use the Strings for the keys
settings.detectorFactory = new LogDetectorFactory()
// Create and defiens all settings
settings.detectorSettings  = settings.detectorSettings = [ 
    		'DO_SUBPIXEL_LOCALIZATION': true,
    		'RADIUS': (double) spotRadius,
    		'TARGET_CHANNEL': (int) 0,
    		'THRESHOLD': (double) spotThr,
    		'DO_MEDIAN_FILTERING': false
		]

//Configure spot filters - Classical filter on quality
def filter1 = new FeatureFilter('QUALITY', spotQ, true)
settings.addSpotFilter(filter1)

// Configure tracker
settings.trackerFactory =  new SparseLAPTrackerFactory()
// Load default and change some 
settings.trackerSettings = LAPUtils.getDefaultLAPSettingsMap()
settings.trackerSettings['ALLOW_TRACK_SPLITTING']   = false
settings.trackerSettings['ALLOW_TRACK_MERGING']     = false
settings.trackerSettings['MAX_FRAME_GAP']           = (int) frameGap
settings.trackerSettings['GAP_CLOSING_MAX_DISTANCE']= (double) gapClosing
settings.trackerSettings['LINKING_MAX_DISTANCE']    = (double) gapClosing

//------------------------
// Trackmate, run
//------------------------
println "Start of : Analysis"
def trackmate = new TrackMate(model, settings) 
if (trackmate.checkInput()) trackmate.process()
println "End of :  Analysis"


//-----------------
// Export Spots from Tracks as ROIs
//-----------------
exportTrackAsROIs(imp , model, roi_name , roi_radius )

println("Jobs Done!")

//-----------------
// HELPER functions
//-----------------

def void exportTrackAsROIs(ImagePlus imp , Model model, String roi_name, Float roi_radius ){
	def c_index = 1
	def z_index = 1 
	
	exportTrackAsROIs( imp ,  model, roi_name, roi_radius , c_index , z_index )
}

def void exportTrackAsROIs(ImagePlus imp , Model model, String roi_name, Float roi_radius , int c_index , int z_index ){
	def cal = imp.getCalibration()
	def width =  2 * cal.getRawX(roi_radius)

	// iterates tracks
	model.getTrackModel().trackIDs(true).eachWithIndex{ id , cntr ->
		def trackSpots = model.getTrackModel().trackSpots( id )
		// order track's spots by frame 
		// ( to add corresponding ROIs in order to the roiManager)
		def sorted = new ArrayList< Spot >( trackSpots );		
		def comparator = Spot.frameComparator;
       	Collections.sort(sorted, comparator);
       	
		sorted.each{
			// Get the position in XY T
			//println(it.getFeatures())
			def x = cal.getRawX(it.getFeature("POSITION_X"))
			def y = cal.getRawY(it.getFeature("POSITION_Y"))
			// no getRawT so has to do it like this ! 
			def t = Math.round(it.getFeature("POSITION_T") / cal.frameInterval) +1
			
			//Make the roi and add to manager
			def roi = new OvalRoi(x-width/2,y-width/2,width,width)
			def czt_position = imp.getStackIndex( c_index as int,  z_index  as int,  t  as int)
			roi.setPosition( czt_position )
			roi.setName(roi_name+" #"+(id+1)+":frame "+IJ.pad((int)t,3))
			rm.addRoi(roi)
		}	
	}	
}






