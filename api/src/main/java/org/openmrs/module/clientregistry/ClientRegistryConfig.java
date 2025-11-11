/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license. Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the
 * OpenMRS graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.clientregistry;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.clientregistry.providers.FhirCRConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Contains module's config.
 */
@Component
public class ClientRegistryConfig {
	
	public final static String MODULE_PRIVILEGE = "Client Registry Privilege";
	
	@Autowired
	@Qualifier("adminService")
	AdministrationService administrationService;
	
	public boolean clientRegistryConnectionEnabled() {
		return StringUtils.isNotBlank(getClientRegistryServerUrl());
	}
	
	public String getClientRegistryServerUrl() {
		return administrationService.getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_SERVER_URL);
	}
	
	public String getClientRegistryGetPatientEndpoint() {
		String globalPropPatientEndpoint = administrationService
		        .getGlobalProperty(ClientRegistryConstants.GP_FHIR_CLIENT_REGISTRY_GET_PATIENT_ENDPOINT);
		
		// default to Patient/$ihe-pix if patient endpoint is not defined in config
		return (globalPropPatientEndpoint == null || globalPropPatientEndpoint.isEmpty()) ? String.format("Patient/%s",
		    FhirCRConstants.IHE_PIX_OPERATION) : globalPropPatientEndpoint;
	}
	
	public String getClientRegistryDefaultPatientIdentifierSystem() {
		return administrationService
		        .getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_DEFAULT_PATIENT_IDENTIFIER_SYSTEM);
	}
	
	public String getClientRegistryUserName() {
		String username = administrationService.getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_USER_NAME);
		return (username == null || username.isEmpty()) ? "openmrs" : username;
	}
	
	public String getClientRegistryPassword() {
		String password = administrationService.getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_PASSWORD);
		return (password == null || password.isEmpty()) ? "openmrs" : password;
	}
	
	public String getClientRegistryIdentifierRoot() {
		return administrationService.getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_IDENTIFIER_ROOT);
	}
	
	public String getClientRegistryTransactionMethod() {
		return administrationService.getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_TRANSACTION_METHOD);
	}
}
