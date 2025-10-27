package no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapperFailOnUnknownProperties
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DagligReiseFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.SkjemaDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkService
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import org.junit.jupiter.api.Test

class SøknadskjemaDagligReiseMapperTest {
    val kodeverkService =
        mockk<KodeverkService>().apply {
            val service = this
            every { service.hentLandkodeIso2(any()) } returns "Land1"
        }
    val mapper =
        SøknadskjemaDagligReiseMapper(
            kodeverkService = kodeverkService,
        )

    @Test
    fun `skal kunne mappe skjema-eksempel fra fyllUtSendInn offentlig transport og privat bil`() {
        val skjema = mapSkjemadata("søknad/dagligReise/eksempel1/skjema-eksempel-offentlig-transport-og-privat-bil.json")
        val mappetSkjema = mapper.mapSkjema(skjema, emptyList())

        val mappetJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/dagligReise/eksempel1/mappet-domene.json", mappetJson)
    }

    @Test
    fun `skal kunne mappe skjema-eksempel fra fyllUtSendInn som har bare offentlig transport`() {
        val skjema = mapSkjemadata("søknad/dagligReise/eksempel2/skjema-eksempel-offentlig-transport.json")
        val mappetSkjema = mapper.mapSkjema(skjema, emptyList())

        val mappetJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/dagligReise/eksempel2/mappet-domene.json", mappetJson)
    }

    @Test
    fun `skal kunne mappe skjema-eksempel fra fyllUtSendInn med bare egen bil`() {
        val skjema = mapSkjemadata("søknad/dagligReise/eksempel3/skjema-eksempel-egen-bil.json")
        val mappetSkjema = mapper.mapSkjema(skjema, emptyList())

        val mappetJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/dagligReise/eksempel3/mappet-domene.json", mappetJson)
    }

    @Test
    fun `skal kunne mappe skjema-eksempel fra fyllUtSendInn med taxi`() {
        val skjema = mapSkjemadata("søknad/dagligReise/eksempel4/skjema-eksempel-taxi.json")
        val mappetSkjema = mapper.mapSkjema(skjema, emptyList())

        val mappetJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/dagligReise/eksempel4/mappet-domene.json", mappetJson)
    }

    private fun mapSkjemadata(skjemaJsonFil: String): SøknadsskjemaDagligReiseFyllUtSendInn {
        val json = FileUtil.readFile(skjemaJsonFil)
        val dagligReise = objectMapperFailOnUnknownProperties.readValue<SkjemaDagligReise>(json)
        return SøknadsskjemaDagligReiseFyllUtSendInn("nb-NO", DagligReiseFyllUtSendInnData(dagligReise))
    }
}
