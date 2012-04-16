<?xml version='1.0' encoding='UTF-8'?>
<Project Type="Project" LVVersion="10008000">
	<Property Name="NI.LV.All.SourceOnly" Type="Bool">true</Property>
	<Property Name="NI.Project.Description" Type="Str"></Property>
	<Item Name="My Computer" Type="My Computer">
		<Property Name="server.app.propertiesEnabled" Type="Bool">true</Property>
		<Property Name="server.control.propertiesEnabled" Type="Bool">true</Property>
		<Property Name="server.tcp.enabled" Type="Bool">false</Property>
		<Property Name="server.tcp.port" Type="Int">0</Property>
		<Property Name="server.tcp.serviceName" Type="Str">My Computer/VI Server</Property>
		<Property Name="server.tcp.serviceName.default" Type="Str">My Computer/VI Server</Property>
		<Property Name="server.vi.callsEnabled" Type="Bool">true</Property>
		<Property Name="server.vi.propertiesEnabled" Type="Bool">true</Property>
		<Property Name="specify.custom.address" Type="Bool">false</Property>
		<Item Name="test.vi" Type="VI" URL="../test.vi"/>
		<Item Name="XComponentSpecification.xctl" Type="XControl" URL="../XComponentSpecification/XComponentSpecification.xctl"/>
		<Item Name="Dependencies" Type="Dependencies">
			<Item Name="vi.lib" Type="Folder">
				<Item Name="Check if File or Folder Exists.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/libraryn.llb/Check if File or Folder Exists.vi"/>
				<Item Name="Error Cluster From Error Code.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Error Cluster From Error Code.vi"/>
				<Item Name="NI_FileType.lvlib" Type="Library" URL="/&lt;vilib&gt;/Utility/lvfile.llb/NI_FileType.lvlib"/>
				<Item Name="Trim Whitespace.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Trim Whitespace.vi"/>
				<Item Name="Version To Dotted String.vi" Type="VI" URL="/&lt;vilib&gt;/_xctls/Version To Dotted String.vi"/>
				<Item Name="whitespace.ctl" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/whitespace.ctl"/>
				<Item Name="XControlSupport.lvlib" Type="Library" URL="/&lt;vilib&gt;/_xctls/XControlSupport.lvlib"/>
			</Item>
			<Item Name="component_path_clusters.ctl" Type="VI" URL="../../../classes/GenericComponent/GenericComponent/component_path_clusters.ctl"/>
			<Item Name="configuration_paths_cluster.ctl" Type="VI" URL="../../../clusters/configuration_paths_cluster.ctl"/>
			<Item Name="get_paths.vi" Type="VI" URL="../../../utilities/get_paths.vi"/>
			<Item Name="get_project_path.vi" Type="VI" URL="../../../utilities/get_project_path.vi"/>
			<Item Name="make_path_absolute.vi" Type="VI" URL="../../../utilities/make_path_absolute.vi"/>
			<Item Name="make_path_relative.vi" Type="VI" URL="../../../utilities/make_path_relative.vi"/>
			<Item Name="plugin_specification.ctl" Type="VI" URL="../../../clusters/plugin_specification.ctl"/>
			<Item Name="populate_config_or_settings_combo.vi" Type="VI" URL="../../../utilities/populate_config_or_settings_combo.vi"/>
			<Item Name="settings_paths_cluster.ctl" Type="VI" URL="../../../clusters/settings_paths_cluster.ctl"/>
			<Item Name="XUserDropdown.xctl" Type="XControl" URL="../../XUserDropdown/XUserDropdown/XUserDropdown.xctl"/>
		</Item>
		<Item Name="Build Specifications" Type="Build"/>
	</Item>
</Project>
