package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.privatbil.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.util.finnNesteSøndag
import no.nav.tilleggsstonader.sak.util.iDagHvisMandagEllerForrigeMandag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBilBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThatThrownBy
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
        val denneUkensMandag = dagensDato.iDagHvisMandagEllerForrigeMandag()

        assertThatThrownBy {
            validerManuellKjøreliste(
                innsendtKjøreliste = InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = lagKjørteDager(ukeFom = denneUkensMandag, ukeTom = denneUkensMandag.finnNesteSøndag())
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
                innsendtKjøreliste = InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = lagKjørteDager(ukeFom = fomJan, ukeTom = 11 januar 2026)
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
                innsendtKjøreliste = InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = lagKjørteDager(ukeFom = 19 januar 2026, ukeTom = 25 januar 2026)
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
                innsendtKjøreliste = InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = listOf(KjørelisteDag(
                        dato = 5 januar 2026,
                        harKjørt = true,
                        parkeringsutgift = null
                    ))
                ),
                rammevedtakForReise = rammeForReise(),
                eksisterendeKjørelister = emptyList(),
            )
        }.hasMessageContaining("Uke 2 er sendt inn ufullstendig.")
    }

    private fun lagKjørelisteForUke(ukeFom: LocalDate, ukeTom: LocalDate): Kjøreliste =
        Kjøreliste(
            id = KjørelisteId.random(),
            journalpostId = "journalpostId",
            fagsakId = FagsakId.random(),
            datoMottatt = LocalDateTime.now(),
            data = InnsendtKjøreliste(
                reiseId = reiseId,
                reisedager = lagKjørteDager(ukeFom, ukeTom)
            )
        )

    private fun lagKjørteDager(ukeFom: LocalDate, ukeTom: LocalDate): List<KjørelisteDag> =
        Datoperiode(fom = ukeFom, tom = ukeTom).alleDatoer().map { dato ->
            KjørelisteDag(
                dato = dato,
                harKjørt = true,
                parkeringsutgift = null
            )
        }

    private fun rammeForReise(
        fom: LocalDate = fomJan,
        tom: LocalDate = tomJan
    ) = RammevedtakForReiseMedPrivatBil(
        reiseId = reiseId,
        aktivitetsadresse = null,
        aktivitetType = AktivitetType.TILTAK,
        tiltaksvariant = null,
        grunnlag = RammevedtakForReiseMedPrivatBilBeregningsgrunnlag(
            fom = fom,
            tom = tom,
            delperioder = emptyList(),
            reiseavstandEnVei = BigDecimal("10"),
            vedtaksperioder = emptyList()
        ),
    )
}