package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.behandlingIdFraUUID
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
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
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregningService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterUtgiftService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.ForenkletAndel
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.mapAndeler
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.mapBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BoutgifterDomenenøkkel
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårLøpendeUtgifterEnBoligDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
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
            unleashService = mockk<UnleashService>(relaxed = true),
        )
    val steg =
        BoutgifterBeregnYtelseSteg(
            beregningService =
            beregningService,
            opphørValideringService = mockk<OpphørValideringService>(relaxed = true), // TODO: Vurder om denne skal være streng
            vedtakRepository = vedtakRepositoryFake,
            tilkjentYtelseService = TilkjentYtelseService(tilkjentYtelseRepositoryFake),
            simuleringService = simuleringServiceMock,
        )

    @Gitt("følgende boutgifter av type {} for behandling={}")
    fun `følgende boutgifter`(
        typeBoutgift: VilkårType,
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        every { behandlingServiceMock.hentSaksbehandling(any<BehandlingId>()) } returns
            dummyBehandling(
                behandlingId = behandlingId,
                steg = StegType.VILKÅR,
            )
        val delvilkår = oppfylteDelvilkårLøpendeUtgifterEnBoligDto()

        val opprettVilkårDto =
            dataTable.mapRad { rad ->
                OpprettVilkårDto(
                    vilkårType = typeBoutgift,
                    behandlingId = behandlingId,
                    delvilkårsett = delvilkår,
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    utgift = parseInt(BoutgifterDomenenøkkel.UTGIFT, rad),
                    erFremtidigUtgift = false,
                )
            }

        opprettVilkårDto.forEach { vilkårService.opprettNyttVilkår(it) }
    }

    @Når("vi innvilger boutgifter for behandling={} med følgende vedtaksperioder")
    fun `følgende vedtaksperioder`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)

        val aktiviteterFraVedtaksperioder =
            dataTable.mapRad { rad ->
                aktivitet(
                    behandlingId = behandlingId,
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetBoutgifter(
                            type = parseValgfriEnum<AktivitetType>(BoutgifterDomenenøkkel.AKTIVITET, rad)!!,
                        ),
                )
            }

        val målgrupperFraVedtaksperioder =
            dataTable.mapRad { rad ->
                målgruppe(
                    behandlingId = behandlingId,
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    begrunnelse = "begrunnelse",
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppe(
                            type = parseValgfriEnum<MålgruppeType>(BoutgifterDomenenøkkel.MÅLGRUPPE, rad)!!,
                        ),
                )
            }

        vilkårperiodeRepositoryFake.insertAll(aktiviteterFraVedtaksperioder)
        vilkårperiodeRepositoryFake.insertAll(målgrupperFraVedtaksperioder)

        every { behandlingServiceMock.hentSaksbehandling(any<BehandlingId>()) } returns
            dummyBehandling(
                behandlingId = behandlingId,
                steg = StegType.BEREGNE_YTELSE,
            )
        val vedtaksperioder = mapVedtaksperioder(dataTable).map { it.tilDto() }
        steg.utførSteg(dummyBehandling(behandlingId), InnvilgelseBoutgifterRequest(vedtaksperioder))
    }

//    @Når("innvilger revurdering med vedtaksperioder for behandling={} med revurderFra={}")
//    fun `innvilger vedtaksperioder for behandling={} med revurderFra={}`(
//        behandlingIdTall: Int,
//        revurderFraStr: String,
//        dataTable: DataTable,
//    ) {
//        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns saksbehandling()
//        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
//        val revurderFra = parseDato(revurderFraStr)
//
//        val vedtaksperioder = mapVedtaksperioderDto(dataTable)
//        steg.utførSteg(dummyBehandling(behandlingId, revurderFra), InnvilgelseBoutgifterRequest(vedtaksperioder))
//    }

    @Når("kopierer perioder fra forrige boutgiftbehandling for behandling={}")
    fun `kopierer perioder`(behandlingIdTall: Int) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val forrigeIverksatteBehandlingId =
            forrigeIverksatteBehandlingId(behandlingId) ?: error("Forventer å finne forrigeIverksatteBehandlingId")

        val tidligereVilkårsperioder = vilkårperiodeRepositoryFake.findByBehandlingId(forrigeIverksatteBehandlingId)
        val tidligereVilkår = vilkårRepositoryFake.findByBehandlingId(forrigeIverksatteBehandlingId)

        vilkårperiodeRepositoryFake.insertAll(tidligereVilkårsperioder.map { it.kopierTilBehandling(behandlingId) })
        vilkårRepositoryFake.insertAll(tidligereVilkår.map { it.kopierTilBehandling(behandlingId) })
    }

