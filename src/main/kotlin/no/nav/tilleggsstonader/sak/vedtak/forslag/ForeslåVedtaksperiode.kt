package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperioderBeholdIdUtil.beholdTidligereIdnForVedtaksperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate

/**
 * Foreslå vedtaksperioder gir et forslag på perioder ut fra målgrupper, aktiviteter og vilkår.
 * Hvilke av disse som brukes avhenger av stønadstypen, se [no.nav.tilleggsstonader.sak.felles.domain.GrunnlagForVedtaksperiode].
 *
 * I en revurdering fylles hull i med nye vedtaksperioder.
 * Dvs både hull mellom 2 tidligere vedtaksperioder og hull etter tidligere vedtaksperioder.
 *
 * Hvis man har en [tidligsteEndring] endres ikke målgruppe/aktivitet før det datoet.
 * Hvis man mangler eller har en [tidligsteEndring] frem i tiden, fylles hull etter den siste tidligere vedtaksperioden.
 */
object ForeslåVedtaksperiode {
    fun finnVedtaksperiode(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
        forrigeVedtaksperioder: List<Vedtaksperiode>,
        tidligsteEndring: LocalDate?,
    ): List<Vedtaksperiode> {
        val forslag =
            ForeslåVedtaksperioderUtil.foreslåPerioder(
                vilkårperioder = vilkårperioder,
                vilkår = vilkår,
            )
        return beholdTidligereIdnForVedtaksperioder(
            forrigeVedtaksperioder = forrigeVedtaksperioder,
            forslag = forslag,
            tidligsteEndring = tidligsteEndring,
        )
    }

    fun finnVedtaksperiodeUtenAktivitet(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
        forrigeVedtaksperioder: List<Vedtaksperiode>,
        tidligsteEndring: LocalDate?,
    ): List<Vedtaksperiode> {
        val forslag =
            ForeslåVedtaksperioderUtil.foreslåPerioderUtenAktivitet(
                vilkårperioder = vilkårperioder,
                vilkår = vilkår,
            )
        return beholdTidligereIdnForVedtaksperioder(
            forrigeVedtaksperioder = forrigeVedtaksperioder,
            forslag = forslag,
            tidligsteEndring = tidligsteEndring,
        )
    }

    fun finnVedtaksperiodeUtenVilkår(
        vilkårperioder: Vilkårperioder,
        forrigeVedtaksperioder: List<Vedtaksperiode>,
        tidligsteEndring: LocalDate?,
    ): List<Vedtaksperiode> {
        val forslag =
            ForeslåVedtaksperioderUtil.foreslåPerioderUtenVilkår(
                vilkårperioder = vilkårperioder,
            )
        return beholdTidligereIdnForVedtaksperioder(
            forrigeVedtaksperioder = forrigeVedtaksperioder,
            forslag = forslag,
            tidligsteEndring = tidligsteEndring,
        )
    }
}
