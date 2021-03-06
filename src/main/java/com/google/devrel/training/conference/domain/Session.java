package com.google.devrel.training.conference.domain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.form.SessionForm;
import com.google.devrel.training.conference.form.SessionForm.TypeOfSession;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

@Entity
// Enable memcache requirement.
@Cache
public class Session {
	//Use autogenerated key instead for example name key for flexibility (Fe, I can create sessions with same father and name ).
	@Id
    private long id;
	@Index
	private String name;
	@Index
	private List<String> highlights;
	@Index
	private Date date;	
	@Ignore
	private String startTime;	
	@Ignore 
	private String websafeKey ;
	@Index
	private String speaker;	
	private int duration;	
	@Index
	private TypeOfSession typeOfSession ;
	@Parent   
    private Key<Conference> conferenceKey;
	private Session(){};
	public Session(long id , SessionForm sessionForm , Key<Conference> conferenceKey){
		this.id = id ;
		this.conferenceKey = conferenceKey ;
		this.updateWithSessionForm(sessionForm);
	};
	public String getName() {
		return name;
	}
	public List<String> getHighlights() {
		return highlights;
	}
	public Date getDate() {
		return date;
	}	
	public String getStartTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.TIME_FORMAT);
	    return dateFormat.format(this.date);				
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
	public String getWebsafeKey() {
	        return Key.create(conferenceKey, Session.class, id).getString();
	}
	public void updateWithSessionForm(SessionForm sessionForm) {
		//If date is null current date is set 
		this.date = (sessionForm.getDate()== null)? new Date() :sessionForm.getDate() ;
		this.duration = sessionForm.getDuration();
		this.highlights = sessionForm.getHighlights();
		this.name = sessionForm.getName();
		this.speaker = sessionForm.getSpeaker();
		//Set typeOfSession to default if not specified 
		this.typeOfSession = (sessionForm.getTypeOfSession() == null) ? TypeOfSession.NOT_SPECIFIED : sessionForm.getTypeOfSession();
		//If startTime is set, will be stored as a part of date property otherwise will be set to the current time
		if(sessionForm.getStartTime() != null){			
			// Input format for tartTime
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIME_FORMAT);
			// set lenient to false for more strict date input. ()  
			sdf.setLenient(false);
			Calendar calendarDate = Calendar.getInstance();
			Calendar calendarTime = Calendar.getInstance();
	        calendarDate.setTime(this.date); 
	        try {
				calendarTime.setTime(sdf.parse(sessionForm.getStartTime()));				
			} catch (ParseException e) {				
				e.printStackTrace();
			}
	        // Set hour and minute in date property from input form
	        calendarDate.set(Calendar.HOUR_OF_DAY , calendarTime.get(Calendar.HOUR_OF_DAY) );
	        calendarDate.set(Calendar.MINUTE , calendarTime.get(Calendar.MINUTE) );
	        this.date = calendarDate.getTime() ;
		} 
		this.websafeKey = Key.create(conferenceKey, Session.class, id).getString();
	}	
}
