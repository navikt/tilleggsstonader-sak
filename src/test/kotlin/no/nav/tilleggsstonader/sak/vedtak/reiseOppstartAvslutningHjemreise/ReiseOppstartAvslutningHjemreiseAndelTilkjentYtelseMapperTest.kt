package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReiseOppstartAvslutningHjemreiseAndelTilkjentYtelseMapperTest {
    @Test
    fun `mapper alle tiltaksvarianter for TSR til riktig TypeAndel`() {
        val forventet =
            mapOf(
                TypeAktivitet.ARBFORB to TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSFORBEREDENDE,
                TypeAktivitet.ARBTREN to TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSTRENING,
                TypeAktivitet.AVKLARAG to TypeAndel.REISE_OPPSTART_TILTAK_AVKLARING,
                TypeAktivitet.ENKELAMO to TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_AMO,
                TypeAktivitet.ENKFAGYRKE to TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
                TypeAktivitet.GRUPPEAMO to TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_AMO,
                TypeAktivitet.GRUFAGYRKE to TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
                TypeAktivitet.HOYEREUTD to TypeAndel.REISE_OPPSTART_TILTAK_HØYERE_UTDANNING,
                TypeAktivitet.JOBBK to TypeAndel.REISE_OPPSTART_TILTAK_JOBBKLUBB,
                TypeAktivitet.INDOPPFAG to TypeAndel.REISE_OPPSTART_TILTAK_OPPFØLGING,
            )

        forventet.forEach { (tiltaksvariant, typeAndel) ->
            assertThat(finnTypeAndelFraTiltaksvariantReiseOppstart(tiltaksvariant)).isEqualTo(typeAndel)
        }
    }

    @Test
    fun `kaster feil for tiltaksvariant som ikke er en del av reise oppstart sitt reduserte tiltakssett`() {
        val feil =
            catchThrowableOfType<IllegalStateException> {
                finnTypeAndelFraTiltaksvariantReiseOppstart(TypeAktivitet.UTVOPPFOPL)
            }
        assertThat(feil.message).contains("Kan ikke mappe til TypeAndel")
    }

    @Test
    fun `mapper offentlig transport for TSO til riktig TypeAndel basert på målgruppe i overlappende vedtaksperiode`() {
        val reisedato = 5 januar 2026 // mandag
        val aktivitetId = VilkårperiodeGlobalId.random()
        val saksbehandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO),
            )
        val vedtaksperioder =
            listOf(
                vedtaksperiode(
                    fom = 1 januar 2026,
                    tom = 31 januar 2026,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                ),
            )

        val beregningsresultat =
            BeregningsresultatOffentligTransport(
                reiseId = dummyReiseId,
                aktivitetId = aktivitetId,
                grunnlag =
                    BeregningsgrunnlagOffentligTransport(
                        adresse = "Oppstartsgata 1",
                        fom = reisedato,
                        tom = reisedato,
                        vedtaksperioder = emptyList(),
                    ),
                beløp = 40.toBigDecimal(),
            )

        val andel =
            beregningsresultat.mapTilAndelTilkjentYtelse(
                saksbehandling = saksbehandling,
                vedtaksperioder = vedtaksperioder,
                aktiviteter = emptyList(),
            )

        assertThat(andel.type).isEqualTo(TypeAndel.REISE_OPPSTART_AAP)
        assertThat(andel.satstype).isEqualTo(Satstype.DAG)
        assertThat(andel.fom).isEqualTo(reisedato)
        assertThat(andel.tom).isEqualTo(reisedato)
        assertThat(andel.beløp).isEqualTo(40)
    }

    @Test
    fun `flytter reisedato som havner på en lørdag til påfølgende mandag`() {
        val lørdag = 3 januar 2026
        val mandag = 5 januar 2026
        val saksbehandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO),
            )
        val vedtaksperioder =
            listOf(
                vedtaksperiode(
                    fom = 1 januar 2026,
                    tom = 31 januar 2026,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                ),
            )
        val beregningsresultat =
            BeregningsresultatOffentligTransport(
                reiseId = dummyReiseId,
                aktivitetId = VilkårperiodeGlobalId.random(),
                grunnlag =
                    BeregningsgrunnlagOffentligTransport(
                        adresse = "Oppstartsgata 1",
                        fom = lørdag,
                        tom = lørdag,
                        vedtaksperioder = emptyList(),
                    ),
                beløp = 40.toBigDecimal(),
            )

        val andel =
            beregningsresultat.mapTilAndelTilkjentYtelse(
                saksbehandling = saksbehandling,
                vedtaksperioder = vedtaksperioder,
                aktiviteter = emptyList(),
            )

        assertThat(andel.fom).isEqualTo(mandag)
        assertThat(andel.tom).isEqualTo(mandag)
        assertThat(andel.utbetalingsdato).isEqualTo(mandag)
    }

    @Test
    fun `kaster feil hvis reisen strekker seg over mer enn én dag`() {
        val saksbehandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO),
            )
        val beregningsresultat =
            BeregningsresultatOffentligTransport(
                reiseId = dummyReiseId,
                aktivitetId = VilkårperiodeGlobalId.random(),
                grunnlag =
                    BeregningsgrunnlagOffentligTransport(
                        adresse = "Oppstartsgata 1",
                        fom = 1 januar 2026,
                        tom = 2 januar 2026,
                        vedtaksperioder = emptyList(),
                    ),
                beløp = 40.toBigDecimal(),
            )

        val feil =
            catchThrowableOfType<Feil> {
                beregningsresultat.mapTilAndelTilkjentYtelse(
                    saksbehandling = saksbehandling,
                    vedtaksperioder = emptyList(),
                    aktiviteter = emptyList(),
                )
            }

        assertThat(feil.message).contains("gjelder én enkelt dag")
    }

    @Test
    fun `mapper offentlig transport for TSR til riktig TypeAndel basert på tiltaksvariant på aktiviteten`() {
        val reisedato = 5 januar 2026 // mandag
        val aktivitet =
            VilkårperiodeTestUtil.aktivitet(tiltaksvariant = TypeAktivitet.GRUPPEAMO)
        val saksbehandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR),
            )
        val beregningsresultat =
            BeregningsresultatOffentligTransport(
                reiseId = dummyReiseId,
                aktivitetId = aktivitet.globalId,
                grunnlag =
                    BeregningsgrunnlagOffentligTransport(
                        adresse = "Oppstartsgata 1",
                        fom = reisedato,
                        tom = reisedato,
                        vedtaksperioder = emptyList(),
                    ),
                beløp = 40.toBigDecimal(),
            )

        val andel =
            beregningsresultat.mapTilAndelTilkjentYtelse(
                saksbehandling = saksbehandling,
                vedtaksperioder = emptyList(),
                aktiviteter = listOf(aktivitet),
            )

        assertThat(andel.type).isEqualTo(TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_AMO)
    }
}
