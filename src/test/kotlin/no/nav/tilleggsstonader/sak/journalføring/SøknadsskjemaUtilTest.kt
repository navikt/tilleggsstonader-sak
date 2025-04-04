package no.nav.tilleggsstonader.sak.journalføring

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SøknadsskjemaUtilTest {
    @Test
    fun `Skal kunne parse søknad av type BARNETILSYN`() {
        val skjema = SøknadUtil.søknadskjemaBarnetilsyn()
        val parsetSkjema =
            SøknadsskjemaUtil.parseSøknadsskjema(
                Stønadstype.BARNETILSYN,
                data = objectMapper.writeValueAsBytes(skjema),
                mottattTidspunkt = LocalDateTime.now(),
            )

        assertThat(parsetSkjema).isEqualTo(skjema)
    }

    @Test
    fun `Skal kunne parse søknad av type LÆREMIDLER`() {
        val skjema = SøknadUtil.søknadskjemaLæremidler()
        val parsetSkjema =
            SøknadsskjemaUtil.parseSøknadsskjema(
                Stønadstype.LÆREMIDLER,
                data = objectMapper.writeValueAsBytes(skjema),
                mottattTidspunkt = LocalDateTime.now(),
            )

        assertThat(parsetSkjema).isEqualTo(skjema)
    }

    @Nested
    inner class ParsingBoutgifter {
        @Test
        fun `Skal kunne parse søknad av type BOUTGIFTER`() {
            val skjema = SøknadBoutgifterUtil.søknadskjemaBoutgifter()
            val parsetSkjema =
                SøknadsskjemaUtil.parseSøknadsskjema(
                    Stønadstype.BOUTGIFTER,
                    data = objectMapper.writeValueAsBytes(skjema),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            assertThat(parsetSkjema).isEqualTo(skjema)
        }

        @Test
        fun `Skal feile hvis et ukjent felt finnes i skjemaet`() {
            val skjema = SøknadBoutgifterUtil.søknadskjemaBoutgifter()
            val json =
                objectMapper.readTree(objectMapper.writeValueAsBytes(skjema)).apply {
                    (this as ObjectNode).put("ukjentFelt", "test")
                }
            assertThatThrownBy {
                SøknadsskjemaUtil.parseSøknadsskjema(
                    Stønadstype.BOUTGIFTER,
                    data = objectMapper.writeValueAsBytes(json),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            }.hasMessageContaining("yolo")
        }
    }
}
