/*
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
*/

package pasta.web.controller;

import java.beans.PropertyEditorSupport;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pasta.domain.UserPermissionLevel;
import pasta.domain.form.UpdateHandMarkingForm;
import pasta.domain.form.validate.UpdateHandMarkingFormValidator;
import pasta.domain.template.HandMarking;
import pasta.domain.template.WeightedField;
import pasta.domain.user.PASTAUser;
import pasta.repository.HandMarkingDAO;
import pasta.service.HandMarkingManager;
import pasta.web.WebUtils;

/**
 * Controller class for Hand marking functions. 
 * <p>
 * Handles mappings of $PASTAUrl$/handMarking/...
 * <p>
 * Only teaching staff can access this url.
 * 
 * @author Alex Radu
 * @version 2.0
 * @since 2013-08-15
 *
 */
@Controller
@RequestMapping("handMarking/")
public class HandMarkingController {

	public HandMarkingController() {
	}

	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private HandMarkingDAO handMarkingDao;
	
	@Autowired
	private HandMarkingManager handMarkingManager;
	
	@Autowired
	private UpdateHandMarkingFormValidator updateValidator;

	// ///////////////////////////////////////////////////////////////////////////
	// Models //
	// ///////////////////////////////////////////////////////////////////////////

	@ModelAttribute("handMarking")
	public HandMarking loadHandMarking(@PathVariable("handMarkingId") long handMarkingId) {
		return handMarkingManager.getHandMarking(handMarkingId);
	}
	
	@ModelAttribute("updateHandMarkingForm")
	public UpdateHandMarkingForm returnUpdateForm(@PathVariable("handMarkingId") long handMarkingId) {
		return new UpdateHandMarkingForm(
				handMarkingManager.getHandMarking(handMarkingId));
	}
	
	@ModelAttribute("user")
	public PASTAUser loadUser(HttpServletRequest request) {
		WebUtils.ensureLoggedIn(request);
		return WebUtils.getUser();
	}

	// ///////////////////////////////////////////////////////////////////////////
	// HAND MARKING //
	// ///////////////////////////////////////////////////////////////////////////

	/**
	 * $PASTAUrl$/handMarking/{handMarkingId}/
	 * <p>
	 * View the hand marking template.
	 * 
	 * If the user has not authenticated: redirect to login.
	 * 
	 * If the user is not a tutor: redirect to home
	 * 
	 * ATTRIBUTES:
	 * <table>
	 * 	<tr><td>handMarking</td><td>the hand marking object</td></tr>
	 * </table>
	 * 
	 * JSP:<ul><li>assessment/view/handMarks</li></ul>
	 * 
	 * @param handMarkingId the id of the hand marking template
	 * @param model the model being used
	 * @return "redirect:/login/" or "redirect:/home/" or "assessment/view/handMarks"
	 */
	@RequestMapping(value = "{handMarkingId}/")
	public String viewHandMarking(
			@PathVariable("handMarkingId") Long handMarkingId,
			@ModelAttribute("handMarking") HandMarking template, Model model) {
		WebUtils.ensureAccess(UserPermissionLevel.TUTOR);
		
		if(model.containsAttribute("hasValidationErrors")) {
			UpdateHandMarkingForm form = (UpdateHandMarkingForm) model.asMap().get("updateHandMarkingForm");
			model.addAttribute("allData", form.getNewData());
			model.addAttribute("allRows", form.getNewRowHeader());
			model.addAttribute("allColumns", form.getNewColumnHeader());
		} else {
			model.addAttribute("allData", template.getData());
			model.addAttribute("allRows", template.getRowHeader());
			model.addAttribute("allColumns", template.getColumnHeader());
		}

		return "assessment/view/handMarks";
	}

	/**
	 * $PASTAUrl/handMarking/{handMarkingId}/ - POST
	 * <p>
	 * Update the hand marking template
	 * 
	 * If the user has not authenticated: redirect to login.
	 * 
	 * If the user is not a tutor: redirect to home
	 * 
	 * If the user is an instructor, update the hand marking object using
	 * {@link pasta.service.HandMarkingManager#updateHandMarking(HandMarking)}
	 * 
	 * @param form the updated hand marking object
	 * @param result the binding result used for feedback
	 * @param handMarkingId the id of the hand marking template
	 * @param model the model being used
	 * @return "redirect:/login/" or "redirect:/home/" or "redirect:."
	 */
	@RequestMapping(value = "{handMarkingId}/", method = RequestMethod.POST)
	public String updateHandMarking(
			@PathVariable("handMarkingId") Long handMarkingId, 
			@ModelAttribute(value = "handMarking") HandMarking template,
			@Valid @ModelAttribute(value = "updateHandMarkingForm") UpdateHandMarkingForm form, BindingResult result,
			RedirectAttributes attr, Model model,HttpServletRequest request) {
		WebUtils.ensureAccess(UserPermissionLevel.INSTRUCTOR);
		
		updateValidator.validate(form, result);
		if(result.hasErrors()) {
			attr.addFlashAttribute("updateHandMarkingForm", form);
			attr.addFlashAttribute("org.springframework.validation.BindingResult.updateHandMarkingForm", result);
			attr.addFlashAttribute("hasValidationErrors", Boolean.TRUE);
			return "redirect:.";
		}
		
		handMarkingManager.updateHandMarking(template, form);
		return "redirect:.";
	}
	
	/**
	 * $PASTAUrl$/handMarking/delete/{handMarkingId}/
	 * <p>
	 * Delete a hand marking template.
	 * 
	 * If the user has not authenticated: redirect to login.
	 * 
	 * If the user is not a tutor: redirect to home
	 * 
	 * If the user is an instructor: remove the hand marking template
	 * using {@link pasta.service.HandMarkingManager#removeHandMarking(String)}
	 * then redirect to $PASTAUrl$/handMarking/
	 * 
	 * @param handMarkingId the id of the hand marking template
	 * @param model the model being used
	 * @return "redirect:/login/" or "redirect:/home/" or "redirect:../../"
	 */
	@RequestMapping(value = "delete/{handMarkingId}/")
	public String deleteHandMarking(
			@PathVariable("handMarkingId") Long handMarkingId, Model model) {
		WebUtils.ensureAccess(UserPermissionLevel.INSTRUCTOR);
		handMarkingManager.removeHandMarking(handMarkingId);
		return "redirect:../../";
	}
	
	
	
	@InitBinder
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
	    // when passing a column/row as an id, convert that id into an actual WeightedField object
		binder.registerCustomEditor(WeightedField.class, new PropertyEditorSupport() {
		    @Override
		    public void setAsText(String text) {
		    	WeightedField field = handMarkingDao.getWeightedField(Long.parseLong(text));
		    	if(field == null) {
		    		field = new WeightedField();
		    		field.setId(Long.parseLong(text));
		    	}
		    	setValue(field);
		    }
	    });
	}

}