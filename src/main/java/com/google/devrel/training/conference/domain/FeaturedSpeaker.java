package com.google.devrel.training.conference.domain;

import java.io.Serializable;
import java.util.List;
/**
 * A simple wrapper to store session names for a featured speaker. .
 */ 
public class FeaturedSpeaker implements Serializable{	 	
		private static final long serialVersionUID = 1L;
		private String featuredSpeaker;
	 	private List<String> sessionNames ;
		
		public List<String> getSessionNames() {
			return sessionNames;
		}		
		public String getFeaturedSpeaker() {
			return featuredSpeaker;
		}
		public FeaturedSpeaker(String featuredSpeaker, List<String> sessionNames) {
			super();
			this.featuredSpeaker = featuredSpeaker;
			this.sessionNames = sessionNames;
		}
		
}
