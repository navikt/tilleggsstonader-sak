package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.lønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.medlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.mottarSykepenger
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.opprettVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.opprettVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Stønadsperiodestatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
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

    @Autowired
    lateinit var stønadsperiodeService: StønadsperiodeService

    @Nested
    inner class OpprettVilkårperiode {

        @Test
        fun `skal opprette periode for målgruppe manuelt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val opprettVilkårperiode = opprettVilkårperiodeMålgruppe(
                medlemskap = VurderingDto(SvarJaNei.NEI, "begrunnelse medlemskap"),
                begrunnelse = "begrunnelse målgruppe",
                behandlingId = behandling.id,
            )

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(opprettVilkårperiode)
            assertThat(vilkårperiode.type).isEqualTo(opprettVilkårperiode.type)
            assertThat(vilkårperiode.kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
            assertThat(vilkårperiode.fom).isEqualTo(opprettVilkårperiode.fom)
            assertThat(vilkårperiode.tom).isEqualTo(opprettVilkårperiode.tom)
            assertThat(vilkårperiode.begrunnelse).isEqualTo("begrunnelse målgruppe")

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat(vilkårperiode.medlemskap.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(vilkårperiode.medlemskap.begrunnelse).isEqualTo("begrunnelse medlemskap")
            assertThat(vilkårperiode.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `skal opprette periode for aktivitet manuelt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val opprettVilkårperiode = opprettVilkårperiodeAktivitet(
                lønnet = VurderingDto(SvarJaNei.NEI, "begrunnelse lønnet"),
                mottarSykepenger = VurderingDto(SvarJaNei.NEI, "begrunnelse sykepenger"),
                begrunnelse = "begrunnelse aktivitet",
                behandlingId = behandling.id,
            )

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(opprettVilkårperiode)
            assertThat(vilkårperiode.type).isEqualTo(opprettVilkårperiode.type)
            assertThat(vilkårperiode.kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
            assertThat(vilkårperiode.fom).isEqualTo(opprettVilkårperiode.fom)
            assertThat(vilkårperiode.tom).isEqualTo(opprettVilkårperiode.tom)
            assertThat(vilkårperiode.begrunnelse).isEqualTo("begrunnelse aktivitet")

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            assertThat(vilkårperiode.lønnet.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(vilkårperiode.lønnet.begrunnelse).isEqualTo("begrunnelse lønnet")
            assertThat(vilkårperiode.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)

            assertThat(vilkårperiode.mottarSykepenger.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(vilkårperiode.mottarSykepenger.begrunnelse).isEqualTo("begrunnelse sykepenger")
            assertThat(vilkårperiode.mottarSykepenger.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @Test
        fun `skal kaste feil hvis målgruppe er ugyldig for stønadstype`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val opprettVilkårperiode = opprettVilkårperiodeMålgruppe(
                type = MålgruppeType.DAGPENGER,
                medlemskap = VurderingDto(SvarJaNei.NEI),
                behandlingId = behandling.id,
            )

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(opprettVilkårperiode)
            }.hasMessageContaining("målgruppe=DAGPENGER er ikke gyldig for ${Stønadstype.BARNETILSYN}")
        }
    }

    @Nested
    inner class OppdaterVilkårPeriode {

        @Test
        fun `skal oppdatere alle felter hvis periode er lagt til manuelt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode =
                vilkårperiodeService.opprettVilkårperiode(
                    opprettVilkårperiodeMålgruppe(
                        medlemskap = null,
                        behandlingId = behandling.id,
                    ),
                )

            val nyttDato = LocalDate.of(2020, 1, 1)
            val oppdatering = vilkårperiode.tilOppdatering().copy(
                fom = nyttDato,
                tom = nyttDato,
                begrunnelse = "Oppdatert begrunnelse",
                delvilkår = DelvilkårMålgruppeDto(
                    medlemskap = VurderingDto(
                        SvarJaNei.JA,
                        begrunnelse = "ny begrunnelse",
                    ),
                ),
            )
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
            assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(oppdatertPeriode.fom).isEqualTo(nyttDato)
            assertThat(oppdatertPeriode.tom).isEqualTo(nyttDato)
            assertThat(oppdatertPeriode.begrunnelse).isEqualTo("Oppdatert begrunnelse")
            assertThat(oppdatertPeriode.medlemskap.svar).isEqualTo(SvarJaNei.JA)
            assertThat(oppdatertPeriode.medlemskap.begrunnelse).isEqualTo("ny begrunnelse")
            assertThat(oppdatertPeriode.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @Test
        fun `skal oppdatere felter for periode som er lagt til av system`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode =
                vilkårperiodeService.opprettVilkårperiode(
                    opprettVilkårperiodeMålgruppe(
                        medlemskap = VurderingDto(
                            SvarJaNei.JA,
                        ),
                        behandlingId = behandling.id,
                    ),
                )

            val oppdatering = vilkårperiode.tilOppdatering().copy(
                begrunnelse = "Oppdatert begrunnelse",
                delvilkår = DelvilkårMålgruppeDto(medlemskap = VurderingDto(SvarJaNei.NEI)),
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
                    periode.tilOppdatering().copy(fom = LocalDate.now().minusYears(32)),
                )
            }.hasMessageContaining("Kan ikke oppdatere fom")

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    periode.id,
                    periode.tilOppdatering().copy(tom = LocalDate.now().plusYears(32)),
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
                    periode.tilOppdatering(),
                )
            }.hasMessageContaining("Kan ikke oppdatere vilkårperiode når behandling er låst for videre redigering")
        }

        private fun Vilkårperiode.tilOppdatering(): LagreVilkårperiode {
            val delvilkårDto = when (this.delvilkår) {
                is DelvilkårMålgruppe ->
                    DelvilkårMålgruppeDto(VurderingDto((this.delvilkår as DelvilkårMålgruppe).medlemskap.svar))

                is DelvilkårAktivitet -> (this.delvilkår as DelvilkårAktivitet).let {
                    DelvilkårAktivitetDto(
                        lønnet = VurderingDto(it.lønnet.svar),
                        mottarSykepenger = VurderingDto(it.mottarSykepenger.svar),
                    )
                }
            }
            return LagreVilkårperiode(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                delvilkår = delvilkårDto,
                begrunnelse = begrunnelse,
                type = type,
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

            assertThat(periode.sporbar.endret.endretAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)

            BrukerContextUtil.testWithBrukerContext(saksbehandler) {
                vilkårperiodeService.slettVilkårperiode(periode.id, SlettVikårperiode(behandling.id, "kommentar"))
            }

            val oppdatertPeriode = vilkårperiodeRepository.findByIdOrThrow(periode.id)
            assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
            assertThat(oppdatertPeriode.sporbar.endret.endretAv).isEqualTo(saksbehandler)
        }
    }

    @Nested
    inner class InngangsvilkårSteg {
        @Test
        fun `skal validere stønadsperioder og gjennomføre steg inngangsvilkår ved opprettelse av vilkårperiode - ingen stønadsperioder`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThat(behandling.steg).isEqualTo(StegType.INNGANGSVILKÅR)

            val periode = vilkårperiodeService.opprettVilkårperiode(
                LagreVilkårperiode(
                    type = MålgruppeType.AAP,
                    fom = LocalDate.now(),
                    tom = LocalDate.now(),
                    delvilkår = VilkårperiodeTestUtil.delvilkårMålgruppeDto(),
                    behandlingId = behandling.id,
                ),
            )

            val response = vilkårperiodeService.oppdaterBehandlingstegOgLagResponse(periode)

            assertThat(testoppsettService.hentBehandling(behandling.id).steg).isEqualTo(StegType.VILKÅR)
            assertThat(response.stønadsperiodeStatus).isEqualTo(Stønadsperiodestatus.OK)
            assertThat(response.stønadsperiodeFeil).isNull()
        }

        @Test
        fun `skal validere stønadsperioder ved oppdatering av vilkårperioder og resette steg inngangsvilkår når status ikke ok`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            assertThat(behandling.steg).isEqualTo(StegType.INNGANGSVILKÅR)

            val fom1 = LocalDate.of(2024, 1, 1)
            val tom1 = LocalDate.of(2024, 2, 1)

            val fom2 = LocalDate.of(2024, 2, 1)
            val tom2 = LocalDate.of(2024, 3, 1)

            val opprettetMålgruppe = vilkårperiodeService.opprettVilkårperiode(
                LagreVilkårperiode(
                    type = MålgruppeType.AAP,
                    fom = fom1,
                    tom = tom1,
                    delvilkår = VilkårperiodeTestUtil.delvilkårMålgruppeDto(),
                    behandlingId = behandling.id,
                ),
            )
            vilkårperiodeService.oppdaterBehandlingstegOgLagResponse(opprettetMålgruppe)
            val opprettetTiltakPeriode = vilkårperiodeService.opprettVilkårperiode(
                LagreVilkårperiode(
                    type = AktivitetType.TILTAK,
                    fom = fom1,
                    tom = tom1,
                    delvilkår = VilkårperiodeTestUtil.delvilkårAktivitetDto(),
                    behandlingId = behandling.id,
                    aktivitetsdager = 5,
                ),
            )
            vilkårperiodeService.oppdaterBehandlingstegOgLagResponse(opprettetTiltakPeriode)
            assertThat(testoppsettService.hentBehandling(behandling.id).steg).isEqualTo(StegType.VILKÅR)

            stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf(nyStønadsperiode(fom1, tom1)))
            assertThat(testoppsettService.hentBehandling(behandling.id).steg).isEqualTo(StegType.VILKÅR)

            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(
                id = opprettetTiltakPeriode.id,
                vilkårperiode = LagreVilkårperiode(
                    type = AktivitetType.TILTAK,
                    fom = fom2,
                    tom = tom2,
                    delvilkår = VilkårperiodeTestUtil.delvilkårAktivitetDto(),
                    behandlingId = behandling.id,
                    aktivitetsdager = 5,
                ),
            )
            vilkårperiodeService.oppdaterBehandlingstegOgLagResponse(oppdatertPeriode)

            assertThat(testoppsettService.hentBehandling(behandling.id).steg).isEqualTo(StegType.INNGANGSVILKÅR)
            assertThat(vilkårperiodeService.oppdaterBehandlingstegOgLagResponse(oppdatertPeriode).stønadsperiodeStatus).isEqualTo(
                Stønadsperiodestatus.FEIL,
            )
        }

        private fun nyStønadsperiode(fom: LocalDate = LocalDate.now(), tom: LocalDate = LocalDate.now()) =
            StønadsperiodeDto(
                id = null,
                fom = fom,
                tom = tom,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )
    }
}
