package no.nav.tilleggsstonader.sak.vedtak.boutgifter

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
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriBoolean
import no.nav.tilleggsstonader.sak.cucumber.verifiserAtListerErLike
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.TilkjentYtelseRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VedtakRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårperiodeRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.innvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregningService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterUtgiftService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.ForenkletAndel
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.mapAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.mapAndeler
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.mapBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.mapMålgrupper
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BoutgifterDomenenøkkel
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterEnBolig
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterToBoliger
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårUtgifterOvernatting
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

@Suppress("unused", "ktlint:standard:function-naming")
class BoutgifterBeregnYtelseStegStepDefinitions {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    val vilkårperiodeRepositoryFake = VilkårperiodeRepositoryFake()
    val vilkårRepositoryFake = VilkårRepositoryFake()
    val vedtakRepositoryFake = VedtakRepositoryFake()
    val tilkjentYtelseRepositoryFake = TilkjentYtelseRepositoryFake()
    val behandlingServiceMock = mockk<BehandlingService>()
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
    val vilkårService =
        VilkårService(
            behandlingService = behandlingServiceMock,
            vilkårRepository = vilkårRepositoryFake,
            barnService = mockk(relaxed = true),
        )
    val boutgifterUtgiftService = BoutgifterUtgiftService(vilkårService = vilkårService)
    val vedtaksperiodeValideringService =
        VedtaksperiodeValideringService(
            vedtakRepository = vedtakRepositoryFake,
            vilkårperiodeService = vilkårperiodeServiceMock,
        )
    val simuleringServiceMock = mockk<SimuleringService>(relaxed = true)
    val beregningService =
        BoutgifterBeregningService(
            boutgifterUtgiftService = boutgifterUtgiftService,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
            vedtakRepository = vedtakRepositoryFake,
        )
    val opphørValideringService =
        OpphørValideringService(
            vilkårsperiodeService = vilkårperiodeServiceMock,
            vilkårService = vilkårService,
        )
    val steg =
        BoutgifterBeregnYtelseSteg(
            beregningService =
            beregningService,
            opphørValideringService = opphørValideringService,
            vedtakRepository = vedtakRepositoryFake,
            tilkjentYtelseService = TilkjentYtelseService(tilkjentYtelseRepositoryFake),
            simuleringService = simuleringServiceMock,
        )

    var feil: Exception? = null

    @Gitt("følgende oppfylte aktiviteter for behandling={}")
    fun `lagre aktiviteter`(
        behandlingIdTall: Int,
        aktivitetData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        vilkårperiodeRepositoryFake.insertAll(
            mapAktiviteter(behandlingId, aktivitetData),
        )
    }

    @Gitt("følgende oppfylte målgrupper for behandling={}")
    fun `lagre målgrupper`(
        behandlingIdTall: Int,
        målgruppeData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        vilkårperiodeRepositoryFake.insertAll(
            mapMålgrupper(målgruppeData, behandlingId),
        )
    }

    @Gitt("følgende boutgifter av type {} for behandling={}")
    @Og("vi legger inn følgende nye utgifter av type {} for behandling={}")
    fun `lagre utgifter`(
        typeBoutgift: TypeBoutgift,
        behandlingIdTall: Int,
        utgifterData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        every { behandlingServiceMock.hentSaksbehandling(any<BehandlingId>()) } returns
            dummyBehandling(
                behandlingId = behandlingId,
                steg = StegType.VILKÅR,
            )

        val opprettVilkårDto =
            mapOpprettVilkårDto(
                typeBoutgift = typeBoutgift,
                behandlingId = behandlingId,
                utgifterData = utgifterData,
            )

        opprettVilkårDto.forEach { vilkårService.opprettNyttVilkår(it) }
    }

