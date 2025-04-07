package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OppfølgingAdminControllerTest : IntegrationTest() {
    @Autowired
    lateinit var oppfølgingRepository: OppfølgingRepository

    @Autowired
    lateinit var oppfølgingAdminController: OppfølgingAdminController

    @Test
    fun `skal mappe oppfølging til nytt format`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val oppfølging = oppfølging(behandling)

        oppfølgingRepository.insert(oppfølging)

        oppfølgingAdminController.handle(oppfølgingRepository.findByIdOrThrow(oppfølging.id))

        val oppdatert = oppfølgingRepository.findByIdOrThrow(oppfølging.id)
        assertThat(oppdatert.data.perioderTilKontroll).containsExactlyInAnyOrder(
            PeriodeForKontroll(
                fom = LocalDate.now().minusDays(1),
                tom = LocalDate.now().minusDays(2),
                type = AktivitetType.UTDANNING,
                endringer =
                    listOf(
                        Kontroll(ÅrsakKontroll.FOM_ENDRET, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1)),
                    ),
            ),
            PeriodeForKontroll(
                fom = LocalDate.now().minusDays(1),
                tom = LocalDate.now().minusDays(2),
                type = MålgruppeType.AAP,
                endringer =
                    listOf(
                        Kontroll(ÅrsakKontroll.FOM_ENDRET, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)),
                    ),
            ),
            PeriodeForKontroll(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                type = MålgruppeType.AAP,
                endringer =
                    listOf(
                        Kontroll(ÅrsakKontroll.FOM_ENDRET, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)),
                    ),
            ),
        )
    }

    private fun oppfølging(behandling: Behandling): Oppfølging {
        val periodeForKontroll1 =
            PeriodeForKontroll(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.UTDANNING,
                endringAktivitet = listOf(),
                endringMålgruppe =
                    listOf(
                        Kontroll(ÅrsakKontroll.FOM_ENDRET, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)),
                    ),
                type = null,
                endringer = null,
            )
        val periodeForKontroll2 =
            PeriodeForKontroll(
                fom = LocalDate.now().minusDays(1),
                tom = LocalDate.now().minusDays(2),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.UTDANNING,
                endringAktivitet =
                    listOf(
                        Kontroll(ÅrsakKontroll.FOM_ENDRET, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1)),
                    ),
                endringMålgruppe =
                    listOf(
                        Kontroll(ÅrsakKontroll.FOM_ENDRET, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)),
                    ),
                type = null,
                endringer = null,
            )
        return Oppfølging(
            behandlingId = behandling.id,
            aktiv = false,
            data =
                OppfølgingData(
                    perioderTilKontroll = listOf(periodeForKontroll1, periodeForKontroll2),
                ),
        )
    }
}
