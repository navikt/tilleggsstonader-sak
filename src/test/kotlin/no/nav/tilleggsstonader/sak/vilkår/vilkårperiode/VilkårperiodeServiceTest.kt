package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.opprettVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.OppdaterVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VilkårperiodeServiceTest : IntegrationTest() {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Nested
    inner class OpprettVilkårperiode {

        @Test
        fun `skal opprette vilkår periode som manuell`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val opprettVilkårperiode = opprettVilkårperiode(medlemskap = SvarJaNei.NEI)

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(behandling.id, opprettVilkårperiode)
            assertThat(vilkårperiode.kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
            assertThat(vilkårperiode.fom).isEqualTo(opprettVilkårperiode.fom)
            assertThat(vilkårperiode.tom).isEqualTo(opprettVilkårperiode.tom)
            assertThat(vilkårperiode.begrunnelse).isEqualTo(opprettVilkårperiode.begrunnelse)

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat((vilkårperiode.delvilkår as DelvilkårMålgruppe).medlemskap.svar).isEqualTo(SvarJaNei.NEI)
            assertThat((vilkårperiode.delvilkår as DelvilkårMålgruppe).medlemskap.resultat)
                .isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `skal kaste feil hvis målgruppe er ugyldig for stønadstype`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val opprettVilkårperiode = opprettVilkårperiode(type = MålgruppeType.DAGPENGER, medlemskap = SvarJaNei.NEI)

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(behandling.id, opprettVilkårperiode)
            }.hasMessageContaining("målgruppe=DAGPENGER er ikke gyldig for ${Stønadstype.BARNETILSYN}")
        }
    }

    @Nested
    inner class OppdaterVilkårPeriode {

        @Test
        fun `skal oppdatere alle felter hvis periode er lagt til manuelt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode =
                vilkårperiodeService.opprettVilkårperiode(behandling.id, opprettVilkårperiode(medlemskap = null))

            val nyttDato = LocalDate.of(2020, 1, 1)
            val oppdatering = vilkårperiode.tilOddatering().copy(
                fom = nyttDato,
                tom = nyttDato,
                begrunnelse = "Oppdatert begrunnelse",
                delvilkår = DelvilkårMålgruppeDto(medlemskap = SvarJaNei.JA),
            )
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
            assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(oppdatertPeriode.fom).isEqualTo(nyttDato)
            assertThat(oppdatertPeriode.tom).isEqualTo(nyttDato)
            assertThat(oppdatertPeriode.begrunnelse).isEqualTo("Oppdatert begrunnelse")
            assertThat((oppdatertPeriode.delvilkår as DelvilkårMålgruppe).medlemskap.svar).isEqualTo(SvarJaNei.JA)
            assertThat((oppdatertPeriode.delvilkår as DelvilkårMålgruppe).medlemskap.resultat)
                .isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @Test
        fun `skal oppdatere felter for periode som er lagt til av system`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode =
                vilkårperiodeService.opprettVilkårperiode(
                    behandling.id,
                    opprettVilkårperiode(medlemskap = SvarJaNei.JA),
                )

            val oppdatering = vilkårperiode.tilOddatering().copy(
                begrunnelse = "Oppdatert begrunnelse",
                delvilkår = DelvilkårMålgruppeDto(medlemskap = SvarJaNei.NEI),
            )
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)

            assertThat(oppdatertPeriode.fom).isEqualTo(vilkårperiode.fom)
            assertThat(oppdatertPeriode.tom).isEqualTo(vilkårperiode.tom)
            assertThat(oppdatertPeriode.begrunnelse).isEqualTo("Oppdatert begrunnelse")
            assertThat((oppdatertPeriode.delvilkår as DelvilkårMålgruppe).medlemskap.svar).isEqualTo(SvarJaNei.NEI)
            assertThat((oppdatertPeriode.delvilkår as DelvilkårMålgruppe).medlemskap.resultat)
                .isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `skal validere at man ikke prøver å endre FOM eller TOM når perioden er opprettet av system`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.SYSTEM,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    periode.id,
                    periode.tilOddatering().copy(fom = LocalDate.now().minusYears(32)),
                )
            }.hasMessageContaining("Kan ikke oppdatere fom")

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    periode.id,
                    periode.tilOddatering().copy(tom = LocalDate.now().plusYears(32)),
                )
            }.hasMessageContaining("Kan ikke oppdatere tom")
        }

        @Test
        fun `skal ikke kunne oppdatere kommentar hvis behandlingen ikke er under behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))

            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.MANUELL,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    periode.id,
                    periode.tilOddatering(),
                )
            }.hasMessageContaining("Kan ikke oppdatere vilkårperiode når behandling er låst for videre redigering")
        }

        private fun Vilkårperiode.tilOddatering(): OppdaterVilkårperiode {
            val delvilkårDto = when (this.delvilkår) {
                is DelvilkårMålgruppe ->
                    DelvilkårMålgruppeDto((this.delvilkår as DelvilkårMålgruppe).medlemskap.svar)

                is DelvilkårAktivitet -> (this.delvilkår as DelvilkårAktivitet).let {
                    DelvilkårAktivitetDto(
                        it.lønnet.svar,
                        it.mottarSykepenger.svar,
                    )
                }
            }
            return OppdaterVilkårperiode(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                delvilkår = delvilkårDto,
                begrunnelse = begrunnelse,
            )
        }
    }

    @Nested
    inner class SlettVilkårperiode {

        @Test
        fun `skal ikke kunne slette kommentar hvis behandlingen ikke er under behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))
            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.MANUELL,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårperiodeService.slettVilkårperiode(periode.id, SlettVikårperiode(behandling.id, "kommentar"))
            }.hasMessageContaining("Kan ikke slette vilkårperiode når behandling er låst for videre redigering")
        }

        @Test
        fun `skal ikke kunne slette kommentar hvis man mangler kommentar`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling())
            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.MANUELL,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårperiodeService.slettVilkårperiode(periode.id, SlettVikårperiode(behandling.id, "    "))
            }.hasMessageContaining("Mangler kommentar")
        }

        @Test
        fun `skal ikke kunne slette kommentar hvis kilden er system`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling())
            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.SYSTEM,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårperiodeService.slettVilkårperiode(periode.id, SlettVikårperiode(behandling.id, "kommentar"))
            }.hasMessageContaining("Kan ikke slette når kilde=")
        }

        @Test
        fun `skal kunne slette kommentar som er manuellt opprettet`() {
            val saksbehandler = "saksbehandlerX"
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling())
            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.MANUELL,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            Assertions.assertThat(periode.sporbar.endret.endretAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)

            BrukerContextUtil.testWithBrukerContext(saksbehandler) {
                vilkårperiodeService.slettVilkårperiode(periode.id, SlettVikårperiode(behandling.id, "kommentar"))
            }

            val oppdatertPeriode = vilkårperiodeRepository.findByIdOrThrow(periode.id)
            Assertions.assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
            Assertions.assertThat(oppdatertPeriode.sporbar.endret.endretAv).isEqualTo(saksbehandler)
        }
    }
}
