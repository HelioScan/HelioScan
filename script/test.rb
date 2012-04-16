require "./Helioscan.rb"
require "./ImagingModeDL090130Frame.rb"


helio = HelioScan.new
imagingMode = helio.imaging_mode

imagingMode.x_resolution = 200
imagingMode.y_resolution = 200
imagingMode.frame_rate = 3
imagingMode.update

#sleep 2
#helio.start
#sleep 5
#helio.stop