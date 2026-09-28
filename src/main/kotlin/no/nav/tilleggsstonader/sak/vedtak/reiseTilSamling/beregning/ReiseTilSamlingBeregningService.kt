package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingValidering.filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingValidering.validerUtgiftHeleVedtaksperioden
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingValidering.validerUtgifterStrekkerSegUtenforVedtaksperiodene
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsgrunnlagOffentligTransportForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsgrunnlagPrivatBilForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.VilkårReiseTilSamlingMapper.mapTilVilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class ReiseTilSamlingBeregningService(
    private val vilkårService: VilkårService,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val satsPrivatBilProvider: SatsPrivatBilProvider,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val vedtakRepository: VedtakRepository,
) {
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        typeVedtak: TypeVedtak,
        beregningsplan: Beregningsplan,
    ): BeregningsresultatReiseTilSamling {
        val forrigeVedtak = hentForrigeIverksatteVedtak(behandling)

        if (beregningsplan.omfang == Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT) {
            return requireNotNull(forrigeVedtak) {
                "Kan ikke gjenbruke forrige beregningsresultat uten forrige iverksatt vedtak"
            }.beregningsresultat.markerAltSomFraTidligereVedtak()
        }

        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )
        val vedtaksperioderBeregning =
            vedtaksperioder.tilVedtaksperiodeBeregning().sorted()

        val oppfylteVilkårReiseTilSamling =
            vilkårService
                .hentOppfylteReiseTilSamlingVilkår(
                    behandling.id,
                ).map { it.mapTilVilkårReiseTilSamling() }
                .sortedBy { it.fom }

        val utgifterTilBeregning =
            oppfylteVilkårReiseTilSamling.filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder(
                vedtaksperioderBeregning,
            )

        val brukersNavKontor =
            if (behandling.stønadstype == Stønadstype.REISE_TIL_SAMLING_TSR) {
                arbeidsfordelingService.hentBrukersNavKontor(behandling.ident).id
            } else {
                null
            }

        validerUtgifter(
            utgifter = utgifterTilBeregning,
            vedtakstype = typeVedtak,
            vedtaksperioder = vedtaksperioder,
        )
        validerUtgiftHeleVedtaksperioden(vedtaksperioder, utgifterTilBeregning)

        validerUtgifterStrekkerSegUtenforVedtaksperiodene(
            utgifterTilBeregning,
            vedtaksperioderBeregning,
        )
        if (typeVedtak == TypeVedtak.INNVILGELSE) {
            validerFinnesSamling(utgifterTilBeregning)
        }

        val beregnFra = beregningsplan.beregnFra()
        val (uendredeUtgifter, berørteUtgifter) = utgifterTilBeregning.splittPåBeregnFra(beregnFra, forrigeVedtak)

        val offentligTransport =
            gjenbrukOffentligTransport(uendredeUtgifter, forrigeVedtak) +
                beregnOffentligTransport(
                    berørteUtgifter,
                    vedtaksperioder,
                    brukersNavKontor,
                )

        val privatBil =
            gjenbrukPrivatBil(uendredeUtgifter, forrigeVedtak) +
                beregnPrivatBil(
                    berørteUtgifter,
                    vedtaksperioder,
                    brukersNavKontor,
                )
        return BeregningsresultatReiseTilSamling(
            offentligTransport = offentligTransport,
            privatBil = privatBil,
        )
    }

    /**
     * Reiser som ikke er berørt av [beregnFra] skal kopieres uendret fra forrige iverksatte vedtak,
     * mens reiser som er berørt (eller nye) skal reberegnes fra bunnen.
     * En reise regnes som berørt dersom den strekker seg til eller forbi [beregnFra], eller dersom
     * [beregnFra] er null (dvs. førstegangsbehandling, alt skal beregnes).
     */
    private fun List<VilkårReiseTilSamling>.splittPåBeregnFra(
        beregnFra: LocalDate?,
        forrigeVedtak: InnvilgelseEllerOpphørReiseTilSamling?,
    ): Pair<List<VilkårReiseTilSamling>, List<VilkårReiseTilSamling>> {
        if (beregnFra == null) {
            return emptyList<VilkårReiseTilSamling>() to this
        }
        return this.partition { it.erUendretFraForrigeVedtak(beregnFra) }
    }

    /**
     * En reise som slutter før [beregnFra] er som hovedregel uendret og kan gjenbrukes.
     * Unntaket er en reise som er forkortet (status ENDRET),da blir [beregnFra] dagen etter den forkortede reisen.
     * Er vilkåret UENDRET er det ikke vits i å slå opp i forrige vedtak, siden reisen ikke kan ha blitt forkortet.
     */
    private fun VilkårReiseTilSamling.erUendretFraForrigeVedtak(beregnFra: LocalDate): Boolean =
        if (tom >= beregnFra) {
            false
        } else if (tom.plusDays(1) == beregnFra && status == VilkårStatus.ENDRET) {
            false
        } else {
            true
        }

    private fun gjenbrukOffentligTransport(
        uendredeUtgifter: List<VilkårReiseTilSamling>,
        forrigeVedtak: InnvilgelseEllerOpphørReiseTilSamling?,
    ): List<BeregningsresultatOffentligTransport> {
        val reiseIder =
            uendredeUtgifter
                .filter { it.fakta is FaktaOffentligTransport }
                .map { (it.fakta as FaktaOffentligTransport).reiseId }
        if (reiseIder.isEmpty()) return emptyList()

        val forrigeResultater =
            requireNotNull(forrigeVedtak) {
                "Kan ikke gjenbruke tidligere reiser uten forrige iverksatt vedtak"
            }.beregningsresultat.offentligTransport

        return reiseIder.map { reiseId ->
            val forrigeResultat =
                forrigeResultater.find { it.reiseId == reiseId }
                    ?: feil(
                        "Fant ikke forrige beregningsresultat for offentlig transport med reiseId=$reiseId " +
                            "ved gjenbruk fra tidligere vedtak",
                    )
            forrigeResultat.copy(fraTidligereVedtak = true)
        }
    }

    private fun gjenbrukPrivatBil(
        uendredeUtgifter: List<VilkårReiseTilSamling>,
        forrigeVedtak: InnvilgelseEllerOpphørReiseTilSamling?,
    ): List<BeregningsresultatPrivatBil> {
        val reiseIder =
            uendredeUtgifter
                .filter { it.fakta is FaktaPrivatBil }
                .map { (it.fakta as FaktaPrivatBil).reiseId }
        if (reiseIder.isEmpty()) return emptyList()

        val forrigeResultater =
            requireNotNull(forrigeVedtak) {
                "Kan ikke gjenbruke tidligere reiser uten forrige iverksatt vedtak"
            }.beregningsresultat.privatBil

        return reiseIder.map { reiseId ->
            val forrigeResultat =
                forrigeResultater.find { it.reiseId == reiseId }
                    ?: feil(
                        "Fant ikke forrige beregningsresultat for privat bil med reiseId=$reiseId " +
                            "ved gjenbruk fra tidligere vedtak",
                    )
            forrigeResultat.copy(fraTidligereVedtak = true)
        }
    }

    private fun BeregningsresultatReiseTilSamling.markerAltSomFraTidligereVedtak() =
        BeregningsresultatReiseTilSamling(
            offentligTransport = offentligTransport.map { it.copy(fraTidligereVedtak = true) },
            privatBil = privatBil.map { it.copy(fraTidligereVedtak = true) },
        )

    private fun hentForrigeIverksatteVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørReiseTilSamling? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørReiseTilSamling>()

    private fun beregnOffentligTransport(
        utgifter: List<VilkårReiseTilSamling>,
        vedtaksperioder: List<Vedtaksperiode>,
        brukersNavKontor: String?,
    ): List<BeregningsresultatOffentligTransport> {
        val oppfylteOffentligTransport =
            utgifter.filter { it.fakta is FaktaOffentligTransport }

        return oppfylteOffentligTransport.map { samling ->
            val fakta = samling.fakta as FaktaOffentligTransport

            BeregningsresultatOffentligTransport(
                reiseId = fakta.reiseId,
                aktivitetId = fakta.aktivitetId,
                grunnlag =
                    BeregningsgrunnlagOffentligTransportForSamling(
                        adresse = fakta.adresse,
                        fom = samling.fom,
                        tom = samling.tom,
                        vedtaksperioder =
                            vedtaksperioder
                                .filter { it.overlapper(samling) }
                                .map(::VedtaksperiodeGrunnlag),
                        brukersNavKontor = brukersNavKontor,
                    ),
                beløp = fakta.utgifterOffentligTransport,
            )
        }
    }

    private fun beregnPrivatBil(
        utgifter: List<VilkårReiseTilSamling>,
        vedtaksperioder: List<Vedtaksperiode>,
        brukersNavKontor: String?,
    ): List<BeregningsresultatPrivatBil> {
        val oppfyltePrivatBil =
            utgifter.filter { it.fakta is FaktaPrivatBil }

        return oppfyltePrivatBil.map { samling ->
            val fakta = samling.fakta as FaktaPrivatBil

            val sats =
                satsPrivatBilProvider
                    .finnRelevantKilometerSatsForPeriode(samling)

            val grunnlag =
                BeregningsgrunnlagPrivatBilForSamling(
                    adresse = fakta.adresse,
                    fom = samling.fom,
                    tom = samling.tom,
                    sats = sats.beløp,
                    totalReiseavstand = fakta.reiseavstand,
                    bompenger = fakta.bompenger,
                    fergekostnad = fakta.fergekostnad,
                    parkering = fakta.parkering,
                    piggdekkavgift = fakta.piggdekkavgift,
                    vedtaksperioder =
                        vedtaksperioder
                            .filter { it.overlapper(samling) }
                            .map(::VedtaksperiodeGrunnlag),
                    brukersNavKontor = brukersNavKontor,
                    satsBekreftet = sats.bekreftet,
                )

            BeregningsresultatPrivatBil(
                reiseId = fakta.reiseId,
                aktivitetId = fakta.aktivitetId,
                grunnlag = grunnlag,
                beløp = beregnBelopForPrivatBil(grunnlag),
            )
        }
    }

    private fun beregnBelopForPrivatBil(grunnlag: BeregningsgrunnlagPrivatBilForSamling): BigDecimal =
        (
            grunnlag.totalReiseavstand
                .multiply(grunnlag.sats)
                .plus(grunnlag.bompenger ?: BigDecimal.ZERO)
                .plus(grunnlag.fergekostnad ?: BigDecimal.ZERO)
                .plus(grunnlag.parkering ?: BigDecimal.ZERO)
                .plus(grunnlag.piggdekkavgift ?: BigDecimal.ZERO)
        ).setScale(0, RoundingMode.HALF_UP)
}

private fun validerFinnesSamling(vilkår: List<VilkårReiseTilSamling>) {
    brukerfeilHvis(vilkår.isEmpty()) {
        "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med samling"
    }
}

fun validerUtgifter(
    utgifter: List<VilkårReiseTilSamling>,
    vedtakstype: TypeVedtak,
    vedtaksperioder: List<Vedtaksperiode>,
) {
    // Tillat opphør av hele saken
    if (vedtakstype == TypeVedtak.OPPHØR && utgifter.isEmpty() && vedtaksperioder.isEmpty()) return

    brukerfeilHvis(utgifter.isEmpty()) {
        "Det er ikke lagt inn noen oppfylte utgiftsperioder"
    }

    val ikkePositivUtgift =
        utgifter
            .mapNotNull {
                (it.fakta as? FaktaOffentligTransport)
                    ?.utgifterOffentligTransport
            }.firstOrNull { it < 0.toBigDecimal() }

    feilHvis(ikkePositivUtgift != null) {
        "Utgiftsperioder inneholder ugyldig utgift: $ikkePositivUtgift"
    }
}
