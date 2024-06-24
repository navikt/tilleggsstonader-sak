package no.nav.tilleggsstonader.sak.behandling.manuell

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

private val xmlMapper = XmlMapper()
    .registerKotlinModule()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

object SøknadTilsynBarnSendInnFyllUtUtil {

    fun parseInfoFraSøknad(data: ByteArray): Søknadsinformasjon {
        val søknad = xmlMapper.readValue<Tilleggsstoenadsskjema>(data)
        val identerBarn = søknad.rettighetstype.tilsynsutgifter
            .flatMap { it.tilsynsutgifterBarn }
            .flatMap { it.barn }
            .map { it.personidentifikator }
            .toSet()
        return Søknadsinformasjon(ident = søknad.personidentifikator, identerBarn = identerBarn)
    }
}

data class Søknadsinformasjon(
    val ident: String,
    val identerBarn: Set<String>,
)

private data class Tilleggsstoenadsskjema(
    val personidentifikator: String,
    val rettighetstype: Rettighetstype,
) {

    data class Rettighetstype(
        @JacksonXmlElementWrapper(useWrapping = false)
        val tilsynsutgifter: List<Tilsynsutgift>,
    )

    data class Tilsynsutgift(
        @JacksonXmlElementWrapper(useWrapping = false)
        val tilsynsutgifterBarn: List<TilsynsutgifterBarn>,
    )

    data class TilsynsutgifterBarn(
        @JacksonXmlElementWrapper(useWrapping = false)
        val barn: List<TilsynsutgiftBarn>,
    )

    data class TilsynsutgiftBarn(
        val personidentifikator: String,
    )
}
