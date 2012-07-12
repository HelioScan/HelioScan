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
		<Item Name="type definitions" Type="Folder">
			<Item Name="FPGA_VI_reference.ctl" Type="VI" URL="../FPGA_VI_reference.ctl"/>
			<Item Name="network_streaming_queues_cluster.ctl" Type="VI" URL="../network_streaming_queues_cluster.ctl"/>
			<Item Name="status_enum.ctl" Type="VI" URL="../status_enum.ctl"/>
		</Item>
		<Item Name="grabber.ico" Type="Document" URL="../grabber.ico"/>
		<Item Name="grabber.vi" Type="VI" URL="../grabber.vi"/>
		<Item Name="initialise.vi" Type="VI" URL="../initialise.vi"/>
		<Item Name="update.vi" Type="VI" URL="../update.vi"/>
		<Item Name="Dependencies" Type="Dependencies">
			<Item Name="vi.lib" Type="Folder">
				<Item Name="Clear Errors.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Clear Errors.vi"/>
				<Item Name="Error Cluster From Error Code.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Error Cluster From Error Code.vi"/>
				<Item Name="Flush And Wait Empty Condition.ctl" Type="VI" URL="/&lt;vilib&gt;/dex/Flush And Wait Empty Condition.ctl"/>
				<Item Name="Trim Whitespace.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/Trim Whitespace.vi"/>
				<Item Name="whitespace.ctl" Type="VI" URL="/&lt;vilib&gt;/Utility/error.llb/whitespace.ctl"/>
			</Item>
			<Item Name="channels_cluster.ctl" Type="VI" URL="../../../common_typedefs/channels_cluster.ctl"/>
			<Item Name="command_enum.ctl" Type="VI" URL="../../type definitions/command_enum.ctl"/>
			<Item Name="config_data_cluster.ctl" Type="VI" URL="../../type definitions/config_data_cluster.ctl"/>
			<Item Name="gate_cluster.ctl" Type="VI" URL="../../type definitions/gate_cluster.ctl"/>
			<Item Name="gate_parameters_cluster.ctl" Type="VI" URL="../../../common_typedefs/gate_parameters_cluster.ctl"/>
			<Item Name="NiFpgaLv.dll" Type="Document" URL="NiFpgaLv.dll">
				<Property Name="NI.PreserveRelativePath" Type="Bool">true</Property>
			</Item>
			<Item Name="PXI-7813R_80MHz.lvbitx" Type="Document" URL="../../FPGA Bitfiles/PXI-7813R_80MHz.lvbitx"/>
			<Item Name="PXI-7813R_120MHz.lvbitx" Type="Document" URL="../../FPGA Bitfiles/PXI-7813R_120MHz.lvbitx"/>
			<Item Name="set_process_priority.vi" Type="VI" URL="../../../../../common/utilities/set_process_priority.vi"/>
			<Item Name="settings_cluster.ctl" Type="VI" URL="../../type definitions/settings_cluster.ctl"/>
			<Item Name="System" Type="VI" URL="System">
				<Property Name="NI.PreserveRelativePath" Type="Bool">true</Property>
			</Item>
		</Item>
		<Item Name="Build Specifications" Type="Build">
			<Item Name="grabber" Type="EXE">
				<Property Name="App_copyErrors" Type="Bool">true</Property>
				<Property Name="App_INI_aliasGUID" Type="Str">{E71C977B-9235-4697-A6BB-FFF7146E3CA5}</Property>
				<Property Name="App_INI_GUID" Type="Str">{97539911-4B2A-4CB7-B2F1-F201864B076C}</Property>
				<Property Name="Bld_buildSpecName" Type="Str">grabber</Property>
				<Property Name="Bld_excludeLibraryItems" Type="Bool">true</Property>
				<Property Name="Bld_excludePolymorphicVIs" Type="Bool">true</Property>
				<Property Name="Bld_localDestDir" Type="Path">../grabber/build</Property>
				<Property Name="Bld_localDestDirType" Type="Str">relativeToCommon</Property>
				<Property Name="Bld_modifyLibraryFile" Type="Bool">true</Property>
				<Property Name="Destination[0].destName" Type="Str">grabber.exe</Property>
				<Property Name="Destination[0].path" Type="Path">../NI_AB_PROJECTNAME/build/grabber.exe</Property>
				<Property Name="Destination[0].preserveHierarchy" Type="Bool">true</Property>
				<Property Name="Destination[0].type" Type="Str">App</Property>
				<Property Name="Destination[1].destName" Type="Str">Support Directory</Property>
				<Property Name="Destination[1].path" Type="Path">../NI_AB_PROJECTNAME/build/data</Property>
				<Property Name="DestinationCount" Type="Int">2</Property>
				<Property Name="Exe_iconItemID" Type="Ref">/My Computer/grabber.ico</Property>
				<Property Name="Source[0].itemID" Type="Str">{A91AD3D9-78B0-42F9-BE05-B439DE101D71}</Property>
				<Property Name="Source[0].type" Type="Str">Container</Property>
				<Property Name="Source[1].destinationIndex" Type="Int">0</Property>
				<Property Name="Source[1].itemID" Type="Ref">/My Computer/grabber.vi</Property>
				<Property Name="Source[1].sourceInclusion" Type="Str">TopLevel</Property>
				<Property Name="Source[1].type" Type="Str">VI</Property>
				<Property Name="SourceCount" Type="Int">2</Property>
				<Property Name="TgtF_companyName" Type="Str">Uni Zürich</Property>
				<Property Name="TgtF_fileDescription" Type="Str">grabber</Property>
				<Property Name="TgtF_fileVersion.major" Type="Int">1</Property>
				<Property Name="TgtF_internalName" Type="Str">grabber</Property>
				<Property Name="TgtF_legalCopyright" Type="Str">Copyright © 2012 Uni Zürich</Property>
				<Property Name="TgtF_productName" Type="Str">grabber</Property>
				<Property Name="TgtF_targetfileGUID" Type="Str">{28FA4B1A-82A0-4AE0-9E95-9F32E136A47F}</Property>
				<Property Name="TgtF_targetfileName" Type="Str">grabber.exe</Property>
			</Item>
		</Item>
	</Item>
</Project>
