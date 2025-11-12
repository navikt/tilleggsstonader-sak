package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.MarkerSomDelAvTidligereUtbetlingUtils.markerSomDelAvTidligereUtbetaling
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseBeregningService(
    private val vilkårService: VilkårService,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val offentligTransportBeregningService: OffentligTransportBeregningService,
    private val vedtakRepository: VedtakRepository,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
) {
    fun beregn(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatDagligReise {
        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )

        val forrigeVedtak = hentForrigeVedtak(behandling)

        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                behandlingId = behandlingId,
                vedtaksperioder = vedtaksperioder,
            )

        val oppfylteVilkår =
            vilkårService.hentOppfylteDagligReiseVilkår(behandlingId).map { it.mapTilVilkårDagligReise() }
        validerFinnesReiser(oppfylteVilkår)

        val oppfylteVilkårGruppertPåType = oppfylteVilkår.filter { it.fakta != null }.groupBy { it.fakta!!.type }

        return if (forrigeVedtak != null) {
            brukerfeilHvis(tidligsteEndring == null) {
                "Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen"
            }

            settSammenGamleOgNyePerioder(
                tidligsteEndring = tidligsteEndring,
                nyttBeregningsresultat =
                    beregnOffentligTransport(
                        vilkår = oppfylteVilkårGruppertPåType,
                        vedtaksperioder = vedtaksperioder.filter { it.fom >= tidligsteEndring.plusDays(1) },
                    )?.reiser
                        ?.flatMap { it.perioder }
                        ?: emptyList(),
                forrigeBeregningsresultat = forrigeVedtak.beregningsresultat.offentligTransport!!,
            )
        } else {
            BeregningsresultatDagligReise(
                offentligTransport = beregnOffentligTransport(oppfylteVilkårGruppertPåType, vedtaksperioder),
            )
        }
    }

    private fun beregnOffentligTransport(
        vilkår: Map<TypeDagligReise, List<VilkårDagligReise>>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatOffentligTransport? {
        val vilkårOffentligTransport = vilkår[TypeDagligReise.OFFENTLIG_TRANSPORT] ?: return null

        return offentligTransportBeregningService.beregn(
            vedtaksperioder = vedtaksperioder,
            oppfylteVilkår = vilkårOffentligTransport,
        )
    }

    private fun validerFinnesReiser(vilkår: List<VilkårDagligReise>) {
        brukerfeilHvis(vilkår.isEmpty()) {
            "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med reise"
        }
    }

    /**
     * Slår sammen perioder fra forrige og nytt vedtak.
     * Beholder perioder fra forrige vedtak frem til tidligsteEndring-datoen.
     * Bruker reberegnede perioder fra og med tidligsteEndring-datoen
     * Dette gjøres for at vi ikke skal reberegne perioder som ikke er med i revurderingen, i tilfelle beregningskoden har endret seg siden sist.
     * Vi trenger derimot å reberegne alle perioder som ligger etter tidligsteEndring-datoen, da utgiftene, antall samlinger osv kan ha endret seg.
     */
    private fun settSammenGamleOgNyePerioder(
        tidligsteEndring: LocalDate,
        nyttBeregningsresultat: List<BeregningsresultatForPeriode>,
        forrigeBeregningsresultat: BeregningsresultatOffentligTransport,
    ): BeregningsresultatDagligReise {
        // Kast en feil dersom dagens dato er det samme som tidligstEndring?

        val perioderFraForrigeVedtakSomSkalBeholdes =
            forrigeBeregningsresultat
                .reiser
                .flatMap { reise ->
                    reise.perioder
                        .filter { it.grunnlag.fom < tidligsteEndring }
                        .markerSomDelAvTidligereUtbetaling()
                }

        val reberegnedePerioder =
            nyttBeregningsresultat
                .filter { it.grunnlag.fom >= tidligsteEndring }
                .markerSomDelAvTidligereUtbetaling(forrigeBeregningsresultat.reiser.flatMap { it.perioder })

        return BeregningsresultatDagligReise(
            BeregningsresultatOffentligTransport(
                reiser =
                    listOf(
                        BeregningsresultatForReise(
                            perioder = perioderFraForrigeVedtakSomSkalBeholdes + reberegnedePerioder,
                        ),
                    ),
            ),
        )
    }

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørDagligReise? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data
}
