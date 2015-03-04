/*
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
 */

package pasta.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.DependsOn;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pasta.domain.PASTATime;
import pasta.domain.form.ReleaseForm;
import pasta.domain.template.Arena;
import pasta.domain.template.Assessment;
import pasta.domain.template.Competition;
import pasta.domain.template.HandMarkData;
import pasta.domain.template.HandMarking;
import pasta.domain.template.WeightedField;
import pasta.domain.template.UnitTest;
import pasta.domain.template.WeightedCompetition;
import pasta.domain.template.WeightedHandMarking;
import pasta.domain.template.WeightedUnitTest;
import pasta.domain.upload.NewHandMarking;
import pasta.util.PASTAUtil;
import pasta.util.ProjectProperties;

/**
 * Data Access Object for Assessments.
 * <p>
 * This class is responsible for all of the interaction between the data layer
 * (disk in this case) and the system for assessments. This includes writing the
 * assessment properties to disk and loading the assessment properties from disk
 * when the system starts. It also handles all of the changes to the objects and
 * holds them cached. There should only be one instance of this object running
 * in the system at any time.
 * 
 * @author Alex Radu
 * @version 2.0
 * @since 2012-11-13
 */

@Repository("assessmentDAO")
@DependsOn("projectProperties")
public class AssessmentDAO extends HibernateDaoSupport {

	// assessmentTemplates are cached
	@Deprecated Map<String, Assessment> allAssessments;
	@Deprecated Map<String, List<Assessment>> allAssessmentsByCategory;
	@Deprecated Map<String, UnitTest> allUnitTests;
	@Deprecated Map<String, HandMarking> allHandMarking;
	Map<String, Competition> allCompetitions;

	protected final Log logger = LogFactory.getLog(getClass());

	// Default required by hibernate
	@Autowired
	public void setMySession(SessionFactory sessionFactory) {
		setSessionFactory(sessionFactory);
	}
	
	public AssessmentDAO() {
	}
	
	/**
	 * Load all assessment modules from disk.
	 * <p>
	 * Load order:
	 * <ol>
	 * <li>Unit test</li>
	 * <li>Hand marking</li>
	 * <li>Competitions</li>
	 * <li>Assessments</li>
	 * </ol>
	 */
	public void init() {
		// load up all cached objects
		
		// load up unit tests
		allUnitTests = new TreeMap<String, UnitTest>();
		loadUnitTests();
		
		// load up hand marking
		allHandMarking = new TreeMap<String, HandMarking>();
		loadHandMarking();
		
		// load up competitions
		allCompetitions = new TreeMap<String, Competition>();
		loadCompetitions();
		
		// load up all assessments
		allAssessmentsByCategory = new TreeMap<String, List<Assessment>>();
		allAssessments = new TreeMap<String, Assessment>();
		loadAssessments();
	}

	public Map<String, List<Assessment>> getAllAssessmentsByCategory() {
		return allAssessmentsByCategory;
	}

	public Map<String, UnitTest> getAllUnitTests() {
		return allUnitTests;
	}

	@Deprecated
	public Assessment getAssessment(String name) {
		return allAssessments.get(name);
	}

	@Deprecated
	public HandMarking getHandMarking(String name) {
		return allHandMarking.get(name);
	}

	public Competition getCompetition(String name) {
		return allCompetitions.get(name);
	}

	public Collection<HandMarking> getHandMarkingList() {
//		return allHandMarking.values();
		return ProjectProperties.getInstance().getHandMarkingDAO().getAllHandMarkings();
	}

	public Collection<Assessment> getAssessmentList() {
		return allAssessments.values();
	}

	public Collection<Competition> getCompetitionList() {
		return allCompetitions.values();
	}

	@Deprecated
	public void addUnitTest(UnitTest newUnitTest) {
		allUnitTests.put(newUnitTest.getShortName(), newUnitTest);
	}

	/**
	 * Add a new assessment.
	 * <p>
	 * If the assessment already exists, update it, otherwise add a new
	 * assessment.
	 * 
	 * @param newAssessment assessment to add
	 */
	public void addAssessment(Assessment newAssessment) {
		// if already exists, update
		if (allAssessments.containsKey(newAssessment.getShortName())) {
			Assessment curAss = allAssessments.get(newAssessment.getShortName());
			// simple
			curAss.setDescription(newAssessment.getDescription());
			curAss.setDueDate(newAssessment.getDueDate());
			curAss.setMarks(newAssessment.getMarks());
			curAss.setNumSubmissionsAllowed(newAssessment.getNumSubmissionsAllowed());
			curAss.setReleasedClasses(newAssessment.getReleasedClasses());
			curAss.setCountUncompilable(newAssessment.isCountUncompilable());
			curAss.setSpecialRelease(newAssessment.getSpecialRelease());
			String oldCategory = "";
			String newCategory = "";
			if (curAss.getCategory() != null) {
				oldCategory = curAss.getCategory();
			}
			if (newAssessment.getCategory() != null) {
				newCategory = newAssessment.getCategory();
			}
			curAss.setCategory(newAssessment.getCategory());

			allAssessmentsByCategory.get(oldCategory).remove(curAss);
			if (!allAssessmentsByCategory.containsKey(newCategory)) {
				allAssessmentsByCategory.put(newCategory, new LinkedList<Assessment>());
			}
			allAssessmentsByCategory.get(newCategory).add(curAss);

			// tests
			curAss.setSecretUnitTests(newAssessment.getSecretUnitTests());
			curAss.setUnitTests(newAssessment.getUnitTests());
			curAss.setHandMarking(newAssessment.getHandMarking());

			// unlink competitions
			for (WeightedCompetition comp : curAss.getCompetitions()) {
				// if not in newAssessment.getCompetitions()
				boolean found = false;
				for (WeightedCompetition newComp : newAssessment.getCompetitions()) {
					if (comp.getCompetition() == newComp.getCompetition()) {
						found = true;
						break;
					}
				}
				if (!found) {
					// remove
					comp.getCompetition().removeAssessment(curAss);
				}
			}
			curAss.setCompetitions(newAssessment.getCompetitions());
			update(curAss);
		} else {
			allAssessments.put(newAssessment.getShortName(), newAssessment);
			String category = "";
			if (newAssessment.getCategory() != null) {
				category = newAssessment.getCategory();
			}

			if (!allAssessmentsByCategory.containsKey(category)) {
				allAssessmentsByCategory.put(category, new LinkedList<Assessment>());
			}
			allAssessmentsByCategory.get(category).add(newAssessment);
			save(newAssessment);
		}
	}

