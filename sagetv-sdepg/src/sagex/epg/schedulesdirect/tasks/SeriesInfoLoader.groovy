/*
 *      Copyright 2014 Battams, Derek
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
package sagex.epg.schedulesdirect.tasks

import org.apache.log4j.Logger
import org.schedulesdirect.api.Program
import org.schedulesdirect.api.Program.Credit

import sagex.api.SeriesInfoAPI

class SeriesInfoLoader implements Runnable {
	static private final Logger LOG = Logger.getLogger(SeriesInfoLoader)
	
	private Collection seriesObjs
	
	public SeriesInfoLoader(Collection seriesObjs) {
		this.seriesObjs = seriesObjs
	}

	@Override
	public void run() {
		def skipped = 0
		def start = System.currentTimeMillis()
		try {
			for(Program p : seriesObjs) {
				def imgUrl = findImgUrl(p)
				if(imgUrl) {
					def people = getPeople(p)
					if(!people[0] || !people[1] || people[0].size() != people[1].size())
						people[1] = null
					SeriesInfoAPI.AddSeriesInfo(p.id[2..-5].toInteger(), p.title, null, !p.description ? null : p.description, null, !p.originalAirDate ? null : p.originalAirDate.format('yyyy-MM-dd'), null, null, null, imgUrl, people[0], people[1])
					if(LOG.isTraceEnabled())
						LOG.trace "Added SeriesInfo object for '$p.title' [$p.id]"
				} else
					++skipped
			}
		} catch(Throwable t) {
			LOG.error("Uncaught error!", t)
		} finally {
			LOG.info "Generated series info entries for ${seriesObjs.size() - skipped} objects in ${System.currentTimeMillis() - start}ms!"
			seriesObjs.clear()
		}
	}

	private String[][] getPeople(Program p) {
		def ppl = []
		def chars = []
		for(Credit c : p.credits) {
			if(c.role.toString().matches('ACTOR|VOICE|HOST|CORRESPONDENT|ANCHOR|JUDGE')) {
				ppl << c.name
				if(c.characterName)
					chars << c.characterName
			}
		}
		return [ppl, chars] as String[][]
	}	
	
	private String findImgUrl(Program p) {
		def imgs = p.images
		return imgs ? imgs[0].toString() : null
	}	
}