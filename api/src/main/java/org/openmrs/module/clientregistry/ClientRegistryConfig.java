/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.clientregistry;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.AdministrationService;
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
	
	public String getClientRegistryUserName() {
		return administrationService.getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_USER_NAME);
	}
	
	public String getClientRegistryPassword() {
		return administrationService.getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_PASSWORD);
	}
	
	public String getClientRegistryIdentifierRoot() {
		return administrationService.getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_IDENTIFIER_ROOT);
	}

	public String getClientRegistryTransactionMethod() {
		return administrationService.getGlobalProperty(ClientRegistryConstants.GP_CLIENT_REGISTRY_TRANSACTION_METHOD);
	}
}
