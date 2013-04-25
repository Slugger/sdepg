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
	def clnt = request.getAttribute('clnt')
	def mub
%>
<h2>Configured Headends</h2>
<form action="index.gsp" method="post">
	<input type="hidden" name="c" value="rmhe" />
	<input type="hidden" name="m" value="headends" />
	<table>
		<tr>
			<th><input type="checkbox" name="_" id="headends" /></th>
			<th>ID</th>
			<th>Total Lineups</th>
			<th>Description</th>
		</tr>
		<% mub = new groovy.xml.MarkupBuilder(out); clnt.headends.each { he ->
			mub.tr {
				td { input(type:'checkbox', name:'heid', value:he.id, class:'regId') }
				td he.id
				td he.lineups.size()
				td "$he.name $he.location"
			}
		} %>
	</table>
	<input type="submit" name="submit" value="Remove Selected" />
</form>

<h2>Add Headend</h2>
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
				<th>Total Lineups</th>
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
		\$(this).attr('disabled', true);
		\$(this).attr('value', 'Searching...');
		\$.ajax({
			url: '/sage/sdjson/ajax.groovy',
			context: \$(this),
			data: {'z': \$('#srchInput').val(), 'c': 'search'},
			success: function() {
				\$('#srchResults').html(arguments[0]);
				\$('#addFrm').css('visibility', 'visible');
			
			},
			error: function() {
				alert('Search failed!  Please try again.');
			},
			complete: function() {
				\$(this).attr('disabled', false);
				\$(this).attr('value', 'Search');
			}
		});
	});
});
-->
</script>
