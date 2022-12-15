package org.openmrs.module.clientregistry.api.providers.r4;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import lombok.AccessLevel;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.module.clientregistry.api.ClientRegistryManager;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.api.search.param.PatientSearchParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.HashSet;

@Component("patientFhirR4ResourceProvider")
@R4Provider
@Setter(AccessLevel.PACKAGE)
public class PatientFhirResourceProvider implements IResourceProvider {

    @Autowired
    private ClientRegistryManager clientRegistryManager;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Patient.class;
    }

    @Read
    public Patient getClientRegistryPatientById(@IdParam @Nonnull IdType id) {
        return clientRegistryManager.getPatientService().getPatient(id.getIdPart());
    }

    @Search
    public IBundleProvider searchClientRegistryPatients(@OptionalParam(name = Patient.SP_NAME) StringAndListParam name,
                                                        @OptionalParam(name = Patient.SP_GIVEN) StringAndListParam given,
                                                        @OptionalParam(name = Patient.SP_FAMILY) StringAndListParam family,
                                                        @OptionalParam(name = Patient.SP_IDENTIFIER) TokenAndListParam identifier,
                                                        @OptionalParam(name = Patient.SP_GENDER) TokenAndListParam gender,
                                                        @OptionalParam(name = Patient.SP_BIRTHDATE) DateRangeParam birthDate,
                                                        @OptionalParam(name = Patient.SP_DEATH_DATE) DateRangeParam deathDate,
                                                        @OptionalParam(name = Patient.SP_DECEASED) TokenAndListParam deceased,
                                                        @OptionalParam(name = Patient.SP_ADDRESS_CITY) StringAndListParam city,
                                                        @OptionalParam(name = Patient.SP_ADDRESS_STATE) StringAndListParam state,
                                                        @OptionalParam(name = Patient.SP_ADDRESS_POSTALCODE) StringAndListParam postalCode,
                                                        @OptionalParam(name = Patient.SP_ADDRESS_COUNTRY) StringAndListParam country,
                                                        @OptionalParam(name = Patient.SP_RES_ID) TokenAndListParam id,
                                                        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
                                                        @IncludeParam(reverse = true, allow = {"Observation:" + Observation.SP_PATIENT,
                                                                "AllergyIntolerance:" + AllergyIntolerance.SP_PATIENT, "DiagnosticReport:" + DiagnosticReport.SP_PATIENT,
                                                                "Encounter:" + Encounter.SP_PATIENT, "MedicationRequest:" + MedicationRequest.SP_PATIENT,
                                                                "ServiceRequest:" + ServiceRequest.SP_PATIENT}) HashSet<Include> revIncludes) {
        return clientRegistryManager.getPatientService().searchForPatients(new PatientSearchParams(name, given, family, identifier, gender, birthDate, deathDate, deceased, city, state, postalCode, country, id, lastUpdated, sort, revIncludes));
    }
}
