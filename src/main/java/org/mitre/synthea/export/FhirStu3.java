package org.mitre.synthea.export;

import ca.uhn.fhir.context.FhirContext;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vividsolutions.jts.geom.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceClinicalStatus;
import org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceCriticality;
import org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceType;
import org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceVerificationStatus;
import org.hl7.fhir.dstu3.model.Basic;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.CarePlan.CarePlanActivityComponent;
import org.hl7.fhir.dstu3.model.CarePlan.CarePlanActivityDetailComponent;
import org.hl7.fhir.dstu3.model.CarePlan.CarePlanActivityStatus;
import org.hl7.fhir.dstu3.model.CarePlan.CarePlanIntent;
import org.hl7.fhir.dstu3.model.CarePlan.CarePlanStatus;
import org.hl7.fhir.dstu3.model.Claim.ClaimStatus;
import org.hl7.fhir.dstu3.model.Claim.ItemComponent;
import org.hl7.fhir.dstu3.model.Claim.ProcedureComponent;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Condition.ConditionClinicalStatus;
import org.hl7.fhir.dstu3.model.Condition.ConditionVerificationStatus;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.DecimalType;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.dstu3.model.Dosage;
import org.hl7.fhir.dstu3.model.Encounter.EncounterHospitalizationComponent;
import org.hl7.fhir.dstu3.model.Encounter.EncounterStatus;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Goal.GoalStatus;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.MedicationRequest.MedicationRequestIntent;
import org.hl7.fhir.dstu3.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent;
import org.hl7.fhir.dstu3.model.Observation.ObservationStatus;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.PositiveIntType;
import org.hl7.fhir.dstu3.model.Procedure.ProcedureStatus;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Timing.TimingRepeatComponent;
import org.hl7.fhir.dstu3.model.Timing;
import org.hl7.fhir.dstu3.model.Timing.UnitsOfTime;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.SimpleCSV;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.CarePlan;
import org.mitre.synthea.world.concepts.HealthRecord.Claim;
import org.mitre.synthea.world.concepts.HealthRecord.ClaimItem;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;
import org.mitre.synthea.world.concepts.HealthRecord.Report;

public class FhirStu3 {
  // HAPI FHIR warns that the context creation is expensive, and should be performed
  // per-application, not per-record
  private static final FhirContext FHIR_CTX = FhirContext.forDstu3();

  private static final String SNOMED_URI = "http://snomed.info/sct";
  private static final String LOINC_URI = "http://loinc.org";
  private static final String RXNORM_URI = "http://www.nlm.nih.gov/research/umls/rxnorm";
  private static final String CVX_URI = "http://hl7.org/fhir/sid/cvx";
  private static final String DISCHARGE_URI = "http://www.nubc.org/patient-discharge";
  private static final String SHR_EXT = "http://standardhealthrecord.org/fhir/StructureDefinition/"; 
  private static final String SYNTHEA_EXT = "http://scottschreckengaust.github.io/synthea/";
  private static final String UNITSOFMEASURE_URI = "http://unitsofmeasure.org";

  private static final Map raceEthnicityCodes = loadRaceEthnicityCodes();
  private static final Map languageLookup = loadLanguageLookup();

  private static final boolean USE_SHR_EXTENSIONS =
      Boolean.parseBoolean(Config.get("exporter.fhir.use_shr_extensions"));
  
  private static final Table<String,String,String> SHR_MAPPING = loadSHRMapping();
  
  @SuppressWarnings("rawtypes")
  private static Map loadRaceEthnicityCodes() {
    String filename = "race_ethnicity_codes.json";
    try {
      String json = Utilities.readResource(filename);
      Gson g = new Gson();
      return g.fromJson(json, HashMap.class);
    } catch (Exception e) {
      System.err.println("ERROR: unable to load json: " + filename);
      e.printStackTrace();
      throw new ExceptionInInitializerError(e);
    }
  }

  @SuppressWarnings("rawtypes")
  private static Map loadLanguageLookup() {
    String filename = "language_lookup.json";
    try {
      String json = Utilities.readResource(filename);
      Gson g = new Gson();
      return g.fromJson(json, HashMap.class);
    } catch (Exception e) {
      System.err.println("ERROR: unable to load json: " + filename);
      e.printStackTrace();
      throw new ExceptionInInitializerError(e);
    }
  }
  

