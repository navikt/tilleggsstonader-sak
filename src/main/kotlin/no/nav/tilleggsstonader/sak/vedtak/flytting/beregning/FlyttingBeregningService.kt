package no.nav.tilleggsstonader.sak.vedtak.flytting.beregning

import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.libs.feil.feilHvisIkke
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.avrundetStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagEgenKjøring
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlyttevilkår
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FlyttingVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.VilkårFlytting
import org.springframework.stereotype.Service
import java.math.BigDecimal

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
                    validerVilkår(vilkår, vedtaksperioder)
                    beregnVilkår(vilkår)
                },
        )
    }

    private fun validerVilkår(
        vilkår: VilkårFlytting,
        vedtaksperioder: List<Vedtaksperiode>,
    ) {
        feilHvisIkke(vilkår.resultat == Vilkårsresultat.OPPFYLT) {
            "Flyttevilkår ${vilkår.id} må være oppfylt før beregning"
        }
        feilHvisIkke(erFullstendigDekket(vilkår, vedtaksperioder)) {
            "Vedtaksperiodene må dekke hele flyttevilkåret ${vilkår.id}"
        }
    }

    private fun beregnVilkår(vilkår: VilkårFlytting): BeregningsresultatFlyttevilkår =
        when (val fakta = vilkår.fakta) {
            is FaktaFlyttebyrå -> beregnFlyttebyrå(vilkår, fakta)
            is FaktaFlytteSelv -> beregnEgenKjøring(vilkår, fakta)
            else -> feil("Flyttevilkår ${vilkår.id} mangler fullstendige flyttefakta")
        }

    private fun beregnFlyttebyrå(
        vilkår: VilkårFlytting,
        fakta: FaktaFlyttebyrå,
    ): BeregningsresultatFlyttevilkår {
        val tilbud1Pris = fakta.tilbud1.pris?.toBigDecimal() ?: feil("Flyttevilkår  mangler pris på tilbud 1")
        val tilbud2Pris = fakta.tilbud2.pris?.toBigDecimal() ?: feil("Flyttevilkår  mangler pris på tilbud 2")
        feilHvis(tilbud1Pris.signum() < 0 || tilbud2Pris.signum() < 0) { "Flyttebyråtilbud kan ikke ha negativ pris" }
        val grunnlag = BeregningsgrunnlagFlyttebyrå(tilbud1Pris, tilbud2Pris)
        return BeregningsresultatFlyttevilkår(
            fom = vilkår.fom,
            tom = vilkår.tom,
            grunnlag = grunnlag,
            beløp = minOf(tilbud1Pris, tilbud2Pris).avrundetStønadsbeløp(),
        )
    }

    private fun beregnEgenKjøring(
        vilkår: VilkårFlytting,
        fakta: FaktaFlytteSelv,
    ): BeregningsresultatFlyttevilkår {
        val avstandEnVei = fakta.avstandEnVei ?: feil("Flyttevilkår mangler avstand én vei")
        feilHvis(avstandEnVei < 0) { "Avstand én vei kan ikke være negativ" }
        val sats =
            satsPrivatBilProvider.finnRelevantKilometerSatsForPeriode(vilkår).let {
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
            fom = vilkår.fom,
            tom = vilkår.tom,
            grunnlag = grunnlag,
            beløp = beløp.avrundetStønadsbeløp(),
        )
    }

    private fun Int?.tilBeløp(): BigDecimal {
        feilHvis(this != null && this < 0) { "Tilleggskostnader kan ikke være negative" }
        return this?.toBigDecimal() ?: BigDecimal.ZERO
    }

    private fun overlapperVedtaksperiode(
        vilkår: VilkårFlytting,
        perioder: List<Vedtaksperiode>,
    ): Boolean = perioder.any { it.overlapper(vilkår) }

    private fun erFullstendigDekket(
        vilkår: VilkårFlytting,
        perioder: List<Vedtaksperiode>,
    ): Boolean = perioder.mergeSammenhengende().any { it.inneholder(vilkår) }
}
