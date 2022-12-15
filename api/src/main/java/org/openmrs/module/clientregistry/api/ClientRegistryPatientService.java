package org.openmrs.module.clientregistry.api;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.search.param.PatientSearchParams;

public interface ClientRegistryPatientService {
	
	IBundleProvider searchForPatients(PatientSearchParams patientSearchParams);

	Patient getPatient(String identifier);
}
