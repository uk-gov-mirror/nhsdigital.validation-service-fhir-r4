package com.example.fhirvalidator.service

import com.example.fhirvalidator.util.applyProfile
import com.example.fhirvalidator.util.getResourcesOfType
import mu.KLogging
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.CapabilityStatement
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.stereotype.Service

@Service
class CapabilityStatementApplier(
    implementationGuideParser: ImplementationGuideParser,
    npmPackages: List<NpmPackage>
) {

    companion object : KLogging()

    private val restResources = npmPackages
        .flatMap { implementationGuideParser.getResourcesOfType(it, CapabilityStatement()) }
        .flatMap { it.rest }
        .flatMap { it.resource }

    fun applyCapabilityStatementProfiles(resource: IBaseResource) {
        // Need to only add profiles if one doesn't exist. Probably need to strip profiles from incoming resources also.
        restResources
            .forEach { applyRestResource(resource, it) }
    }

    private fun applyRestResource(
        resource: IBaseResource,
        restResource: CapabilityStatement.CapabilityStatementRestResourceComponent
    ) {
        val matchingResources = getResourcesOfType(resource, restResource.type)
        if (restResource.hasProfile()) {
            applyProfile(matchingResources, restResource.profileElement)
        }
    }
}
