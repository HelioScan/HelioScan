require "./Component.rb"

class ImagingModeDL090130Frame < Component
	attr_accessor 	:x_resolution, 
			:y_resolution,
			:x_aspect,
			:y_aspect,
			:frame_rate,
			:number_of_frames,
			:number_of_averagings,
			:z_step
			
	
	def local_base_path
		return ($base_path + 'components\ImagingMode\ImagingModeDL090130Frame\ImagingModeDL090130Frame\\')
	end
	
	def update
		#params = Hash.new
		mapping_hash = {
			:x_resolution => "x pixels",
			:y_resolution => "y lines",
			:x_aspect => "x aspect",
			:y_aspect => "y aspect",
			:frame_rate => "frame rate",
			:number_of_frames => "no. of frames",
			:number_of_averagings => "no. of averagings",
			:z_step => "step"
		}
		params = map_parameters(mapping_hash)
		params[:settings] = "user-defined"
		
		interfere_method(:run, params)
		execute_method(:trigger_GUI_event)
	end
	
	def read_settings(settings)
		params = {
			"settings name" => settings
		}
		self.execute_method(:read_settings_file, params)			
	end
	
	
end