//    @Gitt("lagrer beregningsresultatet behandling={}")
//    fun `lagrer beregningsresultat`(
//        behandlingIdTall: Int,
//        dataTable: DataTable,
//    ) {
//        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
//        val utgifter = boutgifterUtgiftService.hentUtgifterTilBeregning(behandlingId)
//        val perioderBeregningsresultat = mapBeregningsresultat(dataTable, utgifter)
//        val vedtaksperiode = mapVedtaksperioder(dataTable)
//        val vedtak =
//            innvilgelseBoutgifter(
//                behandlingId = behandlingId,
//                vedtaksperioder = vedtaksperiode,
//                beregningsresultat = BeregningsresultatBoutgifter(perioderBeregningsresultat),
//            )
//        vedtakRepository.insert(vedtak)
//    }
//
//    @Gitt("lagrer andeler behandling={}")
//    fun `lagrer andeler`(
//        behandlingIdTall: Int,
//        dataTable: DataTable,
//    ) {
//        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
//
//        val andeler =
//            mapAndeler(dataTable)
//                .map {
//                    AndelTilkjentYtelse(
//                        beløp = it.beløp,
//                        fom = it.fom,
//                        tom = it.tom,
//                        satstype = it.satstype,
//                        type = it.type,
//                        kildeBehandlingId = behandlingId,
//                        utbetalingsdato = it.utbetalingsdato,
//                    )
//                }.toSet()
//        tilkjentYtelseRepository.insert(TilkjentYtelse(behandlingId = behandlingId, andelerTilkjentYtelse = andeler))
//    }

    @Når("vi opphører boutgifter behandling={} med revurderFra={}")
    fun `opphør med revurderFra`(
        behandlingIdTall: Int,
        revurderFraStr: String,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val revurderFra = parseDato(revurderFraStr)
        steg.utførSteg(
            dummyBehandling(behandlingId, revurderFra = revurderFra),
            OpphørBoutgifterRequest(
                årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                begrunnelse = "begrunnelse",
            ),
        )
    }

    @Så("kan vi forvente følgende beregningsresultat for behandling={}")
    fun `forvent beregningsresultatet`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val utgifter = boutgifterUtgiftService.hentUtgifterTilBeregning(behandlingId)

        val forventedeBeregningsperioder = mapBeregningsresultat(dataTable, utgifter)

        val beregningsresultat = hentVedtak(behandlingId).beregningsresultat

        forventedeBeregningsperioder.forEachIndexed { index, periode ->
            try {
                assertThat(beregningsresultat.perioder[index]).isEqualTo(periode)
            } catch (e: Throwable) {
                val actual =
                    objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(beregningsresultat.perioder[index])
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
        assertThat(beregningsresultat.perioder).hasSize(forventedeBeregningsperioder.size)
    }

    @Så("kan vi forvente følgende andeler for behandling={}")
    fun `forvent andeler`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)

        val forventedeAndeler = mapAndeler(dataTable)

        val andeler =
            tilkjentYtelseRepositoryFake
                .findByBehandlingId(behandlingId)!!
                .andelerTilkjentYtelse
                .sortedBy { it.fom }

        andeler.map { ForenkletAndel(it) }.forEachIndexed { index, andel ->
            try {
                assertThat(andel).isEqualTo(forventedeAndeler[index])
            } catch (e: Throwable) {
                val actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(andel)
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
        assertThat(andeler).hasSize(forventedeAndeler.size)
    }

    @Så("kan vi forvente følgende vedtaksperioder for behandling={}")
    fun `forvent vedtaksperioder`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)

        val vedtaksperioder = hentVedtak(behandlingId).vedtaksperioder

        val forventedeVedtaksperioder = mapVedtaksperioder(dataTable)

        forventedeVedtaksperioder.forEachIndexed { index, periode ->
            try {
                assertThat(vedtaksperioder[index]).isEqualTo(periode)
            } catch (e: Throwable) {
                val actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(periode)
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
        assertThat(vedtaksperioder).hasSize(forventedeVedtaksperioder.size)
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
        val behandlingIdInt = behandlingIdFraUUID(behandlingId)
        return if (behandlingIdInt > 1) {
            behandlingIdTilUUID.getValue(behandlingIdInt - 1)
        } else {
            null
        }
    }
}
