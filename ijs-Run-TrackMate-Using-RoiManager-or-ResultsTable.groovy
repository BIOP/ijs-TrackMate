#@ Integer frameGap    ( label="Max Frame Gap [frames]" ) 
#@ Double linkDistance ( label="Linking Max Distance [calibrated]" ) 
#@ Double gapDistance  ( label="Gap Closing Max Distance [calibrated]" ) 
#@ Boolean allowSplit  ( label="Allow Track Splitting" ) 
#@ Double splitDistance ( label="Split Distance [calibrated]" )
#@ RoiManager rm
#@ ImagePlus imp

/* 
 *  Adapted from python: https://gist.github.com/ekatrukha/ac32d624665da81ad521ab4b040a4bad by Eugene Katrukha
 *
 * = CODE DESCRIPTION =
 * Will run the TrackMate LAP tracker based on the ROIs in the ROI Manager instead of using TrackMate's spot detection results.
 * It takes the centroid coordinates of the ROIs and uses the ROI area to estiamte a spot radius.
 * It can also use the coordinates from a ResultsTable.
 * 
 * This script uses the LAP tracker from TrackMate and allows for splitting events. 
 * This is then reflected in the names of the ROIs in the Roi Manager. 
 * 	 Each cell will have a unique number when the track is created and that name will be prepended in the daughter cells.
 *   Eg. After being tracked for 6 frames, Cell 4 divides, 
 *       the daughter cells will be called 4-1 and 4-2. 
 *       If cell 4-2 divides, its daughter cells will be named 4-2-1 and 4-2-2
 *   
 *   Graph:         
 *   ------
 *   
 *      4-1
 *     /   
 *  4 -       4-2-1
 *     \     / 
 *      4-2 -
 *    	     \
 *    	      4-2-2
 *    		 
 * 
 * == INPUTS ==
 * 1. An open image: Trackmate will keep the absolute path to this image in its settings file
 * NOTE: We recommend that the the image you are going to process has already been saved or opened from a saved location
 * 
 * WARNING: If you run this script on an image that was never saved before, when you reopen the TrackMate settings, it will be unable to link
 *          the tracks to your image and will create an empty one.
 * 
 * WARNING 2: Trackmate sets the calibration from the image metatada. Make sure that your stack is defined in Frames (T) and not Slices (Z)
 *            User Image > Properties... to confirm this and change it as needed.
 * 
 * 2. The RoiManager populated with the ROIs at the right stack positions
 * NOTE: The script uses the getPosition() method from the ij.gui.Roi class to find the frame
 * 
 * WARNING: Make sure that your ROIs were created on non-hyperstacks (Single channel 2D timelapse).
 *          In case you have hyperstacks, you will need to modify the position of your ROIs BEFORE running the "Tracker" part of this script
 *          so that the "Tracker" can query the frame number using the getPosition() method. See the example in the comments.
 * 
 * 
 * == OUTPUTS ==
 * The Tracking results from Trackmate through the TrackMate GUI for manual curation of the tracks if desired.
 * It also saves both the TrackMate project file and the tracks as XML files.
 *
 * = DEPENDENCIES =
 * FIJI ( which bundles TrackMate )
 * Tested on TrackMate_-6.0.1
 * 
 * = INSTALLATION = 
 * Open the script in FIJI and hit run
 * 
 * = AUTHOR INFORMATION =
 * Orignal Python code by Eugene Katrukha: https://gist.github.com/ekatrukha/ac32d624665da81ad521ab4b040a4bad
 * Code Groovyfied and extended by Olivier Burri, EPFL - SV - PTECH - BIOP
 * Reviewing, Testing by Romain Guiet, EPFL - SV - PTECH - BIOP 
 * February 2021
 * 
 * = COPYRIGHT =
 * © All rights reserved. ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP), 2021
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

//-----------------------------
//			MAIN 
//-----------------------------

// Get ROIs from the Manager
def roiList = rm.getRoisAsArray() as List

// Example: Manipulate ROIS to change the position here if needed here
// EG: Here we switch the T position of a roi to pat to the "regular" position
//def roiList = roiList.collect{ roi -> 
//	def p = roi.getTPosition()
//	roi.setPosition( p )
//	return roi
//}


// Example: Get results table if the spots should be populated by a results table
//def resultsTable = ResultsTable.getResultsTable()

//Remove overlay if any
imp.setOverlay( null )

// Initialize the tracker
def tracker = new TrackerBuilder( imp )
						 .setSpots( roiList )
					  // .setSpots( resultsTable ) // instead of setting them via a list of Rois
					  	 .setLinkingDistance( linkDistance )
					  	 .setFrameGap( frameGap )
					  	 .setGapDistance( gapDistance )
					  	 .setAllowSplit( allowSplit )
					  	 .setSplitDistance( splitDistance )
					  	 .build()

// Do The Tracking
ok = tracker.process()
if ( !ok ) IJ.error(tracker.trackmate.getErrorMessage())

// Display Results in TrackMate if you want to view it.
tracker.displayResultsInGUI()

//Color ROI by track ID
tracker.colorRoisByTrack( rm )

// Sort the tracks in the RoiManager
rm.runCommand( "sort" )

// Example: Export in the same location as original image
def name = FilenameUtils.removeExtension( imp.getTitle() )
def fileDirectory = IJ.getDirectory( "Image" )
def trackmateFile = new File( fileDirectory, name + "_trackmate.xml" )

tracker.export( trackmateFile )

println "Script Done"


//-----------------------------
//			CLASSES 
//-----------------------------
class Tracker {
	
	TrackMate trackmate
	ImagePlus rawImage
	SpotCollection spots

	Double linkDistance
	Integer frameGap
	Double gapDistance
	Boolean allowSplit
	Double splitDistance

	// Custom features to declare for TrackMate
	def customFeatures = 			["ROI_INDEX", "AREA"]
	def customFeatureNames = 		[ROI_INDEX:"ROI Index", AREA:"area"]
	def customFeatureShortNames = 	[ROI_INDEX:"rIdx", AREA:"ar"]
	def customFeatureDimensions = 	[ROI_INDEX:Dimension.NONE, AREA:Dimension.NONE]
	def customFeatureAreBoolean =  	[ROI_INDEX:false, AREA:false]

	// Constructor which should not be called directyle except by the builder
	private Tracker( SpotCollection spots, ImagePlus imp, linkDistance, frameGap, gapDistance, allowSplit, splitDistance ) {
		this.spots = spots
		this.rawImage = imp
		this.linkDistance = linkDistance
		this.frameGap = frameGap
		this.gapDistance = gapDistance
		this.allowSplit = allowSplit
		this.splitDistance = splitDistance
		
		// Create Trackmate instance from all settings on this imageplus if possible
		def cal = imp.getCalibration()
		
		// Prepare model
		def model = new Model()
		model.setLogger( Logger.IJ_LOGGER )
		model.setPhysicalUnits( cal.getUnit(), cal.getTimeUnit() )
		
		//Prepare settings
		def settings = new Settings()
		settings.setFrom( this.rawImage )
	
		//Create the TrackMate instance.
		this.trackmate = new TrackMate( model, settings )
	
		// Add ALL the feature analyzers known to TrackMate, via
		// providers. 
		// They offer automatic analyzer detection, so all the 
		// available feature analyzers will be added. 
		// Some won"t make sense on the binary image (e.g. contrast)
		// but nevermind.
		settings.clearSpotAnalyzerFactories()
		def spotAnalyzerProvider = new SpotAnalyzerProvider()
		spotAnalyzerProvider.getKeys().each{ key -> settings.addSpotAnalyzerFactory( spotAnalyzerProvider.getFactory( key ) ) }
		
		def edgeAnalyzerProvider = new EdgeAnalyzerProvider()
		edgeAnalyzerProvider.getKeys().each{ key -> settings.addEdgeAnalyzer( edgeAnalyzerProvider.getFactory( key ) ) }
	
		def trackAnalyzerProvider = new TrackAnalyzerProvider()

		trackAnalyzerProvider.getKeys().each{ key -> settings.addTrackAnalyzer( trackAnalyzerProvider.getFactory( key ) ) }
	
		this.trackmate.getModel().getLogger().log( settings.toStringFeatureAnalyzersInfo() )
		this.trackmate.computeSpotFeatures( true )
		this.trackmate.computeEdgeFeatures( true )
		this.trackmate.computeTrackFeatures( true )

		// Add custom features here otherwise it will not save
		model.getFeatureModel().declareSpotFeatures( customFeatures, customFeatureNames, customFeatureShortNames, customFeatureDimensions, customFeatureAreBoolean )

		//Skip detection and get spots from results table.
		model.setSpots( spots, false )

		//Configure detector. We put nothing here, since we already have the spots from previous step.
		settings.detectorFactory = new ManualDetectorFactory()
		def detectorSettings = [:]
		settings.detectorSettings["RADIUS"] = 1.0
	
		//Configure tracker
		settings.trackerFactory = new SparseLAPTrackerFactory()
		settings.trackerSettings = LAPUtils.getDefaultLAPSettingsMap()
		settings.trackerSettings[ "LINKING_MAX_DISTANCE" ] 	   = this.linkDistance
		settings.trackerSettings[ "GAP_CLOSING_MAX_DISTANCE" ] = this.gapDistance
		settings.trackerSettings[ "MAX_FRAME_GAP" ]			   = this.frameGap
		settings.trackerSettings["ALLOW_TRACK_SPLITTING"]  	   = this.allowSplit
		settings.trackerSettings["SPLITTING_MAX_DISTANCE"]	   = this.splitDistance
	
		settings.initialSpotFilterValue = -1.0
	}

	def process( ) {
		//Execute the full process EXCEPT for the detection step.
	
		// Check settings.
		def ok = this.trackmate.checkInput()
		// Initial filtering
		println( "Spot initial filtering" )
		ok = ok && this.trackmate.execInitialSpotFiltering()
		// Compute spot features.
		println( "Computing spot features" )
		ok = ok && this.trackmate.computeSpotFeatures( true ) 
		// Filter spots.
		println( "Filtering spots" )
		ok = ok && this.trackmate.execSpotFiltering( true )
		// Track spots.
		println( "Tracking" )
		ok = ok && this.trackmate.execTracking()
		// Compute track features.
		println( "Computing track features" )
		ok = ok && this.trackmate.computeTrackFeatures( true )
		// Filter tracks.
		println( "Filtering tracks" )
		ok = ok && this.trackmate.execTrackFiltering( true )
		// Compute edge features.
		println( "Computing link features" )
		ok = ok && this.trackmate.computeEdgeFeatures( true )
	
		return ok
	}

	/*
	 * Create and show a TrackMate GUI to configure the display of the results. 
	 * This might not always be desriable in e.g. batch mode, but 
	 * this allows to save the data, export statistics in IJ tables then
	 * save them to CSV, export results to AVI etc...
	 */
	def displayResultsInGUI() {
		
		def gui = new TrackMateGUIController( this.trackmate )
	
		//Link displayer and GUI.
		def model = trackmate.getModel()
		def selectionModel = new SelectionModel( model )
		def displayer = new HyperStackDisplayer( model, selectionModel, this.rawImage )
		
		gui.getGuimodel().addView( displayer )
		
		def displaySettings = gui.getGuimodel().getDisplaySettings()
		
		displaySettings.keySet().each{ key -> displayer.setDisplaySettings( key, displaySettings.get( key ) ) }
		
		displayer.render()
		displayer.refresh()
	
		gui.setGUIStateString( "ConfigureViews" )
	}

	/*
	 * Colors the ROIs stored in the specified ROIManager rm using a color
	 * determined by the track ID they have.
	 * We retrieve the IJ ROI that matches the TrackMate Spot because in the
	 * latter we stored the index of the spot.
	 */
	def colorRoisByTrack( RoiManager rm ) {

		def model = this.trackmate.getModel()

		// Store total number of tracks
		def nTracks = model.getTrackModel().nTracks( false )
		
		def track_indices = model.getTrackModel().trackIDs( true ).collect{ it }
		
		// Define track indexes and their colors
		def index = 0

		// Define track colors
		def track_colors = track_indices.collect{ track_id -> return new Color( Color.HSBtoRGB( track_id / nTracks, 0.9, 1.0 ) )	}
	
		// Go through the spots, name them base on philogeny if any
		[track_indices, track_colors].transpose().each{ track_id, track_color ->
			
			IJ.log("Track ID: "+track_id)
			def trackSpots = model.getTrackModel().trackSpots( track_id )
	
			// Need to sort the spots
			def spotsSorted = new ArrayList<Spot>( trackSpots )
			def comparator = Spot.frameComparator
			Collections.sort( spotsSorted, comparator )
	
			// From the sorted spots, we need to name the philogeny
			spotsSorted.eachWithIndex{ spot, idx ->
				// We will need the edges in order to name the child objects of this spot
				def edges = model.getTrackModel().edgesOf(spot)
						
				// If these are sorted then the first one is the start of the track
				if (idx == 0 ) spot.setName( (track_id+1).toString()) // Name the first spot the same as the track ID
						
				def name = spot.getName()
				// We assume the spot is named properly now
	
				// Prepare to name the child spots. Prepare a counter for split events
				// Fort each edge, change only those where the current spot is a source
				def splitIdx = 1
				edges.each{
					if( it.getSource().equals( spot ) ) {
						// Get the child  spot
						def target = it.getTarget()
	
						// If there are more than 2 edges (How we reached the current spot followed by how to go to the next spots from this spot)
						// Then this means there was a division, so we rename the spot
						if (edges.size() > 2) target.setName( name+"-"+ splitIdx++ )
								
						// Otherwise it is the same cell, so no need to change its name
						else target.setName( name )
					}
				}
	
				// Prepare to name the ROI
				def t = spot.getFeature( "FRAME" ) as int
				def roi_idx = spot.getFeature('ROI_INDEX') as int
				def roi = rm.getRoi( roi_idx )
				roi.setFillColor( track_color )
				
				rm.rename(roi_idx ,"Track-"+IJ.pad(track_id+1,3)+":Frame-"+IJ.pad(t,3)+":Cell-"+spot.getName()+":ID-"+roi_idx)
			}
		}
	}

	/*
	 * Return the results of this tracking as a simple results table
	 */
	ResultsTable obtainTrackResults() {
		def rt = new ResultsTable()
		def model = this.trackmate.getModel()
		def trackIDs = model.getTrackModel().trackIDs(false)
		trackIDs.each { id ->
			def spots = model.getTrackModel().trackSpots(id).toSorted{ it.getFeature("POSITION_T") }
			spots.each{ spot ->
				rt.incrementCounter()
				rt.addLabel( measurementImage.getTitle())
				rt.addValue( "Track ID", id)
				rt.addValue( "ROI name", spot.getName())
				rt.addValue( "Frame", spot.getFeature("POSITION_T"))
				rt.addValue( "Area", spot.getFeature("AREA"))
			}
		}
		return rt
	}

	/*		
	 * Export this trackmate result to an XML file to reuse later		
	 * This also records the location of the ImagePlus that was opened. So it is important to save it **BEFORE** running trackmate
	 */ 		
	void export( File outFile ) {
		def  fname = outFile.getName()
		def pos = fname.lastIndexOf(".")
		fname = fname.substring(0, pos)

		// Export the tracks
		def tracksFile = new File( outFile.getParent(), fname+"_Tracks.xml" )
		ExportTracksToXML.export( this.trackmate.getModel(),  this.trackmate.getSettings(), tracksFile )

		// Export the Trackmate file
		def writer = new TmXmlWriter( outFile )
		writer.appendModel( trackmate.getModel() )
		writer.appendSettings( trackmate.getSettings() )
		writer.writeToFile()
	}
}

