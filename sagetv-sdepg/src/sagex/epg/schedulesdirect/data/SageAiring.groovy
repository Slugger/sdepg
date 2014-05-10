/*
 *      Copyright 2012-2014 Battams, Derek
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
package sagex.epg.schedulesdirect.data

import org.schedulesdirect.api.Airing.DolbyStatus
import org.schedulesdirect.api.Airing.FinaleStatus
import org.schedulesdirect.api.Airing.LiveStatus
import org.schedulesdirect.api.Airing.PremiereStatus

class SageAiring {
	String programId
	int stationId
	long startTime
	long durationMillis
	int partNumber
	int totalParts
	String tvRating
	boolean closedCaptioned
	boolean stereo
	boolean hdtv
	boolean subtitled
	boolean sap
	DolbyStatus dolbyStatus
	boolean letterboxed
	boolean newAiring
	PremiereStatus premiereStatus
	FinaleStatus finaleStatus
	LiveStatus liveStatus
	String broadcastLanguage
}
