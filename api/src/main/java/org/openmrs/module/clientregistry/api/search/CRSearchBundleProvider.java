package org.openmrs.module.clientregistry.api.search;

import java.io.Serializable;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.server.SimpleBundleProvider;

public class CRSearchBundleProvider extends SimpleBundleProvider implements Serializable {
	
	public CRSearchBundleProvider(List<? extends IBaseResource> patientList) {
		super(patientList);
	}
	
}
