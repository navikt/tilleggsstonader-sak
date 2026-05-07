package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class AvklartKjørelisteServiceTest {
    private val avklartKjørtUkeRepository = mockk<AvklartKjørtUkeRepository>()
    private val kjørelisteService = mockk<KjørelisteService>()
    private val vedtakService = mockk<VedtakService>()
    private val behandlingService = mockk<BehandlingService>()

    private val service =
        AvklartKjørelisteService(
            vedtakService = vedtakService,
            avklartKjørtUkeRepository = avklartKjørtUkeRepository,
            kjørelisteService = kjørelisteService,
            behandlingService = behandlingService,
        )

    private val reiseId = ReiseId.random()
    private val mandag = 5 januar 2026
    private val fredag = 9 januar 2026
    private val behandlingId = BehandlingId.random()
    private val forrigeBehandlingId = BehandlingId.random()

    @Nested
    inner class OppdaterAvklartUkeStatus {
        @Test
        fun `UENDRET skal bli ENDRET når data faktisk endres`() {
            val eksisterendeUke = lagUke(status = AvklartKjørtUkeStatus.UENDRET, parkeringPåMandag = null)
            val oppdatertSlot = slot<AvklartKjørtUke>()

            settOppMocks(eksisterendeUke)
            every { avklartKjørtUkeRepository.update(capture(oppdatertSlot)) } answers { oppdatertSlot.captured }

            service.oppdaterAvklartUke(
                behandlingId = behandlingId,
                ukeId = eksisterendeUke.id,
                request = lagRequest(parkeringPåMandag = 50),
            )

            assertThat(oppdatertSlot.captured.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.ENDRET)
        }

        @Test
        fun `UENDRET skal forbli UENDRET når data ikke endres`() {
            val eksisterendeUke =
                lagUke(status = AvklartKjørtUkeStatus.UENDRET, parkeringPåMandag = 50, begrunnelsePåMandag = "endret i test")
            val oppdatertSlot = slot<AvklartKjørtUke>()

            settOppMocks(eksisterendeUke)
            every { avklartKjørtUkeRepository.update(capture(oppdatertSlot)) } answers { oppdatertSlot.captured }

            service.oppdaterAvklartUke(
                behandlingId = behandlingId,
                ukeId = eksisterendeUke.id,
                request = lagRequest(parkeringPåMandag = 50),
            )

            assertThat(oppdatertSlot.captured.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.UENDRET)
        }

        @Test
        fun `ENDRET skal bli UENDRET når data endres tilbake til å matche forrige behandling`() {
            val forrigeUke = lagUke(status = AvklartKjørtUkeStatus.UENDRET, behandlingId = forrigeBehandlingId, parkeringPåMandag = null)
            val eksisterendeUke = lagUke(status = AvklartKjørtUkeStatus.ENDRET, parkeringPåMandag = 50)
            val oppdatertSlot = slot<AvklartKjørtUke>()

            settOppMocks(eksisterendeUke, forrigeUke = forrigeUke)
            every { avklartKjørtUkeRepository.update(capture(oppdatertSlot)) } answers { oppdatertSlot.captured }

            // Endrer tilbake til null (matcher forrige behandling)
            service.oppdaterAvklartUke(
                behandlingId = behandlingId,
                ukeId = eksisterendeUke.id,
                request = lagRequest(parkeringPåMandag = null),
            )

            assertThat(oppdatertSlot.captured.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.UENDRET)
        }

        @Test
        fun `ENDRET skal forbli ENDRET når data fortsatt avviker fra forrige behandling`() {
            val forrigeUke = lagUke(status = AvklartKjørtUkeStatus.UENDRET, behandlingId = forrigeBehandlingId, parkeringPåMandag = null)
            val eksisterendeUke = lagUke(status = AvklartKjørtUkeStatus.ENDRET, parkeringPåMandag = 50)
            val oppdatertSlot = slot<AvklartKjørtUke>()

            settOppMocks(eksisterendeUke, forrigeUke = forrigeUke)
            every { avklartKjørtUkeRepository.update(capture(oppdatertSlot)) } answers { oppdatertSlot.captured }

            // Endrer parkering men fortsatt avvikende fra forrige behandling
            service.oppdaterAvklartUke(
                behandlingId = behandlingId,
                ukeId = eksisterendeUke.id,
                request = lagRequest(parkeringPåMandag = 100),
            )

            assertThat(oppdatertSlot.captured.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.ENDRET)
        }

        @Test
        fun `ENDRET skal forbli ENDRET når behandlingen ikke har noen forrige behandling`() {
            val eksisterendeUke = lagUke(status = AvklartKjørtUkeStatus.ENDRET, parkeringPåMandag = 50)
            val oppdatertSlot = slot<AvklartKjørtUke>()

            settOppMocks(eksisterendeUke, harForrigeBehandling = false)
            every { avklartKjørtUkeRepository.update(capture(oppdatertSlot)) } answers { oppdatertSlot.captured }

            service.oppdaterAvklartUke(
                behandlingId = behandlingId,
                ukeId = eksisterendeUke.id,
                request = lagRequest(parkeringPåMandag = null),
            )

            assertThat(oppdatertSlot.captured.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.ENDRET)
        }

        @Test
        fun `NY skal forbli NY uansett`() {
            val eksisterendeUke = lagUke(status = AvklartKjørtUkeStatus.NY, parkeringPåMandag = null)
            val oppdatertSlot = slot<AvklartKjørtUke>()

            settOppMocks(eksisterendeUke)
            every { avklartKjørtUkeRepository.update(capture(oppdatertSlot)) } answers { oppdatertSlot.captured }

            service.oppdaterAvklartUke(
                behandlingId = behandlingId,
                ukeId = eksisterendeUke.id,
                request = lagRequest(parkeringPåMandag = 50),
            )

            assertThat(oppdatertSlot.captured.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.NY)
        }

        @Test
        fun `UENDRET skal bli ENDRET når begrunnelse endres`() {
            val eksisterendeUke = lagUke(status = AvklartKjørtUkeStatus.UENDRET, begrunnelsePåMandag = null)
            val oppdatertSlot = slot<AvklartKjørtUke>()

            settOppMocks(eksisterendeUke)
            every { avklartKjørtUkeRepository.update(capture(oppdatertSlot)) } answers { oppdatertSlot.captured }

            service.oppdaterAvklartUke(
                behandlingId = behandlingId,
                ukeId = eksisterendeUke.id,
                request = lagRequest(begrunnelsePåMandag = "ny begrunnelse"),
            )

            assertThat(oppdatertSlot.captured.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.ENDRET)
        }

        @Test
        fun `ENDRET skal bli UENDRET når begrunnelse endres tilbake til å matche forrige behandling`() {
            val forrigeUke = lagUke(status = AvklartKjørtUkeStatus.UENDRET, behandlingId = forrigeBehandlingId, begrunnelsePåMandag = null)
            val eksisterendeUke = lagUke(status = AvklartKjørtUkeStatus.ENDRET, begrunnelsePåMandag = "endret begrunnelse")
            val oppdatertSlot = slot<AvklartKjørtUke>()

            settOppMocks(eksisterendeUke, forrigeUke = forrigeUke)
            every { avklartKjørtUkeRepository.update(capture(oppdatertSlot)) } answers { oppdatertSlot.captured }

            // Endrer begrunnelse tilbake til null (matcher forrige behandling)
            service.oppdaterAvklartUke(
                behandlingId = behandlingId,
                ukeId = eksisterendeUke.id,
                request = lagRequest(begrunnelsePåMandag = null),
            )

            assertThat(oppdatertSlot.captured.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.UENDRET)
        }
    }

    private fun settOppMocks(
        eksisterendeUke: AvklartKjørtUke,
        forrigeUke: AvklartKjørtUke? = null,
        harForrigeBehandling: Boolean = true,
    ) {
        val forrigeId = if (harForrigeBehandling) forrigeBehandlingId else null
        val behandling = behandling(id = behandlingId, forrigeIverksatteBehandlingId = forrigeId)

        every { avklartKjørtUkeRepository.findByIdOrThrow(eksisterendeUke.id) } returns eksisterendeUke

        val innsendtKjøreliste =
            KjørelisteUtil.kjøreliste(
                reiseId = reiseId,
                periode = Datoperiode(mandag, fredag),
                kjørteDager =
                    (0..4).map { dagOffset ->
                        KjørelisteUtil.KjørtDag(dato = mandag.plusDays(dagOffset.toLong()), parkeringsutgift = null)
                    },
            )
        every { kjørelisteService.hentKjøreliste(eksisterendeUke.kjørelisteId) } returns innsendtKjøreliste
        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        val vedtakData =
            InnvilgelseDagligReise(
                rammevedtakPrivatBil =
                    RammevedtakPrivatBil(
                        reiser = listOf(rammeForReiseMedPrivatBil(reiseId = reiseId, fom = mandag, tom = fredag)),
                    ),
                beregningsresultat = BeregningsresultatDagligReise(offentligTransport = null, privatBil = null),
                vedtaksperioder = emptyList(),
                beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
            )
        val generiskVedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                type = TypeVedtak.INNVILGELSE,
                data = vedtakData,
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
            )
        every { vedtakService.hentVedtakEllerFeil(behandlingId) } returns generiskVedtak

        if (harForrigeBehandling) {
            val forrigeUker = if (forrigeUke != null) listOf(forrigeUke) else emptyList()
            every { avklartKjørtUkeRepository.findByBehandlingId(forrigeBehandlingId) } returns forrigeUker
        }
    }

    private fun lagUke(
        status: AvklartKjørtUkeStatus,
        behandlingId: BehandlingId = this.behandlingId,
        parkeringPåMandag: Int? = null,
        begrunnelsePåMandag: String? = null,
    ): AvklartKjørtUke =
        AvklartKjørtUke(
            id = UUID.randomUUID(),
            behandlingId = behandlingId,
            kjørelisteId = KjørelisteId.random(),
            reiseId = reiseId,
            fom = mandag,
            tom = fredag,
            uke = mandag.tilUkeIÅr(),
            status = UkeStatus.OK_AUTOMATISK,
            avklartKjørtUkeStatus = status,
            dager =
                (0..4)
                    .map { dagOffset ->
                        val dato = mandag.plusDays(dagOffset.toLong())
                        AvklartKjørtDag(
                            dato = dato,
                            godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                            automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                            avvik = emptyList(),
                            parkeringsutgift = if (dagOffset == 0) parkeringPåMandag else null,
                            begrunnelse = if (dagOffset == 0) begrunnelsePåMandag else null,
                        )
                    }.toSet(),
        )

    private fun lagRequest(
        parkeringPåMandag: Int? = null,
        begrunnelsePåMandag: String? = null,
    ) = (0..4).map { dagOffset ->
        val dato = mandag.plusDays(dagOffset.toLong())
        val parkering = if (dagOffset == 0) parkeringPåMandag else null
        val begrunnelse = if (dagOffset == 0) begrunnelsePåMandag else null
        EndreAvklartDagRequest(
            dato = dato,
            godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
            parkeringsutgift = parkering,
            // Begrunnelse kreves ved avvik fra kjøreliste (kjøreliste har null)
            begrunnelse = begrunnelse ?: if (parkering != null) "endret i test" else null,
        )
    }
}
