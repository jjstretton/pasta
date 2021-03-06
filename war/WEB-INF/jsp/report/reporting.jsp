<%--
MIT License

Copyright (c) 2012-2017 PASTA Contributors (see CONTRIBUTORS.txt)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
--%>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="tag" tagdir="/WEB-INF/tags"%>
<%@ taglib prefix="pasta" uri="pastaTag"%>

<h1>Reporting<c:if test="${not empty pretending}"> - ${pretending.username}</c:if></h1>

<c:if test="${not empty pretending}">
	<div class='section'>
		<div class='part'>
			<span class='warning'><span class='fa fa-warning'></span> Warning:</span>
			You are viewing reports as if you were <a href='<c:url value="/student/${pretending.username}/home/"/>'>${pretending.username}</a>.
			Click <a href='<c:url value="/reporting/"/>'>here</a> to view your own reports.
		</div>
	</div>
</c:if>

<c:if test="${empty allReports}">
	<div class='section'>
		<div class='part'>
			<span>No reports to display at this time.</span>
		</div>
	</div>
</c:if>
<c:forEach var="report" items="${allReports}">
	<div class='report'>
		<div class='section-title'>
			<h3 class='report-name'>${report.name}</h3>
			<p>${report.description}</p>
		</div>
		<c:if test="${user.instructor and (empty pretending or pretending.instructor) }">
			<div class='report-controls part'>
				<span class='fa fa-eye'></span>
				<span><a class='edit-permissions'>Who can see this report?</a></span>
			</div>
		</c:if>
		<div class='report-content' data-report='${report.id}'>
		</div>
	</div>
</c:forEach>

<script src='<c:url value="/static/scripts/reporting/reporting.js"/>'></script>
<c:forEach var="report" items="${allReports}">
<script src='<c:url value="/static/scripts/reporting/reports/"/>${report.id}.js'></script>
</c:forEach>