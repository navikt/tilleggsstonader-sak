package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.barn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtaksdataTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Utgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class FlyttBeløpsperioderTilVilkårControllerTest : IntegrationTest() {

    @Autowired
    lateinit var vedtakRepository: TilsynBarnVedtakRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var controller: FlyttBeløpsperioderTilVilkårController

    val behandling = behandling(steg = StegType.BESLUTTE_VEDTAK)
    val barn = behandlingBarn(behandlingId = behandling.id)

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        barnRepository.insert(barn)
    }

    @Test
    fun `skal oppdatere vilkår med utgift fra vedtak`() {
        val fom = YearMonth.of(2023, 1)
        val tom = YearMonth.of(2023, 2)
        val utgift = 100
        val vedtaksdata = VedtaksdataTilsynBarn(
            utgifter = mapOf(
                barn(
                    barnId = barn.id,
                    Utgift(fom, tom, utgift),
                ),
            ),
        )
        vedtakRepository.insert(innvilgetVedtak(behandlingId = behandling.id, vedtak = vedtaksdata))
        val opprinneligVilkår = vilkårRepository.insert(
            vilkår(behandlingId = behandling.id, barnId = barn.id, type = VilkårType.PASS_BARN),
        )

        testWithBrukerContext {
            controller.oppdaterVilkår()
        }

        val oppdatertVilkår = vilkårRepository.findByBehandlingId(behandling.id).single()
        assertThat(oppdatertVilkår)
            .usingRecursiveComparison()
            .ignoringFields("fom", "tom", "utgift")
            .isEqualTo(opprinneligVilkår)

        assertThat(oppdatertVilkår.fom).isEqualTo(fom.atDay(1))
        assertThat(oppdatertVilkår.tom).isEqualTo(tom.atEndOfMonth())
        assertThat(oppdatertVilkår.utgift).isEqualTo(utgift)
    }

    @Test
    fun `skal opprette nye vilkår dersom det finnes fler enn 1 utgift`() {
        val fom = YearMonth.of(2023, 1)
        val tom = YearMonth.of(2023, 2)
        val utgift = 100
        val vedtaksdata = VedtaksdataTilsynBarn(
            utgifter = mapOf(
                barn(
                    barnId = barn.id,
                    Utgift(fom, tom, utgift),
                    Utgift(fom.plusYears(1), tom.plusYears(2), utgift + 1),
                ),
            ),
        )
        vedtakRepository.insert(innvilgetVedtak(behandlingId = behandling.id, vedtak = vedtaksdata))
        val opprinneligVilkår = vilkårRepository.insert(
            vilkår(behandlingId = behandling.id, barnId = barn.id, type = VilkårType.PASS_BARN),
        )

        testWithBrukerContext {
            controller.oppdaterVilkår()
        }

        val oppdaterteVilkår = vilkårRepository.findByBehandlingId(behandling.id)
        val oppdatertVilkår = oppdaterteVilkår.single { it.id == opprinneligVilkår.id }
        val opprettetVilkår = oppdaterteVilkår.single { it.id != opprinneligVilkår.id }
        with(oppdatertVilkår) {
            assertThat(this)
                .usingRecursiveComparison()
                .ignoringFields("fom", "tom", "utgift")
                .isEqualTo(opprinneligVilkår)

            assertThat(this.fom).isEqualTo(fom.atDay(1))
            assertThat(this.tom).isEqualTo(tom.atEndOfMonth())
            assertThat(this.utgift).isEqualTo(utgift)
        }
        assertThat(opprettetVilkår)
            .usingRecursiveComparison()
            .ignoringFields("id", "fom", "tom", "utgift")
            .isEqualTo(oppdatertVilkår)

        assertThat(opprettetVilkår.fom).isEqualTo(fom.atDay(1).plusYears(1))
        assertThat(opprettetVilkår.tom).isEqualTo(tom.atEndOfMonth().plusYears(2))
        assertThat(opprettetVilkår.utgift).isEqualTo(101)
    }

    @Test
    fun `skal oppdatere en behandling som er i steg vedtak som mangler vedtak til steg inngangsvilkår for å kunne legge in perioder og utgift på vilkår`() {
        testWithBrukerContext {
            controller.oppdaterVilkår()
        }

        assertThat(testoppsettService.hentBehandling(behandling.id).steg).isEqualTo(StegType.INNGANGSVILKÅR)
    }

    @Test
    fun `skal ikke oppdatere behandling dersom behandling er henlagt, dermed avsluttet`() {
        testoppsettService.oppdater(behandling.copy(status = BehandlingStatus.FERDIGSTILT))
        testWithBrukerContext {
            controller.oppdaterVilkår()
        }

        assertThat(testoppsettService.hentBehandling(behandling.id).steg).isEqualTo(StegType.BESLUTTE_VEDTAK)
    }
}
