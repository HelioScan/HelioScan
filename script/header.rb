require 'win32ole'


$base_path = File.dirname(__FILE__).split("/")
$base_path = $base_path.take($base_path.length - 1).join("\\") + "\\"

$lv_application_reference = WIN32OLE.new('Labview.Application')

$main_project_path = $base_path + 'main\helioscan.lvproj'
$lv_project_reference = $lv_application_reference.OpenProject($main_project_path)
$lv_application_reference = $lv_project_reference.Application

