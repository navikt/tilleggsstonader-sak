package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDelperiodePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.tilOppdatering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class VilkårperiodeServiceTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Autowired
    lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Nested
    inner class SlettVilkårperiode {
        @Test
        fun `skal ikke kunne slette vilkårperiode hvis behandlingen ikke er under behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))

            val målgruppe =
                målgruppe(
                    behandlingId = behandling.id,
                )

            val lagretPeriode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårperiodeService.slettVilkårperiode(lagretPeriode.id, SlettVikårperiode(behandling.id))
            }.hasMessageContaining("Kan ikke gjøre endringer på denne behandlingen fordi den er ferdigstilt.")
        }

        @Nested
        inner class SlettVilkårperiodePermanent {
            lateinit var behandling: Behandling
            private lateinit var lagretPeriode: Vilkårperiode

            @BeforeEach
            fun setUp() {
                behandling =
                    testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.UTREDES))

                val målgruppe =
                    målgruppe(
                        behandlingId = behandling.id,
                    )

                lagretPeriode = vilkårperiodeRepository.insert(målgruppe)
            }
        }

        @Nested
        inner class SlettGjenbruktVilkårperiode {
            lateinit var revurdering: Behandling
            private lateinit var lagretPeriode: Vilkårperiode

            @BeforeEach
            fun setUp() {
                revurdering = testoppsettService.lagBehandlingOgRevurdering()

                val originalMålgruppe =
                    målgruppe(
                        behandlingId = revurdering.forrigeIverksatteBehandlingId!!,
                    )

                vilkårperiodeRepository.insert(originalMålgruppe)

                val revurderingMålgruppe =
                    originalMålgruppe.copy(
                        id = UUID.randomUUID(),
                        behandlingId = revurdering.id,
                        forrigeVilkårperiodeId = originalMålgruppe.id,
                    )

                lagretPeriode = vilkårperiodeRepository.insert(revurderingMålgruppe)
            }

            @Test
            fun `skal ikke kunne slette gjenbrukt periode uten kommentar`() {
                assertThatThrownBy {
                    vilkårperiodeService.slettVilkårperiode(lagretPeriode.id, SlettVikårperiode(revurdering.id, ""))
                }.hasMessageContaining("Mangler kommentar")

                assertThatThrownBy {
                    vilkårperiodeService.slettVilkårperiode(lagretPeriode.id, SlettVikårperiode(revurdering.id))
                }.hasMessageContaining("Mangler kommentar")
            }

            @Test
            fun `skal slettemarkere gjenbrukt periode om kommentar er sendt med`() {
                val saksbehandler = "saksbehandlerX"

                testWithBrukerContext(saksbehandler) {
                    vilkårperiodeService.slettVilkårperiode(
                        lagretPeriode.id,
                        SlettVikårperiode(revurdering.id, "kommentar"),
                    )
                }

                val oppdatertPeriode = vilkårperiodeRepository.findByIdOrThrow(lagretPeriode.id)
                assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
                assertThat(oppdatertPeriode.sporbar.endret.endretAv).isEqualTo(saksbehandler)
                assertThat(oppdatertPeriode.status).isEqualTo(Vilkårstatus.SLETTET)
            }
        }
    }

    @Nested
    inner class GjenbrukVilkårperioder {
        @Test
        fun `skal gjenbruke vilkår fra forrige behandling`() {
            val revurdering = testoppsettService.lagBehandlingOgRevurdering()

            val eksisterendeVilkårperioder =
                listOf(
                    målgruppe(behandlingId = revurdering.forrigeIverksatteBehandlingId!!),
                    aktivitet(behandlingId = revurdering.forrigeIverksatteBehandlingId),
                )

            vilkårperiodeRepository.insertAll(eksisterendeVilkårperioder)

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeIverksatteBehandlingId, revurdering.id)

            val res = vilkårperiodeRepository.findByBehandlingId(revurdering.id)
            assertThat(res).hasSize(2)

            assertThat(res)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                    "id",
                    "sporbar",
                    "behandlingId",
                    "forrigeVilkårperiodeId",
                    "status",
                ).containsExactlyInAnyOrderElementsOf(eksisterendeVilkårperioder)
            assertThat(res.map { it.status }).containsOnly(Vilkårstatus.UENDRET)
        }

        @Test
        fun `skal beholde gitVersjon`() {
            val gitVersjon = UUID.randomUUID().toString()
            val revurdering = testoppsettService.lagBehandlingOgRevurdering()

            val eksisterendeVilkårperioder =
                listOf(
                    målgruppe(behandlingId = revurdering.forrigeIverksatteBehandlingId!!)
                        .copy(gitVersjon = gitVersjon),
                )
            vilkårperiodeRepository.insertAll(eksisterendeVilkårperioder)

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeIverksatteBehandlingId, revurdering.id)

            val res = vilkårperiodeRepository.findByBehandlingId(revurdering.id)
            assertThat(res).hasSize(1)
            assertThat(res.single().gitVersjon).isEqualTo(gitVersjon)
        }

        @Test
        fun `skal ikke gjenbruke slettede vilkår fra forrige behandling`() {
            val revurdering = testoppsettService.lagBehandlingOgRevurdering()

            val eksisterendeVilkårperioder =
                listOf(
                    målgruppe(behandlingId = revurdering.forrigeIverksatteBehandlingId!!),
                    målgruppe(
                        behandlingId = revurdering.forrigeIverksatteBehandlingId,
                        resultat = ResultatVilkårperiode.SLETTET,
                        slettetKommentar = "slettet",
                    ),
                    aktivitet(behandlingId = revurdering.forrigeIverksatteBehandlingId),
                    aktivitet(
                        behandlingId = revurdering.forrigeIverksatteBehandlingId,
                        resultat = ResultatVilkårperiode.SLETTET,
                        slettetKommentar = "slettet",
                    ),
                )

            vilkårperiodeRepository.insertAll(eksisterendeVilkårperioder)

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeIverksatteBehandlingId, revurdering.id)

            val res = vilkårperiodeRepository.findByBehandlingId(revurdering.id)
            assertThat(res).hasSize(2)
            res.map { assertThat(it.resultat).isNotEqualTo(ResultatVilkårperiode.SLETTET) }
        }
    }

    @Test
    fun `skal oppdatere historikk med utredning påbegynt ved ny vilkårperiode`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.OPPRETTET))
        val målgruppeSomSkalOpprettes =
            dummyVilkårperiodeMålgruppe(
                medlemskap = SvarJaNei.NEI,
                begrunnelse = "begrunnelse målgruppe",
                behandlingId = behandling.id,
            )

        vilkårperiodeService.opprettVilkårperiode(målgruppeSomSkalOpprettes)

        val test = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandling.id)

        assertThat(test.steg).isEqualTo(StegType.INNGANGSVILKÅR)
        assertThat(test.utfall).isEqualTo(StegUtfall.UTREDNING_PÅBEGYNT)
    }

    @Test
    fun `skal oppdatere historikk ved oppdatering av eksisterende vilkårperiode`() {
        val revurdering = testoppsettService.lagBehandlingOgRevurdering()

        val eksisterendeVilkårperiode =
            vilkårperiodeRepository.insert(målgruppe(behandlingId = revurdering.id))

        val oppdatering =
            eksisterendeVilkårperiode.tilOppdatering().copy(
                begrunnelse = "Oppdatert begrunnelse",
            )

        vilkårperiodeService.oppdaterVilkårperiode(eksisterendeVilkårperiode.id, oppdatering)

        val test = behandlingshistorikkService.finnSisteBehandlingshistorikk(revurdering.id)

        assertThat(test.steg).isEqualTo(StegType.INNGANGSVILKÅR)
        assertThat(test.utfall).isEqualTo(StegUtfall.UTREDNING_PÅBEGYNT)
    }

    @Nested
    inner class ValiderAktivitetIkkeReferertAvDagligReiseVilkår {
        private lateinit var lagretAktivitet: Vilkårperiode

        @BeforeEach
        fun setUp() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.UTREDES))
            lagretAktivitet =
                vilkårperiodeRepository.insert(
                    aktivitet(behandlingId = behandling.id, resultat = ResultatVilkårperiode.OPPFYLT, begrunnelse = "begrunnelse"),
                )
        }

        private fun lagDagligReiseVilkår(
            aktivitetGlobalId: VilkårperiodeGlobalId,
            status: VilkårStatus = VilkårStatus.NY,
        ) {
            vilkårRepository.insert(
                vilkår(
                    behandlingId = lagretAktivitet.behandlingId,
                    type = VilkårType.DAGLIG_REISE,
                    status = status,
                    slettetKommentar = if (status == VilkårStatus.SLETTET) "kommentar" else null,
                    fakta =
                        FaktaDagligReisePrivatBil(
                            reiseId = ReiseId.random(),
                            reiseavstandEnVei = BigDecimal(10),
                            faktaDelperioder =
                                listOf(
                                    FaktaDelperiodePrivatBil(
                                        fom = LocalDate.now(),
                                        tom = LocalDate.now().plusDays(10),
                                        reisedagerPerUke = 5,
                                        bompengerPerDag = null,
                                        fergekostnadPerDag = null,
                                    ),
                                ),
                            adresse = "Tiltaksveien 1",
                            aktivitetId = aktivitetGlobalId,
                        ),
                ),
            )
        }

        @Test
        fun `skal kaste feil hvis aktivitet er referert av et aktivt dagligReise-vilkår`() {
            lagDagligReiseVilkår(lagretAktivitet.globalId)

            assertThatThrownBy {
                vilkårperiodeService.slettVilkårperiode(
                    lagretAktivitet.id,
                    SlettVikårperiode(lagretAktivitet.behandlingId, kommentar = "kommentar"),
                )
            }.hasMessageContaining("Aktiviteten er knyttet til et vilkår for daglig reise")
        }

        @Test
        fun `skal tillate sletting av aktivitet hvis refererende dagligReise-vilkår er slettet`() {
            lagDagligReiseVilkår(lagretAktivitet.globalId, status = VilkårStatus.SLETTET)

            vilkårperiodeService.slettVilkårperiode(
                lagretAktivitet.id,
                SlettVikårperiode(lagretAktivitet.behandlingId, kommentar = "kommentar"),
            )
        }

        @Test
        fun `skal tillate sletting av aktivitet hvis ingen dagligReise-vilkår refererer den`() {
            vilkårperiodeService.slettVilkårperiode(
                lagretAktivitet.id,
                SlettVikårperiode(lagretAktivitet.behandlingId, kommentar = "kommentar"),
            )
        }
    }
}
