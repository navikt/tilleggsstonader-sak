package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.TestIdTilBehandlingIdHolder.behandlingIdTilTestId
import no.nav.tilleggsstonader.sak.cucumber.TestIdTilBehandlingIdHolder.testIdTilBehandlingId
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.TilkjentYtelseRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VedtakRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårperiodeRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegelTestUtil.oppfylteSvarOffentligtransport
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

@Suppress("unused", "ktlint:standard:function-naming")
class OffentligTransportBeregningRevurderingStepDefinitions {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    val vilkårperiodeRepositoryFake = VilkårperiodeRepositoryFake()
    val vilkårRepositoryFake = VilkårRepositoryFake()
    val vedtakRepositoryFake = VedtakRepositoryFake()
    val tilkjentYtelseRepositoryFake = TilkjentYtelseRepositoryFake()
    val behandlingServiceMock = mockk<BehandlingService>()
    val utledTidligsteEndringService =
        mockk<UtledTidligsteEndringService> {
            every { utledTidligsteEndringForBeregning(any(), any()) } returns null
        }
    val vilkårperiodeServiceMock =
        mockk<VilkårperiodeService>().apply {
            every { hentVilkårperioder(any()) } answers {
                val vilkårsperioder =
                    vilkårperiodeRepositoryFake.findByBehandlingId(BehandlingId(firstArg<UUID>())).sorted()
                Vilkårperioder(
                    målgrupper = vilkårsperioder.ofType<MålgruppeFaktaOgVurdering>(),
                    aktiviteter = vilkårsperioder.ofType<AktivitetFaktaOgVurdering>(),
                )
            }
        }
    val unleashService = mockUnleashService()
    val vilkårService =
        VilkårService(
            behandlingService = behandlingServiceMock,
            vilkårRepository = vilkårRepositoryFake,
            barnService = mockk(relaxed = true),
        )
    val dagligReiseVilkårService =
        DagligReiseVilkårService(
            vilkårService = vilkårService,
            behandlingService = behandlingServiceMock,
            vilkårRepository = vilkårRepositoryFake,
        )
    val vedtaksperiodeValideringService =
        VedtaksperiodeValideringService(
            vilkårperiodeService = vilkårperiodeServiceMock,
        )
    val simuleringServiceMock = mockk<SimuleringService>(relaxed = true)

    val offentligtransportService =
        OffentligTransportBeregningService()

    val beregningService =
        DagligReiseBeregningService(
            vilkårService = vilkårService,
            offentligTransportBeregningService = offentligtransportService,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
            vedtakRepository = vedtakRepositoryFake,
            utledTidligsteEndringService = utledTidligsteEndringService,
        )
    val opphørValideringService =
        OpphørValideringService(
            vilkårsperiodeService = vilkårperiodeServiceMock,
            vilkårService = vilkårService,
        )
    val steg =
        DagligReiseBeregnYtelseSteg(
            beregningService = beregningService,
            utledTidligsteEndringService = utledTidligsteEndringService,
            vedtakRepository = vedtakRepositoryFake,
            tilkjentYtelseService = TilkjentYtelseService(tilkjentYtelseRepositoryFake),
            simuleringService = simuleringServiceMock,
        )

    var feil: Exception? = null

    @Gitt("følgende aktiviteter for behandling={}")
    fun `lagre aktiviteter daglig reise`(
        behandlingIdTall: Int,
        aktivitetData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        vilkårperiodeRepositoryFake.insertAll(
            mapAktiviteter(behandlingId, aktivitetData),
        )
    }

    @Gitt("følgende målgrupper for behandling={}")
    fun `lagre målgrupper daglig reise`(
        behandlingIdTall: Int,
        målgruppeData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        vilkårperiodeRepositoryFake.insertAll(
            mapMålgrupper(behandlingId, målgruppeData),
        )
    }

