package no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.jsonMapperFailOnUnknownProperties
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.DagligReiseFyllUtSendInnData
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkService
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

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
    fun `skal kunne mappe skjema-eksempel fra fyllUtSendInn med offentlig transport`() {
        val skjema = mapSkjemadata("søknad/dagligReise/eksempel1/skjema-eksempel-offentlig-transport.json")
        val mappetSkjema = mapper.mapSkjema(skjema, emptyList())

        val mappetJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/dagligReise/eksempel1/mappet-domene.json", mappetJson)
    }

    @Test
    fun `skal kunne mappe skjema-eksempel fra fyllUtSendInn med egen bil`() {
        val skjema = mapSkjemadata("søknad/dagligReise/eksempel2/skjema-eksempel-egen-bil.json")
        val mappetSkjema = mapper.mapSkjema(skjema, emptyList())

        val mappetJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/dagligReise/eksempel2/mappet-domene.json", mappetJson)
    }

    @Test
    fun `skal kunne mappe skjema-eksempel fra fyllUtSendInn med taxi`() {
        val skjema = mapSkjemadata("søknad/dagligReise/eksempel3/skjema-eksempel-taxi.json")
        val mappetSkjema = mapper.mapSkjema(skjema, emptyList())

        val mappetJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/dagligReise/eksempel3/mappet-domene.json", mappetJson)
    }

    private fun mapSkjemadata(skjemaJsonFil: String): SøknadsskjemaDagligReiseFyllUtSendInn {
        val json = FileUtil.readFile(skjemaJsonFil)
        val dagligReise = jsonMapperFailOnUnknownProperties.readValue<DagligReiseFyllUtSendInnData>(json)
        return SøknadsskjemaDagligReiseFyllUtSendInn("nb-NO", dagligReise)
    }
}
