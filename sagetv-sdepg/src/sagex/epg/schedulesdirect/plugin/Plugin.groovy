/*
*      Copyright 2011-2012 Battams, Derek
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
package sagex.epg.schedulesdirect.plugin

import org.apache.log4j.Logger

import sage.SageTVPlugin
import sage.SageTVPluginRegistry
import sagex.api.ChannelAPI
import sagex.api.Configuration
import sagex.api.Database
import sagex.api.Global
import sagex.epg.schedulesdirect.EPGImportPluginSchedulesDirect
import sagex.epg.schedulesdirect.logging.InternalLogger
import sagex.plugin.AbstractPlugin
import sagex.plugin.ButtonClickHandler
import sagex.plugin.ConfigValueChangeHandler
import sagex.plugin.IPropertyValidator
import sagex.plugin.IPropertyVisibility
import sagex.plugin.NoPropertyPersistence
import sagex.plugin.PluginProperty
import sagex.plugin.ServerPropertyPersistence

import com.google.code.sagetvaddons.license.License

final class Plugin extends AbstractPlugin {
	static final File RESOURCE_DIR = new File('plugins/sdepg/')
	static final String PLUGIN_ID = 'sdepg' // For sa-license
	static final String PROP_PREFIX = 'sdepg'
	static final String PROP_EPG_PLUGIN = 'epg/epg_import_plugin'
	static final String PROP_REFRESH = "${PROP_PREFIX}/refresh"
	static final String PROP_SHOW_FILTERS = "${PROP_PREFIX}/showFilters"
	static final String PROP_AIR_FILTERS = "${PROP_PREFIX}/airFilters"
	static final String PROP_CHAN_GENERATORS = "${PROP_PREFIX}/chanGenerators"
	static final String PROP_LINEUP_EDITORS = "${PROP_PREFIX}/lineupEditors"
	static final String PROP_AIR_GENERATORS = "${PROP_PREFIX}/airGenerators"
	static final String PROP_SHOW_GENERATORS = "${PROP_PREFIX}/showGenerators"
	static final String PROP_SCRIPT_LOG_LEVEL = "${PROP_PREFIX}/scriptLogLevel"
	static final String PROP_SD_USER = "${PROP_PREFIX}/sdUser"
	static final String PROP_SD_PWD = "${PROP_PREFIX}/sdPassword"
	static final String PROP_LOG_LEVEL = "${PROP_PREFIX}/logLevel"
	static final String PROP_SDJSON_THREADS = "${PROP_PREFIX}/sdjsonThreads"
	static final String PROP_GRABBER_HEAP = "${PROP_PREFIX}/grabberHeap"
	static final String PROP_CACHE_TTL = "${PROP_PREFIX}/cacheTTL";
	static final String PROP_LAST_CACHE_PURGE = "${PROP_PREFIX}/lastCachePurge"
	static final String PROP_SDJSON_URL = "${PROP_PREFIX}/sdjsonUrl"
	static final String PROP_SDJSON_PROG_CHUNK = "${PROP_PREFIX}/sdjsonProgChunk"
	static final String PROP_SDJSON_SCHED_CHUNK = "${PROP_PREFIX}/sdjsonSchedChunk"
	static final String PROP_GRABBER_LOG_LVL = "${PROP_PREFIX}/grabberLogLvl"
	static final String PROP_FORCED_REFRESH = "${PROP_PREFIX}/forcedRefresh"
	
	static { InternalLogger.init(Configuration.GetServerProperty(PROP_LOG_LEVEL, 'INFO')) }
	static private final Logger LOG = Logger.getLogger(Plugin)
	
	static private final class IntRangeValidator implements IPropertyValidator {
		private int min
		private int max
		
		IntRangeValidator(int min, int max) {
			this.min = min
			this.max = max
		}
		
		void validate(String name, String val) {
			def intVal = val.toInteger()
			if(intVal < min || intVal > max)
				throw new IllegalArgumentException("Integer value must be in the range of $min - $max")
		}
	}
	
	static private final class PropVisibility implements IPropertyVisibility, Runnable {
		
		private Boolean isLicensed
		
		PropVisibility() {
			isLicensed = License.isLicensed(PLUGIN_ID).isLicensed()
			new Thread(this).start()
		}
		
		synchronized boolean isVisible() {
			if(isLicensed == null) {
				isLicensed = License.isLicensed(PLUGIN_ID).isLicensed()
				new Thread(this).start()
			}
			return isLicensed
		}
		
		void run() {
			sleep 10000
			synchronized(this) { isLicensed = null }
			LOG.debug 'Cleared the plugin visibility property!'
		}
	}
	
	Plugin(SageTVPluginRegistry registry) {
		super(registry);
	}
	
	@ButtonClickHandler('sdepg/refresh')
	public void refreshEpgData() {
		LOG.info 'EPG refresh forced by user!'
		Configuration.SetServerProperty(PROP_FORCED_REFRESH, 'true')
		def epg = new EPGImportPluginSchedulesDirect()
		epg.getProviders().each {
			LOG.info "Refreshed lineup '${it[1]}'"
			forceRefresh(it[1])
		}
		Global.RemoveUnusedLineups()
	}

	protected void forceRefresh(String lineupName) {
		def chans = Database.GetChannelsOnLineup(lineupName)
		if(chans.size() > 0) {
			def nums = ChannelAPI.GetChannelNumbersForLineup(chans[0], lineupName)
			def vis = ChannelAPI.IsChannelViewableOnNumberOnLineup(chans[0], nums[0], lineupName)
			ChannelAPI.SetChannelViewabilityForChannelNumberOnLineup(chans[0], nums[0], lineupName, !vis)
			ChannelAPI.SetChannelViewabilityForChannelNumberOnLineup(chans[0], nums[0], lineupName, vis)
		}
	}
	
	@Override
	void start() {
		super.start()
		def cls = Configuration.GetServerProperty(PROP_EPG_PLUGIN, '')
		def name = EPGImportPluginSchedulesDirect.class.getName()
		if(!cls || cls != name) {
			Configuration.SetServerProperty(PROP_EPG_PLUGIN, name)
			LOG.warn "EPG import plugin has been set to '$name'; you must restart SageTV for this change to take effect!"
		} else
			LOG.debug 'EPG import plugin is already configured!'
			
		IPropertyVisibility vis = new PropVisibility()
							
		PluginProperty showFilters = new PluginProperty(SageTVPlugin.CONFIG_BOOL, PROP_SHOW_FILTERS, 'true', 'Execute Show Filters', 'When true, all shows are put through the user filter scripts before insertion. Set to false to disable.')
		showFilters.setPersistence(new ServerPropertyPersistence())
		showFilters.setVisibility vis
		
		PluginProperty airFilters = new PluginProperty(SageTVPlugin.CONFIG_BOOL, PROP_AIR_FILTERS, 'true', 'Execute Airing Filters', 'When true, all airings are put through the user filter scripts before insertion. Set to false to disable.')
		airFilters.setPersistence(new ServerPropertyPersistence())
		airFilters.setVisibility vis
			
		PluginProperty refresh = new PluginProperty(SageTVPlugin.CONFIG_BUTTON, PROP_REFRESH, 'Refresh EPG', 'Refresh EPG Data Now', 'Click this button to force a refresh of your EPG data now.  This will delete currently cached EPG data.')
		refresh.setPersistence(new NoPropertyPersistence())
		refresh.setVisibility vis
		
		PluginProperty chanGenerators = new PluginProperty(SageTVPlugin.CONFIG_BOOL, PROP_CHAN_GENERATORS, 'true', 'Execute Channel Generators', 'When true, all channel generators are executed with each EPG update. Set to false to disable.')
		chanGenerators.setPersistence(new ServerPropertyPersistence())
		chanGenerators.setVisibility vis
		
		PluginProperty lineupEditors = new PluginProperty(SageTVPlugin.CONFIG_BOOL, PROP_LINEUP_EDITORS, 'true', 'Execute Lineup Editors', 'When true, lineup editor scripts are executed with each EPG update. Set to false to disable.')
		lineupEditors.setPersistence(new ServerPropertyPersistence())
		lineupEditors.setVisibility vis
		
		PluginProperty airGenerators = new PluginProperty(SageTVPlugin.CONFIG_BOOL, PROP_AIR_GENERATORS, 'true', 'Execute Airing Generators', 'When true, airing generator scripts are executed with each EPG update. Set to false to disable.')
		airGenerators.setPersistence(new ServerPropertyPersistence())
		airGenerators.setVisibility vis
		
		PluginProperty showGenerators = new PluginProperty(SageTVPlugin.CONFIG_BOOL, PROP_SHOW_GENERATORS, 'true', 'Execute Show Generators', 'When true, show generator scripts are executed with each EPG update. Set to false to disable.')
		showGenerators.setPersistence(new ServerPropertyPersistence())
		showGenerators.setVisibility vis
		
		PluginProperty sdUser = new PluginProperty(SageTVPlugin.CONFIG_TEXT, PROP_SD_USER, '', 'Schedules Direct User Name', 'Your Schedules Direct user name.')
		sdUser.setPersistence(new ServerPropertyPersistence())
		
		PluginProperty sdPwd = new PluginProperty(SageTVPlugin.CONFIG_PASSWORD, PROP_SD_PWD, '', 'Schedules Direct Password', 'Your Schedules Direct password.')
		sdPwd.setPersistence(new ServerPropertyPersistence())
		
		PluginProperty logLvl = new PluginProperty(SageTVPlugin.CONFIG_CHOICE, PROP_LOG_LEVEL, 'INFO', 'Log Level', 'Select the level of logging performed by the plugin.  Changes are immediate.', ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'] as String[])
		logLvl.setPersistence(new ServerPropertyPersistence())
		
		PluginProperty sdjsonThreads = new PluginProperty(SageTVPlugin.CONFIG_INTEGER, PROP_SDJSON_THREADS, '200', 'Worker Threads for sdjson Downloads', 'Maximum number of worker threads for downloading EPG data.  Only edit if you are told to or you know what you\'re doing.')
		sdjsonThreads.setPersistence(new ServerPropertyPersistence())
		sdjsonThreads.setValidator(new IntRangeValidator(1, 200))
		
		PluginProperty grabberHeap = new PluginProperty(SageTVPlugin.CONFIG_INTEGER, PROP_GRABBER_HEAP, '512', 'Max Heap for sdjson Grabber', 'Amount of heap to allocate for sdjson grabber, in MB.')
		grabberHeap.setPersistence(new ServerPropertyPersistence())
		grabberHeap.setValidator(new IntRangeValidator(32, 1024))
		
		PluginProperty cacheTTL = new PluginProperty(SageTVPlugin.CONFIG_INTEGER, PROP_CACHE_TTL, '7', 'EPG Cache Cleanup', 'How often, in days, will the EPG cache be cleaned.')
		cacheTTL.setPersistence(new ServerPropertyPersistence())
		cacheTTL.setValidator(new IntRangeValidator(1, 30))
		
		PluginProperty sdjsonUrl = new PluginProperty(SageTVPlugin.CONFIG_TEXT, PROP_SDJSON_URL, 'https://data2.schedulesdirect.org', 'Base URL for sdjson', 'The base URL to use for sdjson; only change if told to.')
		sdjsonUrl.setPersistence(new ServerPropertyPersistence())
		
		PluginProperty sdjsonProgChunk = new PluginProperty(SageTVPlugin.CONFIG_INTEGER, PROP_SDJSON_PROG_CHUNK, '50000', 'Max Program Request for sdjson', 'Maximum size of a program request to Schedules Direct.  Change only if told to.')
		sdjsonProgChunk.setPersistence(new ServerPropertyPersistence())
		sdjsonProgChunk.setValidator(new IntRangeValidator(10, 50000))
		
		PluginProperty sdjsonSchedChunk = new PluginProperty(SageTVPlugin.CONFIG_INTEGER, PROP_SDJSON_SCHED_CHUNK, '1000', 'Max Schedule Request for sdjson', 'Maximum size of a schedule request to Schedules Direct.  Change only if told to.')
		sdjsonSchedChunk.setPersistence(new ServerPropertyPersistence())
		sdjsonSchedChunk.setValidator(new IntRangeValidator(10, 1000))
		
		PluginProperty grabberLogLvl = new PluginProperty(SageTVPlugin.CONFIG_CHOICE, PROP_GRABBER_LOG_LVL, 'INFO', 'Log Level for sdjson Grabber', 'Set the log level for the sdjson grabber application.', ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'] as String[])
		grabberLogLvl.setPersistence(new ServerPropertyPersistence())
		
		addProperty(refresh)
		addProperty(sdUser)
		addProperty(sdPwd)
		addProperty(logLvl)
		addProperty(showFilters)
		addProperty(airFilters)
		addProperty(showGenerators)
		addProperty(airGenerators)
		addProperty(chanGenerators)
		addProperty(lineupEditors)
		addProperty(cacheTTL)
		addProperty(sdjsonUrl)
		addProperty(grabberLogLvl)
		addProperty(grabberHeap)
		addProperty(sdjsonSchedChunk)
		addProperty(sdjsonProgChunk)
		addProperty(sdjsonThreads)
	}
	
	@ConfigValueChangeHandler('sdepg/logLevel')
	void updateInternalLogger() {
		InternalLogger.init()
	}
}