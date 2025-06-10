package org.openmrs.module.clientregistry.providers.r4;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.openmrs.api.context.Context;
import org.openmrs.module.clientregistry.ClientRegistryConfig;
import org.openmrs.module.clientregistry.ClientRegistryConstants;
import org.openmrs.module.clientregistry.api.CRPatientService;
import org.openmrs.module.clientregistry.providers.FhirCRConstants;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.api.search.param.PatientSearchParams;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import static lombok.AccessLevel.PACKAGE;

@Component("crPatientFhirR4ResourceProvider")
@R4Provider
@Setter(PACKAGE)
public class FhirCRPatientResourceProvider implements IResourceProvider {
	
	@Autowired
	private CRPatientService crService;
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Patient.class;
	}
	
	@Operation(name = ClientRegistryConstants.CR_FHIR_OPERATION, idempotent = true)
	public Patient getPatientById(@IdParam @Nonnull IdType id) {
		Patient patient = crService.getPatientById(id.getIdPart());
		if (patient == null) {
			throw new ResourceNotFoundException("Could not find patient with Id " + id.getIdPart());
		}
		return patient;
	}
	
	@Operation(name = ClientRegistryConstants.CR_FHIR_OPERATION)
	public MethodOutcome createPatient(@ResourceParam Patient patient) {
		Patient createdPatient = crService.createPatient(patient);
		return FhirProviderUtils.buildCreate(createdPatient);
	}
	
	@Operation(name = ClientRegistryConstants.CR_FHIR_UPDATE_OPERATION)
	public MethodOutcome updatePatient(@IdParam @Nonnull IdType id, @ResourceParam @Nonnull Patient patient) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update");
		}
		patient.setId(id.getIdPart());
		Patient updatedPatient = crService.updatePatient(patient);
		return FhirProviderUtils.buildUpdate(updatedPatient);
	}
	
	@Operation(name = ClientRegistryConstants.CR_FHIR_DELETE_OPERATION)
	public OperationOutcome deletePatient(@ResourceParam Patient patient) {
		crService.purgePatient(patient);
		return FhirProviderUtils.buildDeleteR4();
	}
	
	/**
	 * FHIR endpoint to get Patient references from external client registry Example request: GET
	 * [fhirbase
	 * ]/Patient/$ihe-pix?sourceIdentifier={sourceSystem|}1234[&targetSystem=system1,system2]
	 * 
	 * @param sourceIdentifierParam patient identifier token. If source system is included in token,
	 *            we will use it to override the module defined source system.
	 * @param targetSystemsParam (optional) Patient assigning authorities (ie systems) from which
	 *            the returned identifiers shall be selected
	 * @return a bundle matching FHIR patients returned by the client registry
	 */
	@Operation(name = FhirCRConstants.IHE_PIX_OPERATION, idempotent = true)
	public IBundleProvider getCRPatientByPix(
	        @OperationParam(name = FhirCRConstants.SOURCE_IDENTIFIER) TokenParam sourceIdentifierParam,
	        @OperationParam(name = FhirCRConstants.TARGET_SYSTEM) StringOrListParam targetSystemsParam) {
		
		ClientRegistryConfig config = Context.getRegisteredComponent("clientRegistryFhirClient", ClientRegistryConfig.class);
		if (sourceIdentifierParam == null || sourceIdentifierParam.getValue() == null) {
			throw new InvalidRequestException("sourceIdentifier must be specified");
		}
		
		List<String> targetSystems = targetSystemsParam == null ? Collections.emptyList()
		        : targetSystemsParam.getValuesAsQueryTokens().stream().filter(Objects::nonNull).map(StringParam::getValue)
		                .collect(Collectors.toList());
		
		// If no sourceSystem provided, use config defined default
		boolean userDefinedSourceSystem = sourceIdentifierParam.getSystem() != null
		        && !sourceIdentifierParam.getSystem().isEmpty();
		String sourceIdentifierSystem = userDefinedSourceSystem ? sourceIdentifierParam.getSystem()
		        : config.getClientRegistryDefaultPatientIdentifierSystem();
		
		if (sourceIdentifierSystem == null || sourceIdentifierSystem.isEmpty()) {
			throw new InvalidRequestException("ClientRegistry module does not have a default source system assigned "
			        + "via the defaultPatientIdentifierSystem property. Source system must be provided as a token in "
			        + "the sourceIdentifier request param");
		}
		
		return crService.getPatientsByPIX(sourceIdentifierSystem, sourceIdentifierSystem, targetSystems);
	}
	
	@Operation(name = ClientRegistryConstants.CR_FHIR_SEARCH_OPERATION, idempotent = true)
	public IBundleProvider searchPatients(@OperationParam(name = Patient.SP_NAME) StringAndListParam name,
	        @OperationParam(name = Patient.SP_GIVEN) StringAndListParam given,
	        @OperationParam(name = Patient.SP_FAMILY) StringAndListParam family,
	        @OperationParam(name = Patient.SP_IDENTIFIER) TokenAndListParam identifier,
	        @OperationParam(name = Patient.SP_GENDER) TokenAndListParam gender,
	        @OperationParam(name = Patient.SP_BIRTHDATE) DateRangeParam birthDate,
	        @OperationParam(name = Patient.SP_DEATH_DATE) DateRangeParam deathDate,
	        @OperationParam(name = Patient.SP_DECEASED) TokenAndListParam deceased,
	        @OperationParam(name = Patient.SP_ADDRESS_CITY) StringAndListParam city,
	        @OperationParam(name = Patient.SP_ADDRESS_STATE) StringAndListParam state,
	        @OperationParam(name = Patient.SP_ADDRESS_POSTALCODE) StringAndListParam postalCode,
	        @OperationParam(name = Patient.SP_ADDRESS_COUNTRY) StringAndListParam country,
	        @OperationParam(name = Patient.SP_RES_ID) TokenAndListParam id,
	        @OperationParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort) {
		return crService.searchPatients(new PatientSearchParams(name, given, family, identifier, gender, birthDate,
		        deathDate, deceased, city, state, postalCode, country, id, null, lastUpdated, sort, null));
	}
}
