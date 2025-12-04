package no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.jsonMapperFailOnUnknownProperties
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.BoutgifterFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.SkjemaBoutgifter
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkService
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class SøknadskjemaBoutgifterMapperTest {
    val kodeverkService =
        mockk<KodeverkService>().apply {
            val service = this
            every { service.hentLandkodeIso2(any()) } returns "Land1"
        }
    val mapper =
        SøknadskjemaBoutgifterMapper(
            kodeverkService = kodeverkService,
        )

    /**
     * skjema-eksempel.json er kopiert inn fra kontrakter skjema-eksempel.json som er generert fra skjema fra fyllut/sendinn
     */
    @Test
    fun `skal kunne mappe skjema-eksempel som inneholder alle felter`() {
        val json = FileUtil.readFile("søknad/boutgifter/eksempel1/skjema-eksempel.json")
        val boutgifter = jsonMapperFailOnUnknownProperties.readValue<SkjemaBoutgifter>(json)
        val skjema = SøknadsskjemaBoutgifterFyllUtSendInn("nb-NO", BoutgifterFyllUtSendInnData(boutgifter))
        val mappetSkjema = mapper.mapSkjema(skjema, emptyList())

        val mappetJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/boutgifter/eksempel1/mappet-domene.json", mappetJson)
    }

    /**
     * eksempel-boutgifter.json er kopiert inn fra kontrakter eksempel-boutgifter.json som er sendt inn fra fyllut/sendinn
     */
    @Test
    fun `skal kunne mappe søknad for boutgifter`() {
        val json = FileUtil.readFile("søknad/boutgifter/eksempel2/eksempel-boutgifter.json")
        val boutgifter = jsonMapperFailOnUnknownProperties.readValue<SøknadsskjemaBoutgifterFyllUtSendInn>(json)
        val mappetSkjema = mapper.mapSkjema(boutgifter, emptyList())

        val mappetJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/boutgifter/eksempel2/mappet-domene.json", mappetJson)
    }
}