  private static Table<String, String, String> loadSHRMapping() {
    if (!USE_SHR_EXTENSIONS) {
      // don't bother creating the table unless we need it
      return null;
    }
    Table<String,String,String> mappingTable = HashBasedTable.create();
    
    List<LinkedHashMap<String,String>> csvData;
    try {
      csvData = SimpleCSV.parse(Utilities.readResource("shr_mapping.csv"));
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    
    for (LinkedHashMap<String,String> line : csvData) {
      String system = line.get("SYSTEM");
      String code = line.get("CODE");
      String url = line.get("URL");
      
      mappingTable.put(system, code, url);
    }
    
    return mappingTable;
  }

  /**
   * Convert the given Person into a JSON String, containing a FHIR Bundle of the Person and the
   * associated entries from their health record.
   * 
   * @param person
   *          Person to generate the FHIR JSON for
   * @param stopTime
   *          Time the simulation ended
   * @return String containing a JSON representation of a FHIR Bundle containing the Person's health
   *         record
   */
  public static String convertToFHIR(Person person, long stopTime) {
    Bundle bundle = new Bundle();
    bundle.setType(BundleType.COLLECTION);

    BundleEntryComponent personEntry = basicInfo(person, bundle, stopTime);

    for (Encounter encounter : person.record.encounters) {
      BundleEntryComponent encounterEntry = encounter(personEntry, bundle, encounter);

      for (HealthRecord.Entry condition : encounter.conditions) {
        condition(personEntry, bundle, encounterEntry, condition);
      }
      
      for (HealthRecord.Entry allergy : encounter.allergies) {
        allergy(personEntry, bundle, encounterEntry, allergy);
      }

      for (Observation observation : encounter.observations) {
        observation(personEntry, bundle, encounterEntry, observation);
      }

      for (Procedure procedure : encounter.procedures) {
        procedure(personEntry, bundle, encounterEntry, procedure);
      }

      for (Medication medication : encounter.medications) {
        medication(personEntry, bundle, encounterEntry, medication);
      }

      for (HealthRecord.Entry immunization : encounter.immunizations) {
        immunization(personEntry, bundle, encounterEntry, immunization);
      }

      for (Report report : encounter.reports) {
        report(personEntry, bundle, encounterEntry, report);
      }

      for (CarePlan careplan : encounter.careplans) {
        careplan(personEntry, bundle, encounterEntry, careplan);
      }

      // one claim per encounter
      encounterClaim(personEntry, bundle, encounterEntry, encounter.claim);
    }

    String bundleJson = FHIR_CTX.newJsonParser().setPrettyPrint(true)
        .encodeResourceToString(bundle);

    return bundleJson;
  }

  /**
   * Map the given Person to a FHIR Patient resource, and add it to the given Bundle.
   * 
   * @param person
   *          The Person
   * @param bundle
   *          The Bundle to add to
   * @param stopTime
   *          Time the simulation ended
   * @return The created Entry
   */
  private static BundleEntryComponent basicInfo(Person person, Bundle bundle, long stopTime) {
    Patient patientResource = new Patient();

    patientResource.addIdentifier().setSystem("https://github.com/scottschreckengaust/synthea")
        .setValue((String) person.attributes.get(Person.ID));

    Code mrnCode = new Code("http://hl7.org/fhir/v2/0203", "MR", "Medical Record Number");
    patientResource.addIdentifier()
        .setType(mapCodeToCodeableConcept(mrnCode, "http://hl7.org/fhir/v2/0203"))
        .setSystem("http://hospital.smarthealthit.org")
        .setValue((String) person.attributes.get(Person.ID));

    Code ssnCode = new Code("http://hl7.org/fhir/identifier-type", "SB", "Social Security Number");
    patientResource.addIdentifier()
        .setType(mapCodeToCodeableConcept(ssnCode, "http://hl7.org/fhir/identifier-type"))
        .setSystem("http://hl7.org/fhir/sid/us-ssn")
        .setValue((String) person.attributes.get(Person.IDENTIFIER_SSN));

    if (person.attributes.get(Person.IDENTIFIER_DRIVERS) != null) {
      Code driversCode = new Code("http://hl7.org/fhir/v2/0203", "DL", "Driver's License");
      patientResource.addIdentifier()
          .setType(mapCodeToCodeableConcept(driversCode, "http://hl7.org/fhir/v2/0203"))
          .setSystem("urn:oid:2.16.840.1.113883.4.3.25")
          .setValue((String) person.attributes.get(Person.IDENTIFIER_DRIVERS));
    }

    if (person.attributes.get(Person.IDENTIFIER_PASSPORT) != null) {
      Code passportCode = new Code("http://hl7.org/fhir/v2/0203", "PPN", "Passport Number");
      patientResource.addIdentifier()
          .setType(mapCodeToCodeableConcept(passportCode, "http://hl7.org/fhir/v2/0203"))
          .setSystem(SHR_EXT + "passportNumber")
          .setValue((String) person.attributes.get(Person.IDENTIFIER_PASSPORT));
    }

    // We do not yet account for mixed race
    Extension raceExtension = new Extension(
        "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
    String race = (String) person.attributes.get(Person.RACE);

    String raceDisplay;
    switch (race) {
      case "white":
        raceDisplay = "White";
        break;
      case "black":
        raceDisplay = "Black or African American";
        break;
      case "asian":
        raceDisplay = "Asian";
        break;
      case "native":
        raceDisplay = "American Indian or Alaska Native";
        break;
      default: // Hispanic or Other (Put Hawaiian and Pacific Islander here for now)
        raceDisplay = "Other";
        break;
    }

    String raceNum = (String) raceEthnicityCodes.get(race);

    if (race != "hispanic") {
      Extension raceCodingExtension = new Extension("ombCategory");
      Coding raceCoding = new Coding();
      raceCoding.setSystem("urn:oid:2.16.840.1.113883.6.238");
      raceCoding.setCode(raceNum);
      raceCoding.setDisplay(raceDisplay);
      raceCodingExtension.setValue(raceCoding);
      raceExtension.addExtension(raceCodingExtension);
    }

    Extension raceTextExtension = new Extension("text");
    raceTextExtension.setValue(new StringType(raceDisplay));

    raceExtension.addExtension(raceTextExtension);

    patientResource.addExtension(raceExtension);

    // We do not yet account for mixed ethnicity
    Extension ethnicityExtension = new Extension(
        "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");
    String ethnicity = (String) person.attributes.get(Person.ETHNICITY);

    String ethnicityDisplay;
    if (race == "hispanic") {
      ethnicity = "hispanic";
      ethnicityDisplay = "Hispanic or Latino";
    } else {
      ethnicity = "nonhispanic";
      ethnicityDisplay = "Not Hispanic or Latino";
    }

    String ethnicityNum = (String) raceEthnicityCodes.get(ethnicity);

    Extension ethnicityCodingExtension = new Extension("ombCategory");
    Coding ethnicityCoding = new Coding();
    ethnicityCoding.setSystem("urn:oid:2.16.840.1.113883.6.238");
    ethnicityCoding.setCode(ethnicityNum);
    ethnicityCoding.setDisplay(ethnicityDisplay);
    ethnicityCodingExtension.setValue(ethnicityCoding);

    ethnicityExtension.addExtension(ethnicityCodingExtension);

    Extension ethnicityTextExtension = new Extension("text");
    ethnicityTextExtension.setValue(new StringType(ethnicityDisplay));

    ethnicityExtension.addExtension(ethnicityTextExtension);

    patientResource.addExtension(ethnicityExtension);

    String firstLanguage = (String) person.attributes.get(Person.FIRST_LANGUAGE);
    Map languageMap = (Map) languageLookup.get(firstLanguage);
    Code languageCode = new Code((String) languageMap.get("system"),
        (String) languageMap.get("code"), (String) languageMap.get("display"));
    List<PatientCommunicationComponent> communication = 
        new ArrayList<PatientCommunicationComponent>();
    communication.add(new PatientCommunicationComponent(
        mapCodeToCodeableConcept(languageCode, (String) languageMap.get("system"))));
    patientResource.setCommunication(communication);

    HumanName name = patientResource.addName();
    name.setUse(HumanName.NameUse.OFFICIAL);
    name.addGiven((String) person.attributes.get(Person.FIRST_NAME));
    name.setFamily((String) person.attributes.get(Person.LAST_NAME));
    if (person.attributes.get(Person.NAME_PREFIX) != null) {
      name.addPrefix((String) person.attributes.get(Person.NAME_PREFIX));
    }
    if (person.attributes.get(Person.NAME_SUFFIX) != null) {
      name.addSuffix((String) person.attributes.get(Person.NAME_SUFFIX));
    }
    if (person.attributes.get(Person.MAIDEN_NAME) != null) {
      HumanName maidenName = patientResource.addName();
      maidenName.setUse(HumanName.NameUse.MAIDEN);
      maidenName.addGiven((String) person.attributes.get(Person.FIRST_NAME));
      maidenName.setFamily((String) person.attributes.get(Person.MAIDEN_NAME));
      if (person.attributes.get(Person.NAME_PREFIX) != null) {
        maidenName.addPrefix((String) person.attributes.get(Person.NAME_PREFIX));
      }
      if (person.attributes.get(Person.NAME_SUFFIX) != null) {
        maidenName.addSuffix((String) person.attributes.get(Person.NAME_SUFFIX));
      }
    }

    Extension birthSexExtension = new Extension(
        "http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex");
    if (person.attributes.get(Person.GENDER).equals("M")) {
      patientResource.setGender(AdministrativeGender.MALE);
      birthSexExtension.setValue(new CodeType("M"));
    } else if (person.attributes.get(Person.GENDER).equals("F")) {
      patientResource.setGender(AdministrativeGender.FEMALE);
      birthSexExtension.setValue(new CodeType("F"));
    }
    patientResource.addExtension(birthSexExtension);

    Extension mothersMaidenNameExtension = new Extension(
        "http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName");
    String mothersMaidenName = (String) person.attributes.get(Person.NAME_MOTHER);
    mothersMaidenNameExtension.setValue(new StringType(mothersMaidenName));
    patientResource.addExtension(mothersMaidenNameExtension);

    long birthdate = (long) person.attributes.get(Person.BIRTHDATE);
    patientResource.setBirthDate(new Date(birthdate));

    String state = (String) person.attributes.get(Person.STATE);
    
    Address addrResource = patientResource.addAddress();
    addrResource.addLine((String) person.attributes.get(Person.ADDRESS))
        .setCity((String) person.attributes.get(Person.CITY))
        .setPostalCode((String) person.attributes.get(Person.ZIP))
        .setState(state).setCountry("US");

    Address birthplace = new Address();
    birthplace.setCity((String) person.attributes.get(Person.BIRTHPLACE)).setState(state)
        .setCountry("US");
    Extension birthplaceExtension = new Extension(
        "http://hl7.org/fhir/StructureDefinition/birthPlace");
    birthplaceExtension.setValue(birthplace);
    patientResource.addExtension(birthplaceExtension);

    if (person.attributes.get(Person.MULTIPLE_BIRTH_STATUS) != null) {
      patientResource.setMultipleBirth(
          new IntegerType((int) person.attributes.get(Person.MULTIPLE_BIRTH_STATUS)));
    } else {
      patientResource.setMultipleBirth(new BooleanType(false));
    }

    patientResource.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE)
        .setUse(ContactPoint.ContactPointUse.HOME)
        .setValue((String) person.attributes.get(Person.TELECOM));

    String maritalStatus = ((String) person.attributes.get(Person.MARITAL_STATUS));
    if (maritalStatus != null) {
      Code maritalStatusCode = new Code("http://hl7.org/fhir/v3/MaritalStatus", maritalStatus,
          maritalStatus);
      patientResource.setMaritalStatus(
          mapCodeToCodeableConcept(maritalStatusCode, "http://hl7.org/fhir/v3/MaritalStatus"));
    } else {
      Code maritalStatusCode = new Code("http://hl7.org/fhir/v3/MaritalStatus", "S",
          "Never Married");
      patientResource.setMaritalStatus(
          mapCodeToCodeableConcept(maritalStatusCode, "http://hl7.org/fhir/v3/MaritalStatus"));
    }

    Point coord = (Point) person.attributes.get(Person.COORDINATE);
    if (coord != null) {
      Extension geolocation = addrResource.addExtension();
      geolocation.setUrl("http://hl7.org/fhir/StructureDefinition/geolocation");
      geolocation.addExtension("latitude", new DecimalType(coord.getY()));
      geolocation.addExtension("longitude", new DecimalType(coord.getX()));
    }

    if (!person.alive(stopTime)) {
      patientResource.setDeceased(convertFhirDateTime(person.record.death, true));
    }

    String generatedBySynthea = "Generated by <a href=\"https://github.com/scottschreckengaust/synthea\">Synthea</a>."
        + "Version identifier: " + Utilities.SYNTHEA_VERSION + " . " 
        + "  Person seed: " + person.seed 
        + "  Population seed: " + person.populationSeed;

    patientResource.setText(new Narrative().setStatus(NarrativeStatus.GENERATED)
        .setDiv(new XhtmlNode(NodeType.Element).setValue(generatedBySynthea)));

    if (USE_SHR_EXTENSIONS) {
      
      patientResource.setMeta(new Meta().addProfile(SHR_EXT + "shr-entity-Patient"));

      // Patient profile requires race, ethnicity, birthsex,
      // MothersMaidenName, FathersName, Person-extension

      patientResource.addExtension()
        .setUrl(SHR_EXT + "shr-actor-FictionalPerson-extension")
        .setValue(new BooleanType(true));
      
      String fathersName = (String) person.attributes.get(Person.NAME_FATHER);
      Extension fathersNameExtension = new Extension(
          SHR_EXT + "shr-entity-FathersName-extension", new HumanName().setText(fathersName));
      patientResource.addExtension(fathersNameExtension);

      String ssn = (String) person.attributes.get(Person.IDENTIFIER_SSN);
      Extension ssnExtension = new Extension(
          SHR_EXT + "shr-demographics-SocialSecurityNumber-extension",
          new StringType(ssn));
      patientResource.addExtension(ssnExtension);
      
      Basic personResource = new Basic();
      // the only required field on this patient resource is code
      
      Coding fixedCode = new Coding(
          "http://standardhealthrecord.org/fhir/basic-resource-type",
          "shr-entity-Person", "shr-entity-Person");
      personResource.setCode(new CodeableConcept().addCoding(fixedCode));

      Meta personMeta = new Meta();
      personMeta.addProfile(SHR_EXT + "shr-entity-Person");
      personResource.setMeta(personMeta);
      
      BundleEntryComponent personEntry = newEntry(bundle, personResource);
      patientResource.addExtension()
          .setUrl(SHR_EXT + "shr-entity-Person-extension")
          .setValue(new Reference(personEntry.getFullUrl()));
    }
    
    // DALY and QALY values
    // we only write the last(current) one to the patient record
    Double dalyValue = (Double) person.attributes.get("most-recent-daly");
    Double qalyValue = (Double) person.attributes.get("most-recent-qaly");
    if (dalyValue != null) {
      Extension dalyExtension = new Extension(SYNTHEA_EXT + "disability-adjusted-life-years");
      DecimalType daly = new DecimalType(dalyValue);
      dalyExtension.setValue(daly);
      patientResource.addExtension(dalyExtension);

      Extension qalyExtension = new Extension(SYNTHEA_EXT + "quality-adjusted-life-years");
      DecimalType qaly = new DecimalType(qalyValue);
      qalyExtension.setValue(qaly);
      patientResource.addExtension(qalyExtension);
    }

    return newEntry(bundle, patientResource);
  }

  /**
   * Map the given Encounter into a FHIR Encounter resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          Entry for the Person
   * @param bundle
   *          The Bundle to add to
   * @param encounter
   *          The current Encounter
   * @return The added Entry
   */
  private static BundleEntryComponent encounter(BundleEntryComponent personEntry, Bundle bundle,
      Encounter encounter) {
    org.hl7.fhir.dstu3.model.Encounter encounterResource = new org.hl7.fhir.dstu3.model.Encounter();

    encounterResource.setSubject(new Reference(personEntry.getFullUrl()));
    encounterResource.setStatus(EncounterStatus.FINISHED);
    if (encounter.codes.isEmpty()) {
      // wellness encounter
      encounterResource.addType().addCoding().setCode("185349003")
          .setDisplay("Encounter for check up").setSystem(SNOMED_URI);

    } else {
      Code code = encounter.codes.get(0);
      encounterResource.addType(mapCodeToCodeableConcept(code, SNOMED_URI));
    }

    encounterResource.setClass_(new Coding().setCode(encounter.type));
    long encounterEnd = encounter.stop > 0 ? encounter.stop
        : encounter.start + TimeUnit.MINUTES.toMillis(15);

    encounterResource
        .setPeriod(new Period().setStart(new Date(encounter.start)).setEnd(new Date(encounterEnd)));

    if (encounter.reason != null) {
      encounterResource.addReason().addCoding().setCode(encounter.reason.code)
          .setDisplay(encounter.reason.display).setSystem(SNOMED_URI);
    }

    if (encounter.provider != null) {
      String providerFullUrl = null;

      for (BundleEntryComponent entry : bundle.getEntry()) {
        if ((entry.getResource().fhirType().equals("Organization"))
            && (entry.getResource().getId().equals(encounter.provider.getResourceID()))) {
          providerFullUrl = entry.getFullUrl();
          break;
        }
      }

      if (providerFullUrl != null) {
        encounterResource.setServiceProvider(new Reference(providerFullUrl));
      } else {
        BundleEntryComponent providerOrganization = provider(bundle, encounter.provider);
        encounterResource.setServiceProvider(new Reference(providerOrganization.getFullUrl()));
      }
    } else { // no associated provider, patient goes to ambulatory provider
      Patient patient = (Patient) personEntry.getResource();
      List<Reference> generalPractitioner = patient.getGeneralPractitioner();

      if (generalPractitioner.size() > 0) {
        String generalPractitionerReference = (String) patient.getGeneralPractitioner().get(0)
            .getReference();

        for (BundleEntryComponent entry : bundle.getEntry()) {
          if ((entry.getResource().fhirType().equals("Organization"))
              && generalPractitionerReference.equals("urn:uuid:" + entry.getResource().getId())) {
            encounterResource.setServiceProvider(new Reference(generalPractitionerReference));
          }
        }
      }
    }

    if (encounter.discharge != null) {
      EncounterHospitalizationComponent hospitalization = new EncounterHospitalizationComponent();
      Code dischargeDisposition = new Code(DISCHARGE_URI, encounter.discharge.code,
          encounter.discharge.display);
      hospitalization
          .setDischargeDisposition(mapCodeToCodeableConcept(dischargeDisposition, DISCHARGE_URI));
      encounterResource.setHospitalization(hospitalization);
    }
    
    if (USE_SHR_EXTENSIONS) {
      encounterResource.setMeta(
          new Meta().addProfile(SHR_EXT + "shr-encounter-EncounterPerformed"));
      // required fields for this profile are status & action-PerformedContext-extension
      
      Extension performedContext = new Extension();
      performedContext.setUrl(SHR_EXT + "shr-action-PerformedContext-extension");
      performedContext.addExtension(
          SHR_EXT + "shr-action-Status-extension", 
          new CodeType("finished"));
      
      encounterResource.addExtension(performedContext);
    }
  
    return newEntry(bundle, encounterResource);
  }

  /**
   * Create an entry for the given Claim, which references a Medication.
   * 
   * @param personEntry
   *          Entry for the person
   * @param bundle
   *          The Bundle to add to
   * @param encounterEntry
   *          The current Encounter
   * @param claim
   *          the Claim object
   * @param medicationEntry
   *          The Entry for the Medication object, previously created
   * @return the added Entry
   */
  private static BundleEntryComponent medicationClaim(BundleEntryComponent personEntry,
      Bundle bundle, BundleEntryComponent encounterEntry, Claim claim,
      BundleEntryComponent medicationEntry) {
    org.hl7.fhir.dstu3.model.Claim claimResource = new org.hl7.fhir.dstu3.model.Claim();
    org.hl7.fhir.dstu3.model.Encounter encounterResource = 
        (org.hl7.fhir.dstu3.model.Encounter) encounterEntry.getResource();

    claimResource.setStatus(ClaimStatus.ACTIVE);
    claimResource.setUse(org.hl7.fhir.dstu3.model.Claim.Use.COMPLETE);

    // duration of encounter
    claimResource.setBillablePeriod(encounterResource.getPeriod());

    claimResource.setPatient(new Reference(personEntry.getFullUrl()));
    claimResource.setOrganization(encounterResource.getServiceProvider());

    // add item for encounter
    claimResource.addItem(new org.hl7.fhir.dstu3.model.Claim.ItemComponent(new PositiveIntType(1))
        .addEncounter(new Reference(encounterEntry.getFullUrl())));

    // add prescription.
    claimResource.setPrescription(new Reference(medicationEntry.getFullUrl()));

    Money moneyResource = new Money();
    moneyResource.setValue(claim.total());
    moneyResource.setCode("USD");
    moneyResource.setSystem("urn:iso:std:iso:4217");
    claimResource.setTotal(moneyResource);

    return newEntry(bundle, claimResource);
  }

  /**
   * Create an entry for the given Claim, associated to an Encounter.
   * 
   * @param personEntry
   *          Entry for the person
   * @param bundle
   *          The Bundle to add to
   * @param encounterEntry
   *          The current Encounter
   * @param claim
   *          the Claim object
   * @return the added Entry
   */
  private static BundleEntryComponent encounterClaim(BundleEntryComponent personEntry,
      Bundle bundle, BundleEntryComponent encounterEntry, Claim claim) {
    org.hl7.fhir.dstu3.model.Claim claimResource = new org.hl7.fhir.dstu3.model.Claim();
    org.hl7.fhir.dstu3.model.Encounter encounterResource = 
        (org.hl7.fhir.dstu3.model.Encounter) encounterEntry.getResource();
    claimResource.setStatus(ClaimStatus.ACTIVE);
    claimResource.setUse(org.hl7.fhir.dstu3.model.Claim.Use.COMPLETE);

    // duration of encounter
    claimResource.setBillablePeriod(encounterResource.getPeriod());

    claimResource.setPatient(new Reference(personEntry.getFullUrl()));
    claimResource.setOrganization(encounterResource.getServiceProvider());

    // add item for encounter
    claimResource.addItem(new ItemComponent(new PositiveIntType(1))
        .addEncounter(new Reference(encounterEntry.getFullUrl())));

    int itemSequence = 2;
    int conditionSequence = 1;
    int procedureSequence = 1;
    for (ClaimItem item : claim.items) {
      if (item.entry instanceof Procedure) {
        Type procedureReference = new Reference(item.entry.fullUrl);
        ProcedureComponent claimProcedure = new ProcedureComponent(
            new PositiveIntType(procedureSequence), procedureReference);
        claimResource.addProcedure(claimProcedure);

        // update claimItems list
        ItemComponent procedureItem = new ItemComponent(new PositiveIntType(itemSequence));
        procedureItem.addProcedureLinkId(procedureSequence);

        // calculate cost of procedure based on rvu values for a facility
        Money moneyResource = new Money();
        moneyResource.setCode("USD");
        moneyResource.setSystem("urn:iso:std:iso:4217");
        moneyResource.setValue(item.cost());
        procedureItem.setNet(moneyResource);
        claimResource.addItem(procedureItem);

        procedureSequence++;
      } else {
        // assume it's a Condition, we don't have a Condition class specifically
        // add diagnosisComponent to claim
        Reference diagnosisReference = new Reference(item.entry.fullUrl);
        org.hl7.fhir.dstu3.model.Claim.DiagnosisComponent diagnosisComponent = 
            new org.hl7.fhir.dstu3.model.Claim.DiagnosisComponent(
                new PositiveIntType(conditionSequence), diagnosisReference);
        claimResource.addDiagnosis(diagnosisComponent);

        // update claimItems with diagnosis
        ItemComponent diagnosisItem = new ItemComponent(new PositiveIntType(itemSequence));
        diagnosisItem.addDiagnosisLinkId(conditionSequence);
        claimResource.addItem(diagnosisItem);

        conditionSequence++;
      }
      itemSequence++;
    }

    Money moneyResource = new Money();
    moneyResource.setCode("USD");
    moneyResource.setSystem("urn:iso:std:iso:4217");
    moneyResource.setValue(claim.total());
    claimResource.setTotal(moneyResource);

    return newEntry(bundle, claimResource);
  }

  /**
   * Map the Condition into a FHIR Condition resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Entry for the Person
   * @param bundle
   *          The Bundle to add to
   * @param encounterEntry
   *          The current Encounter entry
   * @param condition
   *          The Condition
   * @return The added Entry
   */
  private static BundleEntryComponent condition(BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, HealthRecord.Entry condition) {
    Condition conditionResource = new Condition();

    conditionResource.setSubject(new Reference(personEntry.getFullUrl()));
    conditionResource.setContext(new Reference(encounterEntry.getFullUrl()));

    Code code = condition.codes.get(0);
    conditionResource.setCode(mapCodeToCodeableConcept(code, SNOMED_URI));

    conditionResource.setVerificationStatus(ConditionVerificationStatus.CONFIRMED);
    conditionResource.setClinicalStatus(ConditionClinicalStatus.ACTIVE);

    conditionResource.setOnset(convertFhirDateTime(condition.start, true));
    conditionResource.setAssertedDate(new Date(condition.start));

    if (condition.stop != 0) {
      conditionResource.setAbatement(convertFhirDateTime(condition.stop, true));
      conditionResource.setClinicalStatus(ConditionClinicalStatus.RESOLVED);
    }
    
    if (USE_SHR_EXTENSIONS) {
      // TODO: use different categories. would need to add a "category" to GMF Condition state
      // also potentially use Injury profile here, 
      // once different codes map to different categories
      
      conditionResource.addCategory(new CodeableConcept().addCoding(new Coding(
          "http://standardhealthrecord.org/shr/condition/vs/ConditionCategoryVS", "disease",
          "Disease")));
      conditionResource.setMeta(new Meta().addProfile(SHR_EXT + "shr-condition-Condition"));
      // required fields for this profile are clinicalStatus, assertedDate, category
    }

    BundleEntryComponent conditionEntry = newEntry(bundle, conditionResource);

    condition.fullUrl = conditionEntry.getFullUrl();

    return conditionEntry;
  }
  
  /**
   * Map the Condition into a FHIR AllergyIntolerance resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Entry for the Person
   * @param bundle
   *          The Bundle to add to
   * @param encounterEntry
   *          The current Encounter entry
   * @param allergy
   *          The Allergy Entry
   * @return The added Entry
   */
  private static BundleEntryComponent allergy(BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, HealthRecord.Entry allergy) {
    
    AllergyIntolerance allergyResource = new AllergyIntolerance();
    
    allergyResource.setAssertedDate(new Date(allergy.start));
    
    if (allergy.stop == 0) {
      allergyResource.setClinicalStatus(AllergyIntoleranceClinicalStatus.ACTIVE);
    } else {
      allergyResource.setClinicalStatus(AllergyIntoleranceClinicalStatus.INACTIVE);
    }
    
    allergyResource.setType(AllergyIntoleranceType.ALLERGY);
    AllergyIntoleranceCategory category = AllergyIntoleranceCategory.FOOD;
    allergyResource.addCategory(category); // TODO: allergy categories in GMF
    allergyResource.setCriticality(AllergyIntoleranceCriticality.LOW);
    allergyResource.setVerificationStatus(AllergyIntoleranceVerificationStatus.CONFIRMED);
    allergyResource.setPatient(new Reference(personEntry.getFullUrl()));
    Code code = allergy.codes.get(0);
    allergyResource.setCode(mapCodeToCodeableConcept(code, SNOMED_URI));
    
    if (USE_SHR_EXTENSIONS) {
      Meta meta = new Meta();
      meta.addProfile(SHR_EXT + "shr-allergy-AllergyIntolerance");
      // required fields for AllergyIntolerance profile are:
      // verificationStatus, code, patient, assertedDate
      allergyResource.setMeta(meta);
    }
    BundleEntryComponent allergyEntry = newEntry(bundle, allergyResource);
    allergy.fullUrl = allergyEntry.getFullUrl();
    return allergyEntry;
  }
  

  /**
   * Map the given Observation into a FHIR Observation resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Person Entry
   * @param bundle
   *          The Bundle to add to
   * @param encounterEntry
   *          The current Encounter entry
   * @param observation
   *          The Observation
   * @return The added Entry
   */
  private static BundleEntryComponent observation(BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Observation observation) {
    org.hl7.fhir.dstu3.model.Observation observationResource = 
        new org.hl7.fhir.dstu3.model.Observation();

    observationResource.setSubject(new Reference(personEntry.getFullUrl()));
    observationResource.setContext(new Reference(encounterEntry.getFullUrl()));

    observationResource.setStatus(ObservationStatus.FINAL);

    Code code = observation.codes.get(0);
    observationResource.setCode(mapCodeToCodeableConcept(code, LOINC_URI));

    observationResource.addCategory().addCoding().setCode(observation.category)
        .setSystem("http://hl7.org/fhir/observation-category").setDisplay(observation.category);

    if (observation.value != null) {
      Type value = mapValueToFHIRType(observation.value, observation.unit);
      observationResource.setValue(value);
    } else if (observation.observations != null && !observation.observations.isEmpty()) {
      // multi-observation (ex blood pressure)
      for (Observation subObs : observation.observations) {
        ObservationComponentComponent comp = new ObservationComponentComponent();
        comp.setCode(mapCodeToCodeableConcept(subObs.codes.get(0), LOINC_URI));
        Type value = mapValueToFHIRType(subObs.value, subObs.unit);
        comp.setValue(value);
        observationResource.addComponent(comp);
      }
    }

    observationResource.setEffective(convertFhirDateTime(observation.start, true));
    observationResource.setIssued(new Date(observation.start));
    
    if (USE_SHR_EXTENSIONS) {
      Meta meta = new Meta();
      meta.addProfile(SHR_EXT + "shr-finding-Observation"); // all Observations are Observations
      if ("vital-signs".equals(observation.category)) {
        meta.addProfile(SHR_EXT + "shr-vital-VitalSign");
      }
      // add the specific profile based on code
      String codeMappingUri = SHR_MAPPING.get(LOINC_URI, code.code);
      if (codeMappingUri != null) {
        meta.addProfile(codeMappingUri);
      }
      
      observationResource.setMeta(meta);
    }

    BundleEntryComponent entry = newEntry(bundle, observationResource);
    observation.fullUrl = entry.getFullUrl();
    return entry;
  }
  
  private static Type mapValueToFHIRType(Object value, String unit) {
    if (value == null) {
      return null;
      
    } else if (value instanceof Condition) {
      Code conditionCode = ((HealthRecord.Entry) value).codes.get(0);
      return mapCodeToCodeableConcept(conditionCode, SNOMED_URI);
      
    } else if (value instanceof Code) {
      return mapCodeToCodeableConcept((Code) value, SNOMED_URI);
      
    } else if (value instanceof String) {
      return new StringType((String) value);
      
    } else if (value instanceof Number) {
      return new Quantity().setValue(((Number) value).doubleValue())
          .setCode(unit).setSystem(UNITSOFMEASURE_URI)
          .setUnit(unit);
      
    } else {
      throw new IllegalArgumentException("unexpected observation value class: "
          + value.getClass().toString() + "; " + value);
    }
  }

  /**
   * Map the given Procedure into a FHIR Procedure resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Person entry
   * @param bundle
   *          Bundle to add to
   * @param encounterEntry
   *          The current Encounter entry
   * @param procedure
   *          The Procedure
   * @return The added Entry
   */
  private static BundleEntryComponent procedure(BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Procedure procedure) {
    org.hl7.fhir.dstu3.model.Procedure procedureResource = new org.hl7.fhir.dstu3.model.Procedure();

    procedureResource.setStatus(ProcedureStatus.COMPLETED);
    procedureResource.setSubject(new Reference(personEntry.getFullUrl()));
    procedureResource.setContext(new Reference(encounterEntry.getFullUrl()));

    Code code = procedure.codes.get(0);
    CodeableConcept procCode = mapCodeToCodeableConcept(code, SNOMED_URI);
    procedureResource.setCode(procCode);

    if (procedure.stop != 0L) {
      Date startDate = new Date(procedure.start);
      Date endDate = new Date(procedure.stop);
      procedureResource.setPerformed(new Period().setStart(startDate).setEnd(endDate));
    } else {
      procedureResource.setPerformed(convertFhirDateTime(procedure.start, true));
    }
    
    if (!procedure.reasons.isEmpty()) {
      Code reason = procedure.reasons.get(0); // Only one element in list
      for (BundleEntryComponent entry : bundle.getEntry()) {
        if (entry.getResource().fhirType().equals("Condition")) {
          Condition condition = (Condition) entry.getResource();
          Coding coding = condition.getCode().getCoding().get(0); // Only one element in list
          if (reason.code.equals(coding.getCode())) {
            procedureResource.addReasonReference().setReference(entry.getFullUrl())
                .setDisplay(reason.display);
          }
        }
      }
    }
    
    if (USE_SHR_EXTENSIONS) {
      procedureResource.setMeta(
          new Meta().addProfile(SHR_EXT + "shr-procedure-ProcedurePerformed"));
      // required fields for this profile are action-PerformedContext-extension,
      // status, code, subject, performed[x]
      
      Extension performedContext = new Extension();
      performedContext.setUrl(SHR_EXT + "shr-action-PerformedContext-extension");
      performedContext.addExtension(
          SHR_EXT + "shr-action-Status-extension", 
          new CodeType("completed"));
      
      procedureResource.addExtension(performedContext);
    }

    BundleEntryComponent procedureEntry = newEntry(bundle, procedureResource);

    procedure.fullUrl = procedureEntry.getFullUrl();

    return procedureEntry;
  }

  private static BundleEntryComponent immunization(BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, HealthRecord.Entry immunization) {
    Immunization immResource = new Immunization();
    immResource.setStatus(ImmunizationStatus.COMPLETED);
    immResource.setDate(new Date(immunization.start));
    immResource.setVaccineCode(mapCodeToCodeableConcept(immunization.codes.get(0), CVX_URI));
    immResource.setNotGiven(false);
    immResource.setPrimarySource(true);
    immResource.setPatient(new Reference(personEntry.getFullUrl()));
    immResource.setEncounter(new Reference(encounterEntry.getFullUrl()));
    
    if (USE_SHR_EXTENSIONS) {
      immResource.setMeta(new Meta().addProfile(SHR_EXT + "shr-immunization-ImmunizationGiven"));
      // profile requires action-PerformedContext-extension, status, notGiven, vaccineCode, patient,
      // date, primarySource
      
      Extension performedContext = new Extension();
      performedContext.setUrl(SHR_EXT + "shr-action-PerformedContext-extension");
      performedContext.addExtension(
          SHR_EXT + "shr-action-Status-extension", 
          new CodeType("completed"));
      
      immResource.addExtension(performedContext);
    }
    
    return newEntry(bundle, immResource);
  }

  /**
   * Map the given Medication to a FHIR MedicationRequest resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Entry for the Person
   * @param bundle
   *          Bundle to add the Medication to
   * @param encounterEntry
   *          Current Encounter entry
   * @param medication
   *          The Medication
   * @return The added Entry
   */
  private static BundleEntryComponent medication(BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Medication medication) {
    MedicationRequest medicationResource = new MedicationRequest();

    medicationResource.setSubject(new Reference(personEntry.getFullUrl()));
    medicationResource.setContext(new Reference(encounterEntry.getFullUrl()));

    medicationResource.setMedication(mapCodeToCodeableConcept(medication.codes.get(0), RXNORM_URI));

    medicationResource.setAuthoredOn(new Date(medication.start));
    medicationResource.setIntent(MedicationRequestIntent.ORDER);

    if (medication.stop != 0L) {
      medicationResource.setStatus(MedicationRequestStatus.STOPPED);
    } else {
      medicationResource.setStatus(MedicationRequestStatus.ACTIVE);
    }
    
    if (!medication.reasons.isEmpty()) {
      // Only one element in list
      Code reason = medication.reasons.get(0);
      for (BundleEntryComponent entry : bundle.getEntry()) {
        if (entry.getResource().fhirType().equals("Condition")) {
          Condition condition = (Condition) entry.getResource();
          // Only one element in list
          Coding coding = condition.getCode().getCoding().get(0);
          if (reason.code.equals(coding.getCode())) {
            medicationResource.addReasonReference()
                .setReference(entry.getFullUrl());
          }
        }
      }
    }
    
    if (medication.prescriptionDetails != null) {
      JsonObject rxInfo = medication.prescriptionDetails;
      Dosage dosage = new Dosage();
    
      dosage.setSequence(1);
      // as_needed is true if present
      dosage.setAsNeeded(new BooleanType(rxInfo.has("as_needed")));
    
      // as_needed is true if present
      if ((rxInfo.has("dosage")) && (!rxInfo.has("as_needed"))) {
        Timing timing = new Timing();
        TimingRepeatComponent timingRepeatComponent = new TimingRepeatComponent();
        timingRepeatComponent.setFrequency(
            rxInfo.get("dosage").getAsJsonObject().get("frequency").getAsInt());
        timingRepeatComponent.setPeriod(
            rxInfo.get("dosage").getAsJsonObject().get("period").getAsDouble());
        timingRepeatComponent.setPeriodUnit(
            convertUcumCode(rxInfo.get("dosage").getAsJsonObject().get("unit").getAsString()));
        timing.setRepeat(timingRepeatComponent);
        dosage.setTiming(timing);
    
        Quantity dose = new SimpleQuantity().setValue(
            rxInfo.get("dosage").getAsJsonObject().get("amount").getAsDouble());
        dosage.setDose(dose);
    
        if (rxInfo.has("instructions")) {
          for (JsonElement instructionElement : rxInfo.get("instructions").getAsJsonArray()) {
            JsonObject instruction = instructionElement.getAsJsonObject();
            Code instructionCode = new Code(
                SNOMED_URI,
                instruction.get("code").getAsString(),
                instruction.get("display").getAsString()
            );
    
            dosage.addAdditionalInstruction(mapCodeToCodeableConcept(instructionCode, SNOMED_URI));
          }
        }
      }
    
      List<Dosage> dosageInstruction = new ArrayList<Dosage>();
      dosageInstruction.add(dosage);
      medicationResource.setDosageInstruction(dosageInstruction);
    }
    
    if (USE_SHR_EXTENSIONS) {
      
      medicationResource.addExtension()
        .setUrl(SHR_EXT + "shr-base-ActionCode-extension")
        .setValue(PRESCRIPTION_OF_DRUG_CC);

      medicationResource.setMeta(new Meta()
          .addProfile(SHR_EXT + "shr-medication-MedicationRequested"));
      // required fields for this profile are status, action-RequestedContext-extension,
      // medication[x]subject, authoredOn, requester
      
      Extension requestedContext = new Extension();
      requestedContext.setUrl(SHR_EXT + "shr-action-RequestedContext-extension");
      requestedContext.addExtension(
          SHR_EXT + "shr-action-Status-extension", 
          new CodeType("completed"));
      requestedContext.addExtension(
          SHR_EXT + "shr-action-RequestIntent-extension",
          new CodeType("original-order"));
      
      medicationResource.addExtension(requestedContext);
    }
    

    BundleEntryComponent medicationEntry = newEntry(bundle, medicationResource);
    // create new claim for medication
    medicationClaim(personEntry, bundle, encounterEntry, medication.claim, medicationEntry);

    return medicationEntry;
  }
  
  private static final Code PRESCRIPTION_OF_DRUG_CODE =
      new Code("SNOMED-CT","33633005","Prescription of drug (procedure)");
  private static final CodeableConcept PRESCRIPTION_OF_DRUG_CC =
      mapCodeToCodeableConcept(PRESCRIPTION_OF_DRUG_CODE, SNOMED_URI);
  

  /**
   * Map the given Report to a FHIR DiagnosticReport resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Entry for the Person
   * @param bundle
   *          Bundle to add the Report to
   * @param encounterEntry
   *          Current Encounter entry
   * @param report
   *          The Report
   * @return The added Entry
   */
  private static BundleEntryComponent report(BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, Report report) {
    DiagnosticReport reportResource = new DiagnosticReport();
    reportResource.setStatus(DiagnosticReportStatus.FINAL);
    reportResource.setCode(mapCodeToCodeableConcept(report.codes.get(0), LOINC_URI));
    reportResource.setSubject(new Reference(personEntry.getFullUrl()));
    reportResource.setContext(new Reference(encounterEntry.getFullUrl()));
    reportResource.setEffective(convertFhirDateTime(report.start, true));
    reportResource.setIssued(new Date(report.start));
    for (Observation observation : report.observations) {
      Reference reference = new Reference(observation.fullUrl);
      reference.setDisplay(observation.codes.get(0).display);
      reportResource.addResult(reference);
    }

    // no SHR profile for DiagnosticReport
    
    return newEntry(bundle, reportResource);
  }

  /**
   * Map the given CarePlan to a FHIR CarePlan resource, and add it to the given Bundle.
   * 
   * @param personEntry
   *          The Entry for the Person
   * @param bundle
   *          Bundle to add the CarePlan to
   * @param encounterEntry
   *          Current Encounter entry
   * @param carePlan
   *          The CarePlan to map to FHIR and add to the bundle
   * @return The added Entry
   */
  private static BundleEntryComponent careplan(BundleEntryComponent personEntry, Bundle bundle,
      BundleEntryComponent encounterEntry, CarePlan carePlan) {
    org.hl7.fhir.dstu3.model.CarePlan careplanResource = new org.hl7.fhir.dstu3.model.CarePlan();
    careplanResource.setIntent(CarePlanIntent.ORDER);
    careplanResource.setSubject(new Reference(personEntry.getFullUrl()));
    careplanResource.setContext(new Reference(encounterEntry.getFullUrl()));
  
    Code code = carePlan.codes.get(0);
    careplanResource.addCategory(mapCodeToCodeableConcept(code, SNOMED_URI));
  
    CarePlanActivityStatus activityStatus;
    GoalStatus goalStatus;
  
    Period period = new Period().setStart(new Date(carePlan.start));
    careplanResource.setPeriod(period);
    if (carePlan.stop != 0L) {
      period.setEnd(new Date(carePlan.stop));
      careplanResource.setStatus(CarePlanStatus.COMPLETED);
      activityStatus = CarePlanActivityStatus.COMPLETED;
      goalStatus = GoalStatus.ACHIEVED;
    } else {
      careplanResource.setStatus(CarePlanStatus.ACTIVE);
      activityStatus = CarePlanActivityStatus.INPROGRESS;
      goalStatus = GoalStatus.INPROGRESS;
    }
  
    if (!carePlan.activities.isEmpty()) {
      for (Code activity : carePlan.activities) {
        CarePlanActivityComponent activityComponent = new CarePlanActivityComponent();
        CarePlanActivityDetailComponent activityDetailComponent =
            new CarePlanActivityDetailComponent();

        activityDetailComponent.setStatus(activityStatus);

        activityDetailComponent.setCode(mapCodeToCodeableConcept(activity, SNOMED_URI));
        activityComponent.setDetail(activityDetailComponent);

        careplanResource.addActivity(activityComponent);
      }
    }

    if (!carePlan.reasons.isEmpty()) {
      // Only one element in list
      Code reason = carePlan.reasons.get(0);
      for (BundleEntryComponent entry : bundle.getEntry()) {
        if (entry.getResource().fhirType().equals("Condition")) {
          Condition condition = (Condition) entry.getResource();
          // Only one element in list
          Coding coding = condition.getCode().getCoding().get(0);
          if (reason.code.equals(coding.getCode())) {
            careplanResource.addAddresses().setReference(entry.getFullUrl());
          }
        }
      }
    }

    for (JsonObject goal : carePlan.goals) {
      BundleEntryComponent goalEntry = caregoal(bundle, goalStatus, goal);
      careplanResource.addGoal().setReference(goalEntry.getFullUrl());
    }

    return newEntry(bundle, careplanResource);
  }

  /**
   * Map the Provider into a FHIR Organization resource, and add it to the given Bundle.
   * @param bundle The Bundle to add to
   * @param provider The Provider
   * @return The added Entry
   */
  private static BundleEntryComponent provider(Bundle bundle, Provider provider) {
    org.hl7.fhir.dstu3.model.Organization organizationResource =
        new org.hl7.fhir.dstu3.model.Organization();
  
    List<CodeableConcept> organizationType = new ArrayList<CodeableConcept>();
    organizationType.add(
        mapCodeToCodeableConcept(
            new Code(
                "http://hl7.org/fhir/ValueSet/organization-type",
                "prov",
                "Healthcare Provider"),
            "Healthcare Provider"));
  
    Map<String,?> attr = provider.getAttributes();
    
    organizationResource.setId(provider.getResourceID());
    organizationResource.setName((String)attr.get("name"));
    organizationResource.setType(organizationType);
    
    Address address = new Address()
        .addLine(attr.get("address").toString())
        .setCity(attr.get("city").toString())
        .setPostalCode(attr.get("city_zip").toString())
        .setState(attr.get("state").toString())
        .setCountry("US");
    organizationResource.addAddress(address);
    
    if (USE_SHR_EXTENSIONS) {
      organizationResource.setMeta(new Meta().addProfile(SHR_EXT + "shr-entity-Organization"));
      // required fields for this profile are identifier, type, address, and contact
      
      organizationResource.addIdentifier()
          .setSystem("urn:ietf:rfc:3986")
          .setValue(provider.getResourceID());
      organizationResource.addContact().setName(new HumanName().setText("Synthetic Provider"));
    }
  
    return newEntry(bundle, organizationResource);
  }
   
  /*
   * Map the JsonObject into a FHIR Goal resource, and add it to the given Bundle.
   * @param bundle The Bundle to add to
   * @param goalStatus The GoalStatus
   * @param goal The JsonObject
   * @return The added Entry
   */
  private static BundleEntryComponent caregoal(
      Bundle bundle, GoalStatus goalStatus, JsonObject goal) {
    String resourceID = UUID.randomUUID().toString();
  
    org.hl7.fhir.dstu3.model.Goal goalResource =
        new org.hl7.fhir.dstu3.model.Goal();
    goalResource.setStatus(goalStatus);
    goalResource.setId(resourceID);
  
    if (goal.has("text")) {
      CodeableConcept descriptionCodeableConcept = new CodeableConcept();
  
      descriptionCodeableConcept.setText(goal.get("text").getAsString());
      goalResource.setDescription(descriptionCodeableConcept);
    } else if (goal.has("codes")) {
      CodeableConcept descriptionCodeableConcept = new CodeableConcept();
  
      JsonObject code =
          goal.get("codes").getAsJsonArray().get(0).getAsJsonObject();
      descriptionCodeableConcept.addCoding()
        .setSystem(LOINC_URI)
        .setCode(code.get("code").getAsString())
        .setDisplay(code.get("display").getAsString());
  
      descriptionCodeableConcept.setText(code.get("display").getAsString());
      goalResource.setDescription(descriptionCodeableConcept);
    } else if (goal.has("observation")) {
      CodeableConcept descriptionCodeableConcept = new CodeableConcept();
  
      // build up our own text from the observation condition, similar to the graphviz logic
      JsonObject logic = goal.get("observation").getAsJsonObject();
  
      String[] text = {
        logic.get("codes").getAsJsonArray().get(0)
            .getAsJsonObject().get("display").getAsString(),
        logic.get("operator").getAsString(),
        logic.get("value").getAsString()
      };
  
      descriptionCodeableConcept.setText(String.join(" ", text));
      goalResource.setDescription(descriptionCodeableConcept);
    }
  
    if (goal.has("addresses")) {
      for (JsonElement reasonElement : goal.get("addresses").getAsJsonArray()) {
        if (reasonElement instanceof JsonObject) {
          JsonObject reasonObject = reasonElement.getAsJsonObject();
          String reasonCode =
              reasonObject.get("codes")
                  .getAsJsonObject()
                  .get("SNOMED-CT")
                  .getAsJsonArray()
                  .get(0)
                  .getAsString();
  
          for (BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource().fhirType().equals("Condition")) {
              Condition condition = (Condition) entry.getResource();
              // Only one element in list
              Coding coding = condition.getCode().getCoding().get(0);
              if (reasonCode.equals(coding.getCode())) {
                goalResource.addAddresses()
                    .setReference(entry.getFullUrl());
              }
            }
          }
        }
      }
    }
  
    return newEntry(bundle, goalResource);
  }
   
