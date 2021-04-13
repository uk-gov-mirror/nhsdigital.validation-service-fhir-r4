package com.example.fhirvalidator.service

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.JsonParser
import ca.uhn.fhir.parser.StrictErrorHandler
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.stereotype.Service


@Service
class ImplementationGuideParser(private val fhirContext: FhirContext) {

    companion object : KLogging()

    fun createPrePopulatedValidationSupport(npmPackage: NpmPackage): PrePopulatedValidationSupport {
        val prePopulatedSupport = PrePopulatedValidationSupport(fhirContext)

        getResourcesOfType(npmPackage, StructureDefinition()).forEach(prePopulatedSupport::addStructureDefinition)
        getResourcesOfType(npmPackage, CodeSystem()).forEach(prePopulatedSupport::addCodeSystem)
        getResourcesOfType(npmPackage, ValueSet()).forEach(prePopulatedSupport::addValueSet)

        return prePopulatedSupport
    }

    //TODO - can't use listResources or loadExampleResource as for some reason the message definitions are in an "examples" folder. Is this correct?
    // fun getMessageDefinitions(npmPackage: NpmPackage): List<MessageDefinition> {
    //     return getResourcesOfType(npmPackage, MessageDefinition())
    // }
    fun getMessageDefinitions(npmPackage: NpmPackage): List<MessageDefinition> {
        // This is used by EPS validation. Main difference to getResourceExamples is default LenientErrorHandler
        val jsonParser = fhirContext.newJsonParser().setParserErrorHandler(StrictErrorHandler())
        return npmPackage.list("examples")
            .map { npmPackage.load("examples", it) }
            .map(jsonParser::parseResource)
            .filterIsInstance(MessageDefinition::class.java)
    }
    fun <T : Resource> getResourceExamples(npmPackage: NpmPackage, resourceType: T): List<T> {
        // Note use of StrictErrorHandler
        val jsonParser = fhirContext.newJsonParser().setParserErrorHandler(StrictErrorHandler())
        return npmPackage.list("examples")
            .map { npmPackage.load("examples", it) }
            .map(jsonParser::parseResource)
            .filterIsInstance(resourceType.javaClass)
    }
    fun <T> T.log(): T {
        logger.info("{}",this)
        return this
    }

    fun <T : Resource> getResourcesOfType(npmPackage: NpmPackage, resourceType: T): List<T> {
        val jsonParser = fhirContext.newJsonParser()
        return npmPackage.listResources(resourceType.fhirType())
            .map(npmPackage::loadResource)
            .map(jsonParser::parseResource)
            .filterIsInstance(resourceType.javaClass)
    }
}
