<?xml version='1.0' encoding='UTF-8'?>
<Project Type="Project" LVVersion="10008000">
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
		<Item Name="frame scan grabber" Type="Folder">
			<Item Name="grabber_framescan.vi" Type="VI" URL="../grabber_framescan.vi"/>
			<Item Name="initialise_framescan.vi" Type="VI" URL="../initialise_framescan.vi"/>
			<Item Name="update_framescan.vi" Type="VI" URL="../update_framescan.vi"/>
		</Item>
		<Item Name="RAPS grabber" Type="Folder">
			<Item Name="grabber_mc_split_data.vi" Type="VI" URL="../grabber_mc_split_data.vi"/>
			<Item Name="grabber_raps.vi" Type="VI" URL="../grabber_raps.vi"/>
			<Item Name="initialise_raps.vi" Type="VI" URL="../initialise_raps.vi"/>
			<Item Name="process.vi" Type="VI" URL="../process.vi"/>
			<Item Name="replace_zero_elements.vi" Type="VI" URL="../replace_zero_elements.vi"/>
			<Item Name="update_raps.vi" Type="VI" URL="../update_raps.vi"/>
		</Item>
		<Item Name="type definitions" Type="Folder">
			<Item Name="channels_cluster.ctl" Type="VI" URL="../../../common_typedefs/channels_cluster.ctl"/>
			<Item Name="command_enum.ctl" Type="VI" URL="../command_enum.ctl"/>
			<Item Name="configuration_cluster_framescan.ctl" Type="VI" URL="../../FPGAWrapperMH120124FrameScan_PXI7813R/configuration_cluster_framescan.ctl"/>
			<Item Name="configuration_cluster_raps.ctl" Type="VI" URL="../../FPGAWrapperMH120124RAPS_PXI7813R/configuration_cluster_raps.ctl"/>
			<Item Name="DDS_board_init_cluster.ctl" Type="VI" URL="../../fpgaVIsAndControls/DDS_board_init_cluster.ctl"/>
			<Item Name="ellipsoid_parameters_cluster.ctl" Type="VI" URL="../../fpgaVIsAndControls/ellipsoid_parameters_cluster.ctl"/>
			<Item Name="FPGA_framescan_reference.ctl" Type="VI" URL="../../FPGAWrapperMH120124FrameScan_PXI7813R/FPGA_framescan_reference.ctl"/>
			<Item Name="FPGA_raps_reference.ctl" Type="VI" URL="../FPGA_raps_reference.ctl"/>
			<Item Name="gate_cluster.ctl" Type="VI" URL="../../fpgaVIsAndControls/gate_cluster.ctl"/>
			<Item Name="gate_parameters_cluster.ctl" Type="VI" URL="../../../common_typedefs/gate_parameters_cluster.ctl"/>
			<Item Name="gui_refs.ctl" Type="VI" URL="../gui_refs.ctl"/>
			<Item Name="inverse_enum.ctl" Type="VI" URL="../../fpgaVIsAndControls/inverse_enum.ctl"/>
			<Item Name="motion_correction_cluster.ctl" Type="VI" URL="../motion_correction_cluster.ctl"/>
			<Item Name="network_streaming_queues_cluster.ctl" Type="VI" URL="../network_streaming_queues_cluster.ctl"/>
			<Item Name="settings_cluster_framescan.ctl" Type="VI" URL="../../FPGAWrapperMH120124FrameScan_PXI7813R/settings_cluster_framescan.ctl"/>
			<Item Name="settings_cluster_raps.ctl" Type="VI" URL="../../FPGAWrapperMH120124RAPS_PXI7813R/settings_cluster_raps.ctl"/>
			<Item Name="status_enum.ctl" Type="VI" URL="../status_enum.ctl"/>
		</Item>
		<Item Name="grabber.ico" Type="Document" URL="../grabber.ico"/>
		<Item Name="set_process_priority.vi" Type="VI" URL="../../../../../common/utilities/set_process_priority.vi"/>
		<Item Name="Dependencies" Type="Dependencies">
			<Item Name="vi.lib" Type="Folder">
				<Item Name="Clear Errors.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Clear Errors.vi"/>
				<Item Name="Error Cluster From Error Code.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Error Cluster From Error Code.vi"/>
				<Item Name="Flush And Wait Empty Condition.ctl" Type="VI" URL="/&lt;vilib&gt;/dex/Flush And Wait Empty Condition.ctl"/>
				<Item Name="NI_AALPro.lvlib" Type="Library" URL="/&lt;vilib&gt;/Analysis/NI_AALPro.lvlib"/>
				<Item Name="Trim Whitespace.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Trim Whitespace.vi"/>
				<Item Name="whitespace.ctl" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/whitespace.ctl"/>
			</Item>
			<Item Name="FPGA_frame.lvbitx" Type="Document" URL="../../FPGA Bitfiles/FPGA_frame.lvbitx"/>
			<Item Name="FPGA_RAPS.lvbitx" Type="Document" URL="../../FPGA Bitfiles/FPGA_RAPS.lvbitx"/>
			<Item Name="lvanlys.dll" Type="Document" URL="/D/Program Files/National Instruments/LabVIEW 2010/resource/lvanlys.dll"/>
			<Item Name="NiFpgaLv.dll" Type="Document" URL="NiFpgaLv.dll">
				<Property Name="NI.PreserveRelativePath" Type="Bool">true</Property>
			</Item>
			<Item Name="System" Type="VI" URL="System">
				<Property Name="NI.PreserveRelativePath" Type="Bool">true</Property>
			</Item>
		</Item>
		<Item Name="Build Specifications" Type="Build">
			<Item Name="frameScan" Type="EXE">
				<Property Name="App_copyErrors" Type="Bool">true</Property>
				<Property Name="App_INI_aliasGUID" Type="Str">{44F647A3-509C-4E4A-9390-7B5BE3DC45D9}</Property>
				<Property Name="App_INI_GUID" Type="Str">{3B003000-4C1E-4E56-AFE2-DED0AA744995}</Property>
				<Property Name="Bld_buildSpecName" Type="Str">frameScan</Property>
				<Property Name="Bld_excludeLibraryItems" Type="Bool">true</Property>
				<Property Name="Bld_excludePolymorphicVIs" Type="Bool">true</Property>
				<Property Name="Bld_localDestDir" Type="Path">../grabber/executable</Property>
				<Property Name="Bld_localDestDirType" Type="Str">relativeToCommon</Property>
				<Property Name="Bld_modifyLibraryFile" Type="Bool">true</Property>
				<Property Name="Destination[0].destName" Type="Str">grabber_framescan.exe</Property>
				<Property Name="Destination[0].path" Type="Path">../NI_AB_PROJECTNAME/executable/grabber_framescan.exe</Property>
				<Property Name="Destination[0].preserveHierarchy" Type="Bool">true</Property>
				<Property Name="Destination[0].type" Type="Str">App</Property>
				<Property Name="Destination[1].destName" Type="Str">Support Directory</Property>
				<Property Name="Destination[1].path" Type="Path">../NI_AB_PROJECTNAME/executable/data</Property>
				<Property Name="DestinationCount" Type="Int">2</Property>
				<Property Name="Exe_iconItemID" Type="Ref">/My Computer/grabber.ico</Property>
				<Property Name="Source[0].itemID" Type="Str">{C6D583C0-9278-4D56-B918-8E1D5C1BE5A4}</Property>
				<Property Name="Source[0].type" Type="Str">Container</Property>
				<Property Name="Source[1].destinationIndex" Type="Int">0</Property>
				<Property Name="Source[1].itemID" Type="Ref">/My Computer/frame scan grabber/grabber_framescan.vi</Property>
				<Property Name="Source[1].sourceInclusion" Type="Str">TopLevel</Property>
				<Property Name="Source[1].type" Type="Str">VI</Property>
				<Property Name="SourceCount" Type="Int">2</Property>
				<Property Name="TgtF_companyName" Type="Str">Brain Research Institute</Property>
				<Property Name="TgtF_fileDescription" Type="Str">frameScan</Property>
				<Property Name="TgtF_fileVersion.major" Type="Int">1</Property>
				<Property Name="TgtF_internalName" Type="Str">frameScan</Property>
				<Property Name="TgtF_legalCopyright" Type="Str">Copyright © 2012 Brain Research Institute</Property>
				<Property Name="TgtF_productName" Type="Str">frameScan</Property>
				<Property Name="TgtF_targetfileGUID" Type="Str">{2EEA729C-1159-4142-B241-D57C280EE294}</Property>
				<Property Name="TgtF_targetfileName" Type="Str">grabber_framescan.exe</Property>
			</Item>
			<Item Name="RAPS" Type="EXE">
				<Property Name="App_copyErrors" Type="Bool">true</Property>
				<Property Name="App_INI_aliasGUID" Type="Str">{6A8C0214-930A-4890-A4C1-E4EE6FCA7FEE}</Property>
				<Property Name="App_INI_GUID" Type="Str">{98E24571-01C6-4F3C-89D8-AA693AFB045F}</Property>
				<Property Name="Bld_buildSpecName" Type="Str">RAPS</Property>
				<Property Name="Bld_excludeLibraryItems" Type="Bool">true</Property>
				<Property Name="Bld_excludePolymorphicVIs" Type="Bool">true</Property>
				<Property Name="Bld_localDestDir" Type="Path">../grabber/executable</Property>
				<Property Name="Bld_localDestDirType" Type="Str">relativeToCommon</Property>
				<Property Name="Bld_modifyLibraryFile" Type="Bool">true</Property>
				<Property Name="Destination[0].destName" Type="Str">grabber_raps.exe</Property>
				<Property Name="Destination[0].path" Type="Path">../NI_AB_PROJECTNAME/executable/grabber_raps.exe</Property>
				<Property Name="Destination[0].preserveHierarchy" Type="Bool">true</Property>
				<Property Name="Destination[0].type" Type="Str">App</Property>
				<Property Name="Destination[1].destName" Type="Str">Support Directory</Property>
				<Property Name="Destination[1].path" Type="Path">../NI_AB_PROJECTNAME/executable/data</Property>
				<Property Name="DestinationCount" Type="Int">2</Property>
				<Property Name="Exe_iconItemID" Type="Ref">/My Computer/grabber.ico</Property>
				<Property Name="Source[0].itemID" Type="Str">{B6DDBFCE-A583-4A64-A7B7-13F3030F53CE}</Property>
				<Property Name="Source[0].type" Type="Str">Container</Property>
				<Property Name="Source[1].destinationIndex" Type="Int">0</Property>
				<Property Name="Source[1].itemID" Type="Ref">/My Computer/frame scan grabber/grabber_framescan.vi</Property>
				<Property Name="Source[1].type" Type="Str">VI</Property>
				<Property Name="Source[2].destinationIndex" Type="Int">0</Property>
				<Property Name="Source[2].itemID" Type="Ref">/My Computer/RAPS grabber/grabber_raps.vi</Property>
				<Property Name="Source[2].sourceInclusion" Type="Str">TopLevel</Property>
				<Property Name="Source[2].type" Type="Str">VI</Property>
				<Property Name="SourceCount" Type="Int">3</Property>
				<Property Name="TgtF_companyName" Type="Str">Brain Research Institute</Property>
				<Property Name="TgtF_enableDebugging" Type="Bool">true</Property>
				<Property Name="TgtF_fileDescription" Type="Str">RAPS</Property>
				<Property Name="TgtF_fileVersion.major" Type="Int">1</Property>
				<Property Name="TgtF_internalName" Type="Str">RAPS</Property>
				<Property Name="TgtF_legalCopyright" Type="Str">Copyright © 2012 Brain Research Institute</Property>
				<Property Name="TgtF_productName" Type="Str">RAPS</Property>
				<Property Name="TgtF_targetfileGUID" Type="Str">{5E424F0D-5FE0-454C-A94B-A1FD0310DA40}</Property>
				<Property Name="TgtF_targetfileName" Type="Str">grabber_raps.exe</Property>
			</Item>
		</Item>
	</Item>
</Project>
