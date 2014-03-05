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

import sagex.api.UserRecordAPI

/**
 * @author Derek Battams <derek@battams.ca>
 *
 */
class LineupMap {
	static final String USER_RECORD_STORE = 'sdepg'
	static final String USER_RECORD_KEY = 'lineups'
	
	static long addId(String id) {
		def hash = Math.abs(id.hashCode())
		def userRecord = UserRecordAPI.AddUserRecord(USER_RECORD_STORE, USER_RECORD_KEY)
		UserRecordAPI.SetUserRecordData(userRecord, hash.toString(), id)
		return hash
	}
	
	static String getId(long hash) {
		def userRecord = UserRecordAPI.AddUserRecord(USER_RECORD_STORE, USER_RECORD_KEY)
		return UserRecordAPI.GetUserRecordData(userRecord, Long.toString(hash))
	}
}
