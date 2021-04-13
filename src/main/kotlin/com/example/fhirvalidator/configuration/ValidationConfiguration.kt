package com.example.fhirvalidator.configuration

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ConceptValidationOptions
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.validation.FhirValidator
import com.example.fhirvalidator.service.ImplementationGuideParser
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.StructureDefinition
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ValidationConfiguration(private val implementationGuideParser: ImplementationGuideParser) {
    companion object : KLogging()

    @Bean
    fun validator(fhirContext: FhirContext, instanceValidator: FhirInstanceValidator): FhirValidator {
        return fhirContext.newValidator().registerValidatorModule(instanceValidator)
    }

    @Bean
    fun instanceValidator(supportChain: ValidationSupportChain): FhirInstanceValidator {
        var instanceValidator = FhirInstanceValidator(supportChain)
        // KM 12/4/2021 Should be tolerant of extra extensions and profiles
        instanceValidator.setAnyExtensionsAllowed(true)
        instanceValidator.isErrorForUnknownProfiles = false
        return instanceValidator
    }

    @Bean
    fun validationSupportChain(
        fhirContext: FhirContext,
        terminologyValidationSupport: InMemoryTerminologyServerValidationSupport,
        npmPackages: List<NpmPackage>
    ): ValidationSupportChain {
        val supportChain = ValidationSupportChain(
            DefaultProfileValidationSupport(fhirContext),
            CommonCodeSystemsTerminologyService(fhirContext),
            terminologyValidationSupport,
            SnapshotGeneratingValidationSupport(fhirContext)
        )
        npmPackages.map(implementationGuideParser::createPrePopulatedValidationSupport)
            .forEach(supportChain::addValidationSupport)
        generateSnapshots(supportChain)
        return supportChain
    }

    @Bean
    fun terminologyValidationSupport(fhirContext: FhirContext): InMemoryTerminologyServerValidationSupport {
        return object : InMemoryTerminologyServerValidationSupport(fhirContext) {
            override fun validateCodeInValueSet(
                theValidationSupportContext: ValidationSupportContext?,
                theOptions: ConceptValidationOptions?,
                theCodeSystem: String?,
                theCode: String?,
                theDisplay: String?,
                theValueSet: IBaseResource
            ): IValidationSupport.CodeValidationResult? {
                val valueSetUrl = CommonCodeSystemsTerminologyService.getValueSetUrl(theValueSet)

                // KGM same fault with UKCore valueSet
                if (valueSetUrl == "https://fhir.nhs.uk/ValueSet/DM-MedicationRequest-Code"
                    || valueSetUrl == "https://fhir.nhs.uk/ValueSet/DM-MedicationDispense-Code"
                    || valueSetUrl == "https://fhir.nhs.uk/ValueSet/NHSDigital-MedicationRequest-Code"
                    || valueSetUrl == "https://fhir.nhs.uk/ValueSet/NHSDigital-MedicationDispense-Code"
                    || valueSetUrl == "https://fhir.hl7.org.uk/ValueSet/UKCore-MedicationCode"
                    || valueSetUrl == "https://fhir.hl7.org.uk/ValueSet/UKCore-MedicationForm" ) {
                    return IValidationSupport.CodeValidationResult()
                        .setSeverity(IValidationSupport.IssueSeverity.WARNING)
                        .setMessage("Unable to validate medication codes")
                }

                return super.validateCodeInValueSet(
                    theValidationSupportContext,
                    theOptions,
                    theCodeSystem,
                    theCode,
                    theDisplay,
                    theValueSet
                )
            }
        }
    }

    fun generateSnapshots(supportChain: IValidationSupport) {
        supportChain.fetchAllStructureDefinitions<StructureDefinition>()
            .filter { shouldGenerateSnapshot(it) }
            .partition { it.baseDefinition.startsWith("http://hl7.org/fhir/") }
            .toList()
            .flatten()
            .forEach {
                try {
                    supportChain.generateSnapshot(
                        ValidationSupportContext(supportChain),
                        it,
                        it.url,
                        "https://fhir.nhs.uk/R4",
                        it.name
                    )
                } catch (e: Exception) {
                    logger.error("Failed to generate snapshot for $it", e)
                }
            }
    }

    private fun shouldGenerateSnapshot(structureDefinition: StructureDefinition): Boolean {
        return !structureDefinition.hasSnapshot() && structureDefinition.derivation == StructureDefinition.TypeDerivationRule.CONSTRAINT
    }
}
