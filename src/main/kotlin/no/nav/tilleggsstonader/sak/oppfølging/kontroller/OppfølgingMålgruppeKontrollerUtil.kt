package no.nav.tilleggsstonader.sak.oppfølging.kontroller

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.oppfølging.Kontroll
import no.nav.tilleggsstonader.sak.oppfølging.PeriodeForKontroll
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingInngangsvilkårMålgruppe
import no.nav.tilleggsstonader.sak.oppfølging.ÅrsakKontroll
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.YearMonth

object OppfølgingMålgruppeKontrollerUtil {
    fun finnEndringer(
        målgrupper: List<OppfølgingInngangsvilkårMålgruppe>,
        vedtaksperioder: List<Vedtaksperiode>,
        registerYtelser: Map<MålgruppeType, List<Datoperiode>>,
    ): List<PeriodeForKontroll> =
        målgrupper
            .map {
                PeriodeForKontroll(
                    fom = it.fom,
                    tom = it.tom,
                    type = it.målgruppe,
                    endringer = it.finnEndringer(vedtaksperioder, registerYtelser),
                )
            }

    /**
     * Finner ut om en
     * Ønsker å finne om et inngangsvilkår fortsatt er gyldig
     * Om snittet av inngangsvilkåret og registerinfo er kortere enn inngangsvilkåret og om dette påvirker vedtaksperioden
     *
     * Hvis et inngangsvilkår slutter før en vedtaksperiode, er det kun snittet av disse som er interessant
     */
    private fun OppfølgingInngangsvilkårMålgruppe.finnEndringer(
        vedtaksperioder: List<Vedtaksperiode>,
        ytelserPerMålgruppe: Map<MålgruppeType, List<Datoperiode>>,
    ): List<Kontroll> {
        val snittInngangsvilkårVedtaksperiode =
            vedtaksperioder
                .filter { it.målgruppe == this.målgruppe.faktiskMålgruppe() }
                .mapNotNull { it.beregnSnitt(this) }

        val ytelserFraRegister = ytelserPerMålgruppe[this.målgruppe] ?: emptyList()

        return snittInngangsvilkårVedtaksperiode.flatMap { vedtaksperiode ->
            beregnSnitt(ytelserFraRegister, vedtaksperiode)
        }.utenAAPSomGjelderFraOgMedNesteMåned(målgruppe = this.målgruppe)
    }

    @Suppress("SimplifiableCallChain")
    private fun beregnSnitt(
        ytelserFraRegister: List<Datoperiode>,
        vedtaksperiode: Vedtaksperiode
    ): List<Kontroll> {
        val snitt = ytelserFraRegister.mapNotNull { it.beregnSnitt(vedtaksperiode) }.firstOrNull()
        return if (snitt == null) {
            listOf(Kontroll(ÅrsakKontroll.INGEN_TREFF))
        } else {
            OppfølgingKontrollerUtil.finnEndringFomTom(vedtaksperiode, snitt)
        }
    }

    /**
     * Filtrerer vekk kontroller hvis det kun gjelder AAP som påvirkes etter neste måned
     * Dette gjøres fordi AAP ofte blir forlenget og då for å unngå at vi oppretter oppfølging for tidlig
     */
    private fun List<Kontroll>.utenAAPSomGjelderFraOgMedNesteMåned(målgruppe: MålgruppeType): List<Kontroll> {
        val førsteDagINestNesteMåned = YearMonth.now().plusMonths(1).atEndOfMonth()
        return this.filterNot {
            målgruppe == MålgruppeType.AAP &&
                    it.årsak == ÅrsakKontroll.TOM_ENDRET &&
                    it.tom!! >= førsteDagINestNesteMåned
        }
    }
}