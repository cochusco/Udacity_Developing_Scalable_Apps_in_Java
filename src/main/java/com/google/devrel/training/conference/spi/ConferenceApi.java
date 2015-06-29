package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.factory;
import static com.google.devrel.training.conference.service.OfyService.ofy;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Announcement;
import com.google.devrel.training.conference.domain.AppEngineUser;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.FeaturedSpeaker;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.domain.Session;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.google.devrel.training.conference.form.SessionForm;
import com.google.devrel.training.conference.form.SessionForm.TypeOfSession;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;

/**
 * Defines conference APIs.
 */
@Api(name = "conference", version = "v1",
        scopes = { Constants.EMAIL_SCOPE }, clientIds = {
        Constants.WEB_CLIENT_ID,
        Constants.API_EXPLORER_CLIENT_ID , Constants.ANDROID_CLIENT_ID },
        audiences = {Constants.ANDROID_AUDIENCE},
        description = "API for the Conference Central Backend application.")
public class ConferenceApi {

    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
	private static final Logger LOG = Logger.getLogger(ConferenceApi.class.getName());
    private static final Boolean True = null;
    private static final Boolean False = null;
    private static String  userId = null;
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    /**
     * Creates or updates a Profile object associated with the given user
     * object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @param profileForm
     *            A ProfileForm object sent from the client form.
     * @return Profile object just created.
     * @throws UnauthorizedException
     *             when the User object is null.no
     */

    // Declare this method as a method available externally through Endpoints
    @ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
    // The request that invokes this method should provide data that
    // conforms to the fields defined in ProfileForm
    // TODO 1 Pass the ProfileForm parameter
    // TODO 2 Pass the User parameter
    public Profile saveProfile(final User user, ProfileForm profileForm)
            throws UnauthorizedException {

        // TODO 2
        // If the user is not logged in, throw an UnauthorizedException
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO 2
        // Get the userId and mainEmail
        String mainEmail = user.getEmail();
        String userId = getUserId(user);

        // TODO 1
        // Get the displayName and teeShirtSize sent by the request.

        String displayName = profileForm.getDisplayName();
        TeeShirtSize teeShirtSize = profileForm.getTeeShirtSize();

        // Get the Profile from the datastore if it exists
        // otherwise create a new one
        Profile profile = ofy().load().key(Key.create(Profile.class, userId))
                .now();

        if (profile == null) {
            // Populate the displayName and teeShirtSize with default values
            // if not sent in the request
            if (displayName == null) {
                displayName = extractDefaultDisplayNameFromEmail(user
                        .getEmail());
            }
            if (teeShirtSize == null) {
                teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
            }
            // Now create a new Profile entity
            profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
        } else {
            // The Profile entity already exists
            // Update the Profile entity
            profile.update(displayName, teeShirtSize);
        }

        // TODO 3
        // Save the entity in the datastore
        ofy().save().entity(profile).now();

        // Return the profile
        return profile;
    }

    /**
     * Returns a Profile object associated with the given user object. The cloud
     * endpoints system automatically inject the User object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @return Profile object.
     * @throws UnauthorizedException
     *             when the User object is null.
     */
    @ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
    public Profile getProfile(final User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO
        // load the Profile Entity
        String userId = getUserId(user);
        Key key = Key.create(Profile.class, userId);

        Profile profile = (Profile) ofy().load().key(key).now();
        return profile;
    }

