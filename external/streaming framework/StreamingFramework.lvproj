<?xml version='1.0' encoding='UTF-8'?>
<Project Type="Project" LVVersion="11008008">
	<Property Name="NI.LV.All.SourceOnly" Type="Bool">true</Property>
	<Property Name="NI.Project.Description" Type="Str"></Property>
	<Item Name="My Computer" Type="My Computer">
		<Property Name="CCSymbols" Type="Str"></Property>
		<Property Name="server.app.propertiesEnabled" Type="Bool">true</Property>
		<Property Name="server.control.propertiesEnabled" Type="Bool">true</Property>
		<Property Name="server.tcp.acl" Type="Str">0800000008000000</Property>
		<Property Name="server.tcp.enabled" Type="Bool">false</Property>
		<Property Name="server.tcp.port" Type="Int">0</Property>
		<Property Name="server.tcp.serviceName" Type="Str"></Property>
		<Property Name="server.tcp.serviceName.default" Type="Str">My Computer/VI Server</Property>
		<Property Name="server.vi.access" Type="Str"></Property>
		<Property Name="server.vi.callsEnabled" Type="Bool">true</Property>
		<Property Name="server.vi.propertiesEnabled" Type="Bool">true</Property>
		<Property Name="server.viscripting.showScriptingOperationsInContextHelp" Type="Bool">false</Property>
		<Property Name="server.viscripting.showScriptingOperationsInEditor" Type="Bool">false</Property>
		<Property Name="specify.custom.address" Type="Bool">false</Property>
		<Item Name="service manager" Type="Folder">
			<Item Name="boot.vi" Type="VI" URL="../service manager/boot.vi"/>
			<Item Name="service_manager.vi" Type="VI" URL="../StreamingBase/service_manager.vi"/>
		</Item>
		<Item Name="subVIs" Type="Folder">
			<Item Name="fetch_TCP_input.vi" Type="VI" URL="../fetch_TCP_input.vi"/>
		</Item>
		<Item Name="type definitions" Type="Folder"/>
		<Item Name="StreamingBase.lvclass" Type="LVClass" URL="../StreamingBase/StreamingBase.lvclass"/>
		<Item Name="Dependencies" Type="Dependencies">
			<Item Name="vi.lib" Type="Folder">
				<Item Name="Internecine Avoider.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/tcp.llb/Internecine Avoider.vi"/>
				<Item Name="TCP Listen Internal List.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/tcp.llb/TCP Listen Internal List.vi"/>
				<Item Name="TCP Listen List Operations.ctl" Type="VI" URL="/&lt;vilib&gt;/Utility/tcp.llb/TCP Listen List Operations.ctl"/>
				<Item Name="TCP Listen.vi" Type="VI" URL="/&lt;vilib&gt;/Utility/tcp.llb/TCP Listen.vi"/>
			</Item>
		</Item>
		<Item Name="Build Specifications" Type="Build">
			<Item Name="serviceManager" Type="RESTful WS">
				<Property Name="Bld_buildCacheID" Type="Str">{C121B71D-40A4-4723-AC90-69B8DE82561E}</Property>
				<Property Name="Bld_buildSpecName" Type="Str">serviceManager</Property>
				<Property Name="Bld_excludeLibraryItems" Type="Bool">true</Property>
				<Property Name="Bld_excludePolymorphicVIs" Type="Bool">true</Property>
				<Property Name="Bld_localDestDir" Type="Path">../builds/NI_AB_PROJECTNAME/serviceManager</Property>
				<Property Name="Bld_localDestDirType" Type="Str">relativeToCommon</Property>
				<Property Name="Bld_modifyLibraryFile" Type="Bool">true</Property>
				<Property Name="Bld_previewCacheID" Type="Str">{E3E7E295-F9DE-43B2-A6F3-A24623FED572}</Property>
				<Property Name="Destination[0].destName" Type="Str">serviceManager.lvws</Property>
				<Property Name="Destination[0].path" Type="Path">../builds/NI_AB_PROJECTNAME/serviceManager/internal.llb</Property>
				<Property Name="Destination[0].preserveHierarchy" Type="Bool">true</Property>
				<Property Name="Destination[0].type" Type="Str">App</Property>
				<Property Name="Destination[1].destName" Type="Str">Support Directory</Property>
				<Property Name="Destination[1].path" Type="Path">../builds/NI_AB_PROJECTNAME/serviceManager/data</Property>
				<Property Name="DestinationCount" Type="Int">2</Property>
				<Property Name="RESTfulWebSrvc_routingTemplate[0].template" Type="Str">/boot/:asdf</Property>
				<Property Name="RESTfulWebSrvc_routingTemplate[0].VIName" Type="Str">boot.vi</Property>
				<Property Name="RESTfulWebSrvc_routingTemplateCount" Type="Int">1</Property>
				<Property Name="Source[0].itemID" Type="Str">{A6F45EB4-1A31-409D-8273-0855E6604182}</Property>
				<Property Name="Source[0].type" Type="Str">Container</Property>
				<Property Name="Source[1].destinationIndex" Type="Int">0</Property>
				<Property Name="Source[1].itemID" Type="Ref">/My Computer/service manager/boot.vi</Property>
				<Property Name="Source[1].sourceInclusion" Type="Str">TopLevel</Property>
				<Property Name="Source[1].type" Type="Str">RESTfulVI</Property>
				<Property Name="SourceCount" Type="Int">2</Property>
				<Property Name="TgtF_companyName" Type="Str">University of Zurich</Property>
				<Property Name="TgtF_fileDescription" Type="Str">serviceManager</Property>
				<Property Name="TgtF_internalName" Type="Str">serviceManager</Property>
				<Property Name="TgtF_legalCopyright" Type="Str">Copyright © 2011 University of Zurich</Property>
				<Property Name="TgtF_productName" Type="Str">serviceManager</Property>
				<Property Name="TgtF_targetfileGUID" Type="Str">{6D91AD77-026F-49EE-8234-4B3198D528FD}</Property>
				<Property Name="TgtF_targetfileName" Type="Str">serviceManager.lvws</Property>
				<Property Name="WebSrvc_standaloneService" Type="Bool">true</Property>
			</Item>
		</Item>
	</Item>
</Project>
