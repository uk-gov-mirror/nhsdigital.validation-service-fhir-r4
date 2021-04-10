package com.example.fhirvalidator.util

import com.example.fhirvalidator.service.CapabilityStatementApplier
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.hl7.fhir.r4.model.Bundle

fun getResourcesOfType(resource: IBaseResource, resourceType: String?): List<IBaseResource> {
    val matchingResources = mutableListOf<IBaseResource>()
    if (resource.fhirType() == resourceType) {
        matchingResources.add(resource)
    }
    if (resource is Bundle) {
        resource.entry.stream()
            .map { it.resource }
            .filter { it.fhirType() == resourceType }
            .forEach { matchingResources.add(it) }
    }
    return matchingResources
}

fun applyProfile(resources: List<IBaseResource>, profile: IPrimitiveType<String>) {
    resources.stream()
        .filter { !it.meta.profile.contains(profile) }
        .forEach {
            it.meta.addProfile(profile.value)
            CapabilityStatementApplier.logger.info("applying profile {}",profile.value)
        }
}