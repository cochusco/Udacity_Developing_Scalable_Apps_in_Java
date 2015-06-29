package com.google.devrel.training.conference.form;

import java.util.Date;
import java.util.List;

public class SessionForm {
	private String name;
	private List<String> highlights;
	private Date date;		
	private String speaker;
	private int duration;	
	private String startTime;

	private TypeOfSession typeOfSession ;	
	public String getName() {
		return name;
	}
	public List<String> getHighlights() {
		return highlights;
	}
	public Date getDate() {
		return date;
	}	
	public String getSpeaker() {
		return speaker;
	}
	public int getDuration() {
		return duration;
	}
	public TypeOfSession getTypeOfSession() {
		return typeOfSession;
	}
	//Start time , input format must be HH:mm
	 public String getStartTime() {
		return startTime;
	}	
	 
	public static enum TypeOfSession {
	    	NOT_SPECIFIED,
	        WORKSHOP,
	        LECTURE,
	        KEYNOTE
	    }

}
