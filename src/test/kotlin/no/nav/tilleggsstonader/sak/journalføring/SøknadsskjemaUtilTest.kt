package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaFlytting
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaROAH
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flyttemåte
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHAdresseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHAktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHBarnOgSærligeBehovAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHReiseInfo
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil
import no.nav.tilleggsstonader.sak.util.SøknadDagligReiseUtil
import no.nav.tilleggsstonader.sak.util.SøknadFlyttingUtil
import no.nav.tilleggsstonader.sak.util.SøknadReiseTilSamlingUtil
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDateTime

class SøknadsskjemaUtilTest {
    @Test
    fun `skal parse ROAH uten å kreve sluttdato på reisen`() {
        val skjema =
            InnsendtSkjema(
                ident = "12345678910",
                mottattTidspunkt = (1 januar 2026).atTime(12, 0),
                språk = Språkkode.NB,
                skjema =
                    SøknadsskjemaROAH(
                        hovedytelse =
                            HovedytelseAvsnitt(
                                EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                                null,
                            ),
                        aktivitet = ROAHAktivitetAvsnitt(null, null, null),
                        adresser = ROAHAdresseAvsnitt(null, null, null, null),
                        barnOgSærligeBehov = ROAHBarnOgSærligeBehovAvsnitt(null, null, null, null),
                        reiser = listOf(ROAHReiseInfo(null, null, null, null)),
                        dokumentasjon = emptyList(),
                    ),
            )
        val json = jsonMapper.valueToTree<ObjectNode>(skjema)
        (json.path("skjema").path("reiser")[0] as ObjectNode).remove("tom")

        val parset =
            SøknadsskjemaUtil.parseSøknadsskjema(
                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                jsonMapper.writeValueAsBytes(json),
                skjema.mottattTidspunkt,
            )

        assertThat(parset).isEqualTo(skjema)
    }

    @Test
    fun `skal ikke eksponere skjemaverdier i feil fra ROAH-parsing`() {
        assertThatThrownBy {
            SøknadsskjemaUtil.parseSøknadsskjema(
                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                """{"ident":"skal-ikke-logges","mottattTidspunkt":"skal-ikke-logges"}""".toByteArray(),
                (1 januar 2026).atStartOfDay(),
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessage("Kunne ikke lese ROAH-søknad")
            .hasNoCause()
    }

    @Test
    fun `Skal kunne parse søknad av type BARNETILSYN`() {
        val skjema = SøknadUtil.søknadskjemaPassAvBarn()
        val parsetSkjema =
            SøknadsskjemaUtil.parseSøknadsskjema(
                Stønadstype.BARNETILSYN,
                data = jsonMapper.writeValueAsBytes(skjema),
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
                data = jsonMapper.writeValueAsBytes(skjema),
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
                    data = jsonMapper.writeValueAsBytes(skjema),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            assertThat(parsetSkjema.skjema).isEqualTo(skjema)
        }

        @Test
        fun `Skal feile hvis et ukjent felt finnes i skjemaet`() {
            val skjema = SøknadBoutgifterUtil.søknadBoutgifter()
            val json =
                jsonMapper.readTree(jsonMapper.writeValueAsBytes(skjema)).apply {
                    (this as ObjectNode).put("ukjentFelt", "test")
                }
            assertThatThrownBy {
                SøknadsskjemaUtil.parseSøknadsskjema(
                    Stønadstype.BOUTGIFTER,
                    data = jsonMapper.writeValueAsBytes(json),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            }.hasMessageContaining("Unrecognized property \"ukjentFelt\"")
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
                    data = jsonMapper.writeValueAsBytes(skjema),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            assertThat(parsetSkjema.skjema).isEqualTo(skjema)
        }

        @Test
        fun `Skal feile hvis et ukjent felt finnes i skjemaet`() {
            val skjema = SøknadDagligReiseUtil.søknadDagligReise()
            val json =
                jsonMapper.readTree(jsonMapper.writeValueAsBytes(skjema)).apply {
                    (this as ObjectNode).put("ukjentFelt", "test")
                }
            assertThatThrownBy {
                SøknadsskjemaUtil.parseSøknadsskjema(
                    Stønadstype.DAGLIG_REISE_TSO,
                    data = jsonMapper.writeValueAsBytes(json),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            }.hasMessageContaining("Unrecognized property \"ukjentFelt\"")
        }
    }

    @Nested
    inner class ParsingReiseTilSamling {
        @Test
        fun `Skal kunne parse søknad av type REISE_TIL_SAMLING_TSO`() {
            val skjema = SøknadReiseTilSamlingUtil.søknadReiseTilSamling()
            val parsetSkjema =
                SøknadsskjemaUtil.parseSøknadsskjema(
                    Stønadstype.REISE_TIL_SAMLING_TSO,
                    data = jsonMapper.writeValueAsBytes(skjema),
                    mottattTidspunkt = LocalDateTime.now(),
                )
            assertThat(parsetSkjema).isEqualTo(skjema)
        }

        @Nested
        inner class ParsingFlytting {
            @ParameterizedTest
            @EnumSource(value = Stønadstype::class, names = ["FLYTTING_TSO", "FLYTTING_TSR"])
            fun `parser begge flyttemåter med obligatoriske felter og vedlegg`(stønadstype: Stønadstype) {
                Flyttemåte.entries.forEach { flyttemåte ->
                    val skjema =
                        SøknadFlyttingUtil.søknadFlytting(
                            flyttemåte = flyttemåte,
                            medDokumentasjon = true,
                        )
                    val parset =
                        SøknadsskjemaUtil.parseSøknadsskjema(
                            stønadstype,
                            jsonMapper.writeValueAsBytes(skjema),
                            skjema.mottattTidspunkt,
                        )
                    assertThat(parset).isEqualTo(skjema)
                }
            }

            @ParameterizedTest
            @EnumSource(value = Stønadstype::class, names = ["FLYTTING_TSO", "FLYTTING_TSR"])
            fun `Skal kunne parse flyttesøknad`(stønadstype: Stønadstype) {
                val skjema =
                    InnsendtSkjema(
                        ident = "12345678910",
                        mottattTidspunkt = LocalDateTime.now(),
                        språk = Språkkode.NB,
                        skjema =
                            SøknadsskjemaFlytting(
                                hovedytelse =
                                    HovedytelseAvsnitt(
                                        hovedytelse =
                                            EnumFlereValgFelt(
                                                "",
                                                listOf(VerdiFelt(Hovedytelse.AAP, "AAP")),
                                                emptyList(),
                                            ),
                                        arbeidOgOpphold = null,
                                    ),
                                flyttingDetaljer = null,
                                dokumentasjon = emptyList(),
                            ),
                    )

                val parsetSkjema =
                    SøknadsskjemaUtil.parseSøknadsskjema(
                        stønadstype,
                        data = jsonMapper.writeValueAsBytes(skjema),
                        mottattTidspunkt = LocalDateTime.now(),
                    )

                assertThat(parsetSkjema).isEqualTo(skjema)
            }
        }
    }
}