// Inner Builder to set everything up, which contains sensible defaults
class TrackerBuilder {
		SpotCollection spots
		ImagePlus rawImage
		Double linkDistance = 15
		Integer frameGap = 2
		Double gapDistance = 15
		Boolean allowSplit = false
		Double splitDistance = 10
	
		public TrackerBuilder( ImagePlus rawImage ) {
			this.rawImage = rawImage
		}
		
		public TrackerBuilder setSpots( List<Roi> rois ) {
			if( rois.isEmpty() ) 
				IJ.error("Roi List is Empty!")
			this.spots = spotsFromRois(rois, this.rawImage.getCalibration())
			return this
		}
		
		public TrackerBuilder setSpots( ResultsTable results ) {
			// TODO. Check results table contains the right columns
			this.spots = spotsFromResultsTable( results, this.rawImage.getCalibration().frameInterval )
			return this
		}

		public TrackerBuilder setLinkingDistance( Double linkDistance ) {
			this.linkDistance = linkDistance
			return this
		}

		public TrackerBuilder setFrameGap( Double frameGap ) {
			this.frameGap = frameGap
			return this
		}

		public TrackerBuilder setGapDistance( Double gapDistance ) {
			this.gapDistance = gapDistance
			return this
		}

		public TrackerBuilder setAllowSplit( Boolean allowSplit ) {
			this.allowSplit = allowSplit
			return this
		}

