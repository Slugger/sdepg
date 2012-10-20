/*
 *      Copyright 2012 Battams, Derek
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

import org.schedulesdirect.api.Program.MPAARating

class GeneratedProgram {
	String id
	String title
	String episodeTitle
	String description
	String[] categories
	SageCreditList credits
	MPAARating rating
	String[] advisories
	int year
	Date originalAirDate
	short seasonNumber
	short episodeNumber
	boolean forceUnique
}
