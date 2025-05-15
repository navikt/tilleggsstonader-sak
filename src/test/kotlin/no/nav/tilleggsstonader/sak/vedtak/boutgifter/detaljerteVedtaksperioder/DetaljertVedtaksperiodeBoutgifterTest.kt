package no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetaljertVedtaksperiodeBoutgifterTest {
    val førsteJan = LocalDate.of(2024, 1, 1)
    val sisteJan = LocalDate.of(2024, 1, 31)
    val førsteFeb = LocalDate.of(2024, 2, 1)
    val sisteFeb = LocalDate.of(2024, 2, 29)

    val detaljertVedtaksperiodeJanLøpende = detaljertVedtaksperiode(førsteJan, sisteJan, erLøpendeUtgift = true)
    val detaljertVedtaksperiodeFebLøpende = detaljertVedtaksperiode(førsteFeb, sisteFeb, erLøpendeUtgift = true)

    @Nested
    inner class LøpendeVedtaksperioder {
        @Test
        fun `skal kun endre tom og antallMåneder ved sammenslåing av to påfølgende perioder`() {
            val vedtaksperioder =
                listOf(
                    detaljertVedtaksperiodeJanLøpende,
                    detaljertVedtaksperiodeFebLøpende,
                )
            val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
            assertThat(resultat).isEqualTo(listOf(detaljertVedtaksperiodeJanLøpende.copy(tom = sisteFeb, antallMåneder = 2)))
        }

        @Test
        fun `skal ikke slå sammen påfølgende perioder med ulike aktivitet`() {
            val vedtaksperioder =
                listOf(
                    detaljertVedtaksperiodeJanLøpende,
                    detaljertVedtaksperiodeFebLøpende.copy(
                        aktivitet = AktivitetType.TILTAK,
                    ),
                )

            val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
            assertThat(resultat).isEqualTo(vedtaksperioder)
        }

        @Test
        fun `skal ikke slå sammen påfølgende perioder med ulike målgruppe`() {
            val vedtaksperioder =
                listOf(
                    detaljertVedtaksperiodeJanLøpende,
                    detaljertVedtaksperiodeFebLøpende.copy(
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    ),
                )

            val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
            assertThat(resultat).isEqualTo(vedtaksperioder)
        }

        @Test
        fun `skal ikke slå sammen påfølgende perioder med ulik stønadsbeløpMnd`() {
            val vedtaksperioder =
                listOf(
                    detaljertVedtaksperiodeJanLøpende,
                    detaljertVedtaksperiodeFebLøpende.copy(stønadsbeløpMnd = 3000),
                )
            val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
            assertThat(resultat).isEqualTo(vedtaksperioder)
        }

        @Test
        fun `skal ikke slå sammen påfølgende perioder med ulik totalUtgiftMåned`() {
            val vedtaksperioder =
                listOf(
                    detaljertVedtaksperiodeJanLøpende,
                    detaljertVedtaksperiodeFebLøpende.copy(totalUtgiftMåned = 3000),
                )
            val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
            assertThat(resultat).isEqualTo(vedtaksperioder)
        }

        @Test
        fun `skal ikke slå sammen like perioder som ikke er påfølgende`() {
            val vedtaksperioder =
                listOf(
                    detaljertVedtaksperiodeJanLøpende,
                    detaljertVedtaksperiodeFebLøpende.copy(fom = LocalDate.of(2024, 2, 2)),
                )
            val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
            assertThat(resultat).isEqualTo(vedtaksperioder)
        }
    }

    @Nested
    inner class BeggeBoutgifter {
        val vedtaksperiodeJanOvernattingFlereUtgifter =
            detaljertVedtaksperiode(
                førsteJan,
                sisteJan,
                erLøpendeUtgift = false,
                utgifterTilOvernatting =
                    listOf(
                        utgiftTilOvernatting(førsteJan, LocalDate.of(2024, 1, 10)),
                        utgiftTilOvernatting(LocalDate.of(2024, 1, 11), sisteJan),
                    ),
            )

        @Test
        fun `skal Ikke slå sammen overnatting og løpende selv om periodene er påfølgene`() {
            val resultat =
                listOf(vedtaksperiodeJanOvernattingFlereUtgifter, detaljertVedtaksperiodeFebLøpende)
                    .sorterOgMergeSammenhengende()
            assertThat(resultat).isEqualTo(
                listOf(vedtaksperiodeJanOvernattingFlereUtgifter, detaljertVedtaksperiodeFebLøpende),
            )
        }

        @Test
        fun `skal slå sammen løpende men ikke overnatting`() {
            val resultat =
                listOf(
                    vedtaksperiodeJanOvernattingFlereUtgifter,
                    detaljertVedtaksperiodeJanLøpende,
                    detaljertVedtaksperiodeFebLøpende,
                ).sorterOgMergeSammenhengende()

            val sammenslåtteLøpendeUtgifter = detaljertVedtaksperiodeJanLøpende.copy(tom = sisteFeb, antallMåneder = 2)

            assertThat(resultat).isEqualTo(
                listOf(vedtaksperiodeJanOvernattingFlereUtgifter, sammenslåtteLøpendeUtgifter),
            )
        }
    }

    private fun detaljertVedtaksperiode(
        fom: LocalDate,
        tom: LocalDate,
        erLøpendeUtgift: Boolean = true,
        utgifterTilOvernatting: List<UtgiftTilOvernatting> = emptyList(),
        totalUtgift: Int = 8000,
        stønad: Int = 4809,
        aktivitet: AktivitetType = AktivitetType.UTDANNING,
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
        antallMåneder: Int = 1,
    ) = DetaljertVedtaksperiodeBoutgifter(
        fom = fom,
        tom = tom,
        aktivitet = aktivitet,
        målgruppe = målgruppe,
        antallMåneder = antallMåneder,
        erLøpendeUtgift = erLøpendeUtgift,
        totalUtgiftMåned = totalUtgift,
        stønadsbeløpMnd = stønad,
        utgifterTilOvernatting = utgifterTilOvernatting,
    )

    private fun utgiftTilOvernatting(
        fom: LocalDate,
        tom: LocalDate,
        utgift: Int = 1000,
        beløp: Int = 1000,
    ) = UtgiftTilOvernatting(
        fom = fom,
        tom = tom,
        utgift = utgift,
        beløpSomDekkes = beløp,
    )
}
