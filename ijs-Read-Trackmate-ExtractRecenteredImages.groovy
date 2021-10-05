// Script which opens a xml trackmate file (v7) and extract 
// an image for all tracks, a cropped region surrounding the track

#@File(label = "Open your trackmate file") file

logger = Logger.IJ_LOGGER

//-------------------
// Instantiate reader
//-------------------

def reader = new TmXmlReader( file )
if (!reader.isReadingOk()) {
    sys.exit( reader.getErrorMessage() )
}
    
//-----------------
// Get a full model
//-----------------

// This will return a fully working model, with everything
// stored in the file. Missing fields (e.g. tracks) will be
// null or None in python
def model = reader.getModel()
// model is a fiji.plugin.trackmate.Model

//----------------
// Display results
//----------------

// We can now plainly display the model. It will be shown on an
// empty image with default magnification because we do not 
// specify an image to display it. We will see later how to 
// retrieve the image on which the data was generated.

// A selection.
def sm = new SelectionModel( model )

// Read the default display settings.
def ds = DisplaySettingsIO.readUserDefault()

// The viewer.
def displayer = new HyperStackDisplayer( model, sm, ds ) 
displayer.render()

//---------------------------------------------
// Get only part of the data stored in the file
//---------------------------------------------

// You might want to access only separate parts of the
// model.

def spots = model.getSpots()

def imp = reader.readImage()
settings = reader.readSettings( imp )

// If you want to get the tracks, it is a bit trickier.
// Internally, the tracks are stored as a huge mathematical
// simple graph, which is what you retrieve from the file.
// There are methods to rebuild the actual tracks, taking
// into account for everything, but frankly, if you want to
// do that it is simpler to go through the model:

def trackIDs = model.getTrackModel().trackIDs(true) // only filtered out ones

//boolean extracted = false

def trackMate = new TrackMate(model, settings)

for (id in trackIDs) {
    Set<Spot> spots_of_track = model.getTrackModel().trackSpots(id)
	ExtractTrackStackAction.trackStack(trackMate, spots_of_track.iterator().next(), false, logger).show()
}

import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer
import fiji.plugin.trackmate.io.TmXmlReader
import fiji.plugin.trackmate.Logger
import fiji.plugin.trackmate.Settings
import fiji.plugin.trackmate.SelectionModel
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO
import java.io.File
import fiji.plugin.trackmate.action.ExtractTrackStackAction
import fiji.plugin.trackmate.TrackMate
import fiji.plugin.trackmate.Spot