		public TrackerBuilder setSplitDistance( Double splitDistance ) {
			this.splitDistance = splitDistance
			return this
		}

		public Tracker build() {
			return new Tracker( this.spots, this.rawImage, this.linkDistance, this.frameGap, this.gapDistance, this.allowSplit, this.splitDistance )
		}
	/* 
	 *  Creates a spot collection from a series of rois.
	 *  Requires the frame index to be retrievable using roi.getPosition().
	 *  All values will be uncalibrated unless provided with a calibration object
	 */
	def spotsFromRois(def rois, def cal) {
		// We assume that the ROIs have their position set. Not their czt position
		def spots = new SpotCollection()
		
		rois.eachWithIndex{ roi, idx ->
	
			def x =  roi.getContourCentroid()[0]
			def y =  roi.getContourCentroid()[1]
			def z = 0.0 // No support for Z
			def frame = roi.getPosition() - 1
			def t = frame
			def area =  roi.getStatistics().area
			def radius =  Math.sqrt( area / Math.PI )
			def quality = idx
	
			// Apply calibration if available
			if ( cal instanceof Calibration ) {
				x = cal.getX (x )
				y = cal.getY( y )
				radius = cal.getX( radius )
				t = ( frame ) * cal.frameInterval
			}
			// Create the spot
			def spot = new Spot( x, y, z, radius, quality )
	
			// Add basic features
			spot.putFeature( "POSITION_T", t )
			spot.putFeature( "AREA", area )
			spot.putFeature( "ROI_INDEX", idx )
			
			spots.add( spot, frame )
		}
		
		return spots
	}
	
