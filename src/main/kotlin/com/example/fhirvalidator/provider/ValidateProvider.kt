package com.example.fhirvalidator.provider

import ca.uhn.fhir.rest.annotation.ResourceParam
import ca.uhn.fhir.rest.annotation.Validate
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.ValidationModeEnum
import org.hl7.fhir.instance.model.api.IBaseResource
import org.springframework.stereotype.Component

@Component
class ValidateProvider {

    @Validate
    fun validate(@ResourceParam  resource : IBaseResource,
                 @Validate.Mode theMode : ValidationModeEnum,
                 @Validate.Profile theProfile : ValidationModeEnum) : MethodOutcome {

        return MethodOutcome();
    }


}
