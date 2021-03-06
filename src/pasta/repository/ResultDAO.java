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

package pasta.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pasta.domain.result.AssessmentResult;
import pasta.domain.result.AssessmentResultSummary;
import pasta.domain.result.HandMarkingResult;
import pasta.domain.result.UnitTestCaseResult;
import pasta.domain.result.UnitTestResult;
import pasta.domain.template.Assessment;
import pasta.domain.template.WeightedHandMarking;
import pasta.domain.user.PASTAGroup;
import pasta.domain.user.PASTAUser;
import pasta.util.ProjectProperties;

/**
 * Data Access Object for Results.
 * <p>
 * 
 * This class is responsible for all of the interaction
 * between the data layer (disk in this case) and the system
 * for assessment results.
 * 
 * @author Alex Radu
 * @version 2.0
 * @since 2012-11-16
  *
 */
@Transactional
@Repository("resultDAO")
public class ResultDAO extends BaseDAO {
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	public void delete(AssessmentResultSummary result) {
		sessionFactory.getCurrentSession().delete(result);
	}

	/**
	 * Save the assessment summary to the database.
	 *
	 * @param result the assessment summary being saved
	 */
	public void saveOrUpdate(AssessmentResultSummary result) {
		sessionFactory.getCurrentSession().saveOrUpdate(result);
	}

	public AssessmentResult getAssessmentResult(long id) {
		return (AssessmentResult) sessionFactory.getCurrentSession().get(AssessmentResult.class, id);
	}
	
