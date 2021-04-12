package com.example.fhirvalidator

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.LenientErrorHandler
import ca.uhn.fhir.parser.StrictErrorHandler
import com.example.fhirvalidator.service.ImplementationGuideParser
import com.example.fhirvalidator.util.getResourcesOfType
import mu.KLogging
import org.assertj.core.api.Assertions
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.utilities.npm.NpmPackage
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

    //
    //  NHS Digital Medicines - EPS Tests
    //


    @Test
    fun validateNHSDigitalMedicinesPatientExamples() {
        validatePatient("uk.nhsdigital.medicines.r4")
    }

    @Test
    fun validateNHSDigitalMedicinesMessageDefinitionsExamples() {
        resourceFromNpm("uk.nhsdigital.medicines.r4", MessageDefinition())
            .forEach{
            validateResourceGeneric("uk.nhsdigital.medicines.r4",it)
            }
    }

    @Test
    fun validateNHSDigitalMedicinesBundleExamples() {
        resourceFromNpm("uk.nhsdigital.medicines.r4",Bundle())
            .forEach{
                validateResourceGeneric("uk.nhsdigital.medicines.r4",it)
            }
    }

    @Test
    fun validateNHSDigitalMedicinesMedicationRequestExamples() {
        val packageName = "uk.nhsdigital.medicines.r4"
        resourceFromNpm(packageName,MedicationRequest()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
    @Test
    fun validateNHSDigitalMedicinesMedicationDispenseExamples() {
        val packageName = "uk.nhsdigital.medicines.r4"
        resourceFromNpm(packageName,MedicationDispense()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
    @Test
    fun validateNHSDigitalMedicinesMedicationExamples() {
        val packageName = "uk.nhsdigital.medicines.r4"
        resourceFromNpm(packageName,Medication()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }

    @Test
    fun validateNHSDigitalMedicinesClaimExamples() {
        val packageName = "uk.nhsdigital.medicines.r4"
        resourceFromNpm(packageName,Claim()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }

    @Test
    fun validateNHSDigitalMedicinesTaskExamples() {
        val packageName = "uk.nhsdigital.medicines.r4"
        resourceFromNpm(packageName,Task()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }

    //
    //  NHS Digital Medicines - EPS Tests
    //

    @Test
    fun validateNHSDigitalPatientExamples() {
        validatePatient("uk.nhsdigital.r4")
    }

    @Test
    fun validateNHSDigitalPractitionerExamples() {
        val packageName = "uk.nhsdigital.r4"
        resourceFromNpm(packageName,Practitioner()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
    @Test
    fun validateNHSDigitalPractitionerRoleExamples() {
        val packageName = "uk.nhsdigital.r4"
        resourceFromNpm(packageName,PractitionerRole()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
    @Test
    fun validateNHSDigitalOrganizationExamples() {
        val packageName = "uk.nhsdigital.r4"
        resourceFromNpm(packageName,Organization()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
    @Test
    fun validateNHSDigitalHealthcareServiceExamples() {
        val packageName = "uk.nhsdigital.r4"
        resourceFromNpm(packageName,HealthcareService()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }



    @Test
    fun validateNHSDigitalLocationExamples() {
        val packageName = "uk.nhsdigital.r4"
        resourceFromNpm(packageName,Location()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
    @Test
    fun validateNHSDigitalCommunicationRequestExamples() {
        val packageName = "uk.nhsdigital.r4"
        resourceFromNpm(packageName,CommunicationRequest()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
    /*
    @Test
    fun validateNHSDigitalBundleExamples() {
        val packageName = "uk.nhsdigital.r4"
        resourceFromNpm(packageName,Bundle()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
    @Test
    fun validateNHSDigitalEndpointExamples() {
        val packageName = "uk.nhsdigital.r4"
        resourceFromNpm(packageName,Endpoint()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
    @Test
    fun validateNHSDigitalDeviceExamples() {
        val packageName = "uk.nhsdigital.r4"
        resourceFromNpm(packageName,Device()).forEach{
            validateResourceGeneric(packageName,it)
        }
    }
*/


    fun validatePatient(packageName: String) {
        resourceFromNpm(packageName,Patient()).forEach{
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
            Assertions.assertThat(hasErrors(responseResource)).isEqualTo(false)
        }
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

    private fun <T : Resource> resourceFromNpm(packageName : String, resourceType: T) : List<T> {
        return npmPackages.filter { it.name().equals(packageName) }
            .flatMap { implementationGuideParser.getResourceExamples(it,resourceType ) }
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


}
