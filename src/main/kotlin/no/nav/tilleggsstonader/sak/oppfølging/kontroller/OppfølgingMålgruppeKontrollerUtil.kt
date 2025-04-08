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

/**
 * Finner endringer av målgrupper som påvirker vedtaksperiode.
 *
 * Tar ikke hensyn til at man har parallelle perioder, eks AAP og UFØRETRYGD.
 * Eks at AAP blir forkortet, men blir påvirker ikke vedtaksperioden fordi UFØRETRYG-perioden finnes der uansett.
 * AAP: 1.jan - 31.mars
 * UFØRETRYGD: 1.mars - 30.april
 * Vedtaksperiode 1.jan - 31.mars
 * Endring AAP til 1.jan - 28.feb
 * Her vill man få ett utslag fordi AAP kan påvirke vedtaksperioden.
 * Men ettersom bruker har UFØRETRYGD trenger man egentlige ikke å gjøre noe
 */
object OppfølgingMålgruppeKontrollerUtil {
    fun finnEndringer(
        inngangsvilkår: List<OppfølgingInngangsvilkårMålgruppe>,
        vedtaksperioder: List<Vedtaksperiode>,
        registerYtelser: Map<MålgruppeType, List<Datoperiode>>,
    ): List<PeriodeForKontroll> =
        inngangsvilkår
            .map {
                PeriodeForKontroll(
                    fom = it.fom,
                    tom = it.tom,
                    type = it.målgruppe,
                    endringer = it.finnEndringer(vedtaksperioder, registerYtelser),
                )
            }

    /**
     * Finner ut om endringer for et inngangsvilkår påvirker en vedtaksperiode.
     */
    private fun OppfølgingInngangsvilkårMålgruppe.finnEndringer(
        vedtaksperioder: List<Vedtaksperiode>,
        ytelserPerMålgruppe: Map<MålgruppeType, List<Datoperiode>>,
    ): List<Kontroll> {
        val snittInngangsvilkårVedtaksperioder =
            vedtaksperioder
                .filter { it.målgruppe == this.målgruppe.faktiskMålgruppe() }
                .mapNotNull { it.beregnSnitt(this) }

        val ytelserFraRegister = ytelserPerMålgruppe[this.målgruppe] ?: emptyList()

        return snittInngangsvilkårVedtaksperioder
            .flatMap { snittVedtaksperiode -> beregnSnitt(snittVedtaksperiode, ytelserFraRegister) }
            .utenAAPSomGjelderFraOgMedNesteMåned(målgruppe = this.målgruppe)
    }

    /**
     * Finner første snittet av snitt av vedtaksperiode og inngangsvilkår, samt registerdata.
     *
     * Tar ikke hensyn til at ytelse fra registeret kan ha blitt splittet opp med opphør i midten.
     * Her vil man kun finne første snittet og se om det er ulikt eller ikke.
     */
    private fun beregnSnitt(
        snittVedtaksperiode: Vedtaksperiode,
        ytelserFraRegister: List<Datoperiode>,
    ): List<Kontroll> {
        @Suppress("SimplifiableCallChain")
        val snitt = ytelserFraRegister.mapNotNull { it.beregnSnitt(snittVedtaksperiode) }.firstOrNull()
        return if (snitt == null) {
            listOf(Kontroll(ÅrsakKontroll.INGEN_TREFF))
        } else {
            OppfølgingKontrollerUtil.finnEndringFomTom(snittVedtaksperiode, snitt)
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
