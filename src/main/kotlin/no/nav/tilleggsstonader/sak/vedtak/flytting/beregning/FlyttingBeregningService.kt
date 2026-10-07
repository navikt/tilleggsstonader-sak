package no.nav.tilleggsstonader.sak.vedtak.flytting.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.libs.feil.feilHvisIkke
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.avrundetStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagEgenKjøring
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlyttevilkår
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FlyttingVilkårService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class FlyttingBeregningService(
    private val flyttingVilkårService: FlyttingVilkårService,
    private val satsPrivatBilProvider: SatsPrivatBilProvider,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
) {
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatFlytting {
        feilHvis(vedtaksperioder.isEmpty()) { "Vedtaksperioder kan ikke være tomme" }
        // TODO - ikke hardkode INNVILGELSE
        vedtaksperiodeValideringService.validerVedtaksperioder(vedtaksperioder, behandling, typeVedtak = TypeVedtak.INNVILGELSE)

        val vilkår = flyttingVilkårService.hentVilkårForBehandling(behandling.id)
        val relevanteVilkår =
            vilkår
                .filter { it.status != VilkårStatus.SLETTET }
                .filter { vilkår -> overlapperVedtaksperiode(vilkår, vedtaksperioder) }
                .sortedBy { it.fom }

        feilHvis(relevanteVilkår.isEmpty()) { "Fant ingen flyttevilkår som kan beregnes for vedtaksperiodene" }

        return BeregningsresultatFlytting(
            resultater =
                relevanteVilkår.map { vilkår ->
                    val (fom, tom) = validerVilkår(vilkår, vedtaksperioder)
                    beregnVilkår(vilkår, fom, tom)
                },
        )
    }

    private fun validerVilkår(
        vilkår: Vilkår,
        vedtaksperioder: List<Vedtaksperiode>,
    ): Pair<LocalDate, LocalDate> {
        feilHvisIkke(vilkår.resultat == Vilkårsresultat.OPPFYLT) {
            "Flyttevilkår ${vilkår.id} må være oppfylt før beregning"
        }
        val fom =
            vilkår.fom
                ?: feil("Flyttevilkår ${vilkår.id} mangler FOM")
        val tom =
            vilkår.tom
                ?: feil("Flyttevilkår ${vilkår.id} mangler TOM")
        feilHvisIkke(erFullstendigDekket(fom, tom, vedtaksperioder)) {
            "Vedtaksperiodene må dekke hele flyttevilkåret ${vilkår.id}"
        }
        return fom to tom
    }

    private fun beregnVilkår(
        vilkår: Vilkår,
        fom: LocalDate,
        tom: LocalDate,
    ): BeregningsresultatFlyttevilkår =
        when (val fakta = vilkår.fakta) {
            is FaktaFlyttebyrå -> beregnFlyttebyrå(fom, tom, fakta)
            is FaktaFlytteSelv -> beregnEgenKjøring(fom, tom, fakta)
            else -> feil("Flyttevilkår ${vilkår.id} mangler fullstendige flyttefakta")
        }

    private fun beregnFlyttebyrå(
        fom: LocalDate,
        tom: LocalDate,
        fakta: FaktaFlyttebyrå,
    ): BeregningsresultatFlyttevilkår {
        val tilbud1Pris = fakta.tilbud1.pris?.toBigDecimal() ?: feil("Flyttevilkår  mangler pris på tilbud 1")
        val tilbud2Pris = fakta.tilbud2.pris?.toBigDecimal() ?: feil("Flyttevilkår  mangler pris på tilbud 2")
        feilHvis(tilbud1Pris.signum() < 0 || tilbud2Pris.signum() < 0) { "Flyttebyråtilbud kan ikke ha negativ pris" }
        val grunnlag = BeregningsgrunnlagFlyttebyrå(tilbud1Pris, tilbud2Pris)
        return BeregningsresultatFlyttevilkår(
            fom = fom,
            tom = tom,
            grunnlag = grunnlag,
            beløp = minOf(tilbud1Pris, tilbud2Pris).avrundetStønadsbeløp(),
        )
    }

    private fun beregnEgenKjøring(
        fom: LocalDate,
        tom: LocalDate,
        fakta: FaktaFlytteSelv,
    ): BeregningsresultatFlyttevilkår {
        val avstandEnVei = fakta.avstandEnVei ?: feil("Flyttevilkår mangler avstand én vei")
        feilHvis(avstandEnVei < 0) { "Avstand én vei kan ikke være negativ" }
        val sats =
            satsPrivatBilProvider.finnRelevantKilometerSatsForPeriode(PeriodeDato(fom)).let {
                it.beløp to it.bekreftet
            }
        val henger = fakta.henger.tilBeløp()
        val bompenger = fakta.bompenger.tilBeløp()
        val ferge = fakta.ferge.tilBeløp()
        val parkering = fakta.parkering.tilBeløp()
        val grunnlag =
            BeregningsgrunnlagEgenKjøring(
                avstandEnVei = avstandEnVei,
                sats = sats.first,
                satsBekreftet = sats.second,
                henger = henger,
                bompenger = bompenger,
                ferge = ferge,
                parkering = parkering,
            )
        val beløp =
            avstandEnVei.toBigDecimal() * sats.first +
                henger + bompenger + ferge + parkering
        return BeregningsresultatFlyttevilkår(
            fom = fom,
            tom = tom,
            grunnlag = grunnlag,
            beløp = beløp.avrundetStønadsbeløp(),
        )
    }

    private fun Int?.tilBeløp(): BigDecimal {
        feilHvis(this != null && this < 0) { "Tilleggskostnader kan ikke være negative" }
        return this?.toBigDecimal() ?: BigDecimal.ZERO
    }

    private fun overlapperVedtaksperiode(
        vilkår: Vilkår,
        perioder: List<Vedtaksperiode>,
    ): Boolean {
        val fom = vilkår.fom ?: return false
        val tom = vilkår.tom ?: return false
        return perioder.any { !it.tom.isBefore(fom) && !it.fom.isAfter(tom) }
    }

    private fun erFullstendigDekket(
        fom: LocalDate,
        tom: LocalDate,
        perioder: List<Vedtaksperiode>,
    ): Boolean {
        var dekketTil = fom.minusDays(1)
        for (periode in perioder.sortedBy { it.fom }) {
            if (periode.tom.isBefore(fom) || periode.fom.isAfter(tom)) continue
            if (periode.fom.isAfter(dekketTil.plusDays(1))) return false
            if (periode.tom.isAfter(dekketTil)) dekketTil = periode.tom
            if (!dekketTil.isBefore(tom)) return true
        }
        return false
    }

    private data class PeriodeDato(
        override val fom: LocalDate,
        override val tom: LocalDate = fom,
    ) : Periode<LocalDate>
}
