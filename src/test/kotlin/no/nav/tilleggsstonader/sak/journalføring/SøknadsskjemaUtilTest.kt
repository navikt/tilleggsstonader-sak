package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SøknadsskjemaUtilTest {

    @Test
    fun `Skal kunne parse søknad av type BARNETILSYN`() {
        val skjema = SøknadUtil.søknadskjemaBarnetilsyn()
        val parsetSkjema = SøknadsskjemaUtil.parseSøknadsskjema(
            Stønadstype.BARNETILSYN,
            data = objectMapper.writeValueAsBytes(skjema),
        )

        assertThat(parsetSkjema).isEqualTo(skjema)
    }

    @Test
    fun `Skal kunne parse søknad av type LÆREMIDLER`() {
        val skjema = SøknadUtil.søknadskjemaLæremidler()
        val parsetSkjema = SøknadsskjemaUtil.parseSøknadsskjema(
            Stønadstype.LÆREMIDLER,
            data = objectMapper.writeValueAsBytes(skjema),
        )

        assertThat(parsetSkjema).isEqualTo(skjema)
    }
}
