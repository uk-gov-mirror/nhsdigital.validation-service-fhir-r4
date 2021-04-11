package com.example.fhirvalidator

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.LenientErrorHandler
import ca.uhn.fhir.parser.StrictErrorHandler
import com.example.fhirvalidator.service.ImplementationGuideParser
import mu.KLogging
import org.assertj.core.api.Assertions
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.MessageDefinition
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.utilities.cache.NpmPackage
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FHIRIGTest(@Autowired val restTemplate: TestRestTemplate,
                 @Autowired val npmPackages: List<NpmPackage>,
                 @Autowired val implementationGuideParser: ImplementationGuideParser) {


    @LocalServerPort
    private val port = 0

    lateinit var ctx : FhirContext

    companion object : KLogging()

    init {
        ctx = FhirContext.forR4()
        val strictErrorHandler = StrictErrorHandler()
        ctx.setParserErrorHandler(strictErrorHandler)
    }

    private fun validateResource(json: String,mediaType :  MediaType ): ResponseEntity<*>? {
        val headers = HttpHeaders()
        headers.contentType = mediaType
        val entity = HttpEntity<Any>(json, headers)
        return restTemplate.exchange(
            "http://localhost:$port/R4/\$validate", HttpMethod.POST, entity,
            String::class.java
        )
    }


    private fun patientResourceFromNpm(packageName : String) : List<Patient> {
        return npmPackages.filter { it.name().equals(packageName) }
            .flatMap { implementationGuideParser.getPatientExamples(it) }
    }
    private fun messageDefinitionResourceFromNpm(packageName : String) : List<MessageDefinition> {
        return npmPackages.filter { it.name().equals(packageName) }
            .flatMap { implementationGuideParser.getMessageDefinitions(it) }
    }

    private fun bundleResourceFromNpm(packageName : String) : List<Bundle> {
        return npmPackages.filter { it.name().equals(packageName) }
            .flatMap { implementationGuideParser.getBundleExamples(it) }
    }

    private fun getResource(response : ResponseEntity<*>?): IBaseResource? {
        if (response != null && response.body != null && response.body is String) {
            return ctx.newJsonParser().parseResource(response.body as String?)
        }
      return null
    }

    private fun hasErrors(operationOutcome: OperationOutcome) : Boolean {
        operationOutcome.issue.forEach{
            if (it.severity == OperationOutcome.IssueSeverity.ERROR) return true
        }
        return false
    }

    @Test
    @Throws(Exception::class)
    fun metadataShouldReturnCapabilityStatement() {
        logger.info("Capability Statement check")
        Assertions.assertThat(
            restTemplate.getForObject(
                "http://localhost:$port/R4/metadata",
                String::class.java
            )
        ).contains("CapabilityStatement")
    }

    @Test
    fun validateNHSDigitalPatientExamples() {
        validatePatient("uk.nhsdigital.r4")
    }

    @Test
    fun validateNHSDigitalMedicinesPatientExamples() {
        validatePatient("uk.nhsdigital.medicines.r4")
    }

    @Test
    fun validateNHSDigitalMedicinesMessageDefinitionsExamples() {
        messageDefinitionResourceFromNpm("uk.nhsdigital.medicines.r4")
            .forEach{
            validateResourceGeneric("uk.nhsdigital.medicines.r4",it)
            }
    }

    @Test
    fun validateNHSDigitalBundleExamples() {
        bundleResourceFromNpm("uk.nhsdigital.medicines.r4")
            .forEach{
                validateResourceGeneric("uk.nhsdigital.medicines.r4",it)
            }
    }

    fun validatePatient(packageName: String) {
        patientResourceFromNpm(packageName).forEach{
           validateResourceGeneric(packageName,it)
        }
    }

    fun validateResourceGeneric(packageName: String, resource :IBaseResource) {
        var response = validateResource(ctx.newJsonParser().encodeResourceToString(resource),
            MediaType.APPLICATION_JSON)
        Assertions.assertThat(response).isNotNull
        val responseResource = getResource(response)
        Assertions.assertThat(responseResource).isInstanceOf(OperationOutcome::class.java)
        if (responseResource is OperationOutcome) {
            if (hasErrors(responseResource)) {
                logger.error("{} Errors found. Example {}", packageName, resource.idElement.idPart)
                logger.info("{}", ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(responseResource))
            } else {
                logger.info("{} Validated OK Example {}", packageName, resource.idElement.idPart)
            }
        }
    }

}