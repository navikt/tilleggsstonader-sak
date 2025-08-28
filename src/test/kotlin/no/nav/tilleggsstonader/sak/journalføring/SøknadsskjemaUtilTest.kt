package no.nav.tilleggsstonader.sak.journalføring

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil
import no.nav.tilleggsstonader.sak.util.SøknadDagligReiseUtil
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
            val skjema = SøknadBoutgifterUtil.søknadBoutgifter()
            val parsetSkjema =
                SøknadsskjemaUtil.parseSøknadsskjema(
                    Stønadstype.BOUTGIFTER,
                    data = objectMapper.writeValueAsBytes(skjema),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            assertThat(parsetSkjema.skjema).isEqualTo(skjema)
        }

        @Test
        fun `Skal feile hvis et ukjent felt finnes i skjemaet`() {
            val skjema = SøknadBoutgifterUtil.søknadBoutgifter()
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
            }.hasMessageContaining("Unrecognized field \"ukjentFelt\"")
        }
    }

    @Nested
    inner class ParsingDagligReise {
        @ParameterizedTest
        @EnumSource(value = Stønadstype::class, names = ["DAGLIG_REISE_TSO", "DAGLIG_REISE_TSR"])
        fun `Skal kunne parse søknad av type daglig reise`(stønadstype: Stønadstype) {
            val skjema = SøknadDagligReiseUtil.søknadDagligReise()
            val parsetSkjema =
                SøknadsskjemaUtil.parseSøknadsskjema(
                    stønadstype,
                    data = objectMapper.writeValueAsBytes(skjema),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            assertThat(parsetSkjema.skjema).isEqualTo(skjema)
        }

        @Test
        fun `Skal feile hvis et ukjent felt finnes i skjemaet`() {
            val skjema = SøknadDagligReiseUtil.søknadDagligReise()
            val json =
                objectMapper.readTree(objectMapper.writeValueAsBytes(skjema)).apply {
                    (this as ObjectNode).put("ukjentFelt", "test")
                }
            assertThatThrownBy {
                SøknadsskjemaUtil.parseSøknadsskjema(
                    Stønadstype.DAGLIG_REISE_TSO,
                    data = objectMapper.writeValueAsBytes(json),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            }.hasMessageContaining("Unrecognized field \"ukjentFelt\"")
        }
    }
}
