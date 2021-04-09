package com.example.fhirvalidator.server

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.server.RestfulServer
import com.example.fhirvalidator.FhirValidatorApplication
import com.example.fhirvalidator.provider.ValidateProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import java.util.*


class FHIRRestfulServer(theCtx: FhirContext?) : RestfulServer(theCtx) {

    val ctx= theCtx;

    @Autowired
    private val applicationContext: ApplicationContext? = null

    @SuppressWarnings("unchecked")
    override fun initialize() {
        super.initialize()
        fhirContext = ctx;
        val plainProviders: MutableList<Any?> = ArrayList()

        plainProviders.add(applicationContext?.getBean(ValidateProvider::class))


        registerProviders(plainProviders)
    }

}
