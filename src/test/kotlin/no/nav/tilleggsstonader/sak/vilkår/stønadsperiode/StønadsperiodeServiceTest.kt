package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class StønadsperiodeServiceTest : IntegrationTest() {

    @Autowired
    lateinit var stønadsperiodeService: StønadsperiodeService

    @Autowired
    lateinit var stønadsperiodeRepository: StønadsperiodeRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    val SAKSHEH_A = "VL"
    val SAKSHEH_B = "B"

    val FOM = LocalDate.of(2023, 1, 1)
    val TOM = LocalDate.of(2023, 1, 31)

    @Nested
    inner class LagreStønadsperidoder {

        @Test
        fun `skal feile hvis man prøver å opprette en støndsperiode med id`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            vilkårperiodeService.opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            vilkårperiodeService.opprettVilkårperiode(aktivitet(behandlingId = behandling.id))

            val periode = stønadsperiodeDto(UUID.randomUUID())
            assertThatThrownBy {
                stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf(periode))
            }.hasMessageContaining("Kan ikke oppdatere stønadsperiode=${periode.id} som ikke finnes fra før")
        }

        @Test
        fun `skal kunne opprette flere stønadsperioder`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            vilkårperiodeService.opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            vilkårperiodeService.opprettVilkårperiode(aktivitet(behandlingId = behandling.id))

            val lagredeStønadsperioder = testWithBrukerContext(SAKSHEH_A) {
                stønadsperiodeService.lagreStønadsperioder(
                    behandling.id,
                    listOf(
                        stønadsperiodeDto(fom = FOM, tom = FOM),
                        stønadsperiodeDto(fom = FOM.plusDays(1), tom = FOM.plusDays(1)),
                    ),
                )
            }
            val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandling.id)

            assertThat(lagredeStønadsperioder).hasSize(2)
            assertThat(lagredeStønadsperioder).containsExactlyInAnyOrderElementsOf(stønadsperioder.tilSortertDto())
            assertThat(stønadsperioder.first().sporbar.opprettetAv).isEqualTo(SAKSHEH_A)
            assertThat(stønadsperioder.first().sporbar.endret.endretAv).isEqualTo(SAKSHEH_A)
        }

        @Test
        fun `endring av perioden oppdaterer felter men ikke opprettetAv`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            vilkårperiodeService.opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            vilkårperiodeService.opprettVilkårperiode(aktivitet(behandlingId = behandling.id))
            vilkårperiodeService.opprettVilkårperiode(målgruppe(MålgruppeType.OVERGANGSSTØNAD, behandlingId = behandling.id))
            vilkårperiodeService.opprettVilkårperiode(aktivitet(AktivitetType.UTDANNING, lønnet = null, behandlingId = behandling.id))

            val periode = stønadsperiodeService.lagreStønadsperioder(
                behandling.id,
                listOf(stønadsperiodeDto(fom = FOM, tom = FOM)),
            ).single()
            val oppdatertPeriode = periode.copy(
                fom = FOM.plusDays(1),
                tom = FOM.plusDays(10),
                målgruppe = MålgruppeType.OVERGANGSSTØNAD,
                aktivitet = AktivitetType.UTDANNING,
            )
            val oppdatertePerioder = testWithBrukerContext(SAKSHEH_B) {
                stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf(oppdatertPeriode))
            }
            assertThat(oppdatertePerioder).hasSize(1)

            with(stønadsperiodeRepository.findAllByBehandlingId(behandling.id).single()) {
                assertThat(fom).isEqualTo(FOM.plusDays(1))
                assertThat(tom).isEqualTo(FOM.plusDays(10))
                assertThat(målgruppe).isEqualTo(MålgruppeType.OVERGANGSSTØNAD)
                assertThat(aktivitet).isEqualTo(AktivitetType.UTDANNING)

                assertThat(sporbar.opprettetAv).isEqualTo(SAKSHEH_A)
                assertThat(sporbar.endret.endretAv).isEqualTo(SAKSHEH_B)
            }
        }

        @Test
        fun `skal kunne slette en periode`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            vilkårperiodeService.opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            vilkårperiodeService.opprettVilkårperiode(aktivitet(behandlingId = behandling.id))

            stønadsperiodeService.lagreStønadsperioder(
                behandling.id,
                listOf(stønadsperiodeDto(fom = FOM, tom = FOM)),
            )
            stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf())
            val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandling.id)

            assertThat(stønadsperioder).isEmpty()
        }

        @Test
        fun `skal kunne endre, legge til og slette i en oppdatering`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            vilkårperiodeService.opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            vilkårperiodeService.opprettVilkårperiode(aktivitet(behandlingId = behandling.id))
            vilkårperiodeService.opprettVilkårperiode(målgruppe(MålgruppeType.OVERGANGSSTØNAD, behandlingId = behandling.id))
            vilkårperiodeService.opprettVilkårperiode(aktivitet(AktivitetType.UTDANNING, lønnet = null, behandlingId = behandling.id))

            val stønadsperioder = stønadsperiodeService.lagreStønadsperioder(
                behandling.id,
                listOf(
                    stønadsperiodeDto(fom = FOM, tom = FOM),
                    stønadsperiodeDto(fom = TOM, tom = TOM),
                ),
            )
            val førsteStønadsperiode = stønadsperioder.single { it.fom == FOM }
            val stønadsperiodeSomBlirSlettet = stønadsperioder.single { it.fom == TOM }

            val oppdatertPeriode = førsteStønadsperiode.copy(
                fom = FOM.plusDays(1),
                tom = FOM.plusDays(1),
                målgruppe = MålgruppeType.OVERGANGSSTØNAD,
                aktivitet = AktivitetType.UTDANNING,
            )
            val nyPeriode = stønadsperiodeDto(fom = FOM.plusDays(10), tom = FOM.plusDays(10))
            val stønadsperioder2 = testWithBrukerContext(SAKSHEH_B) {
                stønadsperiodeService.lagreStønadsperioder(
                    behandling.id,
                    listOf(oppdatertPeriode, nyPeriode),
                )
            }

            val stønadsperioderFraDb = stønadsperiodeRepository.findAllByBehandlingId(behandling.id)
            assertThat(stønadsperioderFraDb.tilSortertDto())
                .containsExactlyInAnyOrderElementsOf(stønadsperioder2)

            // Slettet
            assertThat(stønadsperioderFraDb.map { it.id }).doesNotContain(stønadsperiodeSomBlirSlettet.id)

            // Oppdatert
            with(stønadsperioderFraDb.single { it.id == førsteStønadsperiode.id }) {
                assertThat(fom).isEqualTo(oppdatertPeriode.fom)
                assertThat(tom).isEqualTo(oppdatertPeriode.tom)
                assertThat(målgruppe).isEqualTo(oppdatertPeriode.målgruppe)
                assertThat(aktivitet).isEqualTo(oppdatertPeriode.aktivitet)

                assertThat(sporbar.opprettetAv).isEqualTo(SAKSHEH_A)
                assertThat(sporbar.endret.endretAv).isEqualTo(SAKSHEH_B)
            }

            // Ny
            with(stønadsperioderFraDb.single { it.id != førsteStønadsperiode.id }) {
                assertThat(fom).isEqualTo(nyPeriode.fom)
                assertThat(tom).isEqualTo(nyPeriode.tom)
                assertThat(målgruppe).isEqualTo(nyPeriode.målgruppe)
                assertThat(aktivitet).isEqualTo(nyPeriode.aktivitet)
                assertThat(sporbar.opprettetAv).isEqualTo(SAKSHEH_B)
                assertThat(sporbar.endret.endretAv).isEqualTo(SAKSHEH_B)
            }
        }

        @Test
        fun `skal kaste feil hvis behandlingen er låst`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))

            assertThatThrownBy {
                stønadsperiodeService.lagreStønadsperioder(behandlingId = behandling.id, listOf())
            }.hasMessageContaining("Kan ikke lagre stønadsperioder når behandlingen er låst")
        }
    }

    private fun målgruppe(
        type: MålgruppeType = MålgruppeType.AAP,
        fom: LocalDate = this.FOM,
        tom: LocalDate = this.TOM,
        medlemskap: SvarJaNei? = null,
        behandlingId: UUID = UUID.randomUUID(),
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        delvilkår = DelvilkårMålgruppeDto(VurderingDto(medlemskap)),
        behandlingId = behandlingId,
    )

    private fun aktivitet(
        type: AktivitetType = AktivitetType.TILTAK,
        fom: LocalDate = this.FOM,
        tom: LocalDate = this.TOM,
        lønnet: SvarJaNei? = SvarJaNei.NEI,
        mottarSykepenger: SvarJaNei? = SvarJaNei.NEI,
        behandlingId: UUID = UUID.randomUUID(),
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        delvilkår = DelvilkårAktivitetDto(VurderingDto(lønnet), VurderingDto(mottarSykepenger)),
        behandlingId = behandlingId,
    )

    private fun stønadsperiodeDto(
        id: UUID? = null,
        fom: LocalDate = this.FOM,
        tom: LocalDate = this.TOM,
        målgruppeType: MålgruppeType = MålgruppeType.AAP,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = StønadsperiodeDto(
        id = id,
        fom = fom,
        tom = tom,
        målgruppe = målgruppeType,
        aktivitet = aktivitet,
    )
}
