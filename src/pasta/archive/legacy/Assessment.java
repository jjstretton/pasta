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

package pasta.archive.legacy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Container class for the assessment.
 * <p>
 * Contains zero to many:
 * <ul>
 * 	<li>unit test assessment modules</li>
 * 	<li>secret unit test assessment modules</li>
 * 	<li>hand marking assessment modules</li>
 * </ul>
 * 
 * Also contains assessment specific information such as:
 * <ul>
 * 	<li>name</li>
 * 	<li>number of marks the assessment is worth</li>
 * 	<li>due date of the assessment</li>
 * 	<li>description (raw html) of the assessment</li>
 * 	<li>number of submissions allowed (0 for infinite submissions allowed)</li>
 * 	<li>category</li>
 * 	<li>list of usernames to whom the assessment has been specially released</li>
 * 	<li>list of classes to whom the assessment has been released (csv of STREAM.CLASS)</li>
 * 	<li>a flag to count submissions that compile towards the limit or not</li>
 * </ul>
 * 
 * String representation: 
 * 
 * <pre>{@code <assessment>
	<name>name</name>
	<category>category</category>
	<releasedClasses>STREAM1.CLASS1,...,STREAMn.CLASSn</releasedClasses>
	<specialRelease>usernames</specialRelease>
	<dueDate>hh:mm dd/MM/yyyy</dueDate>
	<marks>double</marks>
	<submissionsAllowed>int >= 0</submissionsAllowed>
	<countUncompilable>true|false</countUncompilable>
	<unitTestSuite>
		<unitTest name="name" weight="double"/>
		...
		<unitTest name="name" weight="double" [secret="true|false"]/>
	</unitTestSuite>
	<handMarkingSuite>
		<handMarks name="name" weight="double"/>
		...
		<handMarks name="name" weight="double"/>
	</handMarkingSuite>
</assessment>}</pre>
 * 
 * All weighting is relative. If two assessment modules are weighted as 
 * 1, then they are worth 50% of the marks each.
 * 
 * <p>
 * File location on disk: $projectLocation$/template/assessment/$assessmentName$
 * 
 * @author Alex Radu
 * @version 2.0
 * @since 2012-11-13
 *
 */
public class Assessment implements Comparable<Assessment>{
	/*
	 * The assessment modules have to be in a lazy list for the drag and drop
	 * Functionality on the web front end. Without this, there would be errors
	 * when adding assessment modules.
	 */
	private List<WeightedUnitTest> unitTests = new ArrayList<WeightedUnitTest>();
	private List<WeightedUnitTest> secretUnitTests = new ArrayList<WeightedUnitTest>();
	private List<WeightedHandMarking> handMarking = new ArrayList<WeightedHandMarking>();
	private String name;
	private double marks;
	private Date dueDate = new Date();
	private String description;
	private int numSubmissionsAllowed;
	private String category;
	private String specialRelease;
	private String releasedClasses = null;
	private boolean countUncompilable = true;

	protected final Log logger = LogFactory.getLog(getClass());
	
	public String getSpecialRelease() {
		return specialRelease;
	}

	public void setSpecialRelease(String specialRelease) {
		this.specialRelease = specialRelease;
	}

