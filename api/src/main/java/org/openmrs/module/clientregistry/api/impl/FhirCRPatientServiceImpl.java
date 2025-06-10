package org.openmrs.module.clientregistry.api.impl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.module.clientregistry.api.CRPatientService;
import org.openmrs.module.clientregistry.api.search.CRSearchBundleProvider;
import org.openmrs.module.clientregistry.api.search.PatientSearchCriteriaBuilder;
import org.openmrs.module.clientregistry.providers.FhirCRConstants;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.search.param.PatientSearchParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInputAndPartialOutput;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;

@Component
public class FhirCRPatientServiceImpl implements CRPatientService {
	
	@Autowired
	@Qualifier("clientRegistryFhirClient")
	private IGenericClient fhirClient;
	
	@Autowired
	private PatientSearchCriteriaBuilder criteriaBuilder;
	
	@Autowired
	private FhirGlobalPropertyService globalPropertyService;
	
	@Override
	public Patient getPatientById(String id) {
		if (StringUtils.isBlank(id)) {
			return null;
		}
		return fhirClient.read().resource(Patient.class).withId(id).execute();
	}
	
	@Override
	public IBundleProvider getPatientsByPIX(String sourceIdentifier, String sourceIdentifierSystem,
			List<String> targetSystems) {
		// construct request to external FHIR $ihe-pix endpoint
		IOperationUntypedWithInputAndPartialOutput<Parameters> identifiersRequest = fhirClient.operation()
				.onType(FhirConstants.PATIENT).named(FhirCRConstants.IHE_PIX_OPERATION)
				.withSearchParameter(Parameters.class,
						FhirCRConstants.SOURCE_IDENTIFIER, new TokenParam(sourceIdentifierSystem, sourceIdentifier));

		if (!targetSystems.isEmpty()) {
			identifiersRequest.andSearchParameter(FhirCRConstants.TARGET_SYSTEM,
					new StringParam(String.join(",", targetSystems)));
		}

		Parameters crMatchingParams = identifiersRequest.useHttpGet().execute();
		List<String> crIdentifiers = crMatchingParams.getParameter().stream()
				.filter(param -> Objects.equals(param.getName(), "targetId"))
				.map(param -> ((Reference) param.getValue()).getReference()).collect(Collectors.toList());

		if (crIdentifiers.isEmpty()) {
			return new CRSearchBundleProvider(Collections.emptyList());
		}
		String defaultPageSize = globalPropertyService.getGlobalProperty(
			FhirConstants.OPENMRS_FHIR_DEFAULT_PAGE_SIZE, 
			"10"
		);
		Bundle patientBundle = fhirClient.search().forResource(Patient.class)
				.where(new StringClientParam(Patient.SP_RES_ID).matches().values(crIdentifiers))
				.count(Integer.parseInt(defaultPageSize))
				.returnBundle(Bundle.class)
				.execute();

		return new CRSearchBundleProvider(parseCRPatientSearchResults(patientBundle));

	}
	
	@Override
	public IBundleProvider searchPatients(PatientSearchParams patientSearchParams) {
		List<ICriterion<?>> criterions = criteriaBuilder.buildCriteria(patientSearchParams);
		IQuery<IBaseBundle> query = fhirClient.search().forResource(Patient.class);
		
		for (int i = 0; i < criterions.size(); i++) {
			ICriterion<?> criterion = criterions.get(i);
			if (i == 0) {
				query.where(criterion);
			} else {
				query.and(criterion);
			}
		}
		
		String defaultPageSize = globalPropertyService.getGlobalProperty(FhirConstants.OPENMRS_FHIR_DEFAULT_PAGE_SIZE, "10");
		query.count(Integer.parseInt(defaultPageSize));
		
		Bundle patientBundle = query.returnBundle(Bundle.class).execute();
		return new CRSearchBundleProvider(parseCRPatientSearchResults(patientBundle));
	}
	
	@Override
	public Patient createPatient(Patient patient) {
		return (Patient) fhirClient.create().resource(patient).execute().getResource();
	}
	
	@Override
	public Patient updatePatient(Patient patient) {
		return (Patient) fhirClient.update().resource(patient).execute().getResource();
	}
	
	@Override
	public void purgePatient(Patient patient) {
		fhirClient.delete().resource(patient).execute();
	}
	
	/**
	 * Filter and parse out fhir patients from Client Registry Patient Search results
	 */
	private List<Patient> parseCRPatientSearchResults(Bundle patientBundle) {
		return patientBundle.getEntry().stream().filter(entry -> entry.getResource().hasType(FhirConstants.PATIENT))
				.map(entry -> (Patient) entry.getResource()).collect(Collectors.toList());
	}
}