	/**
	 * Add/Update a competition
	 * <p>
	 * Updates the competition on both cache and disk.
	 * 
	 * @param comp the new state of the competition.
	 */
	public void addCompetition(Competition comp) {
		if (allCompetitions.containsKey(comp.getShortName())) {

			// update - flags
			allCompetitions.get(comp.getShortName()).setStudentCreatableArena(comp.isStudentCreatableArena());
			allCompetitions.get(comp.getShortName()).setStudentCreatableRepeatableArena(
					comp.isStudentCreatableRepeatableArena());
			allCompetitions.get(comp.getShortName()).setTested(comp.isTested());
			allCompetitions.get(comp.getShortName()).setTutorCreatableRepeatableArena(
					comp.isTutorCreatableRepeatableArena());

			// update - dates
			allCompetitions.get(comp.getShortName()).setFirstStartDateStr(comp.getFirstStartDateStr());
			allCompetitions.get(comp.getShortName()).setFrequency(comp.getFrequency());

			// arena based competition
			if (!allCompetitions.get(comp.getShortName()).isCalculated()) {
				allCompetitions.get(comp.getShortName()).getOfficialArena()
						.setFirstStartDate(comp.getFirstStartDate());
				allCompetitions.get(comp.getShortName()).getOfficialArena().setFrequency(comp.getFrequency());
			}
		} else {
			// add
			allCompetitions.put(comp.getShortName(), comp);
		}
		// write to disk
		try {

			// create space on the file system.
			(new File(comp.getFileLocation() + "/code/")).mkdirs();

			// generate competitionProperties
			PrintStream out = new PrintStream(comp.getFileLocation() + "/competitionProperties.xml");
			out.print(comp);
			out.close();

			// arenas
			if (!comp.isCalculated()) {
				// official arenas
				writeArenaToDisk(comp.getOfficialArena(), comp);

				// other arenas
				for (Arena arena : comp.getOutstandingArenas()) {
					writeArenaToDisk(arena, comp);
				}

				for (Arena arena : comp.getCompletedArenas()) {
					writeArenaToDisk(arena, comp);
				}

			}

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			(new File(comp.getFileLocation())).delete();
			logger.error("Competition " + comp.getName() + " could not be created successfully!"
					+ System.getProperty("line.separator") + sw.toString());
		}
	}