	public String getCategory() {
		if(category == null){
			return "";
		}
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getReleasedClasses() {
		return releasedClasses;
	}

	public void addUnitTest(WeightedUnitTest test) {
		unitTests.add(test);
	}

	public void removeUnitTest(WeightedUnitTest test) {
		unitTests.remove(test);
	}

	public boolean isReleased() {
		return (releasedClasses != null && !releasedClasses.isEmpty()) || 
				(specialRelease != null && !specialRelease.isEmpty());
	}
	
	public void setReleasedClasses(String released) {
		
			this.releasedClasses = released;
	}
	public void addSecretUnitTest(WeightedUnitTest test) {
		secretUnitTests.add(test);
	}

	public void removeSecretUnitTest(WeightedUnitTest test) {
		secretUnitTests.remove(test);
	}
	
	public void addHandMarking(WeightedHandMarking test) {
		handMarking.add(test);
	}

	public void removeHandMarking(WeightedHandMarking test) {
		handMarking.remove(test);
	}

	public double getMarks() {
		return marks;
	}

	public List<WeightedUnitTest> getUnitTests() {
		return unitTests;
	}
	
	public List<WeightedUnitTest> getAllUnitTests() {
		List<WeightedUnitTest> allUnitTests = new LinkedList<WeightedUnitTest>();
		allUnitTests.addAll(unitTests);
		allUnitTests.addAll(secretUnitTests);
		return allUnitTests;
	}

	public void setUnitTests(List<WeightedUnitTest> unitTests) {
		this.unitTests.clear();
		this.unitTests.addAll(unitTests);
	}

	public List<WeightedUnitTest> getSecretUnitTests() {
		return secretUnitTests;
	}

	public void setSecretUnitTests(List<WeightedUnitTest> secretUnitTests) {
		this.secretUnitTests.clear();
		this.secretUnitTests.addAll(secretUnitTests);
	}
	
	public List<WeightedHandMarking> getHandMarking() {
		return handMarking;
	}

	public void setHandMarking(List<WeightedHandMarking> handMarking) {
		this.handMarking.clear();
		this.handMarking.addAll(handMarking);
	}
	
	public String getName() {
		return name;
	}

	public String getShortName() {
		return name.replace(" ", "");
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMarks(double marks) {
		this.marks = marks;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public String getSimpleDueDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		return sdf.format(dueDate);
	}

	public void setSimpleDueDate(String date) {
		logger.info(date);
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			dueDate = sdf.parse(date.trim());
		} catch (ParseException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.error("Could not parse date " + sw.toString());
		}
		logger.info(dueDate);
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public int getNumSubmissionsAllowed() {
		return numSubmissionsAllowed;
	}

	public void setNumSubmissionsAllowed(int numSubmissionsAllowed) {
		this.numSubmissionsAllowed = numSubmissionsAllowed;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isCompletelyTested() {
		for (WeightedUnitTest test : unitTests) {
			if (!test.getTest().isTested()) {
				return false;
			}
		}

		for (WeightedUnitTest test : secretUnitTests) {
			if (!test.getTest().isTested()) {
				return false;
			}
		}
		return true;
	}

	public boolean isClosed() {
		return (new Date()).after(getDueDate());
	}  
	
	/**
	 * See string representation in class description.
	 */
	@Override
	public String toString() {
		String output = "";
		output += "<assessment>" + System.getProperty("line.separator");
		output += "\t<name>" + getName() + "</name>" + System.getProperty("line.separator");
		output += "\t<category>" + getCategory() + "</category>" + System.getProperty("line.separator");
		if(getReleasedClasses() != null){
			output += "\t<releasedClasses>" + getReleasedClasses() + "</releasedClasses>" + System.getProperty("line.separator");
		}
		if(getSpecialRelease() != null){
			output += "\t<specialRelease>" + getSpecialRelease() + "</specialRelease>" + System.getProperty("line.separator");
		}
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
		output += "\t<dueDate>" + sdf.format(getDueDate()) + "</dueDate>" + System.getProperty("line.separator");
		output += "\t<marks>" + getMarks() + "</marks>" + System.getProperty("line.separator");
		output += "\t<submissionsAllowed>" + getNumSubmissionsAllowed() + "</submissionsAllowed>"
				+ System.getProperty("line.separator");
		output += "\t<countUncompilable>" + isCountUncompilable() + "</countUncompilable>" + System.getProperty("line.separator");
		if (unitTests.size() + secretUnitTests.size() > 0) {
			output += "\t<unitTestSuite>" + System.getProperty("line.separator");
			for (WeightedUnitTest unitTest : unitTests) {
				output += "\t\t<unitTest name=\"" + unitTest.getTest().getShortName() + "\" weight=\""
						+ unitTest.getWeight() + "\"/>" + System.getProperty("line.separator");
			}

			for (WeightedUnitTest unitTest : secretUnitTests) {
				output += "\t\t<unitTest name=\"" + unitTest.getTest().getShortName() + "\" weight=\""
						+ unitTest.getWeight() + "\" secret=\"true\" />" + System.getProperty("line.separator");
			}
			output += "\t</unitTestSuite>" + System.getProperty("line.separator");
		}
		// handMarks
		if (handMarking.size() > 0) {
			output += "\t<handMarkingSuite>" + System.getProperty("line.separator");
			for (WeightedHandMarking handMarks : handMarking) {
				output += "\t\t<handMarks name=\"" + handMarks.getHandMarking().getShortName() + "\" weight=\""
						+ handMarks.getWeight() + "\"/>" + System.getProperty("line.separator");
			}
			output += "\t</handMarkingSuite>" + System.getProperty("line.separator");
		}
		output += "</assessment>" + System.getProperty("line.separator");
		return output;
	}
	
	public double getWeighting(UnitTest test){
		for(WeightedUnitTest myTest: unitTests){
			if(test == myTest.getTest()){
				return myTest.getWeight();
			}
		}
		for(WeightedUnitTest myTest: secretUnitTests){
			if(test == myTest.getTest()){
				return myTest.getWeight();
			}
		}
		return 0;
	}
	
	public double getWeighting(HandMarking test){
		for(WeightedHandMarking myTest: handMarking){
			if(test == myTest.getHandMarking()){
				return myTest.getWeight();
			}
		}
		return 0;
	}

	@Override
	public int compareTo(Assessment o) {
		return getName().compareTo(o.getName());
	}

	public boolean isCountUncompilable() {
		return countUncompilable;
	}

	public void setCountUncompilable(boolean countUncompilable) {
		this.countUncompilable = countUncompilable;
	}
}