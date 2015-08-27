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

package pasta.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import pasta.domain.ratings.AssessmentRating;
import pasta.domain.ratings.RatingForm;
import pasta.domain.template.Assessment;
import pasta.domain.user.PASTAUser;
import pasta.repository.RatingDAO;

/**
 * Rating manager.
 * <p>
 * Manages interaction between controller and data.
 * This class works as an abstraction layer between the controller 
 * and the underlying data models. This class contains the majority
 * of the logic code dealing with objects and their interactions.
 *
 * @author Joshua Stretton
 * @version 1.0
 * @since 2015-04-13
 *
 */
@Service("ratingManager")
@Repository
public class RatingManager {
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private RatingDAO ratingDAO;

	public static final Logger logger = Logger
			.getLogger(RatingManager.class);
	
	public AssessmentRating getRating(Assessment assessment, PASTAUser user) {
		return ratingDAO.getRating(assessment, user);
	}
	
	@Async
	public Future<AssessmentRating> loadRating(Assessment assessment, PASTAUser user) {
		// This method is for test purposes
		try {
			Thread.sleep(5000);
		} catch( Exception e) {
			logger.error("Error sleeping", e);
		}
		
		return new AsyncResult<AssessmentRating>(ratingDAO.getRating(assessment, user));
	}
	
	@Async
	public Future<AssessmentRating> saveRating(Assessment assessment, PASTAUser user, RatingForm form) {
		AssessmentRating rating = ratingDAO.getRating(assessment, user);
		if(rating == null) {
			rating = new AssessmentRating(assessment, user);
		}
		rating.setRating(form.getRating());
		rating.setComment(form.getComment());
		
		ratingDAO.saveOrUpdate(rating);
		
		return new AsyncResult<AssessmentRating>(rating);
	}

	public Map<Long, AssessmentRating> getRatingsForUser(PASTAUser user) {
		Map<Long, AssessmentRating> ratings = new HashMap<Long, AssessmentRating>();
		for(AssessmentRating rating : ratingDAO.getAllRatingsForUser(user)) {
			ratings.put(rating.getAssessment().getId(), rating);
		}
		return ratings;
	}

	public List<AssessmentRating> getRatingsForAssessment(Assessment assessment) {
		return ratingDAO.getAllRatingsForAssessment(assessment);
	}

	public void deleteAllRatingsForAssessment(long assessmentId) {
		for(AssessmentRating rating : ratingDAO.getAllRatingsForAssessment(assessmentId)) {
			ratingDAO.delete(rating);
		}
	}
}
