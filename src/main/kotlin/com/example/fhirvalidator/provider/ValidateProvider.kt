package com.example.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.rest.annotation.Operation
import ca.uhn.fhir.rest.annotation.ResourceParam
import ca.uhn.fhir.rest.annotation.Validate
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.ValidationModeEnum
import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.ValidationOptions
import com.example.fhirvalidator.controller.ValidateController
import com.example.fhirvalidator.service.CapabilityStatementApplier
import com.example.fhirvalidator.service.MessageDefinitionApplier
import com.example.fhirvalidator.util.createOperationOutcome
import mu.KLogging
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.springframework.stereotype.Component
import java.net.URLDecoder
import javax.servlet.http.HttpServletRequest

@Component
class ValidateProvider(private val fhirContext: FhirContext,
                       private val validator: FhirValidator,
                       private val messageDefinitionApplier: MessageDefinitionApplier,
                       private val capabilityStatementApplier: CapabilityStatementApplier) {

    companion object : KLogging()

    @Operation(name = "\$convert", idempotent = true)
    @Throws(Exception::class)
    fun convertJsonOrXml(
        @ResourceParam resource: IBaseResource?
    ): IBaseResource? {
        return resource
    }

    @Validate
    fun validate(theServletRequest : HttpServletRequest, @ResourceParam  resource : IBaseResource,
                 @Validate.Profile theProfile : String?,
                 @Validate.Mode theMode : ValidationModeEnum?
                ) : MethodOutcome {
        // Probably a better way of doing this. Presume @Validate.Profile is to support the Parameter POST operation
        var profile : String? = theProfile
        if (theServletRequest.queryString != null && theProfile == null) {
            val query_pairs: MutableMap<String, String> = LinkedHashMap()
            val query = theServletRequest.queryString
            val pairs = query.split("&".toRegex()).toTypedArray()
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                query_pairs[URLDecoder.decode(pair.substring(0, idx), "UTF-8")] = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            }
            profile = query_pairs["profile"]
        }
        val operationOutcome = parseAndValidateResource(resource, profile)
        val methodOutcome = MethodOutcome()
        methodOutcome.setOperationOutcome(operationOutcome)
        return methodOutcome;
    }

    private fun parseAndValidateResource(resource : IBaseResource, theProfile : String?): IBaseOperationOutcome {
        return try {

            if (resource is Bundle && resource.type == Bundle.BundleType.MESSAGE){
                // EPS MessageDefinition validation logic.
                val messageDefinitionErrors = messageDefinitionApplier.applyMessageDefinition(resource)
                capabilityStatementApplier.applyCapabilityStatementProfiles(resource)
                messageDefinitionErrors ?: validator.validateWithResult(resource).toOperationOutcome()
            } else if (theProfile == null) {
                // Go for default profiles which should be in capabilityStatements
                capabilityStatementApplier.applyCapabilityStatementProfiles(resource)
                return validator.validateWithResult(resource).toOperationOutcome()
            } else  {
                // Go for supplied profile
                theProfile?.let { logger.info("applying profile $it") }
                val validationOptions = ValidationOptions()
                validationOptions.addProfile(theProfile)
                return validator.validateWithResult(resource,validationOptions).toOperationOutcome()
            }
        } catch (e: DataFormatException) {
            ValidateController.logger.error("Caught parser error", e)
            createOperationOutcome("Invalid JSON", null)
        }
    }

}
