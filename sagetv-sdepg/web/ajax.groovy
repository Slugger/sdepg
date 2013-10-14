/*
 *      Copyright 2013 Battams, Derek
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */
import org.schedulesdirect.api.NetworkEpgClient
import sagex.api.*
import sagex.epg.schedulesdirect.io.EpgDownloader

final PLUGIN = PluginAPI.GetInstalledPlugins().find { PluginAPI.GetPluginIdentifier(it) == 'sdepg' }
switch(params.c) {
	case 'search':
		def clnt = new NetworkEpgClient(PluginAPI.GetPluginConfigValue(PLUGIN, 'sdepg/sdUser'), PluginAPI.GetPluginConfigValue(PLUGIN, 'sdepg/sdPassword'), EpgDownloader.generateUserAgent(), PluginAPI.GetPluginConfigValue(PLUGIN, 'sdepg/sdjsonUrl'), false)
		def html = new groovy.xml.MarkupBuilder(out)
		clnt.getHeadends(params.z, params.i).each { he ->
			html.tr {
				td { input(type:'checkbox', name:'heid', value:he.id, class:'newId') }
				td he.id
				td "$he.name $he.location"
			}
		}
}