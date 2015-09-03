/*
*      Copyright 2011-2015 Battams, Derek
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

import org.apache.commons.io.FileUtils
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
		def plugin = PluginAPI.GetInstalledPlugins().find { PluginAPI.GetPluginIdentifier(it) == 'sdepg-oss' }
		def ver = plugin ? PluginAPI.GetPluginVersion(plugin) : 'unknown'
		return "sagetv-sdepg-oss/$ver (${System.getProperty('os.name')} ${System.getProperty('os.arch')} ${System.getProperty('os.version')})"
	}
	
	private def id
	private def pwd
	
	EpgDownloader(def id, def pwd) {
		this.id = id
		this.pwd = pwd
	}
	
	protected void backupLocalCache(File src, def plugin) {
		def root = new File("${Plugin.RESOURCE_DIR}/backups")
		def numToKeep = PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_CACHE_BACKUP_SIZE).toInteger()
		(numToKeep..1).each {
			def old = new File(root, "${src.name}.$it")
			if(it < numToKeep && old.exists()) {
				if(!old.renameTo(new File(root, "${src.name}.${it + 1}")))
					LOG.warn "Unable to rename backup $it to ${it + 1}!"
			} else if(old.exists() && !old.delete())
				LOG.warn "Unable to delete cache backup: $old"
		}
		if(src.exists()) {
			try {
				FileUtils.copyFile(src, new File(root, "${src.name}.1"))
			} catch(IOException e) {
				LOG.warn 'Unable to create EPG cache backup!', e
			}
			LOG.info "Backed up local cache file [$src]"
		} else
			LOG.warn "Skipped local cache backup; it doesn't exist! [$src]"
	}
	
	private List getCurrentEnv() {
		def env = []
		System.getenv().each { k, v ->
			env << "$k=$v"
		}
		env
	}
	
	void download() throws IOException {
		def targetDir = EPGImportPluginSchedulesDirect.EPG_SRC.parentFile
		if(!targetDir.exists())
			targetDir.mkdirs()
		def targetFile = EPGImportPluginSchedulesDirect.EPG_SRC
		def plugin = PluginAPI.GetInstalledPlugins().find { PluginAPI.GetPluginIdentifier(it) == 'sdepg-oss' }
		if(PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SKIP_GRAB).toBoolean()) {
			LOG.warn 'Skipping grabber execution; user disabled!'
			return
		}
		backupLocalCache(targetFile, plugin)
		
		def isWindows = System.getProperty('os.name').toLowerCase().contains('windows')
		def cmd = []
		if(!isWindows)
			cmd << 'bash'
		cmd << new File("${Plugin.RESOURCE_DIR}/tools/grabber/bin/sdjson-grabber${isWindows ? '.bat' : ''}").absolutePath
		cmd << '--username' << id << '--password' << pwd << '--user-agent' << generateUserAgent() << '--max-threads' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_THREADS)
		cmd << '--url' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_URL)
		cmd << '--grabber-log-level' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_GRABBER_LOG_LVL)
		cmd << 'grab' << '--target' << targetFile.absolutePath
		cmd << '--max-prog-chunk' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_PROG_CHUNK)
		cmd << '--max-sched-chunk' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_SCHED_CHUNK)
		def stationFile = new File(targetDir, 'stations.txt')
		stationFile.delete()
		def stationList = ChannelAPI.GetAllChannels().findAll { ChannelAPI.IsChannelViewable(it) }
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
				
		// Now need to set JVM opts as env var
		def jvmOpts = ["-Xmx${PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_GRABBER_HEAP)}m"]
		jvmOpts << "-Dsdjson.fs.capture=\"${new File('plugins/sdepg/capture/grabber').absolutePath}\""
		def capSettings = PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_CAP)
		if(capSettings == 'JSON' || capSettings == 'ALL')
			jvmOpts << '-Dsdjson.capture.json-errors' << '-Dsdjson.capture.encode-errors'
		if(capSettings == 'HTTP' || capSettings == 'ALL')
			jvmOpts << '-Dsdjson.capture.http' << '-Dsdjson.capture.http.content'
		
		LOG.info "JVM options: $jvmOpts"
		LOG.info cmd.toString().replace(pwd, '*****')
		def env = currentEnv
		env << "SDJSON_GRABBER_OPTS=${jvmOpts.join(' ')}"
		def p = cmd.execute(env, null)
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
