class Component
	def local_base_path
	end

	def execute_method(function, params = {})
		vi_path = self.local_base_path + function.to_s + '.vi'
		vi_reference = $lv_application_reference.GetVIReference(vi_path, "", true)
		param_names = Array.new
		param_values = Array.new
		params.each_key do |key|
			param_names << key
			param_values << params[key]
		end
		vi_reference.Call(param_names, param_values)	
	end
	
	def interfere_method(function, params = {})
		vi_path = self.local_base_path + function.to_s + '.vi'
		vi_reference = $lv_application_reference.GetVIReference(vi_path, "", true)
		puts vi_reference.inspect
		params.each_key do |key|
			vi_reference.SetControlValue(key.to_s, params[key])
		end	
	end
	
	def map_parameters(mapping_hash)
		params = Hash.new
		mapping_hash.each_pair do |property, control_name|
			value = instance_variable_get("@#{property}")
			params[control_name] = value unless value.nil?	
		end
		return params
	end
	
	
end