	/*
	 * Creates a spot collection from a results table in ImageJ.
	 * Requires the current results table, in which the results from 
	 * particle analysis should be. We need at least the center
	 * of mass, the area and the slice to be specified there.
	 * We also query the frame interval to properly generate the 
	 * POSITION_T spot feature.
	 */
	def spotsFromResultsTable( results_table, frame_interval ) {
	
		def frames = results_table.getColumnAsDoubles( results_table.getColumnIndex( "Slice" ) )
		def xs = results_table.getColumnAsDoubles( results_table.getColumnIndex( "XM" ) )
		def ys = results_table.getColumnAsDoubles( results_table.getColumnIndex( "YM" ) )
		def z = 0.
		//Get radiuses from area.
		areas = results_table.getColumnAsDoubles( results_table.getColumnIndex( "Area" ) )
		spots = new SpotCollection()
	
		(0..<xs.length).each{ i->
			x = xs[i]
			y = ys[i]
			frame = frames[i]
			area = areas[i]
			t = (frame - 1) * frame_interval
			radius = Math.sqrt( area / Math.PI )
			quality = i // Store the line index, to later retrieve the ROI.
			spot = new Spot( x, y, z, radius, quality )
			spot.putFeature( "POSITION_T", t )
			spot.putFeature( "AREA", area )
			spot.putFeature( "ROI_INDEX", i )
			spots.add( spot, frame as int )
		}
		return spots	
	}
}

