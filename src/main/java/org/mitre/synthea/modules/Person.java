package org.mitre.synthea.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.mitre.synthea.modules.HealthRecord.Code;
import org.mitre.synthea.world.Hospital;
import org.mitre.synthea.world.Provider;

import com.vividsolutions.jts.geom.Point;

public class Person {
	
	public static final String BIRTHDATE = "birthdate";
	public static final String NAME = "name";
	public static final String SOCIOECONOMIC_CATEGORY = "socioeconomic_category";
	public static final String RACE = "race";
	public static final String GENDER = "gender";
	public static final String ID = "id";
	public static final String ADDRESS = "address";
	public static final String CITY = "city";
	public static final String STATE = "state";
	public static final String ZIP = "zip";
	public static final String COORDINATE = "coordinate";
	public static final String HEIGHT = "height";
	public static final String WEIGHT = "weight";
	
	private Random random;
	public Map<String,Object> attributes;
	private Map<String,Map<String,Integer>> symptoms;
	public EventList events;
	public HealthRecord record;
	/** history of the currently active module */
	public List<State> history;
	
	public Person(long seed) {
		random = new Random(seed);
		attributes = new ConcurrentHashMap<String,Object>();
		symptoms = new ConcurrentHashMap<String,Map<String,Integer>>();
		events = new EventList();
		record = new HealthRecord();
	}

	public double rand() {
		return random.nextDouble();
	}
	
	public double rand(double low, double high) {
		return (low + ((high - low) * random.nextDouble()));
	}
	
	public long ageInMilliseconds(long time) {
		long age = 0;
		if(attributes.containsKey(BIRTHDATE)) {
			age = time - (long)attributes.get(BIRTHDATE);
		}
		return age;
	}
	
	public int ageInMonths(long time)
	{
		// TODO - would prefer something more robust for these
		long age = ageInMilliseconds(time);
		return (int) (TimeUnit.MILLISECONDS.toDays(age) / (365.25 / 12));
	}
	
	public int ageInYears(long time) {
		long age = ageInMilliseconds(time);
		return (int) (TimeUnit.MILLISECONDS.toDays(age) / 365.25);
	}
	
	public boolean alive(long time) {
		return (events.event(Event.BIRTH) != null && events.before(time, Event.DEATH).isEmpty());
	}
	
	public void setSymptom(String cause, String type, int value) {
		if(!symptoms.containsKey(type)) {
			symptoms.put(type, new ConcurrentHashMap<String,Integer>());
		}
		symptoms.get(type).put(cause, value);
	}
	
	public int getSymptom(String type) {
		int max = 0;
		if(symptoms.containsKey(type)) {
			Map<String,Integer> typedSymptoms = symptoms.get(type);
			for(String cause : typedSymptoms.keySet()) {
				if(typedSymptoms.get(cause) > max) {
					max = typedSymptoms.get(cause);
				}
			}
		}
		return max;
	}
	
	public void recordDeath(long time, Code cause, String ruleName)
	{
		events.create(time, Event.DEATH, ruleName, true);
	}
	
	/**
	 * @return total : sum of all the symptom severities. This number drives care-seeking behaviors.
	 */
	public int symptomTotal() {
		int total = 0;
		for(String type : symptoms.keySet()) {
			total += getSymptom(type);			
		}
		return total;
	}

	public boolean hadPriorState(String name) {
		if(history == null) {
			return false;
		}
		for(State state : history) {
			if(state.name == name) {
				return true;
			}
		}
		return false;
	}

	public boolean hadPriorStateSince(String priorState, long time) {
		if(history == null) {
			return false;
		}
		for(State state : history) {
			if(state.name == priorState && state.exited > time) {
				return true;
			}
		}
		return false;
	}

	public boolean hadPriorStateSince(String priorState, String sinceState) {
		if(history == null) {
			return false;
		}
		for(State state : history) {
			if(state.name == priorState) {
				return true;
			} else if(state.name == sinceState) {
				return false;
			}
		}
		return false;
	}
	
	// Providers API -----------------------------------------------------------
	public static final String CURRENTPROVIDER = "currentProvider";
	public static final String PREFERREDAMBULATORYPROVIDER = "preferredAmbulatoryProvider";
	public static final String PREFERREDINPATIENTPROVIDER = "preferredInpatientProvider";
	public static final String PREFERREDEMERGENCYPROVIDER = "preferredEmergencyProvider";
	
	
	public Provider getAmbulatoryProvider(){
		return (Provider) attributes.get(PREFERREDAMBULATORYPROVIDER);
	}
	
	public void setAmbulatoryProvider(){
		Point personLocation = (Point) attributes.get(Person.COORDINATE);
		Provider provider = Hospital.findClosestAmbulatory(personLocation);
		attributes.put(PREFERREDAMBULATORYPROVIDER, provider);
	}
	
	public void setAmbulatoryProvider(Provider provider){
		attributes.put(PREFERREDAMBULATORYPROVIDER, provider);
	}
	
	public Provider getInpatientProvider(){
		return (Provider) attributes.get(PREFERREDINPATIENTPROVIDER);
	}
	
	public void setInpatientProvider(){
		Point personLocation = (Point) attributes.get(Person.COORDINATE);
		Provider provider = Hospital.findClosestInpatient(personLocation);
		attributes.put(PREFERREDINPATIENTPROVIDER, provider);
	}
	
	public void setInpatientProvider(Provider provider){
		attributes.put(PREFERREDINPATIENTPROVIDER, provider);
	}
	
	public Provider getEmergencyProvider(){
		return (Provider) attributes.get(PREFERREDEMERGENCYPROVIDER);
	}

	public void setEmergencyProvider(){
		Point personLocation = (Point) attributes.get(Person.COORDINATE);
		Provider provider = Hospital.findClosestEmergency(personLocation);
		attributes.put(PREFERREDEMERGENCYPROVIDER, provider);
	}
	
	public void setEmergencyProvider(Provider provider){
		if(provider == null){
			Point personLocation = (Point) attributes.get(Person.COORDINATE);
			provider = Hospital.findClosestEmergency(personLocation);
		}
		attributes.put(PREFERREDEMERGENCYPROVIDER, provider);
	}
	
	public void addCurrentProvider(String context, Provider provider){
		if(attributes.containsKey(CURRENTPROVIDER)){
			Map<String, Provider> currentProviders = (Map) attributes.get(CURRENTPROVIDER);
			currentProviders.put(context, provider);
			attributes.put(CURRENTPROVIDER, currentProviders);
		} else {
			Map<String, Provider> currentProviders = new HashMap<String, Provider>();
			currentProviders.put(context, provider);
			attributes.put(CURRENTPROVIDER, currentProviders);
		}
	}
	
	public void removeCurrentProvider(String module){
		Map<String, Provider> currentProviders = (Map) attributes.get(CURRENTPROVIDER);
		currentProviders.remove(module);
	}
	
	public Provider getCurrentProvider(String module){
		Map<String, Provider> currentProviders = (Map) attributes.get(CURRENTPROVIDER);
		return currentProviders.get(module);
	}
}
