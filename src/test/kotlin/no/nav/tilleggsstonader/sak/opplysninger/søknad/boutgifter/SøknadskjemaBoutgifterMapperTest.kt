package no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.spyk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapperFailOnUnknownProperties
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArsakOppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.BoutgifterFyllUtSendInnData
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.SkjemaBoutgifter
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1
import kotlin.reflect.full.primaryConstructor

class SøknadskjemaBoutgifterMapperTest {
    /**
     * skjema-eksempel.json er kopiert inn fra kontrakter skjema-eksempel.json som er generert fra skjema fra fyllut/sendinn
     */
    @Test
    fun `skal kunne mappe skjema-eksempel som inneholder alle felter`() {
        val json = FileUtil.readFile("søknad/boutgifter/eksempel1/skjema-eksempel.json")
        val boutgifter = objectMapperFailOnUnknownProperties.readValue<SkjemaBoutgifter>(json)
        val skjema = SøknadsskjemaBoutgifterFyllUtSendInn(BoutgifterFyllUtSendInnData(boutgifter))
        val mappetSkjema = SøknadskjemaBoutgifterMapper.mapSkjema(skjema, emptyList())

        val mappetJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/boutgifter/eksempel1/mappet-domene.json", mappetJson)
    }

    /**
     * eksempel-boutgifter.json er kopiert inn fra kontrakter eksempel-boutgifter.json som er sendt inn fra fyllut/sendinn
     */
    @Test
    fun `skal kunne mappe søknad for boutgifter`() {
        val json = FileUtil.readFile("søknad/boutgifter/eksempel2/eksempel-boutgifter.json")
        val boutgifter = objectMapperFailOnUnknownProperties.readValue<SøknadsskjemaBoutgifterFyllUtSendInn>(json)
        val mappetSkjema = SøknadskjemaBoutgifterMapper.mapSkjema(boutgifter, emptyList())

        val mappetJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappetSkjema)
        assertFileIsEqual("søknad/boutgifter/eksempel2/mappet-domene.json", mappetJson)
    }

    @Test
    fun `ArsakOppholdUtenforNorge - valider at alle felter mappes`() {
        validerAlleFelterMappes(
            ArsakOppholdUtenforNorge::class,
            SøknadskjemaBoutgifterMapper::mapÅrsakOppholdUtenforNorge,
        )
    }

    @Test
    fun `Hovedytelse - valider at alle felter mappes`() {
        validerAlleFelterMappes(Hovedytelse::class, SøknadskjemaBoutgifterMapper::mapHovedytelse)
    }

    private inline fun <reified T : Any> validerAlleFelterMappes(
        kClass: KClass<T>,
        mapper: KFunction1<T, Any>,
    ) {
        val primaryConstructor = kClass.primaryConstructor
        val obj = spyk(primaryConstructor!!.call(*primaryConstructor.parameters.map { false }.toTypedArray()))

        val accessTracker = mutableMapOf<String, Boolean>()

        primaryConstructor.parameters.forEach { param ->
            every { obj.getProperty(param.name!!) } answers {
                accessTracker[param.name!!] = true
                callOriginal()
            }
        }

        mapper.call(obj)
        primaryConstructor.parameters.forEach { param -> assertThat(accessTracker).containsKey(param.name!!) }
    }
}