    private fun mapOpprettVilkårDto(
        typeBoutgift: TypeBoutgift,
        behandlingId: BehandlingId,
        utgifterData: DataTable,
    ): List<OpprettVilkårDto> =
        utgifterData.mapRad { rad ->
            val delvilkår = mapDelvilkår(rad, typeBoutgift)
            OpprettVilkårDto(
                vilkårType = typeBoutgift.tilVilkårType(),
                behandlingId = behandlingId,
                delvilkårsett = delvilkår,
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                utgift = parseInt(BoutgifterDomenenøkkel.UTGIFT, rad),
                erFremtidigUtgift = false,
            )
        }

    private fun mapDelvilkår(
        rad: Map<String, String>,
        typeBoutgift: TypeBoutgift,
    ): List<DelvilkårDto> {
        val harHøyereUtgifter =
            parseValgfriBoolean(BoutgifterDomenenøkkel.HØYERE_UTGIFTER, rad)
                ?.takeIf { it }
                ?.let { SvarId.JA }
                ?: SvarId.NEI
        return when (typeBoutgift) {
            TypeBoutgift.UTGIFTER_OVERNATTING -> oppfylteDelvilkårUtgifterOvernatting(harHøyereUtgifter)
            TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG -> oppfylteDelvilkårLøpendeUtgifterEnBolig(harHøyereUtgifter)
            TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER -> oppfylteDelvilkårLøpendeUtgifterToBoliger(harHøyereUtgifter)
        }.map { it.tilDto() }
    }

