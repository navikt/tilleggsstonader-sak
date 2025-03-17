package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDate.now
import java.util.UUID

const val SAKSBEHANDLER_VL = "VL"
const val SAKSBEHANDLER_B = "B"

class StønadsperiodeServiceTest : IntegrationTest() {
    @Autowired
    lateinit var stønadsperiodeService: StønadsperiodeService

    @Autowired
    lateinit var stønadsperiodeRepository: StønadsperiodeRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    val førsteJanuar: LocalDate = LocalDate.of(2023, 1, 1)
    val sisteJanuar: LocalDate = LocalDate.of(2023, 1, 31)

    val enMånedSiden: LocalDate = now().minusMonths(1)
    val enMånedFram: LocalDate = now().plusMonths(1)

    @Nested
    inner class LagreStønadsperioder {
        @Test
        fun `skal feile hvis man prøver å opprette en støndsperiode med id`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            opprettVilkårperiode(aktivitet(behandlingId = behandling.id))

            val periode = stønadsperiodeDto(UUID.randomUUID())
            assertThatThrownBy {
                stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf(periode))
            }.hasMessageContaining("Kan ikke oppdatere stønadsperiode=${periode.id} som ikke finnes fra før")
        }

        @Test
        fun `skal kunne opprette flere stønadsperioder`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            opprettVilkårperiode(aktivitet(behandlingId = behandling.id))

            val lagredeStønadsperioder =
                testWithBrukerContext(SAKSBEHANDLER_VL) {
                    stønadsperiodeService.lagreStønadsperioder(
                        behandling.id,
                        listOf(
                            stønadsperiodeDto(fom = førsteJanuar, tom = førsteJanuar),
                            stønadsperiodeDto(fom = førsteJanuar.plusDays(1), tom = førsteJanuar.plusDays(1)),
                        ),
                    )
                }
            val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandling.id)

            assertThat(lagredeStønadsperioder).hasSize(2)
            assertThat(lagredeStønadsperioder).containsExactlyInAnyOrderElementsOf(stønadsperioder.tilSortertDto())
            assertThat(stønadsperioder.first().sporbar.opprettetAv).isEqualTo(SAKSBEHANDLER_VL)
            assertThat(
                stønadsperioder
                    .first()
                    .sporbar.endret.endretAv,
            ).isEqualTo(SAKSBEHANDLER_VL)
        }

        @Test
        fun `endring av perioden oppdaterer felter men ikke opprettetAv`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            opprettVilkårperiode(aktivitet(behandlingId = behandling.id))
            opprettVilkårperiode(
                målgruppe(
                    type = MålgruppeType.OVERGANGSSTØNAD,
                    dekkesAvAnnetRegelverk = null,
                    behandlingId = behandling.id,
                ),
            )
            opprettVilkårperiode(
                aktivitet(
                    AktivitetType.UTDANNING,
                    lønnet = null,
                    behandlingId = behandling.id,
                ),
            )

            val periode =
                stønadsperiodeService
                    .lagreStønadsperioder(
                        behandling.id,
                        listOf(stønadsperiodeDto(fom = førsteJanuar, tom = førsteJanuar)),
                    ).single()
            val oppdatertPeriode =
                periode.copy(
                    fom = førsteJanuar.plusDays(1),
                    tom = førsteJanuar.plusDays(10),
                    målgruppe = MålgruppeType.OVERGANGSSTØNAD,
                    aktivitet = AktivitetType.UTDANNING,
                )
            val oppdatertePerioder =
                testWithBrukerContext(SAKSBEHANDLER_B, groups = listOf(rolleConfig.saksbehandlerRolle)) {
                    stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf(oppdatertPeriode))
                }
            assertThat(oppdatertePerioder).hasSize(1)

            with(stønadsperiodeRepository.findAllByBehandlingId(behandling.id).single()) {
                assertThat(fom).isEqualTo(førsteJanuar.plusDays(1))
                assertThat(tom).isEqualTo(førsteJanuar.plusDays(10))
                assertThat(målgruppe).isEqualTo(MålgruppeType.OVERGANGSSTØNAD)
                assertThat(aktivitet).isEqualTo(AktivitetType.UTDANNING)
                assertThat(status).isEqualTo(StønadsperiodeStatus.NY)

                assertThat(sporbar.opprettetAv).isEqualTo(SAKSBEHANDLER_VL)
                assertThat(sporbar.endret.endretAv).isEqualTo(SAKSBEHANDLER_B)
            }
        }

        @Test
        fun `skal kunne slette en periode`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            opprettVilkårperiode(aktivitet(behandlingId = behandling.id))

            stønadsperiodeService.lagreStønadsperioder(
                behandling.id,
                listOf(stønadsperiodeDto(fom = førsteJanuar, tom = førsteJanuar)),
            )
            stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf())
            val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandling.id)

            assertThat(stønadsperioder).isEmpty()
        }

        @Test
        fun `skal kunne endre, legge til og slette i en oppdatering`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            opprettVilkårperiode(målgruppe(behandlingId = behandling.id))
            opprettVilkårperiode(aktivitet(behandlingId = behandling.id))
            opprettVilkårperiode(
                målgruppe(
                    type = MålgruppeType.OVERGANGSSTØNAD,
                    dekkesAvAnnetRegelverk = null,
                    behandlingId = behandling.id,
                ),
            )
            opprettVilkårperiode(
                aktivitet(
                    AktivitetType.UTDANNING,
                    lønnet = null,
                    behandlingId = behandling.id,
                ),
            )

            val stønadsperioder =
                stønadsperiodeService.lagreStønadsperioder(
                    behandling.id,
                    listOf(
                        stønadsperiodeDto(fom = førsteJanuar, tom = førsteJanuar),
                        stønadsperiodeDto(fom = sisteJanuar, tom = sisteJanuar),
                    ),
                )
            val førsteStønadsperiode = stønadsperioder.single { it.fom == førsteJanuar }
            val stønadsperiodeSomBlirSlettet = stønadsperioder.single { it.fom == sisteJanuar }

            val oppdatertPeriode =
                førsteStønadsperiode.copy(
                    fom = førsteJanuar.plusDays(1),
                    tom = førsteJanuar.plusDays(1),
                    målgruppe = MålgruppeType.OVERGANGSSTØNAD,
                    aktivitet = AktivitetType.UTDANNING,
                )
            val nyPeriode = stønadsperiodeDto(fom = førsteJanuar.plusDays(10), tom = førsteJanuar.plusDays(10))
            val stønadsperioder2 =
                testWithBrukerContext(SAKSBEHANDLER_B, groups = listOf(rolleConfig.saksbehandlerRolle)) {
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

                assertThat(sporbar.opprettetAv).isEqualTo(SAKSBEHANDLER_VL)
                assertThat(sporbar.endret.endretAv).isEqualTo(SAKSBEHANDLER_B)
            }

            // Ny
            with(stønadsperioderFraDb.single { it.id != førsteStønadsperiode.id }) {
                assertThat(fom).isEqualTo(nyPeriode.fom)
                assertThat(tom).isEqualTo(nyPeriode.tom)
                assertThat(målgruppe).isEqualTo(nyPeriode.målgruppe)
                assertThat(aktivitet).isEqualTo(nyPeriode.aktivitet)
                assertThat(sporbar.opprettetAv).isEqualTo(SAKSBEHANDLER_B)
                assertThat(sporbar.endret.endretAv).isEqualTo(SAKSBEHANDLER_B)
            }
        }

        @Test
        fun `skal kaste feil hvis behandlingen er låst`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))

            assertThatThrownBy {
                stønadsperiodeService.lagreStønadsperioder(behandlingId = behandling.id, listOf())
            }.hasMessageContaining("Kan ikke gjøre endringer på denne behandlingen fordi den er ferdigstilt.")
        }
    }

    @Nested
    inner class EndringHvisStønadsperiodeBegynnerFørRevurderFra {
        val behandling = behandling(type = BehandlingType.REVURDERING)

        val eksisterendeStønadsperiode =
            stønadsperiode(
                behandlingId = behandling.id,
                fom = enMånedSiden,
                tom = enMånedFram,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
                status = StønadsperiodeStatus.UENDRET,
            )

        @BeforeEach
        fun setUp() {
            val revurdering =
                testoppsettService.opprettBehandlingMedFagsak(behandling.copy(revurderFra = enMånedSiden))
            opprettVilkårperiode(målgruppe(behandlingId = revurdering.id, fom = enMånedSiden, tom = enMånedFram))
            opprettVilkårperiode(aktivitet(behandlingId = revurdering.id, fom = enMånedSiden, tom = enMånedFram))
            opprettVilkårperiode(
                aktivitet(
                    behandlingId = revurdering.id,
                    fom = enMånedSiden,
                    tom = enMånedFram,
                    type = AktivitetType.UTDANNING,
                    lønnet = null,
                ),
            )
        }

        @Test
        fun `kan ikke opprette periode som starter før revurderFra`() {
            testoppsettService.oppdater(behandling.copy(revurderFra = now()))

            assertThatThrownBy {
                val endretStønadsperiode = stønadsperiodeDto(fom = now().minusDays(1), tom = now().minusDays(1))
                stønadsperiodeService.lagreStønadsperioder(behandlingId = behandling.id, listOf(endretStønadsperiode))
            }.hasMessageContaining("Kan ikke opprette periode")
        }

        @Test
        fun `kan ikke oppdatere periode som begynner før revurderFra`() {
            testoppsettService.oppdater(behandling.copy(revurderFra = now()))
            stønadsperiodeRepository.insert(eksisterendeStønadsperiode)

            val oppdatertStønadsperiode = eksisterendeStønadsperiode.tilDto().copy(aktivitet = AktivitetType.UTDANNING)

            assertThatThrownBy {
                stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf(oppdatertStønadsperiode))
            }.hasMessageContaining("Ugyldig endring på periode")
        }

        @Test
        fun `kan ikke slette periode som begynner før revurderFra`() {
            stønadsperiodeRepository.insert(eksisterendeStønadsperiode)
            testoppsettService.oppdater(behandling.copy(revurderFra = now()))

            assertThatThrownBy {
                stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf())
            }.hasMessageContaining("Kan ikke slette periode")
        }

        @Nested
        inner class OppdateringAvStatus {
            @BeforeEach
            fun setUp() {
                stønadsperiodeRepository.insert(eksisterendeStønadsperiode)
            }

            @Test
            fun `ingen endring på eksisterende periode skal ha status UENDRET`() {
                val res =
                    stønadsperiodeService.lagreStønadsperioder(
                        behandling.id,
                        listOf(eksisterendeStønadsperiode.tilDto()),
                    )

                assertThat(res).hasSize(1)
                assertThat(res[0].status).isEqualTo(StønadsperiodeStatus.UENDRET)
            }

            @Test
            fun `oppdatering av eksisterende periode skal gi status ENDRET`() {
                val res =
                    stønadsperiodeService.lagreStønadsperioder(
                        behandling.id,
                        listOf(eksisterendeStønadsperiode.tilDto()),
                    )

                assertThat(res).hasSize(1)
                assertThat(res.single().status).isEqualTo(StønadsperiodeStatus.UENDRET)
            }

            @Test
            fun `ny periode skal få status NY`() {
                val nyPeriode =
                    StønadsperiodeDto(
                        fom = now().plusDays(1),
                        tom = now().plusMonths(1),
                        målgruppe = MålgruppeType.AAP,
                        aktivitet = AktivitetType.TILTAK,
                        status = null,
                    )
                val res = stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf(nyPeriode))

                assertThat(res).hasSize(1)
                assertThat(res.single().status).isEqualTo(StønadsperiodeStatus.NY)
            }
        }
    }

    @Nested
    inner class GjenbrukStønadsperioder {
        @Test
        fun `skal gjenbruke stønadsperioder fra forrige behandlingen`() {
            val revurdering = testoppsettService.lagBehandlingOgRevurdering()

            val eksisterendeStønadsperidoder =
                listOf(
                    stønadsperiode(
                        behandlingId = revurdering.forrigeIverksatteBehandlingId!!,
                        fom = LocalDate.of(2024, 1, 1),
                        tom = LocalDate.of(2024, 1, 31),
                        målgruppe = MålgruppeType.AAP,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                    stønadsperiode(
                        behandlingId = revurdering.forrigeIverksatteBehandlingId,
                        fom = LocalDate.of(2024, 2, 1),
                        tom = LocalDate.of(2024, 1, 10),
                        målgruppe = MålgruppeType.OVERGANGSSTØNAD,
                        aktivitet = AktivitetType.UTDANNING,
                    ),
                )
            stønadsperiodeRepository.insertAll(eksisterendeStønadsperidoder)

            stønadsperiodeService.gjenbrukStønadsperioder(
                forrigeIverksatteBehandlingId = revurdering.forrigeIverksatteBehandlingId,
                nyBehandlingId = revurdering.id,
            )

            val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(revurdering.id)

            assertThat(stønadsperioder).hasSize(2)

            assertThat(stønadsperioder)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                    "id",
                    "sporbar",
                    "behandlingId",
                    "status",
                ).containsExactlyInAnyOrderElementsOf(eksisterendeStønadsperidoder)

            assertThat(stønadsperioder.map { it.status }).containsOnly(StønadsperiodeStatus.UENDRET)
        }
    }

    @Nested
    inner class StatusStønadsperioder {
        val behandling = behandling(type = BehandlingType.REVURDERING, revurderFra = enMånedSiden)

        val eksisterendeStønadsperiode =
            stønadsperiode(
                behandlingId = behandling.id,
                fom = enMånedSiden,
                tom = enMånedFram,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
                status = StønadsperiodeStatus.UENDRET,
            )

        @BeforeEach
        fun setUp() {
            val revurdering = testoppsettService.opprettBehandlingMedFagsak(behandling)
            opprettVilkårperiode(målgruppe(behandlingId = revurdering.id, fom = enMånedSiden, tom = enMånedFram))
            opprettVilkårperiode(aktivitet(behandlingId = revurdering.id, fom = enMånedSiden, tom = enMånedFram))
            stønadsperiodeRepository.insert(eksisterendeStønadsperiode)
        }

        @Test
        fun `ingen endring på eksisterende periode skal ha status UENDRET`() {
            val res =
                stønadsperiodeService.lagreStønadsperioder(
                    behandling.id,
                    listOf(eksisterendeStønadsperiode.tilDto()),
                )

            assertThat(res).hasSize(1)
            assertThat(res[0].status).isEqualTo(StønadsperiodeStatus.UENDRET)
        }

        @Test
        fun `oppdatering av eksisterende periode skal gi status ENDRET`() {
            val res =
                stønadsperiodeService.lagreStønadsperioder(
                    behandling.id,
                    listOf(eksisterendeStønadsperiode.tilDto()),
                )

            assertThat(res).hasSize(1)
            assertThat(res.single().status).isEqualTo(StønadsperiodeStatus.UENDRET)
        }

        @Test
        fun `ny periode skal få status NY`() {
            val nyPeriode =
                StønadsperiodeDto(
                    fom = now().plusDays(1),
                    tom = now().plusMonths(1),
                    målgruppe = MålgruppeType.AAP,
                    aktivitet = AktivitetType.TILTAK,
                    status = null,
                )
            val res = stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf(nyPeriode))

            assertThat(res).hasSize(1)
            assertThat(res.single().status).isEqualTo(StønadsperiodeStatus.NY)
        }
    }

    private fun målgruppe(
        type: MålgruppeType = MålgruppeType.AAP,
        fom: LocalDate = this.førsteJanuar,
        tom: LocalDate = this.sisteJanuar,
        medlemskap: SvarJaNei? = null,
        dekkesAvAnnetRegelverk: SvarJaNei? = SvarJaNei.NEI,
        behandlingId: BehandlingId = BehandlingId.random(),
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        faktaOgSvar =
            FaktaOgSvarMålgruppeDto(
                svarMedlemskap = medlemskap,
                svarUtgifterDekketAvAnnetRegelverk = dekkesAvAnnetRegelverk,
            ),
        behandlingId = behandlingId,
    )

    private fun aktivitet(
        type: AktivitetType = AktivitetType.TILTAK,
        fom: LocalDate = this.førsteJanuar,
        tom: LocalDate = this.sisteJanuar,
        lønnet: SvarJaNei? = SvarJaNei.NEI,
        behandlingId: BehandlingId = BehandlingId.random(),
        aktivitetsdager: Int = 5,
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        faktaOgSvar =
            FaktaOgSvarAktivitetBarnetilsynDto(
                aktivitetsdager = aktivitetsdager,
                svarLønnet = lønnet,
            ),
        behandlingId = behandlingId,
    )

    private fun stønadsperiodeDto(
        id: UUID? = null,
        fom: LocalDate = this.førsteJanuar,
        tom: LocalDate = this.sisteJanuar,
        målgruppeType: MålgruppeType = MålgruppeType.AAP,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
        status: StønadsperiodeStatus = StønadsperiodeStatus.NY,
    ) = StønadsperiodeDto(
        id = id,
        fom = fom,
        tom = tom,
        målgruppe = målgruppeType,
        aktivitet = aktivitet,
        status = status,
    )

    private fun opprettVilkårperiode(periode: LagreVilkårperiode): LagreVilkårperiodeResponse {
        val oppdatertPeriode = vilkårperiodeService.opprettVilkårperiode(periode)
        return vilkårperiodeService.validerOgLagResponse(
            behandlingId = oppdatertPeriode.behandlingId,
            periode = oppdatertPeriode,
        )
    }
}
