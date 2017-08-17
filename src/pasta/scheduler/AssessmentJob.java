/*
Copyright (c) 2015, Joshua Stretton
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

package pasta.scheduler;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import pasta.docker.Language;
import pasta.docker.LanguageManager;
import pasta.domain.result.AssessmentResult;
import pasta.domain.template.Assessment;
import pasta.domain.user.PASTAUser;
import pasta.util.PASTAUtil;
import pasta.util.ProjectProperties;

/**
 * Class that holds the details of jobs.
 * <p>
 * This is a the holding object that deals with the 'job' system
 * used in PASTA to ensure only one assessment is executed at a time.
 * 
 * Database schema is automatically created based on this class.
 * 
 * Database schema:
 * <pre>
 * 	Integer ID, 
 *  Text username (not null),
 *  Text assessmentName (not null),
 *  Data runDate (not null)
 * </pre>
 * 
 * For competitions, the username will be PASTACompetitionRunner.
 * For calculated competition, the assessmentName will be the name of the competition.
 * For arena based competition, the assessmentName will be competitionName#PASTAArena#arenaName(
 * e.g. BattleShipLeague#PASTAArena#OfficialArena).
 * 
 * @author Alex Radu
 * @version 2.0
 * @since 2012-12-04
 * 
 */
@Entity
@Table(name = "assessment_jobs",
		uniqueConstraints = { @UniqueConstraint(name="unique_job_per_user_per_assessment", columnNames={
				"user_id", "assessment_id", "run_date"
		})})
public class AssessmentJob extends Job implements Serializable{

	private static final long serialVersionUID = 2058301754166837748L;
	
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private PASTAUser user;
	
	@Column(name = "assessment_id", nullable = false)
	private long assessmentId;
	
	@ManyToOne
	@JoinColumn (name = "assessment_result_id")
	private AssessmentResult results;
	
	private boolean running;
	
	@Transient
	private Language language;
	
	public AssessmentJob(){}
	
	public AssessmentJob(PASTAUser user, long assessmentId, Date runDate, AssessmentResult result){
		super(runDate);
		this.user = user;
		this.assessmentId = assessmentId;
		this.results = result;
		this.running = false;
	}

	public PASTAUser getUser() {
		return user;
	}
	public void setUser(PASTAUser user) {
		this.user = user;
	}

	public long getAssessmentId() {
		return assessmentId;
	}
	public void setAssessmentId(long assessmentId) {
		this.assessmentId = assessmentId;
	}

	public AssessmentResult getResults() {
		return results;
	}
	public void setResults(AssessmentResult results) {
		this.results = results;
	}
	
	public boolean isRunning() {
		return running;
	}
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	public Language getLanguage() {
		if(language != null) {
			return language;
		}
		Assessment assessment = results.getAssessment();
		language = LanguageManager.getInstance().guessLanguage(assessment.getSolutionName(), "", getSubmissionRoot());
		return language;
	}
	
	public File getSubmissionRoot() {
		String submissionHome = ProjectProperties.getInstance().getSubmissionsLocation() + user.getUsername() + "/assessments/"
				+ getAssessmentId() + "/" + PASTAUtil.formatDate(getRunDate()) + "/submission";
		return new File(submissionHome);
	}

	@Override
	public String toString() {
		return "Assessment " + assessmentId + 
				" job for " + user.getUsername() + 
				" submitted at " + results.getSubmissionDate() + 
				" by " + results.getSubmittedBy().getUsername() +
				(running ? " (running)" : ""); 
	}
}