	/**
	 * Release assessment
	 * 
	 * @param assessmentName the name of the assessment
	 * @param released the form detailing the users / streams / classes to release
	 *          to
	 */
	@Deprecated
	public void releaseAssessment(String assessmentName, ReleaseForm released) {
		Assessment assessment = allAssessments.get(assessmentName);
		
		assessment.setReleasedClasses(released.getList());
		if (released.getSpecialRelease() != null
				&& !released.getSpecialRelease().isEmpty()) {
			assessment.setSpecialRelease(released.getSpecialRelease());
		}
		try {
			// save to file
			PrintWriter out = new PrintWriter(new File(ProjectProperties.getInstance().getProjectLocation()
					+ "/template/assessment/" + assessmentName + "/assessmentProperties.xml"));
			out.print(allAssessments.get(assessmentName).toString());
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Update in database
		update(assessment);
	}
	
	/**
	 * Release assessment 
	 * 
	 * @param assessmentName the name of the assessment
	 * @param released the form detailing the users / streams / classes to release to
	 */
	public void releaseAssessment(long assessmentId, ReleaseForm released) {
		Assessment assessment = getAssessment(assessmentId);
		
		assessment.setReleasedClasses(released.getList());
		if (released.getSpecialRelease() != null
				&& !released.getSpecialRelease().isEmpty()) {
			assessment.setSpecialRelease(released.getSpecialRelease());
		}
		
		// Update in database
		update(assessment);
	}

	/**
	 * Delete a unit test from the system.
	 * <p>
	 * Go through all assessments and remove test from them.
	 * 
	 * @param unitTestName unit test short name (no spaces) to remove.
	 */
	@Deprecated
	public void removeUnitTest(String unitTestName) {
		// go through all assessments and remove the unit test from them
		for (Assessment assessment : allAssessments.values()) {
			for (WeightedUnitTest test : assessment.getAllUnitTests()) {
				if (test.getTest().getShortName().equals(unitTestName)) {
					// got a little lazy, should fix
					assessment.removeSecretUnitTest(test);
					assessment.removeUnitTest(test);
				}
			}
		}
		allUnitTests.remove(unitTestName);
		try {
			FileUtils.deleteDirectory(new File(ProjectProperties.getInstance().getUnitTestsLocation()
					+ unitTestName));
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not delete the folder for unit test " + unitTestName + "\r\n" + sw.toString());
		}
	}
	
	/**
	 * Delete a unit test from the system.
	 * <p>
	 * Go through all assessments and remove test from them.
	 * 
	 * @param unitTestId unit test id to remove.
	 */
	public void removeUnitTest(long unitTestId) {
		// go through all assessments and remove the unit test from them
		for(Assessment assessment: getAllAssessments()){
			for(WeightedUnitTest test: assessment.getAllUnitTests()){
				if(test.getTest().getId() == unitTestId){
					// got a little lazy, should fix
					assessment.removeSecretUnitTest(test);
					assessment.removeUnitTest(test);
				}
			}
		}
	}

	/**
	 * Delete an assessment from the system.
	 * <p>
	 * Iterates over all competitions and removes itself from them. Competitions
	 * are the only assessment modules that contain a link to the assessments they
	 * are used in.
	 * 
	 * @param assessmentName the short name (no spaces) of the assessment
	 */
	@Deprecated
	public void removeAssessment(String assessmentName) {

		Assessment assessmentToRemove = allAssessments.get(assessmentName);
		if (assessmentToRemove == null) {
			return;
		}

		// remove links from competitions
		for (WeightedCompetition comp : assessmentToRemove.getCompetitions()) {
			comp.getCompetition().removeAssessment(assessmentToRemove);
		}

		allAssessmentsByCategory.get(allAssessments.get(assessmentName).getCategory()).remove(
				allAssessments.get(assessmentName));
		allAssessments.remove(assessmentName);
		try {
			FileUtils.deleteDirectory(new File(ProjectProperties.getInstance().getProjectLocation()
					+ "/template/assessment/" + assessmentName));
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not delete the folder for assessment " + assessmentName + "\r\n" + sw.toString());
		}
		
		delete(assessmentToRemove);
	}
	
	/**
	 * Delete an assessment from the system.
	 * <p>
	 * 
	 * Iterates over all competitions and removes itself from them.
	 * Competitions are the only assessment modules that contain
	 * a link to the assessments they are used in.
	 * 
	 * @param assessmentName the short name (no spaces) of the assessment 
	 */
	public void removeAssessment(long assessmentId) {
		Assessment assessmentToRemove = getAssessment(assessmentId);
		if(assessmentToRemove == null){
			return;
		}
		
		// remove links from competitions
		for(WeightedCompetition comp : assessmentToRemove.getCompetitions()){
			comp.getCompetition().removeAssessment(assessmentToRemove);
		}
		
		delete(assessmentToRemove);
	}

	/**
	 * Delete an competition from the system.
	 * <p>
	 * Iterates over all assessments and removes itself from them.
	 * 
	 * @param competitionName the short name (no spaces) of the competition
	 */
	public void removeCompetition(String competitionName) {
		// go through all assessments and remove the competition from them
		for (Assessment assessment : allAssessments.values()) {
			List<WeightedCompetition> comps = new LinkedList<WeightedCompetition>();
			comps.addAll(assessment.getCompetitions());
			for (WeightedCompetition comp : comps) {
				if (comp.getCompetition().getShortName().equals(competitionName)) {
					// got a little lazy, should fix
					assessment.removeCompetition(comp);
				}
			}
		}
		allCompetitions.remove(competitionName);
		try {
			FileUtils.deleteDirectory(new File(ProjectProperties.getInstance().getCompetitionsLocation()
					+ competitionName));
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not delete the folder for competition " + competitionName + "\r\n" + sw.toString());
		}
	}

	/**
	 * Delete a hand marking template from the system.
	 * <p>
	 * Iterates over all assessments and removes itself from them.
	 * 
	 * @param handMarkingName the short name (no spaces) of the hand marking
	 *          template
	 */
	@Deprecated
	public void removeHandMarking(String handMarkingName) {
		// remove from assessments
		for (Entry<String, Assessment> ass : allAssessments.entrySet()) {
			List<WeightedHandMarking> marking = ass.getValue().getHandMarking();
			for (WeightedHandMarking weighted : marking) {
				if (weighted.getHandMarkingName().equals(handMarkingName)) {
					ass.getValue().getHandMarking().remove(weighted);
					break;
				}
			}
		}

		// remove from set
		allHandMarking.remove(handMarkingName);

		try {
			FileUtils.deleteDirectory(new File(ProjectProperties.getInstance().getProjectLocation()
					+ "/template/handMarking/" + handMarkingName));
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger
					.error("Could not delete the folder for hand marking " + handMarkingName + "\r\n" + sw.toString());
		}
	}
	
	/**
	 * Delete a hand marking template from the system.
	 * <p>
	 * Iterates over all assessments and removes itself from them.
	 * 
	 * @param handMarkingId the id of the hand marking template 
	 */
	public void removeHandMarking(long handMarkingId) {
		// remove from assessments
		for(Assessment ass : getAllAssessments()) {
			List<WeightedHandMarking> marking = ass.getHandMarking();
			for (WeightedHandMarking template : marking) {
				if(template.getHandMarking().getId() == handMarkingId) {
					ass.removeHandMarking(template);
					break;
				}
			}
		}
	}

	/**
	 * Load all unit tests.
	 * <p>
	 * Calls {@link #getUnitTestFromDisk(String)} multiple times.
	 */
	@Deprecated
	private void loadUnitTests() {
		for(UnitTest test : ProjectProperties.getInstance().getUnitTestDAO().getAllUnitTests()) {
			allUnitTests.put(test.getName(), test);
		}
//		// get unit test location
//		String allTestLocation = ProjectProperties.getInstance()
//				.getProjectLocation() + "/template/unitTest";
//		String[] allUnitTestNames = (new File(allTestLocation)).list();
//		if (allUnitTestNames != null && allUnitTestNames.length > 0) {
//			Arrays.sort(allUnitTestNames);
//
//			// load properties
//			for (String name : allUnitTestNames) {
//				UnitTest test = getUnitTestFromDisk(allTestLocation + "/"
//						+ name);
//				if (test != null) {
//					allUnitTests.put(name, test);
//				}
//			}
//		}
	}

	/**
	 * Load all competitions.
	 * <p>
	 * Calls {@link #getCompetitionFromDisk(String)} multiple times.
	 */
	private void loadCompetitions() {
		// get unit test location
		String allCompetitionLocation = ProjectProperties.getInstance().getCompetitionsLocation();
		String[] allCompetitionNames = (new File(allCompetitionLocation)).list();
		if (allCompetitionNames != null && allCompetitionNames.length > 0) {
			Arrays.sort(allCompetitionNames);

			// load properties
			for (String name : allCompetitionNames) {
				Competition comp = getCompetitionFromDisk(allCompetitionLocation + "/" + name);
				if (comp != null) {
					allCompetitions.put(name, comp);
				}
			}
		}

	}

	/**
	 * Load all assessments
	 * <p>
	 * Calls {@link #getAssessmentFromDisk(String)} multiple times.
	 */
	@Deprecated
	private void loadAssessments() {
		for(Assessment assessment : getAllAssessments()) {
			allAssessments.put(assessment.getShortName(), assessment);
			String category = assessment.getCategory();
			if (category == null) {
				category = "";
			}
			if (!allAssessmentsByCategory.containsKey(category)) {
				allAssessmentsByCategory.put(category,
						new LinkedList<Assessment>());
			}
			allAssessmentsByCategory.get(category).add(assessment);
		}
		
//		
//		// get unit test location
//		String allTestLocation = ProjectProperties.getInstance()
//				.getProjectLocation() + "/template/assessment";
//		String[] allAssessmentNames = (new File(allTestLocation)).list();
//		if (allAssessmentNames != null && allAssessmentNames.length > 0) {
//			Arrays.sort(allAssessmentNames);
//
//			// load properties
//			for (String name : allAssessmentNames) {
//				Assessment assessment = getAssessmentFromDisk(allTestLocation
//						+ "/" + name);
//				if (assessment != null) {
//					allAssessments.put(name, assessment);
//					String category = assessment.getCategory();
//					if (category == null) {
//						category = "";
//					}
//					if (!allAssessmentsByCategory.containsKey(category)) {
//						allAssessmentsByCategory.put(category,
//								new LinkedList<Assessment>());
//					}
//					allAssessmentsByCategory.get(category).add(assessment);
//				}
//			}
//		}
	}

	/**
	 * Load all handmarkings templates
	 * <p>
	 * Calls {@link #getHandMarkingFromDisk(String)} multiple times.
	 */
	@Deprecated
	private void loadHandMarking() {
		for(HandMarking template : ProjectProperties.getInstance().getHandMarkingDAO().getAllHandMarkings()) {
			allHandMarking.put(template.getName(), template);
		}
//		
//		// get hand marking location
//		String allTestLocation = ProjectProperties.getInstance()
//				.getProjectLocation() + "/template/handMarking";
//		String[] allHandMarkingNames = (new File(allTestLocation)).list();
//		if (allHandMarkingNames != null && allHandMarkingNames.length > 0) {
//			Arrays.sort(allHandMarkingNames);
//
//			// load properties
//			for (String name : allHandMarkingNames) {
//				HandMarking test = getHandMarkingFromDisk(allTestLocation + "/"
//						+ name);
//				if (test != null) {
//					allHandMarking.put(test.getShortName(), test);
//				}
//			}
//		}
	}

	/**
	 * Method to get a unit test from a location.
	 * <p>
	 * Loads the unitTestProperties.xml from file into the cache.
	 * 
	 * @param location - the location of the unit test
	 * @return null - there is no unit test at that location to be retrieved
	 * @return test - the unit test at that location.
	 */
	private UnitTest getUnitTestFromDisk(String location) {
		try {

			File fXmlFile = new File(location + "/unitTestProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			String name = doc.getElementsByTagName("name").item(0).getChildNodes().item(0).getNodeValue();
			boolean tested = Boolean.parseBoolean(doc.getElementsByTagName("tested").item(0).getChildNodes()
					.item(0).getNodeValue());

			return new UnitTest(name, tested);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not read unit test " + location + System.getProperty("line.separator")
					+ sw.toString());
			return null;
		}
	}

	/**
	 * Method to get a competition from a location
	 * <p>
	 * Loads the competitionProperties.xml from file into the cache.
	 * 
	 * @param location - the location of the competition
	 * @return null - there is no competition at that location to be retrieved
	 * @return comp - the competition at that location.
	 */
	private Competition getCompetitionFromDisk(String location) {
		try {

			File fXmlFile = new File(location + "/competitionProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			Competition comp = new Competition();

			// name
			comp.setName(doc.getElementsByTagName("name").item(0).getChildNodes().item(0).getNodeValue());

			// tested
			comp.setTested(Boolean.parseBoolean(doc.getElementsByTagName("tested").item(0).getChildNodes().item(0)
					.getNodeValue()));

			// can students create an arena
			comp.setStudentCreatableArena(Boolean.parseBoolean(doc.getElementsByTagName("studentCreatableArena")
					.item(0).getChildNodes().item(0).getNodeValue()));

			// can students create repeatable arenas
			comp.setStudentCreatableRepeatableArena(Boolean.parseBoolean(doc
					.getElementsByTagName("studentCreatableRepeatableArena").item(0).getChildNodes().item(0)
					.getNodeValue()));

			// can tutors create repeatableArenas
			comp.setTutorCreatableRepeatableArena(Boolean.parseBoolean(doc
					.getElementsByTagName("tutorCreatableRepeatableArena").item(0).getChildNodes().item(0)
					.getNodeValue()));

			// is the competition hidden or not
			if (doc.getElementsByTagName("hidden") != null && doc.getElementsByTagName("hidden").getLength() != 0) {
				comp.setHidden(Boolean.parseBoolean(doc.getElementsByTagName("hidden").item(0).getChildNodes()
						.item(0).getNodeValue()));
			}

			// first start date - only for calculated comps
			if (doc.getElementsByTagName("firstStartDate") != null
					&& doc.getElementsByTagName("firstStartDate").getLength() != 0) {
				comp.setFirstStartDate(PASTAUtil.parseDate(doc.getElementsByTagName("firstStartDate").item(0)
						.getChildNodes().item(0).getNodeValue()));
			}

			// frequency - only for calculated comps
			if (doc.getElementsByTagName("frequency") != null
					&& doc.getElementsByTagName("frequency").getLength() != 0) {
				comp.setFrequency(new PASTATime(doc.getElementsByTagName("frequency").item(0).getChildNodes().item(0)
						.getNodeValue()));
			}

			// arenas
			// official

			String[] arenaList = new File(location + "/arenas/").list();

			if (arenaList != null) {

				LinkedList<Arena> completedArenas = new LinkedList<Arena>();
				LinkedList<Arena> outstandingArenas = new LinkedList<Arena>();

				for (String arenaName : arenaList) {
					Arena arena = getArenaFromDisk(location + "/arenas/" + arenaName);
					if (arena != null) {
						if (arena.getName().replace(" ", "").toLowerCase().equals("officialarena")) {
							comp.setOfficialArena(arena);
						} else if (new File(location + "/arenas/" + arenaName + "/results.csv").exists()
								&& !arena.isRepeatable()) {
							completedArenas.add(arena);
						} else {
							outstandingArenas.add(arena);
						}
					}
				}

				comp.setCompletedArenas(completedArenas);
				comp.setOutstandingArenas(outstandingArenas);
			} else {
				Map<String, Arena> nothing = null;
				comp.setOutstandingArenas(nothing);
			}

			return comp;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not read competition " + location + System.getProperty("line.separator")
					+ sw.toString());
			return null;
		}
	}

	/**
	 * Method to get an arena from a location
	 * <p>
	 * Loads the arenaProperties.xml from file into the cache.
	 * 
	 * @param location - the location of the arena
	 * @return null - there is no arena at that location to be retrieved
	 * @return arena - the arena at that location.
	 */
	private Arena getArenaFromDisk(String location) {

		try {
			File fXmlFile = new File(location + "/arenaProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			Arena arena = new Arena();

			// name
			arena.setName(doc.getElementsByTagName("name").item(0).getChildNodes().item(0).getNodeValue());

			// first start date
			if (doc.getElementsByTagName("firstStartDate") != null
					&& doc.getElementsByTagName("firstStartDate").getLength() != 0) {
				arena.setFirstStartDate(PASTAUtil.parseDate(doc.getElementsByTagName("firstStartDate").item(0)
						.getChildNodes().item(0).getNodeValue()));
			}

			// frequency
			if (doc.getElementsByTagName("repeats") != null && doc.getElementsByTagName("repeats").getLength() != 0) {
				arena.setFrequency(new PASTATime(doc.getElementsByTagName("repeats").item(0).getChildNodes().item(0)
						.getNodeValue()));
			}

			// password
			if (doc.getElementsByTagName("password") != null
					&& doc.getElementsByTagName("password").getLength() != 0) {
				arena
						.setPassword(doc.getElementsByTagName("password").item(0).getChildNodes().item(0).getNodeValue());
			}

			// players
			String[] players = new File(location + "/players/").list();

			if (players != null) {
				for (String player : players) {
					Scanner in = new Scanner(new File(location + "/players/" + player));

					String username = player.split("\\.")[0];
					while (in.hasNextLine()) {
						arena.addPlayer(username, in.nextLine());
					}

					in.close();
				}
			}

			return arena;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not read arena " + location + System.getProperty("line.separator") + sw.toString());
			return null;
		}
	}

	/**
	 * Method to get an assessment from a location
	 * <p>
	 * Loads the assessmentProperties.xml from file into the cache.
	 * 
	 * @param location - the location of the assessment
	 * @return null - there is no assessment at that location to be retrieved
	 * @return assessment - the assessment at that location.
	 */
	private Assessment getAssessmentFromDisk(String location) {
		try {

			File fXmlFile = new File(location + "/assessmentProperties.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			Assessment currentAssessment = new Assessment();

			currentAssessment.setName(doc.getElementsByTagName("name").item(0).getChildNodes().item(0)
					.getNodeValue());
			currentAssessment.setMarks(Double.parseDouble(doc.getElementsByTagName("marks").item(0).getChildNodes()
					.item(0).getNodeValue()));
			try {
				currentAssessment.setReleasedClasses(doc.getElementsByTagName("releasedClasses").item(0)
						.getChildNodes().item(0).getNodeValue());
			} catch (Exception e) {
				// not released
			}

			try {
				currentAssessment.setCategory(doc.getElementsByTagName("category").item(0).getChildNodes().item(0)
						.getNodeValue());
			} catch (Exception e) {
				// no category
			}

			try {
				currentAssessment.setCountUncompilable(Boolean.parseBoolean(doc
						.getElementsByTagName("countUncompilable").item(0).getChildNodes().item(0).getNodeValue()));
			} catch (Exception e) {
				// no countUncompilable tag - defaults to true
			}

			try {
				currentAssessment.setSpecialRelease(doc.getElementsByTagName("specialRelease").item(0)
						.getChildNodes().item(0).getNodeValue());
			} catch (Exception e) {
				// not special released
			}
			currentAssessment.setNumSubmissionsAllowed(Integer.parseInt(doc
					.getElementsByTagName("submissionsAllowed").item(0).getChildNodes().item(0).getNodeValue()));

			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
			currentAssessment.setDueDate(sdf.parse(doc.getElementsByTagName("dueDate").item(0).getChildNodes()
					.item(0).getNodeValue()));

			// load description from file
			String description = "";
			try {
				Scanner in = new Scanner(new File(location + "/description.html"));
				while (in.hasNextLine()) {
					description += in.nextLine() + System.getProperty("line.separator");
				}
				in.close();
			} catch (Exception e) {
				description = "<pre>Error loading description" + System.getProperty("line.separator") + e + "</pre>";
			}
			currentAssessment.setDescription(description);

			// add unit tests
			NodeList unitTestList = doc.getElementsByTagName("unitTest");
			if (unitTestList != null && unitTestList.getLength() > 0) {
				for (int i = 0; i < unitTestList.getLength(); i++) {
					Node unitTestNode = unitTestList.item(i);
					if (unitTestNode.getNodeType() == Node.ELEMENT_NODE) {
						Element unitTestElement = (Element) unitTestNode;

						WeightedUnitTest weightedTest = new WeightedUnitTest();
						weightedTest.setTest(allUnitTests.get(unitTestElement.getAttribute("name")));
						weightedTest.setWeight(Double.parseDouble(unitTestElement.getAttribute("weight")));
						if (unitTestElement.getAttribute("secret") != null
								&& Boolean.parseBoolean(unitTestElement.getAttribute("secret"))) {
							currentAssessment.addSecretUnitTest(weightedTest);
						} else {
							currentAssessment.addUnitTest(weightedTest);
						}
					}
				}
			}

			// add hand marking
			NodeList handMarkingList = doc.getElementsByTagName("handMarks");
			if (handMarkingList != null && handMarkingList.getLength() > 0) {
				for (int i = 0; i < handMarkingList.getLength(); i++) {
					Node handMarkingNode = handMarkingList.item(i);
					if (handMarkingNode.getNodeType() == Node.ELEMENT_NODE) {
						Element handMarkingElement = (Element) handMarkingNode;

						WeightedHandMarking weightedHandMarking = new WeightedHandMarking();
						weightedHandMarking.setHandMarking(allHandMarking.get(handMarkingElement.getAttribute("name")));
						weightedHandMarking.setWeight(Double.parseDouble(handMarkingElement.getAttribute("weight")));
						currentAssessment.addHandMarking(weightedHandMarking);
					}
				}
			}

			// add competitions
			NodeList competitionList = doc.getElementsByTagName("competition");
			if (competitionList != null && competitionList.getLength() > 0) {
				for (int i = 0; i < competitionList.getLength(); i++) {
					Node competitionNode = competitionList.item(i);
					if (competitionNode.getNodeType() == Node.ELEMENT_NODE) {
						Element competitionElement = (Element) competitionNode;

						WeightedCompetition weightedComp = new WeightedCompetition();
						weightedComp.setCompetition(allCompetitions.get(competitionElement.getAttribute("name")));
						weightedComp.setWeight(Double.parseDouble(competitionElement.getAttribute("weight")));
						weightedComp.getCompetition().addAssessment(currentAssessment);
						currentAssessment.addCompetition(weightedComp);
					}
				}
			}

			return currentAssessment;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not read assessment " + location + System.getProperty("line.separator")
					+ sw.toString());
			return null;
		}
	}
//
//	/**
//	 * Method to get a handmarking from a location
//	 * <p>
//	 * Loads the handMarkingProperties.xml from file into the cache. 
//	 * Also loads the multiple .html files which are the descriptions
//	 * in each box of the hand marking template.
//	 * 
//	 * @param location
//	 *            - the location of the handmarking
//	 * @return null - there is no handmarking at that location to be retrieved
//	 * @return test - the handmarking at that location.
//	 */
//	private HandMarking getHandMarkingFromDisk(String location) {
//		try {
//
//			File fXmlFile = new File(location + "/handMarkingProperties.xml");
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
//					.newInstance();
//			DocumentBuilder dBuilder;
//			dBuilder = dbFactory.newDocumentBuilder();
//			Document doc = dBuilder.parse(fXmlFile);
//			doc.getDocumentElement().normalize();
//
//			HandMarking markingTemplate = new HandMarking();
//
//			// load name
//			markingTemplate.setName(doc.getElementsByTagName("name").item(0)
//					.getChildNodes().item(0).getNodeValue());
//
//			// load column list
//			NodeList columnList = doc.getElementsByTagName("column");
//			List<WeightedField> columnHeaderList = new ArrayList<WeightedField>();
//			if (columnList != null && columnList.getLength() > 0) {
//				for (int i = 0; i < columnList.getLength(); i++) {
//					Node columnNode = columnList.item(i);
//					if (columnNode.getNodeType() == Node.ELEMENT_NODE) {
//						Element columnElement = (Element) columnNode;
//
//						WeightedField field = new WeightedField();
//						field.setName(columnElement.getAttribute("name"));
//						field.setWeight(Double.parseDouble(columnElement
//								.getAttribute("weight")));
//
//						columnHeaderList.add(field);
//					}
//				}
//			}
//			markingTemplate.setColumnHeader(columnHeaderList);
//
//			// load row list
//			NodeList rowList = doc.getElementsByTagName("row");
//			List<WeightedField> rowHeaderList = new ArrayList<WeightedField>();
//			if (rowList != null && rowList.getLength() > 0) {
//				for (int i = 0; i < rowList.getLength(); i++) {
//					Node rowNode = rowList.item(i);
//					if (rowNode.getNodeType() == Node.ELEMENT_NODE) {
//						Element rowElement = (Element) rowNode;
//
//						WeightedField field = new WeightedField();
//						field.setName(rowElement.getAttribute("name"));
//						field.setWeight(Double.parseDouble(rowElement
//								.getAttribute("weight")));
//
//						rowHeaderList.add(field);
//					}
//				}
//			}
//			markingTemplate.setRowHeader(rowHeaderList);
//
//			// load data
//			Map<Long, Map<Long, String>> descriptionMap = new TreeMap<Long, Map<Long, String>>();
//			for (WeightedField column : markingTemplate.getColumnHeader()) {
//				Map<Long, String> currDescriptionMap = new TreeMap<Long, String>();
//				for (WeightedField row : markingTemplate.getRowHeader()) {
//					try {
//						Scanner in = new Scanner(new File(location + "/"
//								+ column.getName().replace(" ", "") + "-"
//								+ row.getName().replace(" ", "") + ".txt"));
//						String description = "";
//						while (in.hasNextLine()) {
//							description += in.nextLine()
//									+ System.getProperty("line.separator");
//						}
//						currDescriptionMap.put(row.getId(), description);
//						in.close();
//					} catch (Exception e) {
//						// do nothing
//					}
//				}
//				descriptionMap.put(column.getId(), currDescriptionMap);
//			}
//
//			markingTemplate.setData(descriptionMap);
//
//			return markingTemplate;
//		} catch (Exception e) {
//			StringWriter sw = new StringWriter();
//			PrintWriter pw = new PrintWriter(sw);
//			e.printStackTrace(pw);
//			logger.error("Could not read hand marking " + location
//					+ System.getProperty("line.separator") + sw.toString());
//			return null;
//		}
//	}

	/**
	 * Update a hand marking template
	 * <p>
	 * Updates cache and writes everything to file. Pretty much used all the time,
	 * because when you make a new hand marking template, you generate one based
	 * on defaults.
	 * 
	 * @param newHandMarking the new hand marking template
	 */
	public void updateHandMarking(HandMarking newHandMarking) {
	
		ProjectProperties.getInstance().getHandMarkingDAO().saveOrUpdate(newHandMarking);
		
//		if (allHandMarking.containsKey(newHandMarking.getShortName())) {
//			HandMarking currMarking = allHandMarking.get(newHandMarking
//					.getShortName());
//
//			currMarking.setColumnHeader(newHandMarking.getColumnHeader());
//			currMarking.setData(newHandMarking.getData());
//			currMarking.setRowHeader(newHandMarking.getRowHeader());
//		} else {
//			allHandMarking.put(newHandMarking.getShortName(), newHandMarking);
//		}
//		// save to drive
//
//		String location = ProjectProperties.getInstance().getProjectLocation()
//				+ "/template/handMarking/" + newHandMarking.getShortName();
//
//		try {
//			FileUtils.deleteDirectory(new File(location));
//		} catch (IOException e) {
//			// Don't care if it doesn't exist
//		}
//
//		// make the folder
//		(new File(location)).mkdirs();
//
//		try {
//			PrintWriter handMarkingProperties = new PrintWriter(new File(
//					location + "/handMarkingProperties.xml"));
//			handMarkingProperties.println("<handMarkingProperties>");
//			// name
//			handMarkingProperties.println("\t<name>" + newHandMarking.getName()
//					+ "</name>");
//			// columns
//			if (!newHandMarking.getColumnHeader().isEmpty()) {
//				handMarkingProperties.println("\t<columns>");
//				for (WeightedField column : newHandMarking.getColumnHeader()) {
//					handMarkingProperties.println("\t\t<column name=\""
//							+ column.getName() + "\" weight=\""
//							+ column.getWeight() + "\"/>");
//				}
//				handMarkingProperties.println("\t</columns>");
//			}
//
//			// rows
//			if (!newHandMarking.getRowHeader().isEmpty()) {
//				handMarkingProperties.println("\t<rows>");
//				for (WeightedField row : newHandMarking.getRowHeader()) {
//					handMarkingProperties.println("\t\t<row name=\""
//							+ row.getName() + "\" weight=\"" + row.getWeight()
//							+ "\"/>");
//				}
//				handMarkingProperties.println("\t</rows>");
//			}
//			handMarkingProperties.println("</handMarkingProperties>");
//			handMarkingProperties.close();
//
//			for (Entry<Long, Map<Long, String>> entry1 : newHandMarking
//					.getData().entrySet()) {
//				for (Entry<Long, String> entry2 : entry1.getValue()
//						.entrySet()) {
//					PrintWriter dataOut = new PrintWriter(new File(location
//							+ "/" + entry1.getKey() + "-"
//							+ entry2.getKey() + ".txt"));
//
//					dataOut.println(entry2.getValue());
//					dataOut.close();
//				}
//			}
//		} catch (FileNotFoundException e) {
//			StringWriter sw = new StringWriter();
//			PrintWriter pw = new PrintWriter(sw);
//			e.printStackTrace(pw);
//			logger.error("Could not update hand marking " + location
//					+ System.getProperty("line.separator") + sw.toString());
//		}
	}

	/**
	 * Creates a default hand marking template. Default columns are:
	 * <ul>
	 * <li>Poor : 0%</li>
	 * <li>Acceptable : 50%</li>
	 * <li>Excellent : 100%</li>
	 * </ul>
	 * Default rows are:
	 * <ul>
	 * <li>Formatting : 20%</li>
	 * <li>Code Reuse : 40%</li>
	 * <li>Variable naming : 40%</li>
	 * </ul>
	 * Default descriptions are empty.
	 * 
	 * @param newHandMarking the new hand marking
	 */
	public void newHandMarking(NewHandMarking newHandMarking) {

		HandMarking newMarking = new HandMarking();
		newMarking.setName(newHandMarking.getName());

		newMarking.addColumn(new WeightedField("Poor", 0));
		newMarking.addColumn(new WeightedField("Acceptable", 0.5));
		newMarking.addColumn(new WeightedField("Excellent", 1));

		newMarking.addRow(new WeightedField("Formatting", 0.2));
		newMarking.addRow(new WeightedField("Code Reuse", 0.4));
		newMarking.addRow(new WeightedField("Variable naming", 0.4));

		for (WeightedField column : newMarking.getColumnHeader()) {
			for (WeightedField row : newMarking.getRowHeader()) {
				newMarking.addData(new HandMarkData(column, row, ""));
			}
		}

		updateHandMarking(newMarking);
	}

	/**
	 * Write an arena to disk
	 * 
	 * @param arena the arena getting written to disk
	 * @param comp the comp the arena belongs to
	 */
	public void writeArenaToDisk(Arena arena, Competition comp) {
		if (arena != null && comp != null) {
			// ensure folder exists for plaers
			(new File(comp.getFileLocation() + "/arenas/" + arena.getName() + "/players/")).mkdirs();

			// write arena properties
			try {

				new File(comp.getFileLocation() + "/arenas/" + arena.getName()).mkdirs();

				PrintStream arenaOut = new PrintStream(comp.getFileLocation() + "/arenas/" + arena.getName()
						+ "/arenaProperties.xml");
				arenaOut.print(arena);
				arenaOut.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			// write players
			for (Entry<String, Set<String>> entry : arena.getPlayers().entrySet()) {
				updatePlayerInArena(comp, arena, entry.getKey(), entry.getValue());
			}

		}
	}

	/**
	 * Update the players taking part in an arena
	 * <p>
	 * Updates in both cache and disk. If the set players is empty, all players
	 * for this user are removed. The players are stored as $username$.players
	 * files where the name of each player is on a separate line within
	 * $compLocation$/arenas/$arenaName/players/ This was done to ensure there
	 * were limited concurrency problems. This could obviously be moved onto the
	 * database but I didn't want to at the time because I wanted to limit the
	 * reliance on a database.
	 * 
	 * @param comp the competition
	 * @param arena the arena
	 * @param username the name of the user updating which of their players are
	 *          participating in the arena.
	 * @param players the set of players, if empty, all players will be removed.
	 */
	public void updatePlayerInArena(Competition comp, Arena arena, String username, Set<String> players) {
		if (comp != null && arena != null && username != null && !username.isEmpty() && players != null
				&& players.isEmpty()) {
			// if the players set is empty, delete them from the arena.
			new File(comp.getFileLocation() + "/arenas/" + arena.getName() + "/players/" + username + ".players")
					.delete();
		} else if (comp != null && arena != null && username != null && !username.isEmpty() && players != null
				&& !players.isEmpty()) {
			try {
				(new File(comp.getFileLocation() + "/arenas/" + arena.getName() + "/players/")).mkdirs();

				PrintStream out = new PrintStream(comp.getFileLocation() + "/arenas/" + arena.getName() + "/players/"
						+ username + ".players");

				for (String player : players) {
					out.println(player);
				}

				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void save(Assessment assessment) {
		try {
			getHibernateTemplate().saveOrUpdate(assessment);
			logger.info("Saved assessment " + assessment.getName());
		} catch (Exception e) {
			logger.error("Could not save assessment " + assessment.getName(), e);
		}
	}
	
	public void update(Assessment assessment) {
		try {
			getHibernateTemplate().saveOrUpdate(assessment);
			logger.info("Updated assessment " + assessment.getName());
		} catch (Exception e) {
			logger.error("Could not update assessment " + assessment.getName(), e);
		}
	}
	
	public void delete(Assessment assessment) {
		try {
			getHibernateTemplate().delete(assessment);
			logger.info("Deleted assessment " + assessment.getName());
		} catch (Exception e) {
			logger.error("Could not delete assessment " + assessment.getName(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Assessment> getAllAssessments() {
		return getHibernateTemplate().find("FROM Assessment");
	}
	
	public Assessment getAssessment(long id) {
		return getHibernateTemplate().get(Assessment.class, id);
	}
}