	public AssessmentResultSummary getAssessmentResultSummary(PASTAUser user, Assessment assessment) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResultSummary.class);
		cr.add(Restrictions.eq("id.assessment", assessment));
		cr.add(Restrictions.eq("id.user", user));

		@SuppressWarnings("unchecked")
		AssessmentResultSummary result = (AssessmentResultSummary) DataAccessUtils.uniqueResult(cr.list());
		return result;
	}

	public int getSubmissionCount(PASTAUser user, long assessmentId, boolean includeGroup, boolean includeCompileErrors) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		cr.createCriteria("assessment").add(Restrictions.eq("id", assessmentId));
		restrictCriteriaUser(cr, user, includeGroup, assessmentId);
		
		if(includeCompileErrors) {
			cr.setProjection(Projections.rowCount());
			return DataAccessUtils.intResult(cr.list());
		}
		
		@SuppressWarnings("unchecked")
		List<AssessmentResult> results = cr.list();
		int count = 0;
		for(AssessmentResult result : results) {
			if(!result.isError()) {
				count++;
			}
		}
		return count;
	}
	
	public AssessmentResult getResult(PASTAUser user, long assessmentId, Date submissionDate, boolean includeGroup) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		cr.createCriteria("assessment").add(Restrictions.eq("id", assessmentId));
		cr.add(Restrictions.eq("submissionDate", submissionDate));
		restrictCriteriaUser(cr, user, includeGroup, assessmentId);
		
		@SuppressWarnings("unchecked")
		AssessmentResult result = (AssessmentResult) DataAccessUtils.uniqueResult(cr.list());
		if(result != null) {
			refreshHandMarking(result);
		}
		return result;
	}
	
	public AssessmentResult getLatestIndividualResult(PASTAUser user, long assessmentId) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		cr.createCriteria("assessment").add(Restrictions.eq("id", assessmentId));
		cr.addOrder(Order.desc("submissionDate"));
		restrictCriteriaUser(cr, user, false, assessmentId);
		cr.setMaxResults(1);
		
		@SuppressWarnings("unchecked")
		AssessmentResult result = (AssessmentResult) DataAccessUtils.uniqueResult(cr.list());
		if(result != null) {
			refreshHandMarking(result);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public AssessmentResult getLatestGroupResult(PASTAUser user, long assessmentId) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		cr.createCriteria("assessment").add(Restrictions.eq("id", assessmentId));
		cr.addOrder(Order.desc("submissionDate"));
		restrictCriteriaUser(cr, user, true, assessmentId);
		
		DetachedCriteria groupCr = DetachedCriteria.forClass(PASTAGroup.class);
		groupCr.setProjection(Projections.property("id"));
		groupCr.createCriteria("assessment").add(Restrictions.eq("id", assessmentId));
		groupCr.createAlias("members", "member");
		groupCr.add(Restrictions.eq("member.id", user.getId()));
		
		cr.add(Subqueries.propertyEq("user.id", groupCr));
		cr.setMaxResults(1);
		
		AssessmentResult result = (AssessmentResult) DataAccessUtils.uniqueResult(cr.list());
		if(result != null) {
			refreshHandMarking(result);
		}
		return result;
	}
	
	public List<AssessmentResult> getAllResults(PASTAUser user, long assessmentId, boolean latestFirst, boolean includeGroup) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		if(user != null) {
			restrictCriteriaUser(cr, user, includeGroup, assessmentId);
		}
		if(assessmentId > 0) {
			cr.createCriteria("assessment").add(Restrictions.eq("id", assessmentId));
		}
		if(latestFirst) {
			cr.addOrder(Order.desc("submissionDate"));
		} else {
			cr.addOrder(Order.asc("submissionDate"));
		}
		@SuppressWarnings("unchecked")
		List<AssessmentResult> results = cr.list();
		if(results != null) {
			for(AssessmentResult result : results) {
				refreshHandMarking(result);
			}
		}
		return results;
	}
	
	@SuppressWarnings("unchecked")
	public List<Date> getAllSubmissionDates(PASTAUser user, long assessmentId, boolean latestFirst, boolean includeGroup) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		if(user != null) {
			restrictCriteriaUser(cr, user, includeGroup, assessmentId);
		}
		if(assessmentId > 0) {
			cr.createCriteria("assessment").add(Restrictions.eq("id", assessmentId));
		}
		if(latestFirst) {
			cr.addOrder(Order.desc("submissionDate"));
		} else {
			cr.addOrder(Order.asc("submissionDate"));
		}
		cr.setProjection(Projections.property("submissionDate"));
		return cr.list();
	}
	
	public List<AssessmentResult> getResultsForMultiUserAssessment(List<PASTAUser> users,
			long assessmentId, int resultCount, boolean latestFirst) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		if(users.isEmpty()) {
			return new LinkedList<AssessmentResult>();
		}
		if(users.size() == 1) {
			cr.add(Restrictions.eq("user", users.get(0)));
		} else {
			cr.add(Restrictions.in("user", users));
		}
		if(assessmentId > 0) {
			cr.createCriteria("assessment").add(Restrictions.eq("id", assessmentId));
		}
		if(latestFirst) {
			cr.addOrder(Order.desc("submissionDate"));
		} else {
			cr.addOrder(Order.asc("submissionDate"));
		}
		cr.setMaxResults(resultCount);
		@SuppressWarnings("unchecked")
		List<AssessmentResult> results = cr.list();
		if(results != null) {
			for(AssessmentResult result : results) {
				refreshHandMarking(result);
			}
		}
		return results;
	}
	
		@SuppressWarnings("unchecked")
		public List<AssessmentResult> getLatestResultsForMultiUser(List<PASTAUser> users) {
			if(users.isEmpty()) {
				return new LinkedList<AssessmentResult>();
			}

			DetachedCriteria latestSub = DetachedCriteria.forClass(AssessmentResult.class);
			latestSub.setProjection(
					Projections.projectionList()
					.add(Projections.groupProperty("user"))
					.add(Projections.groupProperty("assessment"))
					.add(Projections.max("submissionDate"))
					);
			latestSub.add(Restrictions.in("user", users));

			Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class)
					.add(Subqueries.propertiesIn(new String[] {"user", "assessment", "submissionDate"}, latestSub));

			return cr.list();
		}

		public List<AssessmentResultSummary> getResultsSummaryForMultiUser(Set<PASTAUser> users) {
			if(users.isEmpty()) {
				return new LinkedList<AssessmentResultSummary>();
			}
		
			Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResultSummary.class)
					.add(Restrictions.in("id.user", users));

			@SuppressWarnings("unchecked")
			List<AssessmentResultSummary> list = (List<AssessmentResultSummary>)cr.list();
			return list;
		}

	private void restrictCriteriaUser(Criteria cr, PASTAUser user, boolean includeGroup, long assessmentId) {
		if(includeGroup) {
			DetachedCriteria groupCr = DetachedCriteria.forClass(PASTAGroup.class);
			groupCr.setProjection(Projections.property("id"));
			if(assessmentId > 0) {
				groupCr.createCriteria("assessment").add(Restrictions.eq("id", assessmentId));
			}
			groupCr.createAlias("members", "member");
			groupCr.add(Restrictions.eq("member.id", user.getId()));
			Criterion userGroupCr = assessmentId > 0 ?
					Subqueries.propertyEq("user.id", groupCr) :
					Subqueries.propertyIn("user.id", groupCr);
			cr.add(Restrictions.or(Restrictions.eq("user", user), userGroupCr));
		} else {
			cr.add(Restrictions.eq("user", user));
		}
	}
	
	public File getLastestSubmission(PASTAUser user, Assessment assessment) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		cr.setProjection(Projections.property("submissionDate"));
		cr.createCriteria("user").add(Restrictions.eq("id", user.getId()));
		cr.createCriteria("assessment").add(Restrictions.eq("id", assessment.getId()));
		cr.addOrder(Order.desc("submissionDate"));
		cr.setMaxResults(1);
		@SuppressWarnings("unchecked")
		List<Date> results = cr.list();
		if(results == null || results.isEmpty()) {
			return null;
		}
		Date subDate = results.get(0);
		return new File(ProjectProperties.getInstance().getSubmissionsLocation() + "assessments/" + assessment.getId() + "/" + subDate + "/submission");
	}
	
	/**
	 * Load a unit test result from a location
	 * 
	 * @param location the location of the test
	 * @return the result of the unit test
	 */
	public UnitTestResult getUnitTestResultFromDisk(String location){
		return getUnitTestResultFromDisk(location, null, null);
	}
	
	/**
	 * Load a unit test result from a location, including error line numbers in
	 * the 'type' if the error occurred in one of the files listed in the given
	 * context.
	 * 
	 * @param location the location of the test
	 * @param errorContext a list of file names to check for error context
	 * @param testDescriptions 
	 * @return the result of the unit test
	 */
	public UnitTestResult getUnitTestResultFromDisk(String location, Collection<String> errorContext, Map<String, String> testDescriptions){
		UnitTestResult result = new UnitTestResult();
		
		//TODO: replace with generic file
		// check to see if there is a results.xml file
		File testResults = new File(location+"/result.xml");
		if(testResults.exists() && testResults.length() != 0){
			try{	
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder;
				dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(testResults);
				doc.getDocumentElement().normalize();
				
				ArrayList<UnitTestCaseResult> testCases = new ArrayList<UnitTestCaseResult>();
				
				NodeList unitTestList = doc.getElementsByTagName("testcase");
				if (unitTestList != null && unitTestList.getLength() > 0) {
					for (int i = 0; i < unitTestList.getLength(); i++) {
						Node unitTestNode = unitTestList.item(i);
						if (unitTestNode.getNodeType() == Node.ELEMENT_NODE) {
							UnitTestCaseResult caseResult = new UnitTestCaseResult();
							
							Element unitTestElement = (Element) unitTestNode;
							// name
							caseResult.setTestName(unitTestElement.getAttribute("name"));
							// time
							caseResult.setTime(Double.parseDouble(unitTestElement.getAttribute("time")));
							// test case - assume it is a pass.
							caseResult.setTestResult(UnitTestCaseResult.PASS);
							
							if(testDescriptions != null) {
								caseResult.setTestDescription(testDescriptions.get(caseResult.getTestName()));
							}
							
							// if failed
							if(unitTestElement.hasChildNodes()){
								Element failedUnitTestElement = (Element) unitTestNode.getChildNodes().item(1);
								
								// new result
								caseResult.setTestResult(failedUnitTestElement.getNodeName());
								// message
								if(failedUnitTestElement.hasAttribute("message")){
									caseResult.setTestMessage(failedUnitTestElement.getAttribute("message"));
								}
								// type
								if(failedUnitTestElement.hasAttribute("type")){
									caseResult.setType((failedUnitTestElement.getAttribute("type")));
								}
								// extended message
								if(getText(failedUnitTestElement) != null){
									String message = getText(failedUnitTestElement);
									caseResult.setExtendedMessage(message);
									
									// Include error line number for Java submissions
									if(errorContext != null && caseResult.getType() != null && caseResult.isError()) {
										String find = "(\\(.+?\\.java:[0-9]+\\))";
										Pattern p = Pattern.compile(find);
										Matcher m = p.matcher(message);
										boolean found = false;
										while(m.find() && !found) {
											String line = m.group();
											for(String file : errorContext) {
												if(line.contains(file)) {
													caseResult.setType(caseResult.getType() + " at " + line);
													found = true;
													break;
												}
											}
										}
									}
								}
							}
							testCases.add(caseResult);
						}
					}
				}
				result.setTestCases(testCases);
				return result;
			} 
			catch (Exception e){
				logger.error("Could not read result.xml", e);
			}
		}
		
		return null;
	}
	
	private void refreshHandMarking(AssessmentResult result) {
		List<HandMarkingResult> oldResults = new ArrayList<HandMarkingResult>(result.getHandMarkingResults());
		result.getHandMarkingResults().clear();
				
		int same = 0;
		int initSize = oldResults.size();
		for(WeightedHandMarking template : result.getAssessment().getHandMarking()) {
			if(result.isGroupResult() != template.isGroupWork()) {
				continue;
			}
			
			boolean found = false;
			for(HandMarkingResult currResult : oldResults) {
				if(currResult.getWeightedHandMarking().getId() == template.getId()) {
					found = true;
					result.addHandMarkingResult(currResult);
					same++;
					break;
				}
			}
			if(!found) {
				HandMarkingResult newResult = new HandMarkingResult();
				newResult.setWeightedHandMarking(template);
				saveOrUpdate(newResult);
				result.addHandMarkingResult(newResult);
			}
		}
		
		if(same != initSize || result.getHandMarkingResults().size() != initSize) {
			update(result);
		}
	}

	public void unlinkUnitTest(long id) {
		Session session = sessionFactory.getCurrentSession();
		@SuppressWarnings("unchecked")
		List<AssessmentResult> results = session.createCriteria(AssessmentResult.class)
				.createCriteria("unitTests", "utResult")
				.createCriteria("utResult.weightedUnitTest.test", "test")
				.add(Restrictions.eq("test.id", id))
				.list();
		for(AssessmentResult result : results) {
			Iterator<UnitTestResult> utIt = result.getUnitTests().iterator();
			while(utIt.hasNext()) {
				UnitTestResult utResult = utIt.next();
				if(utResult.getTest().getId() == id) {
					utResult.setWeightedUnitTest(null);
					session.update(utResult);
					utIt.remove();
				}
			}
			session.update(result);
		}
	}

	@SuppressWarnings("unchecked")
	public List<AssessmentResult> getWaitingResults() {
		return sessionFactory.getCurrentSession()
				.createCriteria(AssessmentResult.class)
				.add(Restrictions.eq("waitingToRun", true))
				.list();
	}

	private static String getText(Element element) {
		StringBuffer stringBuffer = new StringBuffer();
		NodeList elementChildren = element.getChildNodes();
		boolean found = false;
		for (int i = 0; i < elementChildren.getLength(); i++) {
		  Node node = elementChildren.item(i);
		  if (node.getNodeType() == Node.TEXT_NODE) {
		    stringBuffer.append(node.getNodeValue());
		    found = true;
		  }
		}
		return found ? stringBuffer.toString() : null;
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getAllTestCaseDetails() {
		String sql = "SELECT ar.id AS 'submission_id', utcr.name AS 'test_case', utcr.result, (wut.weight / tcc.test_case_count) AS 'test_case_weight' " + 
				"FROM assessment_results ar " + 
				"INNER JOIN unit_test_results utr ON ar.id = utr.assessment_result_id " + 
				"INNER JOIN unit_test_case_results utcr ON utcr.unit_test_result_id = utr.id " + 
				"INNER JOIN weighted_unit_tests wut ON wut.id = utr.weighted_unit_test_id " + 
				"INNER JOIN ( " + 
				"  SELECT unit_test_result_id AS 'utr_id', count(*) AS 'test_case_count' FROM unit_test_case_results GROUP BY unit_test_result_id " + 
				") AS tcc ON tcc.utr_id = utr.id " + 
				"ORDER BY ar.id, utcr.name";
		SQLQuery query = sessionFactory.getCurrentSession().createSQLQuery(sql);
		return query.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<Object[]> getAllSubmissionDetails() {
		String sql = "SELECT ar.id AS 'submission_id', a.id AS 'assessment_id', a.name AS 'assessment_name', " + 
				"rd.date AS 'assessment_release_date', a.dueDate AS 'assessment_due_date', " + 
				"grades.auto_percent * 100.0 AS 'auto_mark_weighted_percentage', " + 
				"ar.submission_date, u1.username AS 'user', u1.permission_level, " + 
				"u2.username AS 'submitted_by', IFNULL(agm.members,'') AS 'group_members' " + 
				"FROM assessment_results ar " + 
				"INNER JOIN users u1 ON ar.user_id = u1.id " + 
				"INNER JOIN users u2 ON ar.submitted_by = u2.id " + 
				"INNER JOIN assessments a ON ar.assessment_id = a.id " + 
				"INNER JOIN ( " + 
				"  SELECT rel_a.id, IFNULL(rel_rd.release_date, '') AS 'date' FROM assessments rel_a LEFT OUTER JOIN rules_date rel_rd ON (rel_a.release_rule_id = rel_rd.id) " + 
				") rd ON (rd.id = a.id) " + 
				"LEFT OUTER JOIN ( " + 
				"  SELECT ag.id AS 'group_id', GROUP_CONCAT(u.username SEPARATOR ',') AS 'members' FROM assessment_groups ag INNER JOIN assessment_group_members agm ON (ag.id = agm.assessment_group_id) inner join users u on (agm.user_id = u.id) group by ag.id " + 
				") agm ON (agm.group_id = ar.user_id) " + 
				"INNER JOIN ( " + 
				"  SELECT ar.id AS submission_id, (SUM(((correct.pass_test_case_count / tcc.test_case_count) * wut.weight)) / SUM(wut.weight)) AS auto_percent " + 
				"  FROM assessment_results ar " + 
				"  INNER JOIN unit_test_results utr ON ar.id = utr.assessment_result_id " + 
				"  INNER JOIN weighted_unit_tests wut ON wut.id = utr.weighted_unit_test_id " + 
				"  INNER JOIN ( " + 
				"    SELECT unit_test_result_id AS 'utr_id', count(*) AS 'test_case_count' " + 
				"    FROM unit_test_case_results " + 
				"    GROUP BY unit_test_result_id " + 
				"  ) AS tcc ON tcc.utr_id = utr.id " + 
				"  INNER JOIN ( " + 
				"    SELECT unit_test_result_id AS 'utr_id', SUM(result='pass') AS 'pass_test_case_count' " + 
				"    FROM unit_test_case_results " + 
				"    GROUP BY unit_test_result_id " + 
				"  ) AS correct ON correct.utr_id = utr.id " + 
				"  GROUP BY ar.id " + 
				") AS grades ON grades.submission_id = ar.id " + 
				"ORDER BY ar.submission_date";
		SQLQuery query = sessionFactory.getCurrentSession().createSQLQuery(sql);
		return query.list();
	}

	public List<AssessmentResult> getAllResultsForAssessment(Assessment assessment) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		cr.createCriteria("assessment").add(Restrictions.eq("id", assessment.getId()));
		@SuppressWarnings("unchecked")
		List<AssessmentResult> results = cr.list();
		if(results != null) {
			for(AssessmentResult result : results) {
				refreshHandMarking(result);
			}
		}
		return results;
	}

	public List<AssessmentResult> getAllResultsForUser(PASTAUser user) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResult.class);
		cr.createCriteria("user").add(Restrictions.eq("id", user.getId()));
		@SuppressWarnings("unchecked")
		List<AssessmentResult> results = cr.list();
		if(results != null) {
			for(AssessmentResult result : results) {
				refreshHandMarking(result);
			}
		}
		return results;
	}
	
	@SuppressWarnings("unchecked")
	public List<AssessmentResultSummary> getAllResultSummariesForUser(PASTAUser user) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResultSummary.class);
		cr.add(Restrictions.eq("id.user", user));
		return cr.list();
	}
	@SuppressWarnings("unchecked")
	public List<AssessmentResultSummary> getAllResultSummariesForAssessment(Assessment assessment) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(AssessmentResultSummary.class);
		cr.add(Restrictions.eq("id.assessment", assessment));
		return cr.list();
	}
}
