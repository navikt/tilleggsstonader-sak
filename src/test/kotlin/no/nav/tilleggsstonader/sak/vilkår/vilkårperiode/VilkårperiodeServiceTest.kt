package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.RegisterAktivitetClientConfig.Companion.resetMock
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetClient
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate.now
import java.util.UUID

class VilkårperiodeServiceTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var stønadsperiodeService: StønadsperiodeService

    @Autowired
    lateinit var registerAktivitetClient: RegisterAktivitetClient

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        resetMock(registerAktivitetClient)
    }

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

            @Test
            fun `kan ikke slette periode hvis periode begynner før revurderFra`() {
                val behandling = testoppsettService.oppdater(revurdering.copy(revurderFra = now()))

                val aktivitet =
                    aktivitet(
                        behandlingId = behandling.id,
                        fom = now().minusMonths(1),
                        tom = now().plusMonths(1),
                    )
                val periode = vilkårperiodeRepository.insert(aktivitet)

                assertThatThrownBy {
                    vilkårperiodeService.slettVilkårperiode(
                        periode.id,
                        SlettVikårperiode(revurdering.id, "kommentar"),
                    )
                }.hasMessageContaining("Kan ikke slette periode")
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
                    aktivitet(behandlingId = revurdering.forrigeIverksatteBehandlingId!!),
                )

            vilkårperiodeRepository.insertAll(eksisterendeVilkårperioder)

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeIverksatteBehandlingId!!, revurdering.id)

            val res = vilkårperiodeRepository.findByBehandlingId(revurdering.id)
            assertThat(res).hasSize(2)

            // TODO "nullstiller" felter fordi usingRecursiveComparison ikke virker som forventet
            val nullstilteRes =
                res.map { oppdatert ->
                    val forrige = eksisterendeVilkårperioder.single { it.id == oppdatert.forrigeVilkårperiodeId!! }
                    oppdatert.copy(
                        id = forrige.id,
                        sporbar = forrige.sporbar,
                        behandlingId = forrige.behandlingId,
                        forrigeVilkårperiodeId = forrige.forrigeVilkårperiodeId,
                        status = forrige.status,
                    )
                }
            assertThat(nullstilteRes)
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

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeIverksatteBehandlingId!!, revurdering.id)

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
                        behandlingId = revurdering.forrigeIverksatteBehandlingId!!,
                        resultat = ResultatVilkårperiode.SLETTET,
                        slettetKommentar = "slettet",
                    ),
                    aktivitet(behandlingId = revurdering.forrigeIverksatteBehandlingId!!),
                    aktivitet(
                        behandlingId = revurdering.forrigeIverksatteBehandlingId!!,
                        resultat = ResultatVilkårperiode.SLETTET,
                        slettetKommentar = "slettet",
                    ),
                )

            vilkårperiodeRepository.insertAll(eksisterendeVilkårperioder)

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeIverksatteBehandlingId!!, revurdering.id)

            val res = vilkårperiodeRepository.findByBehandlingId(revurdering.id)
            assertThat(res).hasSize(2)
            res.map { assertThat(it.resultat).isNotEqualTo(ResultatVilkårperiode.SLETTET) }
        }
    }
}
