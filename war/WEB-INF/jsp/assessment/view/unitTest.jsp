<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<h1> Unit Test - ${unitTest.name}</h1>

<ul class="list">
<jsp:include page="../../recursive/fileWriter.jsp"/>
</ul>

<table>
	<tr><td>Has been tested:</td><td class="pastaTF pastaTF${unitTest.tested}">${unitTest.tested}</td></tr>
</table> 

<button id="newPopup"> Test Unit Test </button>
<button id="updateTest"> Update Test </button>

<div id="testUnitTest" syle="display:none;">
<span class="button bClose">
	<span><b>X</b></span>
</span>
	<h1> Test Unit Test </h1>
	<form:form commandName="submission" enctype="multipart/form-data" method="POST">
		<table>
			<tr><td>Unit Test Code:</td><td><form:input type="file" path="file"/></td></tr>
		</table>
		<form:input type="hidden" path="submittingUsername" value="${submittingUsername}"/>
		<form:input type="hidden" path="submittingForUsername" value="${submittingForUsername}"/>
		<form:input type="hidden" path="assessment" value="UNIT TEST CHECK - ${unitTest.shortName}"/>
    	<input type="submit" value="Upload" id="submit"/>
	</form:form>
</div>

<div id="updateUnitTest" syle="display:none;">
<span class="button bClose">
	<span><b>X</b></span>
</span>
	<span class="button bClose">
		<span><b>X</b></span>
	</span>
	<h1> Update Unit Test </h1>
	<form:form commandName="newUnitTestModel" enctype="multipart/form-data" method="POST">
		<table>
			<form:input autocomplete="off" type="hidden" path="testName" value="${unitTest.shortName}"/>
			<tr><td>Unit Test Code:</td><td><form:input type="file" path="file"/></td></tr>
		</table>
    	<input type="submit" value="Update" id="submit"/>
	</form:form>
</div>

<c:if test="${not empty latestResult}">
	<h2>Latest Test results
		<c:if test="${not empty latestResult.compileErrors}">
			- Compilation Errors Detected
		</c:if>
		<c:if test="${not empty latestResult.runtimeErrors}">
			- Runtime Errors Detected
		</c:if>
		<c:if test="${not empty latestResult.testCases}">
			- Execution Successful
		</c:if>
	</h2>
	<c:if test="${not empty latestResult.compileErrors}">
		<div class="ui-state-error">
			<pre>${latestResult.compileErrors}</pre>
		</div>
	</c:if>
	<c:if test="${not empty latestResult.runtimeErrors}">
		<div class="ui-state-error">
			<pre>${latestResult.runtimeErrors}</pre>
		</div>
	</c:if>
	<c:if test="${not empty latestResult.testCases}">
		<c:forEach var="testCase" items="${latestResult.testCases}">
				<div class="pastaUnitTestBoxResult pastaUnitTestBoxResult${testCase.testResult}" title="${testCase.testName}">&nbsp;</div>
		</c:forEach>
		</br>
		</br>
		<button id="acceptUnitTest" onClick="document.getElementById('comfirmButton').onclick = function(){ location.href='../tested/${unitTest.shortName}/'};$('#comfirmPopup').bPopup();">Working as intended</button></td>
		<table class="pastaTable">
			<tr><th>Status</th><th>Test Name</th><th>Execution Time</th><th>Message</th></tr>
			<c:forEach var="testCase" items="${latestResult.testCases}">
				<tr>
					<td><span class="pastaUnitTestResult pastaUnitTestResult${testCase.testResult}">${testCase.testResult}</span></td>
					<td style="text-align:left;">${testCase.testName}</td>
					<td>${testCase.time}</td>
					<td>
						<pre>${testCase.type} - ${testCase.testMessage}</pre>
					</td>
				</tr>
			</c:forEach>
		</table>
	</c:if>
</c:if>

<div id="comfirmPopup" >
	<span class="button bClose">
		<span><b>X</b></span>
	</span>
	<h1>Are you sure you want to do that?</h1>
	<button id="comfirmButton" onClick="">Confirm</button>
</div>

<script>
	;(function($) {

         // DOM Ready
        $(function() {

            // Binding a click event
            // From jQuery v.1.7.0 use .on() instead of .bind()
            $('#newPopup').bind('click', function(e) {

                // Prevents the default action to be triggered. 
                e.preventDefault();

                // Triggering bPopup when click event is fired
                $('#testUnitTest').bPopup();

            });
            

            // Binding a click event
            // From jQuery v.1.7.0 use .on() instead of .bind()
            $('#updateTest').bind('click', function(e) {

                // Prevents the default action to be triggered. 
                e.preventDefault();

                // Triggering bPopup when click event is fired
                $('#updateUnitTest').bPopup();

            });

        });

    })(jQuery);
</script>