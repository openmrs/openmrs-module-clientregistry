package org.openmrs.module.clientregistry.api.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.clientregistry.api.ClientRegistryPatientService;
import org.openmrs.module.fhir2.api.search.param.PatientSearchParams;

public class FhirPatientServiceImpl implements ClientRegistryPatientService {
	@Override
	public IBundleProvider searchForPatients(PatientSearchParams patientSearchParams) {
		return null;
	}

	@Override
	public Patient getPatient(String identifier) {
		return null;
	}
}