  /**
   * Convert the unit into a UnitsOfTime.
   *
   * @param unit unit String
   * @return a UnitsOfTime representing the given unit
   */
  private static UnitsOfTime convertUcumCode(String unit) {
    // From: http://hl7.org/fhir/ValueSet/units-of-time
    switch (unit) {
      case "seconds":
        return UnitsOfTime.S;
      case "minutes":
        return UnitsOfTime.MIN;
      case "hours":
        return UnitsOfTime.H;
      case "days":
        return UnitsOfTime.D;
      case "weeks":
        return UnitsOfTime.WK;
      case "months":
        return UnitsOfTime.MO;
      case "years":
        return UnitsOfTime.A;
      default:
        return null;
    }
  }
    
  /**
   * Convert the timestamp into a FHIR DateType or DateTimeType.
   * 
   * @param datetime
   *          Timestamp
   * @param time
   *          If true, return a DateTime; if false, return a Date.
   * @return a DateType or DateTimeType representing the given timestamp
   */
  private static Type convertFhirDateTime(long datetime, boolean time) {
    Date date = new Date(datetime);

    if (time) {
      return new DateTimeType(date);
    } else {
      return new DateType(date);
    }
  }

  /**
   * Helper function to convert a Code into a CodeableConcept. Takes an optional system, which
   * replaces the Code.system in the resulting CodeableConcept if not null.
   * 
   * @param from
   *          The Code to create a CodeableConcept from.
   * @param system
   *          The system identifier, such as a URI. Optional; may be null.
   * @return The converted CodeableConcept
   */
  private static CodeableConcept mapCodeToCodeableConcept(Code from, String system) {
    CodeableConcept to = new CodeableConcept();

    if (from.display != null) {
      to.setText(from.display);
    }

    Coding coding = new Coding();
    coding.setCode(from.code);
    coding.setDisplay(from.display);
    if (system == null) {
      coding.setSystem(from.system);
    } else {
      coding.setSystem(system);
    }

    to.addCoding(coding);

    return to;
  }

  /**
   * Helper function to create an Entry for the given Resource within the given Bundle. Sets the
   * resourceID to a random UUID, sets the entry's fullURL to that resourceID, and adds the entry to
   * the bundle.
   * 
   * @param bundle
   *          The Bundle to add the Entry to
   * @param resource
   *          Resource the new Entry should contain
   * @return the created Entry
   */
  private static BundleEntryComponent newEntry(Bundle bundle, Resource resource) {
    BundleEntryComponent entry = bundle.addEntry();

    String resourceID = UUID.randomUUID().toString();
    resource.setId(resourceID);
    entry.setFullUrl("urn:uuid:" + resourceID);

    entry.setResource(resource);

    return entry;
  }
}
