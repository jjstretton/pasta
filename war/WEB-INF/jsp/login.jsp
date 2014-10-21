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
<h1>Login</h1>
<br />
<div class="susk-form" style="text-align:center; width:500px">
	<form:errors path="loginForm.*">
		<div class="susk-info-bar error"><span class="image"></span>
			<p class="message"><spring:message code="errors.message" /></p>
		</div>
	</form:errors>
	<form:form method="post" commandName="LOGINFORM" autocomplete="off">
		<div>
			<form:label for="unikey" path="unikey" cssClass="required">UniKey <span class="star-required">*</span></form:label>
			<form:input path="unikey" size="50"  type="text" name="unikey" id="unikey" />
			<form:errors path="unikey" cssClass="susk-form-errors" element="div" />
			<script>document.getElementById('unikey').focus()</script>
		</div>
		<div class="susk-form-clear"></div>
		<div>
			<form:label path="password" cssClass="required">Password <span class="star-required">*</span></form:label> 
			<form:password path="password" size="50" name="password" id="password" />
			<form:errors path="password" cssClass="susk-form-errors" element="div" />
		</div>
		<div class="susk-form-clear"></div>
		
		<div style="text-align:left">
			<button type="submit" style= "margin-left: 17.5em; padding-left: 1em;padding-right: 1em;"id="Submit" name="Submit">Login</button>
		</div>
		<div class="susk-form-clear"></div>
	</form:form>
	<div class="susk-form-clear"></div>
</div>