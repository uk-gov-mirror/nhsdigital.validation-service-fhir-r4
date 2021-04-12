package com.example.fhirvalidator.configuration

import com.example.fhirvalidator.model.SimplifierPackage
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.NpmPackage
import org.hl7.fhir.utilities.npm.ToolsVersion
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL
import java.util.*
import kotlin.streams.toList

@Configuration
class PackageConfiguration(val objectMapper: ObjectMapper) {
    companion object : KLogging()

    @Bean
    fun getPackages(): List<NpmPackage> {
        val inputStream = ClassPathResource("manifest.json").inputStream
        val packages = objectMapper.readValue(inputStream, Array<SimplifierPackage>::class.java)
        try {
            return Arrays.stream(packages)

                    .map { "${it.packageName}-${it.version}.tgz" }
                    .map { ClassPathResource(it).inputStream }
                    .map { NpmPackage.fromPackage(it) }
                    .toList()
        } catch (ex :FileNotFoundException) {
                println("oopsie")
            val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION);
            return Arrays.stream(packages)
                    .map { getGetPackage(pcm, it.packageName,  it.version, it.download) }
                    .toList()
        }
    }

    fun getGetPackage(pcm : FilesystemPackageCacheManager, packageName : String, version : String, download : Boolean? ) : NpmPackage {
        if (download != null && !download) {
            return pcm.loadPackage( packageName,  version)
        } else {
            return getPackageFromUrl(pcm,packageName, version )
        }
    }

    @Throws(Exception::class)
    private fun getPackageFromUrl(pcm: FilesystemPackageCacheManager, packageName: String, version: String ): NpmPackage {
        val stream: InputStream?
        try {
            val url = "https://packages.simplifier.net/${packageName}/-/${packageName}-${version}.tgz"
            println(url)
            if (url.contains(".tgz")) {
                stream = fetchFromUrlSpecific(url, true)
                if (stream != null) {
                    return pcm.addPackageToCache(packageName, version, stream, url)
                }
            }
        } catch (ex: Exception) {
            println("ERROR")
        }
        throw FHIRException("Package not found")
    }

    @Throws(FHIRException::class)
    private fun fetchFromUrlSpecific(source: String, optional: Boolean): InputStream? {
        return try {
            val url = URL(source)
            val c = url.openConnection()
            c.getInputStream()
        } catch (var5: Exception) {
            if (optional) {
                null
            } else {
                throw FHIRException(var5.message, var5)
            }
        }
    }


}

