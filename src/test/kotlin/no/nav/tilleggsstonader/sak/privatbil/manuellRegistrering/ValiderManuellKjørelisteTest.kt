package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.privatbil.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.util.finnMandagNesteUke
import no.nav.tilleggsstonader.sak.util.finnNesteSøndag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBilBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ValiderManuellKjørelisteTest {
    val reiseId = ReiseId.random()
    val fomJan = 5 januar 2026
    val tomJan = 18 januar 2026

    @Test
    fun `skal ikke være mulig å sende inn kjørelister fremover i tid`() {
        val dagensDato = LocalDate.now()
        val fom = dagensDato.minusMonths(1)
        val tom = dagensDato.plusMonths(1)
        val mandagNesteUke = dagensDato.finnMandagNesteUke()

        assertThatThrownBy {
            validerManuellKjøreliste(
                innsendtKjøreliste =
                    InnsendtKjøreliste(
                        reiseId = reiseId,
                        reisedager = lagKjørteDager(ukeFom = mandagNesteUke, ukeTom = mandagNesteUke.finnNesteSøndag()),
                    ),
                rammevedtakForReise = rammeForReise(fom, tom),
                eksisterendeKjørelister = emptyList(),
            )
        }.hasMessageContaining("Kan ikke registrere kjøreliste for dager som er fremover i tid")
    }

    @Test
    fun `skal ikke være mulig å registrere kjørelister for uker som alt er innsendt`() {
        assertThatThrownBy {
            validerManuellKjøreliste(
                innsendtKjøreliste =
                    InnsendtKjøreliste(
                        reiseId = reiseId,
                        reisedager = lagKjørteDager(ukeFom = fomJan, ukeTom = 11 januar 2026),
                    ),
                rammevedtakForReise = rammeForReise(),
                eksisterendeKjørelister = listOf(lagKjørelisteForUke(ukeFom = fomJan, ukeTom = tomJan)),
            )
        }.hasMessageContaining("Innsendte dager overlapper med tidligere innsendte kjørelister")
    }

    @Test
    fun `skal ikke være mulig å sende inn kjøreliste hvis rammevedtak ikke finnes`() {
        assertThatThrownBy {
            validerManuellKjøreliste(
                innsendtKjøreliste = InnsendtKjøreliste(reiseId = reiseId, reisedager = lagKjørteDager(fomJan, tomJan)),
                rammevedtakForReise = null,
                eksisterendeKjørelister = emptyList(),
            )
        }.hasMessageContaining("Fant ikke rammevedtak for reise")
    }

    @Test
    fun `skal ikke kunne sende inn kjøreliste utenfor rammevedtaket`() {
        assertThatThrownBy {
            validerManuellKjøreliste(
                innsendtKjøreliste =
                    InnsendtKjøreliste(
                        reiseId = reiseId,
                        reisedager = lagKjørteDager(ukeFom = 19 januar 2026, ukeTom = 25 januar 2026),
                    ),
                rammevedtakForReise = rammeForReise(),
                eksisterendeKjørelister = emptyList(),
            )
        }.hasMessageContaining("Perioden for innsendt kjøreliste er utenfor rammevedtaket")
    }

    @Test
    fun `skal ikke kunne sende inn ufullstendige uker`() {
        assertThatThrownBy {
            validerManuellKjøreliste(
                innsendtKjøreliste =
                    InnsendtKjøreliste(
                        reiseId = reiseId,
                        reisedager =
                            listOf(
                                KjørelisteDag(
                                    dato = 5 januar 2026,
                                    harKjørt = true,
                                    parkeringsutgift = null,
                                ),
                            ),
                    ),
                rammevedtakForReise = rammeForReise(),
                eksisterendeKjørelister = emptyList(),
            )
        }.hasMessageContaining("Uke 2 er sendt inn ufullstendig.")
    }

    @Nested
    inner class Overlapper {
        // Uke 3 2026: 12–18 jan | Uke 4: 19–25 jan | Uke 5: 26 jan–1 feb | Uke 6: 2–8 feb

        @Test
        fun `overlapper returnerer true når to kjørelister deler reisedager i samme uke`() {
            // Kjøreliste A: uke 3 og uke 5
            // Kjøreliste B: uke 4 og uke 5  →  uke 5 er felles
            val a =
                InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = lagKjørteDager(12 januar 2026, 18 januar 2026) + lagKjørteDager(26 januar 2026, 1 februar 2026),
                )
            val b =
                InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = lagKjørteDager(19 januar 2026, 25 januar 2026) + lagKjørteDager(26 januar 2026, 1 februar 2026),
                )

            assertThat(a.overlapper(b)).isTrue()
        }

        @Test
        fun `overlapper returnerer false når kjørelister har dager i forskjellige uker, selv om fom-tom overlapper`() {
            // Kjøreliste A: uke 3 og uke 5  (fom=12 jan, tom=1 feb)
            // Kjøreliste B: uke 4 og uke 6  (fom=19 jan, tom=8 feb)
            // Gammel Periode.overlapper ville gitt true fordi fom/tom-rangene overlapper,
            // men ingen reisedag er felles.
            val a =
                InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = lagKjørteDager(12 januar 2026, 18 januar 2026) + lagKjørteDager(26 januar 2026, 1 februar 2026),
                )
            val b =
                InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = lagKjørteDager(19 januar 2026, 25 januar 2026) + lagKjørteDager(2 februar 2026, 8 februar 2026),
                )

            assertThat(a.overlapper(b)).isFalse()
        }
    }

    private fun lagKjørelisteForUke(
        ukeFom: LocalDate,
        ukeTom: LocalDate,
    ): Kjøreliste =
        Kjøreliste(
            id = KjørelisteId.random(),
            journalpostId = "journalpostId",
            fagsakId = FagsakId.random(),
            datoMottatt = LocalDateTime.now(),
            data =
                InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = lagKjørteDager(ukeFom, ukeTom),
                ),
        )

    private fun lagKjørteDager(
        ukeFom: LocalDate,
        ukeTom: LocalDate,
    ): List<KjørelisteDag> =
        Datoperiode(fom = ukeFom, tom = ukeTom).alleDatoer().map { dato ->
            KjørelisteDag(
                dato = dato,
                harKjørt = true,
                parkeringsutgift = null,
            )
        }

    private fun rammeForReise(
        fom: LocalDate = fomJan,
        tom: LocalDate = tomJan,
    ) = RammevedtakForReiseMedPrivatBil(
        reiseId = reiseId,
        aktivitetsadresse = null,
        aktivitetType = AktivitetType.TILTAK,
        tiltaksvariant = null,
        grunnlag =
            RammevedtakForReiseMedPrivatBilBeregningsgrunnlag(
                fom = fom,
                tom = tom,
                delperioder = emptyList(),
                reiseavstandEnVei = BigDecimal("10"),
                vedtaksperioder = emptyList(),
            ),
    )
}
