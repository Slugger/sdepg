<@ webserver/templates/header.gsp @>
<div id="airdetailedinfo">
<%
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
	import sagex.api.*
	import org.schedulesdirect.api.NetworkEpgClient
	import org.schedulesdirect.api.exception.ServiceOfflineException
	import sagex.epg.schedulesdirect.io.EpgDownloader
	import sagex.epg.schedulesdirect.plugin.Plugin
	
	final PLUGIN = PluginAPI.GetInstalledPlugins().find { PluginAPI.GetPluginIdentifier(it) == 'sdepg' }
	request.setAttribute('plugin', PLUGIN)
	try {
		request.setAttribute('clnt', new NetworkEpgClient(PluginAPI.GetPluginConfigValue(PLUGIN, 'sdepg/sdUser'), PluginAPI.GetPluginConfigValue(PLUGIN, 'sdepg/sdPassword'), EpgDownloader.generateUserAgent(), PluginAPI.GetPluginConfigValue(PLUGIN, 'sdepg/sdjsonUrl'), false))	
		if(params.c) {
			def clnt = request.getAttribute('clnt')
			def result = []
			switch(params.c) {
				case 'rmmsg':
					request.getParameterValues('msgids').each { id ->
						def msg = clnt.userStatus.systemMessages.find { it.id == id }
						if(!msg)
							msg = clnt.userStatus.userMessages.find { it.id == id }
						if(!msg)
							result << "$id: FAILED [Message id not found!]"
						else
							try {
								msg.delete()
							} catch(IOException e) {
								result << "$id: FAILED [$e.message]"
							}
					}
					if(!result.size())
						out << '<p>All selected messages were deleted!'
					else
						result.each { out << "<p>$it</p>" }
					break
				case 'addhe':
					def vals = request.getParameterValues('heid')
					vals.each { id ->
						try {
							clnt.registerLineup(id)
						} catch(IOException e) {
							result << "$id: FAILED [$e.message]"
						}
					}
					if(!result.size())
						out << '<p>All selected lineups were registered successfully!</p>'
					else
						result.each { out << "<p>$it</p>" }
					if(result.size() < vals.size()) {
						Plugin.forceEpgRefresh()
						out << '<p>EPG update initiated!</p>'
					}
					break
				case 'rmhe':
					request.getParameterValues('heid').each { id ->
						try {
							clnt.unregisterLineup(id)
						} catch(IOException e) {
							result << "$id: FAILED [$e.message]"
						}
					}
					if(!result.size())
						out << '<p>All selected lineups were removed successfully!'
					else
						result.each { out << "<p>$it</p>" }
					break
			}
		}
		def module = params.m ?: 'status'
		request.getRequestDispatcher("${module}.gsp").include(request, response)
	} catch(ServiceOfflineException e) {
		def stack = new StringWriter()
		e.printStackTrace(new PrintWriter(stack))
		def mb = new groovy.xml.MarkupBuilder(out)
		mb.h2('Schedules Direct service is currently offline!')
		mb.p('Please come back later.  Details of the situation are below.')
		mb.pre stack
	}
%>
</div>
<div id="commands">
    <ul>
        <li><a href="/sage/sdjson/index.gsp?m=status">Messages/Status</a></li>
		<li><a href="/sage/sdjson/index.gsp?m=headends">Configure Lineups</a></li>
    </ul>
</div>
<@ webserver/templates/footer.gsp @>
