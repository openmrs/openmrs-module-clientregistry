package org.openmrs.module.clientregistry.providers.r4;

import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

import org.openmrs.api.context.Context;
import org.openmrs.module.clientregistry.ClientRegistryConfig;
import org.openmrs.module.clientregistry.api.ClientRegistryManager;
import org.openmrs.module.clientregistry.providers.FhirCRConstants;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PACKAGE;

@Component("crPatientFhirR4ResourceProvider")
@R4Provider
@Setter(PACKAGE)
public class FhirCRPatientResourceProvider implements IResourceProvider {
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Patient.class;
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
	 * @return List of matching FHIR patients returned by the client registry
	 */
	@Operation(name = FhirCRConstants.IHE_PIX_OPERATION, idempotent=true, type = Patient.class, bundleType = BundleTypeEnum.SEARCHSET)
	public List<Patient> getCRPatientById(
			@OperationParam(name = FhirCRConstants.SOURCE_IDENTIFIER) TokenParam sourceIdentifierParam,
	        @OperationParam(name = FhirCRConstants.TARGET_SYSTEM) StringOrListParam targetSystemsParam
	) {
		ClientRegistryManager clientRegistryManager = Context.getRegisteredComponent("clientRegistryManager",
	    ClientRegistryManager.class);
	
		ClientRegistryConfig config = Context.getRegisteredComponent("clientRegistryFhirClient",
	    ClientRegistryConfig.class);
		if (sourceIdentifierParam == null || sourceIdentifierParam.getValue() == null) {
			throw new InvalidRequestException("sourceIdentifier must be specified");
		}

		List<String> targetSystems = targetSystemsParam == null
				? Collections.emptyList()
				: targetSystemsParam.getValuesAsQueryTokens().stream().filter(Objects::nonNull).map(StringParam::getValue).collect(Collectors.toList());

		// If no sourceSystem provided, use config defined default
		boolean userDefinedSourceSystem = sourceIdentifierParam.getSystem() != null && !sourceIdentifierParam.getSystem().isEmpty();
		String sourceIdentifierSystem = userDefinedSourceSystem
				? sourceIdentifierParam.getSystem()
				: config.getClientRegistryDefaultPatientIdentifierSystem();

		if (sourceIdentifierSystem == null || sourceIdentifierSystem.isEmpty()) {
			throw new InvalidRequestException("ClientRegistry module does not have a default source system assigned " +
					"via the defaultPatientIdentifierSystem property. Source system must be provided as a token in " +
					"the sourceIdentifier request param");
		}

		List<Patient> patients = clientRegistryManager.getPatientService().getCRPatients(
				sourceIdentifierParam.getValue(), sourceIdentifierSystem, targetSystems
		);

		if (patients.isEmpty()) {
			throw new ResourceNotFoundException("No Client Registry patients found.");
		}

		return patients;
	}
	

	@Operation(name = "$search", idempotent = true)
	public List<Patient> searchClientRegistryPatients() {
		throw new NotImplementedOperationException("search client registry is not yet implemented");
	}
}
