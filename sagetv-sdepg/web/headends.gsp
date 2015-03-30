<%
/*
 *      Copyright 2013-2015 Battams, Derek
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
 	import groovy.json.JsonSlurper
	import groovy.json.JsonOutput
	
	def clnt = request.getAttribute('clnt')
	def json = new JsonSlurper().parseText(clnt.getAvailableThings('countries'))
	def mub
%>
<h2>Configured Lineups</h2>
<form action="index.gsp" method="post">
	<input type="hidden" name="c" value="rmhe" />
	<input type="hidden" name="m" value="headends" />
	<table>
		<tr>
			<th><input type="checkbox" name="_" id="headends" /></th>
			<th>ID</th>
			<th>Description</th>
		</tr>
		<% mub = new groovy.xml.MarkupBuilder(out); clnt.lineups.each { he ->
			mub.tr {
				td { input(type:'checkbox', name:'heid', value:"lineups/$he.id", class:'regId') }
				td he.id
				td "$he.name $he.location"
			}
		} %>
	</table>
	<input type="submit" name="submit" value="Remove Selected" />
</form>

<h2>Add Lineup</h2>
<div>
	<span>Select country:</span>
	<span>
		<select name="country" id="srchCountry">
		<%
			def vars = [:]
			def singles = [:]
			def opts = [:]
			json.each { k, v ->
				json."$k".each {
					opts[it.fullName] = it
				}
			}
			opts = opts.sort { it.value.fullName }
			mub.option(value: '', '------ Select ------')
			opts.each { k, v ->
				mub.option(value: v.shortName, k)
				if(!v.onePostalCode)
					vars[v.shortName] = v.postalCode
				else
					singles[v.shortName] = v.postalCodeExample
			}
		%>
		</select>
	</span>
</div>
<div>
	<span>Enter zip/postal code:</span>
	<span><input type="text" name="zip" id="srchInput" /></span>
	<span><input type="button" id="search" value="Search" /></span>
</div>
<form action="index.gsp" method="post" id="addFrm" style="visibility: hidden;">
	<input type="hidden" name="c" value="addhe" />
	<input type="hidden" name="m" value="headends" />
	<table>
		<thead>
			<tr>
				<th><input type="checkbox" name="_" id="newHeadends" /></th>
				<th>ID</th>
				<th>Description</th>
			</tr>
		</thead>
		<tbody id="srchResults">

		</tbody>
	</table>
	<input type="submit" name="submit" value="Register Selected" />
</form>
<script>
<!--
var multis = <% out << JsonOutput.toJson(vars) << ';' %>
var singles = <% out << JsonOutput.toJson(singles) << ';' %>
\$(document).ready(function() {
	\$('#headends').change(function() {
		var val = \$(this).prop('checked');
		\$('.regId').each(function() {
			\$(this).prop('checked', val);
		});
	});
	\$('#newHeadends').change(function() {
		var val = \$(this).prop('checked');
		\$('.newId').each(function() {
			\$(this).prop('checked', val);
		});
	});
	\$('#search').click(function() {
		var country = \$('#srchCountry').val();
		if(country.length == 0)
			alert('Select a country!');
		else if(country in multis) {
			var i = multis[country].lastIndexOf('/');
			var opts = multis[country].length > i + 1 ? multis[country].slice(i + 1) : '';
			var exp = multis[country].slice(1, i);
			var regex = new RegExp('^' + exp + '\$', opts);
			if(!regex.test(\$('#srchInput').val()))
				alert('Zip is not valid for selected country!\\n' + multis[country]);
			else
				doSearch(\$(this), country);
		} else
			doSearch(\$(this), country);
	});
	\$('#srchCountry').change(function() {
		var input = \$(this);
		var box = \$('#srchInput');
		if(input.val() in singles) {
			box.val(singles[input.val()]);
			box.prop('readonly', true);
		} else {
			box.prop('readonly', false);
			box.val('');
		}
	});
});
function doSearch(btn, country) {
	btn.attr('disabled', true);
	btn.attr('value', 'Searching...');
	\$.ajax({
		url: '/sage/sdjson/ajax.groovy',
		context: btn,
		data: {'z': \$('#srchInput').val(), 'c': 'search', 'i': country},
		success: function() {
			\$('#srchResults').html(arguments[0]);
			\$('#addFrm').css('visibility', 'visible');
		
		},
		error: function() {
			alert('Search failed!  Please try again.');
		},
		complete: function() {
			btn.attr('disabled', false);
			btn.attr('value', 'Search');
		}
	});
}
-->
</script>
