<!-- 
Copyright (c) 2014, Alex Radu
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer. 
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies, 
either expressed or implied, of the PASTA Project.
-->

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<h1>Arena: ${arena.name}</h1>


<c:choose>
	<c:when test="${completed}">
		Next Execution : Arena is completed
	</c:when>
	<c:otherwise>
		Next Execution : ${arena.nextRunDate}
	</c:otherwise>
</c:choose>

<c:if test="${not completed}">
	<table>
		<c:forEach var="player" items="${players}">
			<c:set var="found" value="false"/>
			<c:if test="${not empty arena.players[unikey.username]}">
				<c:forEach items="${arena.players[unikey.username]}" var="value">
				    <c:if test="${value == player.playerName}">
				        <c:set var="found" value="true" />
				    </c:if>
				</c:forEach>
			</c:if>
			<c:if test="${not empty player.activePlayer or found}">
				<tr>
					<td>${player.playerName}</td>
					<c:choose>
						<c:when test="${found}">
							<td><a href="remove/${player.playerName}/">-</a></td>
						</c:when>
						<c:otherwise>
							<td><a href="add/${player.playerName}/">+</a></td>
						</c:otherwise>
					</c:choose>
					
				</tr>
			</c:if>
		</c:forEach>
	</table>
</c:if>

<c:if test="${not empty results}">
<h2>Results:</h2>
<c:choose>
	<c:when test="${unikey.tutor}">
		<c:set var="categories" value="${results.categories}"/>
	</c:when>
	<c:otherwise>
		<c:set var="categories" value="${results.studentVisibleCategories}"/>
	</c:otherwise>
</c:choose>
<table id="resultTable" class="tablesorter">
	<thead>
		<tr>
			<th>Player</th>
			<c:forEach items="${categories}" var="category">
				<th>${category}</th>
			</c:forEach>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${results.data}" var="dataLine">
			<tr>
				<td><a href="../${fn:split(dataLine.key, '.')[0]}/players/">${dataLine.key}</a></td>
				<c:forEach items="${categories}" var="category">
					<td><fmt:formatNumber type="number" maxFractionDigits="3" value="${dataLine.value[category]}" /></td>
				</c:forEach>
			</tr>
		</c:forEach>
	</tbody>
</table>

<script>
	$(document).ready(function() 
	    { 
	        $("#resultTable").tablesorter( {sortList: [[2,0]]} ); 
	    } 
	); 
</script>

</c:if>

<h2>Users Participating:</h2>
<c:forEach items="${arena.players}" var="user">
	<c:if test="${not empty user.value}">
		<c:forEach items="${user.value}" var="player">
			<a href="../${user.key}/players/">${user.key} : ${player} </a><br/>
		</c:forEach>
	</c:if>
</c:forEach>
