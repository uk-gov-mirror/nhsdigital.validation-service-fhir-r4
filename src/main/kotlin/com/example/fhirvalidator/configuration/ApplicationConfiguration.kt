package com.example.fhirvalidator.configuration

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.LenientErrorHandler
import com.example.fhirvalidator.server.FHIRRestfulServer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import javax.servlet.Servlet

@Configuration
class ApplicationConfiguration {
    @Bean
    fun fhirContext(): FhirContext {
        val lenientErrorHandler = LenientErrorHandler()
        lenientErrorHandler.isErrorOnInvalidValue = false
        val fhirContext = FhirContext.forR4()
        fhirContext.setParserErrorHandler(lenientErrorHandler)
        return fhirContext
    }

    @Bean
    fun FHIRServerR4RegistrationBean(ctx: FhirContext? ): ServletRegistrationBean<*>? {
        val registration: ServletRegistrationBean<*> = ServletRegistrationBean<Servlet?>(FHIRRestfulServer(ctx), "/R4/*")
        val params: MutableMap<String, String> = HashMap()
        params["FhirVersion"] = "R4"
        params["ImplementationDescription"] = "FHIR Validation Server"
        registration.initParameters = params
        registration.setName("FhirR4Servlet")
        registration.setLoadOnStartup(1)
        return registration
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}