    @Gitt("vi fjerner utgiftene på behandling={}")
    fun `sletter og legger inn nye utgifter`(behandlingIdTall: Int) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        vilkårRepositoryFake.deleteByBehandlingId(behandlingId)
    }

    @Når("vi innvilger boutgifter for behandling={} med følgende vedtaksperioder")
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

        kjørMedFeilkontekst {
            steg.utførSteg(dummyBehandling(behandlingId), InnvilgelseBoutgifterRequest(vedtaksperioder))
        }
    }

    @Gitt("vi kopierer perioder fra forrige behandling for behandling={}")
    fun `kopierer perioder`(behandlingIdTall: Int) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val forrigeIverksatteBehandlingId =
            forrigeIverksatteBehandlingId(behandlingId) ?: error("Forventer å finne forrigeIverksatteBehandlingId")

        val tidligereVilkårsperioder = vilkårperiodeRepositoryFake.findByBehandlingId(forrigeIverksatteBehandlingId)
        val tidligereVilkår = vilkårRepositoryFake.findByBehandlingId(forrigeIverksatteBehandlingId)

        vilkårperiodeRepositoryFake.insertAll(tidligereVilkårsperioder.map { it.kopierTilBehandling(behandlingId) })
        vilkårRepositoryFake.insertAll(tidligereVilkår.map { it.kopierTilBehandling(behandlingId) })
    }

    /**
     * Her forutsetter vi at vi har lagret utgifter på behandlingen først
     */
    @Gitt("vi har lagret følgende beregningsresultat for behandling={}")
    fun `lagrer beregningsresultat`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val utgifter = boutgifterUtgiftService.hentUtgifterTilBeregning(behandlingId)
        val perioderBeregningsresultat = mapBeregningsresultat(dataTable, utgifter)
        val vedtaksperiode = mapVedtaksperioder(dataTable)
        val vedtak =
            innvilgelseBoutgifter(
                behandlingId = behandlingId,
                vedtaksperioder = vedtaksperiode,
                beregningsresultat = BeregningsresultatBoutgifter(perioderBeregningsresultat),
            )
        vedtakRepositoryFake.insert(vedtak)
    }

    @Når("vi opphører boutgifter behandling={} med revurderFra={}")
    fun `opphør med revurderFra`(
        behandlingIdTall: Int,
        revurderFraStr: String,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val revurderFra = parseDato(revurderFraStr)

        kjørMedFeilkontekst {
            steg.utførSteg(
                dummyBehandling(behandlingId, revurderFra = revurderFra),
                OpphørBoutgifterRequest(
                    årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                    begrunnelse = "begrunnelse",
                ),
            )
        }
    }

    @Når("vi innvilger boutgifter behandling={} med revurderFra={} med følgende vedtaksperioder")
    fun `innvilgelse med revurderFra`(
        behandlingIdTall: Int,
        revurderFraStr: String,
        vedtaksperiodeData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val revurderFra = parseDato(revurderFraStr)
        val vedtaksperioder = mapVedtaksperioder(vedtaksperiodeData).map { it.tilDto() }

        kjørMedFeilkontekst {
            steg.utførSteg(
                dummyBehandling(behandlingId, revurderFra = revurderFra),
                InnvilgelseBoutgifterRequest(vedtaksperioder = vedtaksperioder),
            )
        }
    }

    @Så("kan vi forvente følgende beregningsresultat for behandling={}")
    @Og("følgende beregningsresultat for behandling={}")
    fun `forvent beregningsresultatet`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val utgifter = boutgifterUtgiftService.hentUtgifterTilBeregning(behandlingId)

        val forventedeBeregningsperioder = mapBeregningsresultat(dataTable, utgifter)

        val beregningsresultat = hentVedtak(behandlingId).beregningsresultat

        verifiserAtListerErLike(faktisk = beregningsresultat.perioder, forventet = forventedeBeregningsperioder)
    }

    @Så("kan vi forvente følgende andeler for behandling={}")
    @Og("følgende andeler for behandling={}")
    fun `forvent andeler`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

        val forventedeAndeler = mapAndeler(dataTable)

        val andeler =
            tilkjentYtelseRepositoryFake
                .findByBehandlingId(behandlingId)!!
                .andelerTilkjentYtelse
                .sortedBy { it.fom }

        verifiserAtListerErLike(faktisk = andeler.map { ForenkletAndel(it) }, forventet = forventedeAndeler)
    }

    @Så("kan vi forvente følgende vedtaksperioder for behandling={}")
    @Og("følgende vedtaksperioder for behandling={}")
    fun `forvent vedtaksperioder`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

        val vedtaksperioder = hentVedtak(behandlingId).vedtaksperioder

        val forventedeVedtaksperioder = mapVedtaksperioder(dataTable)

        verifiserAtListerErLike(vedtaksperioder, forventedeVedtaksperioder)
    }

    @Så("forvent følgende feilmelding: {}")
    fun `forvent følgende feil`(forventetFeilmelding: String) {
        assertThat(feil).isNotNull
        assertThat(feil?.message).contains(forventetFeilmelding)
    }

    @Så("forvent følgende feilmelding:")
    fun `forvent følgende feilmelding`(forventetFeilmelding: String) {
        assertThat(feil).isNotNull
        assertThat(feil?.message).contains(forventetFeilmelding)
    }

    private fun hentVedtak(behandlingId: BehandlingId): InnvilgelseEllerOpphørBoutgifter =
        vedtakRepositoryFake
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørBoutgifter>()
            .data

    private fun dummyBehandling(
        behandlingId: BehandlingId,
        steg: StegType = StegType.BEREGNE_YTELSE,
        revurderFra: LocalDate? = null,
    ): Saksbehandling {
        val forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId(behandlingId)
        return saksbehandling(
            id = behandlingId,
            steg = steg,
            fagsak = fagsak(stønadstype = Stønadstype.BOUTGIFTER),
            forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
            revurderFra = revurderFra,
            type = if (forrigeIverksatteBehandlingId != null) BehandlingType.REVURDERING else BehandlingType.FØRSTEGANGSBEHANDLING,
        )
    }

    private fun forrigeIverksatteBehandlingId(behandlingId: BehandlingId): BehandlingId? {
        val behandlingIdInt = behandlingIdTilTestId(behandlingId)
        return if (behandlingIdInt > 1) {
            testIdTilBehandlingId.getValue(behandlingIdInt - 1)
        } else {
            null
        }
    }

    private fun kjørMedFeilkontekst(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.error(e.message, e)
            feil = e
        }
    }
}
