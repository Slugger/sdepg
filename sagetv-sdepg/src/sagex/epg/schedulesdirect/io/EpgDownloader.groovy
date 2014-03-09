/*
*      Copyright 2011-2014 Battams, Derek
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
package sagex.epg.schedulesdirect.io

import org.apache.commons.io.IOUtils
import org.apache.log4j.Logger

import sagex.api.ChannelAPI
import sagex.api.Configuration
import sagex.api.PluginAPI
import sagex.epg.schedulesdirect.EPGImportPluginSchedulesDirect
import sagex.epg.schedulesdirect.plugin.Plugin

class EpgDownloader {
	static private final Logger LOG = Logger.getLogger(EpgDownloader)
	static String generateUserAgent() {
		def plugin = PluginAPI.GetInstalledPlugins().find { PluginAPI.GetPluginIdentifier(it) == 'sdepg' }
		def ver = plugin ? PluginAPI.GetPluginVersion(plugin) : 'unknown'
		return "sagetv-sdepg/$ver (${System.getProperty('os.name')} ${System.getProperty('os.arch')} ${System.getProperty('os.version')})"
	}
	
	private def id
	private def pwd
	private def lineupName
	
	EpgDownloader(def id, def pwd, def lineupName) {
		this.id = id
		this.pwd = pwd
		this.lineupName = lineupName
		LOG.info "Downloading EPG data for lineup: '$lineupName'"
	}
		
	void download() throws IOException {
		def targetDir = EPGImportPluginSchedulesDirect.EPG_SRC.parentFile
		if(!targetDir.exists())
			targetDir.mkdirs()
		def targetFile = EPGImportPluginSchedulesDirect.EPG_SRC
		def plugin = PluginAPI.GetInstalledPlugins().find { PluginAPI.GetPluginIdentifier(it) == 'sdepg' }
		def cmd = [new File("${System.getProperty('java.home')}/bin/java").absolutePath, "-Xmx${PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_GRABBER_HEAP)}m", "-Dsdjson.fs.capture=${new File('plugins/sdepg/capture/grabber').absolutePath}"]
		def capSettings = PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_CAP)
		if(capSettings == 'JSON' || capSettings == 'ALL')
			cmd << '-Dsdjson.capture.json-errors' << '-Dsdjson.capture.encode-errors'
		if(capSettings == 'HTTP' || capSettings == 'ALL')
			cmd << '-Dsdjson.capture.http' << '-Dsdjson.capture.http.content'
		cmd << '-jar' << new File("${Plugin.RESOURCE_DIR}/tools/sdjson.jar").absolutePath
		cmd << '--username' << id << '--password' << pwd << '--user-agent' << generateUserAgent() << '--max-threads' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_THREADS)
		cmd << '--url' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_URL)
		cmd << '--grabber-log-level' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_GRABBER_LOG_LVL)
		cmd << 'grab' << '--target' << targetFile.absolutePath
		cmd << '--max-prog-chunk' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_PROG_CHUNK)
		cmd << '--max-sched-chunk' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_SCHED_CHUNK)
		def stationFile = new File(targetDir, 'stations.txt')
		stationFile.delete()
		def stationList = ChannelAPI.GetAllChannels().findAll { ChannelAPI.IsChannelViewableOnLineup(it, lineupName) }
		stationFile.withWriterAppend('UTF-8') { f ->
			stationList.each { f.append(Integer.toString(ChannelAPI.GetStationID(it)) + IOUtils.LINE_SEPARATOR)}
		}
		cmd << '--stations' << stationFile.absolutePath
		LOG.info "Requesting ${stationList.size()} channels"
		def cachePurged = false
		if(System.currentTimeMillis() - (PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_CACHE_TTL).toLong() * 86400000L) > Configuration.GetServerProperty(Plugin.PROP_LAST_CACHE_PURGE, '0').toLong()) {
			cmd << '--purge-cache'
			cachePurged = true
		}
		if(Boolean.parseBoolean(Configuration.GetServerProperty(Plugin.PROP_FORCED_REFRESH, 'false'))) {
			Configuration.SetServerProperty(Plugin.PROP_FORCED_REFRESH, 'false')
			cmd << '--force-download'
			LOG.info('--force-download flag inserted via user refresh request!')
		}
		LOG.info cmd.toString().replace(pwd, '*****')
		def p = cmd.execute()
		def stdout = new StringBuilder()
		def stderr = new StringBuilder()
		p.consumeProcessOutput(stdout, stderr)
		if(p.waitFor()) {
			LOG.error("sdjson download failed! [rc=${p.exitValue()}]")
			LOG.error("stdout:\n$stdout")
			LOG.error("stderr:\n$stderr")
			throw new IOException(" download failed! [rc=${p.exitValue()}]")
		} else {
			if(cachePurged)
				Configuration.SetServerProperty(Plugin.PROP_LAST_CACHE_PURGE, System.currentTimeMillis().toString())
			if(LOG.isDebugEnabled()) {
				LOG.debug("stdout:\n$stdout")
				LOG.debug("stderr:\n$stderr")
			}
		}
	}
}