// All Imports
import java.awt.Color

import ij.*
import ij.gui.*
import ij.measure.ResultsTable
import ij.plugin.ChannelSplitter
import ij.measure.Calibration
import ij.plugin.frame.RoiManager

import fiji.plugin.trackmate.*
import fiji.plugin.trackmate.tracking.sparselap.*
import fiji.plugin.trackmate.tracking.LAPUtils
import fiji.plugin.trackmate.action.ExportTracksToXML
import fiji.plugin.trackmate.detection.ManualDetectorFactory
import fiji.plugin.trackmate.providers.* 
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer
import fiji.plugin.trackmate.gui.TrackMateGUIController
import fiji.plugin.trackmate.io.TmXmlWriter
import fiji.plugin.trackmate.Dimension

import org.apache.commons.io.FilenameUtils

import fiji.plugin.trackmate.*
import fiji.plugin.trackmate.tracking.sparselap.*
import fiji.plugin.trackmate.tracking.LAPUtils
import fiji.plugin.trackmate.action.ExportTracksToXML
import fiji.plugin.trackmate.detection.ManualDetectorFactory
import fiji.plugin.trackmate.providers.* 
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer
import fiji.plugin.trackmate.gui.TrackMateGUIController
import fiji.plugin.trackmate.io.TmXmlWriter
import fiji.plugin.trackmate.Dimension

import org.apache.commons.io.FilenameUtils
