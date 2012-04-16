require "./header.rb"



class HelioScan
	IMAGING_MODE = 0
	SCAN_HEAD = 1
	DAQ = 2
	IMAGE_ASSEMBLER = 3
	DISPLAY = 4
	DATA_COLLECTION = 5
	ANALYSER = 6
	STIMULATOR = 7
	EXPERIMENT_CONTROLLER = 8
	INSTRUMENT = 9
	STAGE = 10
	
	
	def initialize
		sleep 1 #seems necessary to prevent buffer overflow
	end

	def start
		self.send_main_trigger(3) # "start" trigger
	end

	def stop
		self.send_main_trigger(4) # "stop" trigger
		sleep 1 #seems necessary to prevent buffer overflow
	end
	
	def exit
		self.send_main_trigger(2) # "terminate" trigger
	end
	
	def send_main_trigger(trigger)
		vi_path = $base_path + 'main\labview_source\main\send_main_trigger.vi'
		vi_reference = $lv_application_reference.GetVIReference(vi_path, "", true)
		vi_reference.FPWinOpen = false		

		#vi_reference.Run() #We can't use this one when it's already running...
		vi_reference.Call(["trigger"], [trigger])
	end
	
	def imaging_mode
		return_top_level_component(IMAGING_MODE)
	end
	
	def scan_head
		return_top_level_component(SCAN_HEAD)
	end
	
	def daq
		return_top_level_component(DAQ)
	end
	
	def image_assembler
		return_top_level_component(IMAGE_ASSEMBLER)
	end
	
	def display
		return_top_level_component(DISPLAY)
	end
	
	def data_collection
		return_top_level_component(DATA_COLLECTION)
	end
	
	def analyser
		return return_top_level_component(ANALYSER)
	end
	
	def stimulator
		return_top_level_component(STIMULATOR)
	end
	
	def experiment_controller
		return_top_level_component(EXPERIMENT_CONTROLLER)
	end
	
	def instrument
		return_top_level_component(INSTRUMENT)
	end
	
	def stage
		return_top_level_component(STAGE)
	end
	
	def return_top_level_component(type)
		vi_path = $base_path + 'main\labview_source\main\get_top-level_component_class_name.vi'
		vi_reference = $lv_application_reference.GetVIReference(vi_path, "", true)
		vi_reference.FPWinOpen = true	
		paramNames = ["top-level component type", "component class name"]
		paramValues = [type, ""]
		vi_reference.Call(paramNames, paramValues)
		type = vi_reference.getControlValue("component class name")
		vi_reference.CloseFrontPanel()
		component = eval(type).new # one can also use Object.const_get(type).new instead (see http://blog.sidu.in/2008/02/loading-classes-from-strings-in-ruby.html)
		return component
	end
	
	
	
end


