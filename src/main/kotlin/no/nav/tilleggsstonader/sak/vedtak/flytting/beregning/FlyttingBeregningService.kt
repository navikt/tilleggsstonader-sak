package no.nav.tilleggsstonader.sak.vedtak.flytting.beregning

import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.isEqualOrAfter
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.avrundetStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlytteSelv
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlyttevilkår
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FlyttingVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.VilkårFlytting
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class FlyttingBeregningService(
    private val flyttingVilkårService: FlyttingVilkårService,
    private val vedtakService: VedtakService,
    private val satsPrivatBilProvider: SatsPrivatBilProvider,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
) {
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        beregningsplan: Beregningsplan,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatFlytting {
        val omfang = beregningsplan.omfang

        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder,
            behandling,
            typeVedtak = typeVedtak,
        )

        val oppfylteVilkår = flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id)

        val resultater =
            when (omfang) {
                Beregningsomfang.ALLE_PERIODER -> {
                    oppfylteVilkår.map { beregnVilkår(it, finnTilhørendeVedtaksperiode(it, vedtaksperioder)) }
                }

                Beregningsomfang.FRA_DATO -> {
                    val (nyeVilkår, gamleVilkår) =
                        oppfylteVilkår
                            .partition {
                                val beregningsplanFraDato =
                                    beregningsplan.fraDato
                                        ?: feil("Beregningsplan fraDato mangler når omfang er FRA_DATO, skal ikke skje")
                                it.fom.isEqualOrAfter(beregningsplanFraDato)
                            }

                    val nyeBeregningsresultat =
                        nyeVilkår.map {
                            beregnVilkår(
                                it,
                                finnTilhørendeVedtaksperiode(it, vedtaksperioder),
                            )
                        }

                    val forrigeVedtaksresultater =
                        hentForrigeIverksatteVedtak(behandling)?.beregningsresultat?.resultater

                    val gamleBeregningsresultat =
                        gamleVilkår.mapNotNull { gammeltVilkår ->
                            val vilkåretsResultat =
                                forrigeVedtaksresultater?.find { gammeltVilkår.fom == it.fom && gammeltVilkår.tom == it.tom }

                            vilkåretsResultat?.copy(fraTidligereVedtak = true)
                        }

                    return BeregningsresultatFlytting(nyeBeregningsresultat + gamleBeregningsresultat)
                }

                Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT -> {
                    val forrigeVedtak =
                        hentForrigeIverksatteVedtak(behandling)
                            ?: feil("Forrige vedtak mangler ved gjenbruk av forrige resultat")

                    val resultat = forrigeVedtak.beregningsresultat

                    return resultat.copy(resultater = resultat.resultater.map { it.copy(fraTidligereVedtak = true) })
                }

                else -> {
                    feil("Ustøttet beregningsomfang for flytting: $omfang")
                }
            }.sortedBy { it.fom }

        return BeregningsresultatFlytting(
            resultater = resultater,
        )
    }

    private fun finnTilhørendeVedtaksperiode(
        vilkår: VilkårFlytting,
        vedtaksperioder: List<Vedtaksperiode>,
    ): Vedtaksperiode =
        vedtaksperioder.single {
            it.overlapper(vilkår)
        }

    private fun hentForrigeIverksatteVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørFlytting? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data

    private fun hentVedtak(behandlingId: BehandlingId) = vedtakService.hentVedtak<InnvilgelseEllerOpphørFlytting>(behandlingId)

    private fun beregnVilkår(
        vilkår: VilkårFlytting,
        vedtaksperiode: Vedtaksperiode,
    ): BeregningsresultatFlyttevilkår {
        val (grunnlag, beløp) =
            when (val fakta = vilkår.fakta) {
                is FaktaFlyttebyrå -> beregnFlyttebyrå(fakta)
                is FaktaFlytteSelv -> beregnFlytteSelv(vilkår, fakta)
                else -> feil("Flyttevilkår ${vilkår.id} mangler fullstendige flyttefakta")
            }

        return BeregningsresultatFlyttevilkår(
            grunnlag = grunnlag,
            fraTidligereVedtak = false,
            fom = vilkår.fom,
            tom = vilkår.tom,
            beløp = beløp,
            målgruppe = vedtaksperiode.målgruppe,
        )
    }

    private fun beregnFlyttebyrå(fakta: FaktaFlyttebyrå): Pair<BeregningsgrunnlagFlyttebyrå, BigDecimal> {
        val tilbud1Pris = fakta.tilbud1.pris?.toBigDecimal() ?: feil("Flyttevilkår  mangler pris på tilbud 1")
        val tilbud2Pris = fakta.tilbud2.pris?.toBigDecimal() ?: feil("Flyttevilkår  mangler pris på tilbud 2")
        feilHvis(tilbud1Pris.signum() < 0 || tilbud2Pris.signum() < 0) { "Flyttebyråtilbud kan ikke ha negativ pris" }

        val grunnlag =
            BeregningsgrunnlagFlyttebyrå(
                tilbud1Pris = tilbud1Pris,
                tilbud2Pris = tilbud2Pris,
                erBetalingDokumentert = fakta.erBetalingDokumentert,
            )
        val beløp = minOf(tilbud1Pris, tilbud2Pris).avrundetStønadsbeløp()

        return grunnlag to beløp
    }

    private fun beregnFlytteSelv(
        vilkår: VilkårFlytting,
        fakta: FaktaFlytteSelv,
    ): Pair<BeregningsgrunnlagFlytteSelv, BigDecimal> {
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
            BeregningsgrunnlagFlytteSelv(
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

        return grunnlag to beløp
    }

    private fun Int?.tilBeløp(): BigDecimal {
        feilHvis(this != null && this < 0) { "Tilleggskostnader kan ikke være negative" }
        return this?.toBigDecimal() ?: BigDecimal.ZERO
    }
}