    @Gitt("følgende daglig reise for behandling={}")
    @Og("vi legger inn følgende daglig reise endringer for behandling={}")
    fun `lagre utgifter daglig reise`(
        behandlingIdTall: Int,
        utgifterData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        every { behandlingServiceMock.hentSaksbehandling(any<BehandlingId>()) } returns
            dummyBehandling(
                behandlingId = behandlingId,
                steg = StegType.VILKÅR,
            )

        // oppdatere kopien istedenfor å lage nyttvilkår

        utgifterData.mapRad { rad ->
            val nyttVilkår = mapTilVilkårDagligReise(rad)
            dagligReiseVilkårService.opprettNyttVilkår(behandlingId = behandlingId, nyttVilkår = nyttVilkår)
        }
    }

    @Når("vi innvilger daglig reise for behandling={} med følgende vedtaksperioder")
    fun `følgende vedtaksperioder`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

        every { behandlingServiceMock.hentSaksbehandling(any<BehandlingId>()) } returns
            dummyBehandling(
                behandlingId = behandlingId,
                steg = StegType.BEREGNE_YTELSE,
            )
        val vedtaksperioder = mapVedtaksperioder(dataTable).map { it.tilDto() }
        steg.utførSteg(dummyBehandling(behandlingId), InnvilgelseDagligReiseRequest(vedtaksperioder))
    }

    @Gitt("vi kopierer perioder fra forrige daglig reise behandling for behandling={}")
    fun `kopierer perioder`(behandlingIdTall: Int) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val forrigeIverksatteBehandlingId =
            forrigeIverksatteBehandlingId(behandlingId) ?: error("Forventer å finne forrigeIverksatteBehandlingId")

        val tidligereVilkårsperioder = vilkårperiodeRepositoryFake.findByBehandlingId(forrigeIverksatteBehandlingId)
        val tidligereVilkår = vilkårRepositoryFake.findByBehandlingId(forrigeIverksatteBehandlingId)

        vilkårperiodeRepositoryFake.insertAll(tidligereVilkårsperioder.map { it.kopierTilBehandling(behandlingId) })
        vilkårRepositoryFake.insertAll(tidligereVilkår.map { it.kopierTilBehandling(behandlingId) })
    }

    @Når("vi innvilger daglig reise behandling={} med tidligsteEndring={} med følgende vedtaksperioder")
    fun `innvilgelse med tidligsteEndring`(
        behandlingIdTall: Int,
        tidligsteEndringStr: String,
        vedtaksperiodeData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val tidligsteEndring = parseDato(tidligsteEndringStr)
        val vedtaksperioder = mapVedtaksperioder(vedtaksperiodeData).map { it.tilDto() }

        every {
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                behandlingId,
                any(),
            )
        } returns tidligsteEndring

        steg.utførSteg(
            dummyBehandling(behandlingId),
            InnvilgelseDagligReiseRequest(vedtaksperioder = vedtaksperioder),
        )
    }

    @Så("kan vi forvente følgende daglig reise beregningsresultat for behandling={}")
    @Og("følgende  daglig reise beregningsresultat for behandling={}")
    fun `forvent daglig reise beregningsresultatet`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

        val vedtaksperioder = mapVedtaksperioder(dataTable)

        val faktiskeBeregningsperioder =
            beregningService
                .beregn(
                    behandlingId,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                    vedtaksperioder = vedtaksperioder,
                    behandling = dummyBehandling(behandlingId),
                ).offentligTransport
                ?.reiser
                ?.flatMap { it.perioder }!!

        val vedtak = hentVedtak(behandlingId).beregningsresultat

        val forventedeBeregningsperioder =
            BeregningsresultatForReise(
                perioder = vedtak.offentligTransport?.reiser?.flatMap { it.perioder }!!,
            ).perioder

        assertThat(faktiskeBeregningsperioder.size).isEqualTo(forventedeBeregningsperioder.size)

        forventedeBeregningsperioder.forEachIndexed { index, periode ->
            assertThat(faktiskeBeregningsperioder[index].grunnlag.fom).isEqualTo(periode.grunnlag.fom)
            assertThat(faktiskeBeregningsperioder[index].grunnlag.tom).isEqualTo(periode.grunnlag.tom)
            assertThat(faktiskeBeregningsperioder[index].beløp).isEqualTo(periode.beløp)
            assertThat(faktiskeBeregningsperioder[index].billettdetaljer)
                .isEqualTo(periode.billettdetaljer)
        }
    }

    @Så("kan vi forvente følgende daglig reise vedtaksperioder for behandling={}")
    @Og("følgende daglig reise vedtaksperioder for behandling={}")
    fun `forvent vedtaksperioder`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

        val vedtaksperioder = hentVedtak(behandlingId).vedtaksperioder

        val forventedeVedtaksperioder = mapVedtaksperioder(dataTable)

        assertThat(vedtaksperioder.size).isEqualTo(forventedeVedtaksperioder.size)

        forventedeVedtaksperioder.forEachIndexed { index, periode ->
            assertThat(vedtaksperioder[index].fom).isEqualTo(periode.fom)
            assertThat(vedtaksperioder[index].tom).isEqualTo(periode.tom)
            assertThat(vedtaksperioder[index].målgruppe).isEqualTo(periode.målgruppe)
            assertThat(vedtaksperioder[index].aktivitet).isEqualTo(periode.aktivitet)
        }
    }

    fun mapTilVilkårDagligReise(rad: Map<String, String>): LagreDagligReise =
        LagreDagligReise(
            fom = parseDato(DomenenøkkelFelles.FOM, rad),
            tom = parseDato(DomenenøkkelFelles.TOM, rad),
            svar = oppfylteSvarOffentligtransport,
            fakta =
                FaktaOffentligTransport(
                    reisedagerPerUke =
                        parseInt(
                            DomenenøkkelOffentligtransport.ANTALL_REISEDAGER_PER_UKE,
                            rad,
                        ),
                    prisEnkelbillett = parseValgfriInt(DomenenøkkelOffentligtransport.PRIS_ENKELTBILLETT, rad),
                    prisSyvdagersbillett = parseValgfriInt(DomenenøkkelOffentligtransport.PRIS_SYV_DAGERS_BILLETT, rad),
                    prisTrettidagersbillett =
                        parseValgfriInt(
                            DomenenøkkelOffentligtransport.PRIS_TRETTI_DAGERS_BILLETT,
                            rad,
                        ),
                ),
        )

    private fun hentVedtak(behandlingId: BehandlingId): InnvilgelseEllerOpphørDagligReise =
        vedtakRepositoryFake
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()
            .data

    private fun dummyBehandling(
        behandlingId: BehandlingId,
        steg: StegType = StegType.BEREGNE_YTELSE,
    ): Saksbehandling {
        val forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId(behandlingId)
        return saksbehandling(
            id = behandlingId,
            steg = steg,
            fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
            forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
            type = if (forrigeIverksatteBehandlingId != null) BehandlingType.REVURDERING else BehandlingType.FØRSTEGANGSBEHANDLING,
        )
    }

    private fun BeregningsresultatForPeriode.utenVedtaksperiodeId(): BeregningsresultatForPeriode =
        this.copy(
            grunnlag =
                this.grunnlag.copy(
                    vedtaksperioder =
                        this.grunnlag.vedtaksperioder.map {
                            it.copy(id = UUID(0, 0))
                        },
                ),
        )

    private fun forrigeIverksatteBehandlingId(behandlingId: BehandlingId): BehandlingId? {
        val behandlingIdInt = behandlingIdTilTestId(behandlingId)
        return if (behandlingIdInt > 1) {
            testIdTilBehandlingId.getValue(behandlingIdInt - 1)
        } else {
            null
        }
    }
}