    /**
     * Gets the Profile entity for the current user
     * or creates it if it doesn't exist
     * @param user
     * @return user's Profile
     */
    private static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, getUserId(user))).now();
        if (profile == null) {
            // Create a new Profile if it doesn't exist.
            // Use default displayName and teeShirtSize
            String email = user.getEmail();
            profile = new Profile(getUserId(user),
                    extractDefaultDisplayNameFromEmail(email), email, TeeShirtSize.NOT_SPECIFIED);
        }
        return profile;
    }

    /**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
        throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // Allocate Id first, in order to make the transaction idempotent.
        final String userId = getUserId(user);
        Key<Profile> profileKey = Key.create(Profile.class, userId);
        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);
        final long conferenceId = conferenceKey.getId();
        final Queue queue = QueueFactory.getDefaultQueue();

        // Start a transaction.
        Conference conference = ofy().transact(new Work<Conference>() {
            @Override
            public Conference run() {
                // Fetch user's Profile.
                Profile profile = getProfileFromUser(user);
                Conference conference = new Conference(conferenceId, userId, conferenceForm);
                // Save Conference and Profile.
                ofy().save().entities(conference, profile).now();
                queue.add(ofy().getTransaction(),
                        TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                        .param("email", profile.getMainEmail())
                        .param("conferenceInfo", conference.toString()));
                return conference;
            }
        });
        return conference;
    }


    @ApiMethod(
            name = "queryConferences_nofilters",
            path = "queryConferences_nofilters",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> queryConferences_nofilters() {
        // Find all entities of type Conference
        Query<Conference> query = ofy().load().type(Conference.class).order("name");

        return query.list();
    }

    /**
     * Queries against the datastore with the given filters and returns the result.
     *
     * Normally this kind of method is supposed to get invoked by a GET HTTP method,
     * but we do it with POST, in order to receive conferenceQueryForm Object via the POST body.
     *
     * @param conferenceQueryForm A form object representing the query.
     * @return A List of Conferences that match the query.
     */
    @ApiMethod(
            name = "queryConferences",
            path = "queryConferences",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> queryConferences(ConferenceQueryForm conferenceQueryForm) {
        Iterable<Conference> conferenceIterable = conferenceQueryForm.getQuery();
        List<Conference> result = new ArrayList<>(0);
        List<Key<Profile>> organizersKeyList = new ArrayList<>(0);
        for (Conference conference : conferenceIterable) {
            organizersKeyList.add(Key.create(Profile.class, conference.getOrganizerUserId()));
            result.add(conference);
        }
        // To avoid separate datastore gets for each Conference, pre-fetch the Profiles.
        ofy().load().keys(organizersKeyList);
        return result;
    }

    


    /**
     * Returns a list of Conferences that the user created.
     * In order to receive the websafeConferenceKey via the JSON params, uses a POST method.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @return a list of Conferences that the user created.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
            name = "getConferencesCreated",
            path = "getConferencesCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> getConferencesCreated(final User user) throws UnauthorizedException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        String userId = getUserId(user);
        Key<Profile> userKey = Key.create(Profile.class, userId);
        return ofy().load().type(Conference.class)
                .ancestor(userKey)
                .order("name").list();
    }

    /**
     * Just a wrapper for Boolean.
     * We need this wrapped Boolean because endpoints functions must return
     * an object instance, they can't return a Type class such as
     * String or Integer or Boolean
     */
    public static class WrappedBoolean {

        private final Boolean result;
        private final String reason;

        public WrappedBoolean(Boolean result) {
            this.result = result;
            this.reason = "";
        }

        public WrappedBoolean(Boolean result, String reason) {
            this.result = result;
            this.reason = reason;
        }

        public Boolean getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Returns a Conference object with the given conferenceId.
     *
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return a Conference object with the given conferenceId.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "getConference",
            path = "conference/{websafeConferenceKey}",
            httpMethod = HttpMethod.GET
    )
    public Conference getConference(
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws NotFoundException {
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (conference == null) {
            throw new NotFoundException("No Conference found with key: " + websafeConferenceKey);
        }
        return conference;
    }

    /**
     * Register to attend the specified Conference.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "registerForConference",
            path = "conference/{websafeConferenceKey}/registration",
            httpMethod = HttpMethod.POST
    )

    public WrappedBoolean registerForConference(final User user,
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException,
            ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId
        final String userId = getUserId(user);

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                try {

                // Get the conference key
                // Will throw ForbiddenException if the key cannot be created
                Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                // Get the Conference entity from the datastore
                Conference conference = ofy().load().key(conferenceKey).now();

                // 404 when there is no Conference with the given conferenceId.
                if (conference == null) {
                    return new WrappedBoolean (false,
                            "No Conference found with key: "
                                    + websafeConferenceKey);
                }

                // Get the user's Profile entity
                Profile profile = getProfileFromUser(user);

                // Has the user already registered to attend this conference?
                if (profile.getConferenceKeysToAttend().contains(
                        websafeConferenceKey)) {
                    return new WrappedBoolean (false, "Already registered");
                } else if (conference.getSeatsAvailable() <= 0) {
                    return new WrappedBoolean (false, "No seats available");
                } else {
                    // All looks good, go ahead and book the seat
                    profile.addToConferenceKeysToAttend(websafeConferenceKey);
                    conference.bookSeats(1);

                    // Save the Conference and Profile entities
                    ofy().save().entities(profile, conference).now();
                    // We are booked!
                    return new WrappedBoolean(true, "Registration successful");
                }

                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");

                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Conference found with key")) {
                throw new NotFoundException (result.getReason());
            }
            else if (result.getReason() == "Already registered") {
                throw new ConflictException("You have already registered");
            }
            else if (result.getReason() == "No seats available") {
                throw new ConflictException("There are no seats available");
            }
            else {
                throw new ForbiddenException("Unknown exception");
            }
        }
        return result;
    }


    /**
     * Unregister from the specified Conference.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key to unregister
     *                             from.
     * @return Boolean true when success, otherwise false.
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "unregisterFromConference",
            path = "conference/{websafeConferenceKey}/registration",
            httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromConference(final User user,
                                            @Named("websafeConferenceKey")
                                            final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
                Conference conference = ofy().load().key(conferenceKey).now();
                // 404 when there is no Conference with the given conferenceId.
                if (conference == null) {
                    return new  WrappedBoolean(false,
                            "No Conference found with key: " + websafeConferenceKey);
                }

                // Un-registering from the Conference.
                Profile profile = getProfileFromUser(user);
                if (profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                    profile.unregisterFromConference(websafeConferenceKey);
                    conference.giveBackSeats(1);
                    ofy().save().entities(profile, conference).now();
                    return new WrappedBoolean(true);
                } else {
                    return new WrappedBoolean(false, "You are not registered for this conference");
                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Conference found with key")) {
                throw new NotFoundException (result.getReason());
            }
            else {
                throw new ForbiddenException(result.getReason());
            }
        }
        // NotFoundException is actually thrown here.
        return new WrappedBoolean(result.getResult());
    }

    /**
     * Returns a collection of Conference Object that the user is going to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of Conferences that the user is going to attend.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getConferencesToAttend",
            path = "getConferencesToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Conference> getConferencesToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Profile profile = ofy().load().key(Key.create(Profile.class, getUserId(user))).now();
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }
        List<String> keyStringsToAttend = profile.getConferenceKeysToAttend();
        List<Key<Conference>> keysToAttend = new ArrayList<>();
        for (String keyString : keyStringsToAttend) {
            keysToAttend.add(Key.<Conference>create(keyString));
        }
        return ofy().load().keys(keysToAttend).values();
    }


    
  
    @ApiMethod(
            name = "getAnnouncement",
            path = "announcement",
            httpMethod = HttpMethod.GET
    )
    public Announcement getAnnouncement() {
        MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
        Object message = memcacheService.get(Constants.MEMCACHE_ANNOUNCEMENTS_KEY);
        if (message != null) {
            return new Announcement(message.toString());
        }
        return null;
    }
    
    /**
     * This is an ugly workaround for null userId for Android clients.
     *
     * @param user A User object injected by the cloud endpoints.
     * @return the App Engine userId for the user.
     */
    private static String getUserId(User user) {
        String userId = user.getUserId();
        if (userId == null) {
            LOG.info("userId is null, so trying to obtain it from the datastore.");
            AppEngineUser appEngineUser = new AppEngineUser(user);
            ofy().save().entity(appEngineUser).now();
            // Begin new session for not using session cache.
            Objectify objectify = ofy().factory().begin();
            AppEngineUser savedUser = objectify.load().key(appEngineUser.getKey()).now();
            userId = savedUser.getUser().getUserId();
            LOG.info("Obtained the userId: " + userId);
        }
        return userId;
    }
    

    
    
    
    /**
     * Returns a list of Sessions for the Conference with key conferenceKeyStr.
     * @param conferenceKeyStr key of the Conference in String format.
     * @return Returns a list of Sessions for the Conference with key conferenceKeyStr.
     */
    @ApiMethod(
            name = "getConferenceSessions",
            path = "getConferenceSessions",
            httpMethod = HttpMethod.GET
    )
    public List<Session> getConferenceSessions(@Named("websafeConferenceKey") final String conferenceKeyStr){
    	Key<Conference> conferenceKey = Key.create(conferenceKeyStr);    	
    	List<Session> list= ofy().load().type(Session.class).ancestor(conferenceKey).list();
    	return list;
    }
    
    
    
    
    
    
    /**
     * Returns a list of Sessions of type typeOfSession for the Conference with key conferenceKeyStr.
     * @param conferenceKeyStr key of the Conference in String format.
     * @param typeOfSession type of session.
     * @return Returns a list of Sessions of type typeOfSession for the Conference with key conferenceKeyStr.
     */
    @ApiMethod(
            name = "getConferenceSessionsByType",
            path = "getConferenceSessionsByType",
            httpMethod = HttpMethod.GET
    )
    public List<Session> getConferenceSessionsByType(@Named("websafeConferenceKey")  final String conferenceKeyStr , @Named("typeOfSession") SessionForm.TypeOfSession typeOfSession){
    	Key<Conference> conferenceKey = Key.create(conferenceKeyStr);    	
    	List<Session> list= ofy().load().type(Session.class).ancestor(conferenceKey).filter("typeOfSession == " ,typeOfSession ).order("name").list();
    	return list;
    }       
    
    
    /**
     * Returns a list of Sessions for the for a selected speaker.
     * @param speaker name of the speaker.
     * @return Returns a list of Sessions for the for a selected speaker..
     */
    @ApiMethod(
            name = "getSessionsBySpeaker",
            path = "getSessionsBySpeaker",
            httpMethod = HttpMethod.GET
    )
    public List<Session> getSessionsBySpeaker(@Named("speaker") final String speaker){    	
    	List<Session> list= ofy().load().type(Session.class).filter("speaker == " , speaker).order("name").list();
    	return list;
    }
       
            
    /**
     * Creates a new Session object and stores it to the datastore , parent Conference and session creator must be the same anyway UnauthorizedException will be throw , if no exists a Conference with the provided 
     * conferenceKeyStr a NotFoundException will be throw .
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param sessionForm A SessionForm object representing user's inputs.
     * @param conferenceKeyStr key of the Conference belonging the session in String format. 
     * @return Created session
     * @throws UnauthorizedException
     * @throws NotFoundException if no exists a Conference with the provided   
     * @throws BadRequestException 
     */
    @ApiMethod(
            name = "createSession",
            path = "createSession",
            httpMethod = HttpMethod.POST
    )
    public Session createSession(User user , SessionForm sessionForm, @Named("websafeConferenceKey") final String conferenceKeyStr) throws UnauthorizedException, NotFoundException, BadRequestException{
    	if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }   	    	
    	Key<Conference> conferenceKey = Key.create(conferenceKeyStr);
    	// Check if the conference exists.
    	Conference conference= ofy().load().key(conferenceKey).now() ;
    	if (conference==null) {
            throw new NotFoundException ( "No Conference found with key: " + conferenceKeyStr);
        }    
    	// Check if the conference creator is the same as session creator
    	Profile profile = ofy().load().key(conference.getProfileKey()).now() ;    	
    	if(!(user.getUserId()).equals(profile.getUserId())){
    		throw new UnauthorizedException("Conference and session creator must be the same");
    	}    
    	// Check sessionForm StartTime field.
    	if(sessionForm.getStartTime() != null){			
			// Input format for tartTime
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIME_FORMAT);
			// set lenient to false for more strict date input. ()  
			sdf.setLenient(false);			
			try {
				//check format by parser				
				sdf.parse(sessionForm.getStartTime());
				//check by pattern
				Pattern p = Pattern.compile(Constants.TIME_FORMAT_PATTERN);
				Matcher m = p.matcher(sessionForm.getStartTime());
				if(!m.matches())throw new com.google.api.server.spi.response.BadRequestException("Incorrect startTime format : " + sessionForm.getStartTime() ) ;
			} catch (ParseException e) {
				throw new com.google.api.server.spi.response.BadRequestException("Incorrect startTime format : " + sessionForm.getStartTime() ) ;
			}						
		}    	    	
        final Key<Session> sessionKey = factory().allocateId(conferenceKey, Session.class);
    	Session session = new Session(sessionKey.getId(),sessionForm , conferenceKey);
    	ofy().save().entity(session).now();    	
    	// Check if there speaker is in more than one session at this conference.    
    	// only if the speaker is not null and not empty
        if((sessionForm.getSpeaker() != null)&&(sessionForm.getSpeaker()!= "") ){
        	List<Session> sessions =  ofy().load().type(Session.class).ancestor(conferenceKey).filter("speaker = " , sessionForm.getSpeaker()).list();
        	if(sessions.size()>1){
        		 List<String> sessionNames = new ArrayList<String>() ;
        		 for (Session sessionAux : sessions ) {
        			 sessionNames.add(sessionAux.getName());
				}        		 
        	     MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
        	     memcacheService.put(Constants.MEMCACHE_FEATUREDSPEAKER_KEY, new FeaturedSpeaker(sessionForm.getSpeaker(),sessionNames));
        	} ;  
        }
    	
    	return session;
    }
   
    
    
    /**
     * Add a session with key websafeSessionKey to the user wish list ,the session only be added if exist the session for the 
     * key , if exist the profile and the user is registered in the parent session conference anyway a NotFoundException will be thrown.  
     * @param user the user to add the wish list.
     * @param websafeSessionKey
     * @return result of the operation , in a WrappedBoolean.
     * @throws UnauthorizedException
     * @throws NotFoundException
     */
    @ApiMethod(
            name = "addSessionToWishlist",
            path = "addSessionToWishlist",
            httpMethod = HttpMethod.POST
    )
    public WrappedBoolean addSessionToWishlist(User user , @Named("websafeSessionKey") final String websafeSessionKey )throws UnauthorizedException, NotFoundException{
    	if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
    	Session session = (Session) ofy().load().key(Key.create(websafeSessionKey)).now();
    	if (session ==null) {
            throw new NotFoundException ( "No Session found with key: " + websafeSessionKey);
        }      	
    	Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId()))
                 .now();
    	if(profile ==null){
            throw new NotFoundException ( "No Profile found with id: " + user.getUserId());
        }  
    	Conference conference =   (Conference) ofy().load().key(Key.create(websafeSessionKey).getParent()).now() ;
    	// Check if the user is attending the parent conference for the session. 
     	if(!profile.getConferenceKeysToAttend().contains(conference.getWebsafeKey())) throw new NotFoundException ( "No Conference to attend with session id: " + websafeSessionKey);   	   	
    	profile.addToSessionKeysWishlist(websafeSessionKey);
    	ofy().save().entity(profile).now() ;
    	return new WrappedBoolean(true , "Successful added to wishlist") ;
    	
    }
    
    
    /**
     * Get the sessions in the user wish list , if user don't exists or have a profile an exception will be thrown.
     * @param user
     * @return the sessions in the user wish list.
     * @throws UnauthorizedException
     * @throws NotFoundException
     */
    @ApiMethod(
            name = "getSessionsInWishlist",
            path = "getSessionsInWishlist",
            httpMethod = HttpMethod.GET
    )
    public  List<Session> getSessionsInWishlist(User user) throws UnauthorizedException, NotFoundException{
    	if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }    	
    	Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId()))
                 .now();
    	if(profile ==null){
            throw new NotFoundException ( "No Profile found with id: " + user.getUserId());
        }       
    	List <Session> sessions = new ArrayList<Session>() ;
    	for(String sessionKey :profile.getSessionKeysWishlist() ){    		
    		sessions.add((Session) ofy().load().key(Key.create(sessionKey)).now());
    	}
    	return sessions ;
    	
    }
    
   
   
    
    
    /**
     * Get the last FeaturedSpeaker if it exist in memcache .     
     */
    @ApiMethod(
            name = "getFeaturedSpeaker",
            path = "getFeaturedSpeaker",
            httpMethod = HttpMethod.GET
    )
    public FeaturedSpeaker getFeaturedSpeaker(){		
		   MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
		   FeaturedSpeaker message = (FeaturedSpeaker) memcacheService.get(Constants.MEMCACHE_FEATUREDSPEAKER_KEY);
	        if (message != null) {
	            return message;
	        }
	        return null;
    	
    }
    
    /**
     * Own query 1 return all session with type typeOfSession after now , ordered by date.
     * @return
     */
    public List<Session> ownQueryOne(@Named("typeOfSession") TypeOfSession typeOfSession ) {
    	Query<Session> sessionsQuery = ofy().load().type(Session.class).filter("typeOfSession == ", typeOfSession).filter("date >= ", new java.util.Date()).order("date");     	
        return sessionsQuery.list();
    }
    

    /**
     * Own query 2 return all sessions featured by the speakers contained in wrappedStringList with highlight variable in highlights , ordered by date.
     * @return
     */      
    public List<Session> ownQueryTwo( @Named("highlight") final String highlight , WrappedStringList wrappedStringList ) {
    	Query<Session> sessionsQuery = ofy().load().type(Session.class).filter("highlights = ", highlight).filter("speaker IN ", wrappedStringList.getStringList()).order("speaker").order("date");
        return sessionsQuery.list();
    }
    
    public static  class WrappedStringList {        
        private List<Object> stringList;
		public List<Object> getStringList() {
			return stringList;
		}
		public void setStringList(List<Object> stringList) {
			this.stringList = stringList;
		}
    }
}
