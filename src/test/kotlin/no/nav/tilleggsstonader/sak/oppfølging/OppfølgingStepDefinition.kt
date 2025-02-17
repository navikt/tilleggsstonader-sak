package no.nav.tilleggsstonader.sak.oppfølging

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.tilleggsstonader.sak.cucumber.StønadsperiodeCucumberUtil.StønadsperiodeNøkler
import no.nav.tilleggsstonader.sak.cucumber.StønadsperiodeCucumberUtil.mapStønadsperioder
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerBeregnYtelseStegStepDefinitions.ForenkletAndel
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.RegisterAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagTestUtil.periodeGrunnlagAktivitet
import org.assertj.core.api.Assertions.assertThat

class OppfølgingStepDefinition {

    val behandling = behandling()
    val behandlingRepository = mockk<BehandlingRepository>().apply {
        val repository = this
        every { repository.finnGjeldendeIverksatteBehandlinger() } returns listOf(behandling)
    }
    val fagsakService = mockk<FagsakService>().apply {
        val service = this
        every { service.hentMetadata(any()) } answers {
            val fagsakIds = firstArg<List<FagsakId>>()
            fagsakIds.associateWith { FagsakMetadata(it, 1, Stønadstype.BARNETILSYN, "1") }
        }
    }
    val stønadsperiodeService = mockk<StønadsperiodeService>()
    val registerAktivitetService = mockk<RegisterAktivitetService>()

    val oppfølgingService = OppfølgingService(
        behandlingRepository = behandlingRepository,
        stønadsperiodeService = stønadsperiodeService,
        registerAktivitetService = registerAktivitetService,
        fagsakService = fagsakService,
    )

    var hentBehandlingerForOppfølging: List<BehandlingForOppfølgingDto> = emptyList()

    @Gitt("følgende stønadsperioder")
    fun følgendeStønadsperioder(dataTable: DataTable) {
        every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
                mapStønadsperioder(behandling.id, dataTable).tilSortertDto()
    }

    @Gitt("følgende registeraktiviteter")
    fun følgendeRegisteraktiviteter(dataTable: DataTable) {
        every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                mapRegisterAktiviteter(dataTable)

    }

    @Når("følger opp")
    fun beregner() {
        hentBehandlingerForOppfølging = oppfølgingService.hentBehandlingerForOppfølging()
    }

    fun mapRegisterAktiviteter(
        dataTable: DataTable,
    ) = dataTable.mapRad { rad ->
        aktivitetArenaDto(
            fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
            tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
        )
    }

}