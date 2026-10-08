package no.nav.tilleggsstonader.sak.vedtak.flytting.beregning

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.libs.feil.feilHvisIkke
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.avrundetStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
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
    private val vedtakRepository: VedtakRepository,
) {
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        beregningsplan: Beregningsplan,
    ): BeregningsresultatFlytting {
        feilHvis(beregningsplan.omfang == Beregningsomfang.KUN_NYE_KJORELISTE_UKER) {
            "Beregningsomfang ${beregningsplan.omfang} støttes ikke for flytting"
        }
        brukerfeilHvis(vedtaksperioder.isEmpty()) {
            "Innvilgelse krever vedtaksperioder. Opphør av siste flytteutgift støttes ikke"
        }
        // TODO - ikke hardkode INNVILGELSE
        vedtaksperiodeValideringService.validerVedtaksperioder(vedtaksperioder, behandling, typeVedtak = TypeVedtak.INNVILGELSE)

        val vilkår = flyttingVilkårService.hentVilkårForBehandling(behandling.id)
        val relevanteVilkår =
            vilkår
                .filter { it.status != VilkårStatus.SLETTET }
                .filter { vilkår -> overlapperVedtaksperiode(vilkår, vedtaksperioder) }
                .sortedBy { it.fom }

        brukerfeilHvis(relevanteVilkår.isEmpty()) {
            "Innvilgelse krever minst ett flyttevilkår. Opphør av siste flytteutgift støttes ikke"
        }
        feilHvis(relevanteVilkår.map { it.fakta.flyttingId }.distinct().size != relevanteVilkår.size) {
            "Flere flyttevilkår har samme flyttingId"
        }

        val forrigeBehandlingId = behandling.forrigeIverksatteBehandlingId
        val forrigeResultat =
            if (beregningsplan.omfang != Beregningsomfang.ALLE_PERIODER) {
                val id = forrigeBehandlingId ?: feil("Kan ikke gjenbruke flytting uten forrige iverksatt vedtak")
                vedtakRepository
                    .findByIdOrThrow(id)
                    .withTypeOrThrow<InnvilgelseFlytting>()
                    .data.beregningsresultat
            } else {
                null
            }
        val tidligereVilkår =
            if (forrigeResultat != null) {
                flyttingVilkårService
                    .hentVilkårForBehandling(forrigeBehandlingId ?: feil("Mangler tidligere behandling"))
                    .filter { it.status != VilkårStatus.SLETTET }
                    .associateBy { it.fakta.flyttingId }
            } else {
                emptyMap()
            }
        val tidligereResultater = forrigeResultat?.resultater?.associateBy { it.flyttingId }.orEmpty()

        return BeregningsresultatFlytting(
            resultater =
                relevanteVilkår.map { vilkår ->
                    validerVilkår(vilkår, vedtaksperioder)
                    val tidligere = tidligereVilkår[vilkår.fakta.flyttingId]
                    val uendret =
                        tidligere != null && tidligere.fom == vilkår.fom && tidligere.tom == vilkår.tom &&
                            tidligere.fakta == vilkår.fakta && tidligere.resultat == vilkår.resultat
                    val gjenbruk =
                        when (beregningsplan.omfang) {
                            Beregningsomfang.ALLE_PERIODER -> false
                            Beregningsomfang.FRA_DATO ->
                                uendret && vilkår.tom < (beregningsplan.fraDato ?: feil("FRA_DATO mangler beregningsgrense"))
                            Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT -> {
                                feilHvisIkke(uendret) { "Kan ikke gjenbruke endret eller nytt flyttevilkår" }
                                true
                            }
                            Beregningsomfang.KUN_NYE_KJORELISTE_UKER -> feil("Ustøttet beregningsomfang for flytting")
                        }
                    if (gjenbruk) {
                        val tidligereResultat =
                            tidligereResultater[vilkår.fakta.flyttingId]
                                ?: feil("Fant ikke tidligere beregningsresultat for flyttingId=${vilkår.fakta.flyttingId}")
                        tidligereResultat.copy(fraTidligereVedtak = true)
                    } else {
                        beregnVilkår(vilkår, målgruppeVedFom(vilkår, vedtaksperioder))
                    }
                },
        )
    }

    private fun målgruppeVedFom(
        vilkår: VilkårFlytting,
        vedtaksperioder: List<Vedtaksperiode>,
    ): FaktiskMålgruppe {
        val målgrupper = vedtaksperioder.filter { vilkår.fom in it.fom..it.tom }.map { it.målgruppe }.distinct()
        feilHvis(målgrupper.size != 1) { "Forventer én målgruppe ved flyttevilkårets FOM" }
        return målgrupper.single()
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

    private fun beregnVilkår(
        vilkår: VilkårFlytting,
        målgruppe: FaktiskMålgruppe,
    ): BeregningsresultatFlyttevilkår =
        when (val fakta = vilkår.fakta) {
            is FaktaFlyttebyrå -> beregnFlyttebyrå(vilkår, fakta, målgruppe)
            is FaktaFlytteSelv -> beregnEgenKjøring(vilkår, fakta, målgruppe)
            else -> feil("Flyttevilkår ${vilkår.id} mangler fullstendige flyttefakta")
        }

    private fun beregnFlyttebyrå(
        vilkår: VilkårFlytting,
        fakta: FaktaFlyttebyrå,
        målgruppe: FaktiskMålgruppe,
    ): BeregningsresultatFlyttevilkår {
        val tilbud1Pris = fakta.tilbud1.pris?.toBigDecimal() ?: feil("Flyttevilkår  mangler pris på tilbud 1")
        val tilbud2Pris = fakta.tilbud2.pris?.toBigDecimal() ?: feil("Flyttevilkår  mangler pris på tilbud 2")
        feilHvis(tilbud1Pris.signum() < 0 || tilbud2Pris.signum() < 0) { "Flyttebyråtilbud kan ikke ha negativ pris" }
        val grunnlag =
            BeregningsgrunnlagFlyttebyrå(
                tilbud1Pris = tilbud1Pris,
                tilbud2Pris = tilbud2Pris,
                erBetalingDokumentert = fakta.erBetalingDokumentert,
            )
        return BeregningsresultatFlyttevilkår(
            flyttingId = fakta.flyttingId,
            målgruppe = målgruppe,
            fom = vilkår.fom,
            tom = vilkår.tom,
            grunnlag = grunnlag,
            beløp = minOf(tilbud1Pris, tilbud2Pris).avrundetStønadsbeløp(),
        )
    }

    private fun beregnEgenKjøring(
        vilkår: VilkårFlytting,
        fakta: FaktaFlytteSelv,
        målgruppe: FaktiskMålgruppe,
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
            flyttingId = fakta.flyttingId,
            målgruppe = målgruppe,
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
