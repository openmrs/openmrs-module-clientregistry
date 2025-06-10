package org.openmrs.module.clientregistry.providers.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.module.clientregistry.api.CRPatientService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.PatientSearchParams;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class FhirCRPatientResourceProviderTest {
	
	private static final String PATIENT_UUID = "01v312b1-cfv6-43ab-ae87-24070c801d1b";
	
	private static final String WRONG_PATIENT_UUID = "417312c1-cx56-b3ab-aeb7-41070b401d1p";
	
	private static final String NAME = "John";
	
	private static final String GIVEN_NAME = "Smith";
	
	private static final String FAMILY_NAME = "Doe";
	
	private static final String GENDER = "male";
	
	private static final String IDENTIFIER = "F10000";
	
	private static final String BIRTH_DATE = "1967-02-04";
	
	private static final String CITY = "Los Angeles";
	
	private static final String STATE = "California";
	
	private static final String COUNTRY = "USA";
	
	private static final String POSTAL_CODE = "90210";
	
	private static final String LAST_UPDATED_DATE = "2020-12-15";
	
	@Mock
	private CRPatientService crService;
	
	private FhirCRPatientResourceProvider resourceProvider;
	
	private Patient patient;
	
	@Before
	public void setup() {
		resourceProvider = new FhirCRPatientResourceProvider();
		resourceProvider.setCrService(crService);
		
		// init patient
		HumanName name = new HumanName();
		name.addGiven(GIVEN_NAME);
		name.setFamily(FAMILY_NAME);
		name.addGiven(NAME);
		patient = new Patient();
		patient.setId(PATIENT_UUID);
		patient.addName(name);
		patient.setActive(true);
		patient.setBirthDate(parseDate(BIRTH_DATE));
		patient.setDeceased(new BooleanType(true));
		patient.setGender(Enumerations.AdministrativeGender.MALE);
	}
	
	@Test
	public void getPatientById_shouldGetPatientById() {
		IdType id = new IdType();
		id.setValue(PATIENT_UUID);
		when(crService.getPatientById(PATIENT_UUID)).thenReturn(patient);
		
		Patient result = resourceProvider.getPatientById(id);
		assertThat(result.isResource(), is(true));
		assertThat(result.getId(), equalTo(PATIENT_UUID));
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void getPatientById_shouldThrowAnExceptionIfPatientWasNotFound() {
		IdType idType = new IdType();
		idType.setValue(WRONG_PATIENT_UUID);
		assertThat(resourceProvider.getPatientById(idType), nullValue());
	}
	
	@Test
	public void createPatient_shouldCreateNewPatient() {
		when(crService.createPatient(patient)).thenReturn(patient);
		
		MethodOutcome result = resourceProvider.createPatient(patient);
		
		assertThat(result, notNullValue());
		assertThat(result.getResource(), equalTo(patient));
	}
	
	@Test
	public void updatePatient_shouldUpdateRequestedPatient() {
		when(crService.updatePatient(patient)).thenReturn(patient);
		
		MethodOutcome result = resourceProvider.updatePatient(new IdType().setValue(PATIENT_UUID), patient);
		
		assertThat(result, notNullValue());
		assertThat(result.getResource(), equalTo(patient));
	}
	
	@Test
	public void deletePatient_shouldDeleteRequestedPatient() {
		OperationOutcome result = resourceProvider.deletePatient(patient);
		
		assertThat(result, notNullValue());
		assertThat(result.getIssueFirstRep().getSeverity(), equalTo(OperationOutcome.IssueSeverity.INFORMATION));
		assertThat(result.getIssueFirstRep().getDetails().getCodingFirstRep().getCode(), equalTo("MSG_DELETED"));
		assertThat(result.getIssueFirstRep().getDetails().getCodingFirstRep().getDisplay(),
		    equalTo("This resource has been deleted"));
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByName() {
		StringAndListParam nameParam = createStringParam(NAME);
		when(
		    crService.searchPatients(new PatientSearchParams(nameParam, null, null, null, null, null, null, null, null,
		            null, null, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(nameParam, null, null, null, null, null, null, null, null,
		    null, null, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByGivenName() {
		StringAndListParam givenNameParam = createStringParam(NAME);
		when(
		    crService.searchPatients(new PatientSearchParams(null, givenNameParam, null, null, null, null, null, null, null,
		            null, null, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, givenNameParam, null, null, null, null, null, null,
		    null, null, null, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByFamilyName() {
		StringAndListParam familyNameParam = createStringParam(FAMILY_NAME);
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, familyNameParam, null, null, null, null, null,
		            null, null, null, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, familyNameParam, null, null, null, null, null,
		    null, null, null, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByIdentifier() {
		TokenAndListParam identifierParam = new TokenAndListParam().addAnd(new TokenOrListParam().add(IDENTIFIER));
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, identifierParam, null, null, null, null,
		            null, null, null, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, identifierParam, null, null, null, null,
		    null, null, null, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByGender() {
		TokenAndListParam genderParam = new TokenAndListParam().addAnd(new TokenOrListParam().add(GENDER));
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, null, genderParam, null, null, null, null,
		            null, null, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, null, genderParam, null, null, null,
		    null, null, null, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByBirthDate() {
		DateRangeParam birthDateParam = new DateRangeParam().setLowerBound(BIRTH_DATE).setUpperBound(BIRTH_DATE);
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, null, null, birthDateParam, null, null, null,
		            null, null, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, null, null, birthDateParam, null, null,
		    null, null, null, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByDeceased() {
		TokenAndListParam deceasedParam = new TokenAndListParam().addAnd(new TokenOrListParam().add("true"));
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, null, null, null, null, deceasedParam, null,
		            null, null, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, null, null, null, null, deceasedParam,
		    null, null, null, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByCity() {
		StringAndListParam cityParam = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(CITY)));
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, null, null, null, null, null, cityParam,
		            null, null, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, null, null, null, null, null, cityParam,
		    null, null, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByState() {
		StringAndListParam stateParam = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(STATE)));
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, null, null, null, null, null, null,
		            stateParam, null, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, null, null, null, null, null, null,
		    stateParam, null, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByPostalCode() {
		StringAndListParam postalCodeParam = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(
		        POSTAL_CODE)));
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, null, null, null, null, null, null, null,
		            postalCodeParam, null, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, null, null, null, null, null, null,
		    null, postalCodeParam, null, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByCountry() {
		StringAndListParam countryParam = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(
		        COUNTRY)));
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, null, null, null, null, null, null, null,
		            null, countryParam, null, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, null, null, null, null, null, null,
		    null, null, countryParam, null, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByUUID() {
		TokenAndListParam uuid = new TokenAndListParam().addAnd(new TokenParam(PATIENT_UUID));
		
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, null, null, null, null, null, null, null,
		            null, null, uuid, null, null, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, null, null, null, null, null, null,
		    null, null, null, uuid, null, null);
		
		verifySinglePatientResult(results);
	}
	
	@Test
	public void searchPatients_shouldReturnMatchingBundleOfPatientsByLastUpdated() {
		DateRangeParam lastUpdated = new DateRangeParam().setLowerBound(LAST_UPDATED_DATE).setUpperBound(LAST_UPDATED_DATE);
		
		when(
		    crService.searchPatients(new PatientSearchParams(null, null, null, null, null, null, null, null, null, null,
		            null, null, null, null, lastUpdated, null, null))).thenReturn(getBundleProvider(patient));
		
		IBundleProvider results = resourceProvider.searchPatients(null, null, null, null, null, null, null, null, null,
		    null, null, null, null, lastUpdated, null);
		
		verifySinglePatientResult(results);
	}
	
	private void verifySinglePatientResult(IBundleProvider results) {
		List<IBaseResource> resources = results.getResources(0, 10);
		
		assertThat(resources, notNullValue());
		assertThat(resources, hasSize(equalTo(1)));
		assertThat(resources.get(0).fhirType(), is(FhirConstants.PATIENT));
		assertThat(resources.get(0).getIdElement().getIdPart(), is(PATIENT_UUID));
	}
	
	private StringAndListParam createStringParam(String paramValue) {
		return new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(paramValue)));
	}
	
	private IBundleProvider getBundleProvider(Patient patient) {
		return getBundleProvider(Collections.singletonList(patient));
	}
	
	private IBundleProvider getBundleProvider(List<Patient> patientList) {
		return new SimpleBundleProvider(patientList);
	}
	
	private Date parseDate(String dateString) {
		Date parsed = null;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			parsed = formatter.parse(dateString);
		}
		catch (Exception ignored) {}
		return parsed;
	}
	
}
