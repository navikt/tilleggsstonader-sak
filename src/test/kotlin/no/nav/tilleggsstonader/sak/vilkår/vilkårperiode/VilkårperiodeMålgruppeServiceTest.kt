package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.medlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.tilOppdatering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MedlemskapVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMottarSykepengerForFulltidsstilling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDate.now

class VilkårperiodeMålgruppeServiceTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Nested
    inner class OpprettVilkårperiode {
        @Test
        fun `skal kunne opprette ny målgruppe`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val målgruppeSomSkalOpprettes =
                dummyVilkårperiodeMålgruppe(
                    medlemskap = SvarJaNei.NEI,
                    begrunnelse = "begrunnelse målgruppe",
                    behandlingId = behandling.id,
                )
            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(målgruppeSomSkalOpprettes)

            assertThat(vilkårperiode.type).isEqualTo(målgruppeSomSkalOpprettes.type)
            assertThat(vilkårperiode.kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
            assertThat(vilkårperiode.fom).isEqualTo(målgruppeSomSkalOpprettes.fom)
            assertThat(vilkårperiode.tom).isEqualTo(målgruppeSomSkalOpprettes.tom)
            assertThat(vilkårperiode.begrunnelse).isEqualTo("begrunnelse målgruppe")
            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat(vilkårperiode.faktaOgVurdering.vurderinger).isInstanceOf(MedlemskapVurdering::class.java)
            assertThat(vilkårperiode.medlemskap.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(vilkårperiode.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
            assertThat(vilkårperiode.faktaOgVurdering.vurderinger).isNotInstanceOf(DekketAvAnnetRegelverkVurdering::class.java)
            assertThat(vilkårperiode.gitVersjon).isEqualTo(Applikasjonsversjon.versjon)
        }

        @Test
        fun `skal kaste feil hvis målgruppe er ugyldig for stønadstype`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val opprettVilkårperiode =
                dummyVilkårperiodeMålgruppe(
                    type = MålgruppeType.DAGPENGER,
                    medlemskap = SvarJaNei.NEI,
                    behandlingId = behandling.id,
                )

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(opprettVilkårperiode)
            }.hasMessageContaining("målgruppe=DAGPENGER er ikke gyldig for ${Stønadstype.BARNETILSYN}")
        }

        @Test
        fun `skal kaste feil ved opprettelse av vilkårperiode med vurdering av medlemskap uten begrunnelse`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    dummyVilkårperiodeMålgruppe(
                        begrunnelse = "",
                        medlemskap = SvarJaNei.NEI,
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for vurdering av medlemskap")
        }

        @Test
        fun `skal kaste feil ved opprettelse av vilkårperiode hvis ikke oppfylt delvilkår mangler begrunnelse - dekkes annet regelverk`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    dummyVilkårperiodeMålgruppe(
                        type = MålgruppeType.AAP,
                        begrunnelse = "",
                        dekkesAvAnnetRegelverk = SvarJaNei.JA,
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for vurderingen av 'utgifter dekket av annet regelverk'")
        }

        @Test
        fun `skal feile dersom manglende begrunnelse når målgruppe er nedsatt arbeidsevne`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    dummyVilkårperiodeMålgruppe(
                        begrunnelse = "",
                        type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for nedsatt arbeidsevne")
        }

        @Test
        fun `skal kaste feil ved tom og null begrunnelse på ingen målgruppe`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    dummyVilkårperiodeMålgruppe(
                        begrunnelse = "",
                        type = MålgruppeType.INGEN_MÅLGRUPPE,
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for ingen målgruppe")

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    dummyVilkårperiodeMålgruppe(
                        begrunnelse = null,
                        type = MålgruppeType.INGEN_MÅLGRUPPE,
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for ingen målgruppe")
        }
    }

    @Nested
    inner class Oppdater {
        @Test
        fun `skal oppdatere alle felter på målgruppe`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val eksisterendeMålgruppe =
                vilkårperiodeService.opprettVilkårperiode(
                    dummyVilkårperiodeMålgruppe(
                        medlemskap = null,
                        behandlingId = behandling.id,
                    ),
                )

            val nyDato = LocalDate.of(2020, 1, 1)
            val oppdatering =
                eksisterendeMålgruppe.tilOppdatering().copy(
                    fom = nyDato,
                    tom = nyDato,
                    begrunnelse = "Oppdatert begrunnelse",
                    faktaOgSvar =
                        FaktaOgSvarMålgruppeDto(
                            svarMedlemskap = SvarJaNei.JA,
                            svarUtgifterDekketAvAnnetRegelverk = null,
                        ),
                )
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(eksisterendeMålgruppe.id, oppdatering)

            assertThat(eksisterendeMålgruppe.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
            assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(oppdatertPeriode.fom).isEqualTo(nyDato)
            assertThat(oppdatertPeriode.tom).isEqualTo(nyDato)
            assertThat(oppdatertPeriode.begrunnelse).isEqualTo("Oppdatert begrunnelse")
            assertThat(oppdatertPeriode.medlemskap.svar).isEqualTo(SvarJaNei.JA)
            assertThat(oppdatertPeriode.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @Test
        fun `skal oppdatere gitVersjon`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val eksisterendeMålgruppeId =
                vilkårperiodeService
                    .opprettVilkårperiode(
                        dummyVilkårperiodeMålgruppe(
                            medlemskap = null,
                            behandlingId = behandling.id,
                        ),
                    ).id
            jdbcTemplate.update(
                "update vilkar_periode SET git_versjon=:versjon",
                mapOf("versjon" to "versjon"),
            )

            val målgruppe = vilkårperiodeRepository.findByIdOrThrow(eksisterendeMålgruppeId)
            assertThat(målgruppe.gitVersjon).isEqualTo("versjon")
            val faktaOgSvar =
                FaktaOgSvarMålgruppeDto(
                    svarMedlemskap = SvarJaNei.JA,
                    svarUtgifterDekketAvAnnetRegelverk = null,
                )
            val oppdatering =
                målgruppe.tilOppdatering().copy(
                    fom = LocalDate.of(2020, 1, 1),
                    tom = LocalDate.of(2020, 1, 1),
                    begrunnelse = "Oppdatert begrunnelse",
                    faktaOgSvar = faktaOgSvar,
                )
            vilkårperiodeService.oppdaterVilkårperiode(eksisterendeMålgruppeId, oppdatering)
            assertThat(vilkårperiodeRepository.findByIdOrThrow(eksisterendeMålgruppeId).gitVersjon)
                .isEqualTo(Applikasjonsversjon.versjon)
        }

        @Test
        fun `skal oppdatere felter for periode som er lagt til av system`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode =
                vilkårperiodeService.opprettVilkårperiode(
                    dummyVilkårperiodeMålgruppe(
                        begrunnelse = "Begrunnelse",
                        medlemskap = SvarJaNei.JA,
                        behandlingId = behandling.id,
                    ),
                )

            val oppdatering =
                vilkårperiode.tilOppdatering().copy(
                    begrunnelse = "Oppdatert begrunnelse",
                    faktaOgSvar =
                        FaktaOgSvarMålgruppeDto(
                            svarMedlemskap = SvarJaNei.NEI,
                            svarUtgifterDekketAvAnnetRegelverk = null,
                        ),
                )
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)

            assertThat(oppdatertPeriode.fom).isEqualTo(vilkårperiode.fom)
            assertThat(oppdatertPeriode.tom).isEqualTo(vilkårperiode.tom)
            assertThat(oppdatertPeriode.begrunnelse).isEqualTo("Oppdatert begrunnelse")
            assertThat(oppdatertPeriode.medlemskap.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(oppdatertPeriode.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `endring av vilkårperioder opprettet i denne behandlingen skal beholde status NY`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode =
                vilkårperiodeService.opprettVilkårperiode(
                    dummyVilkårperiodeMålgruppe(
                        medlemskap = null,
                        behandlingId = behandling.id,
                    ),
                )
            val oppdatering = vilkårperiode.tilOppdatering()
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)
            assertThat(vilkårperiode.status).isEqualTo(Vilkårstatus.NY)
            assertThat(oppdatertPeriode.status).isEqualTo(Vilkårstatus.NY)
        }

        @Test
        fun `skal feile dersom manglende begrunnelse når dekket av annet regelverk endres til ja`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode =
                vilkårperiodeService.opprettVilkårperiode(
                    dummyVilkårperiodeMålgruppe(
                        type = MålgruppeType.UFØRETRYGD,
                        dekkesAvAnnetRegelverk = SvarJaNei.NEI,
                        behandlingId = behandling.id,
                    ),
                )

            val oppdatering =
                vilkårperiode.tilOppdatering().copy(
                    begrunnelse = "",
                    faktaOgSvar =
                        FaktaOgSvarMålgruppeDto(
                            svarMedlemskap = null,
                            svarUtgifterDekketAvAnnetRegelverk = SvarJaNei.JA,
                        ),
                )
            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)
            }.hasMessageContaining("Mangler begrunnelse for vurderingen av 'utgifter dekket av annet regelverk'")
        }

        @Test
        fun `skal ikke kunne oppdatere kommentar hvis behandlingen ikke er under behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))

            val målgruppe = målgruppe(behandlingId = behandling.id)
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    periode.id,
                    periode.tilOppdatering(),
                )
            }.hasMessageContaining("Kan ikke gjøre endringer på denne behandlingen fordi den er ferdigstilt.")
        }
    }

    @Nested
    inner class OppdaterRevurdering {
        @Test
        fun `endring av vilkårperioder opprettet kopiert fra tidligere behandling skal få status ENDRET`() {
            val revurdering = lagRevurderingMedKopiertMålgruppe()

            val opprinneligVilkårperiode = vilkårperiodeRepository.findByBehandlingId(revurdering.forrigeIverksatteBehandlingId!!).single()
            val vilkårperiodeFørOppdatering = vilkårperiodeRepository.findByBehandlingId(revurdering.id).single()
            val oppdatertPeriode =
                vilkårperiodeService.oppdaterVilkårperiode(
                    vilkårperiodeFørOppdatering.id,
                    vilkårperiodeFørOppdatering.tilOppdatering(),
                )

            assertThat(opprinneligVilkårperiode.status).isEqualTo(Vilkårstatus.NY)
            assertThat(vilkårperiodeFørOppdatering.status).isEqualTo(Vilkårstatus.UENDRET)
            assertThat(oppdatertPeriode.status).isEqualTo(Vilkårstatus.ENDRET)
        }

        @Test
        fun `kan ikke fjerne begrunnelse hvis den er obligatorisk pga eksisterende svar`() {
            val originalMålgruppe =
                målgruppe(
                    fom = now().minusMonths(1),
                    tom = now().plusMonths(1),
                    faktaOgVurdering = faktaOgVurderingMålgruppe(dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(SvarJaNei.JA)),
                    begrunnelse = "Begrunnelse",
                )
            val behandling = lagRevurderingMedKopiertMålgruppe(originalMålgruppe)
            val målgruppeFørOppdatering = vilkårperiodeRepository.findByBehandlingId(behandling.id).single()

            val nyTom = now().plusMonths(2)

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    målgruppeFørOppdatering.id,
                    målgruppeFørOppdatering
                        .tilOppdatering()
                        .copy(tom = nyTom, begrunnelse = null),
                )
            }.hasMessageContaining("Mangler begrunnelse")
        }

        @Test
        fun `kan ikke utvide målgruppe dersom den inneholder svar GAMMEL_MANGLER_DATA`() {
            val originalMålgruppe =
                målgruppe(
                    fom = now().minusMonths(1),
                    tom = now().plusMonths(1),
                    begrunnelse = "Begrunnelse",
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppe(
                            type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                            mottarSykepengerForFulltidsstilling =
                                VurderingMottarSykepengerForFulltidsstilling(
                                    svar = SvarJaNei.GAMMEL_MANGLER_DATA,
                                    resultat = ResultatDelvilkårperiode.IKKE_VURDERT,
                                ),
                        ),
                )
            val behandling = lagRevurderingMedKopiertMålgruppe(originalMålgruppe)
            val målgruppeFørOppdatering = vilkårperiodeRepository.findByBehandlingId(behandling.id).single()

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    målgruppeFørOppdatering.id,
                    målgruppeFørOppdatering
                        .tilOppdatering()
                        .copy(tom = målgruppeFørOppdatering.tom.plusMonths(1)),
                )
            }.hasMessageContaining("${SvarJaNei.GAMMEL_MANGLER_DATA.name} er ugyldig for nye eller oppdaterte vurderinger")
        }

        @Test
        fun `kan ikke utvide målgruppe dersom den krysser aldersgrense`() {
            val originalMålgruppe =
                målgruppe(
                    fom = now().minusMonths(1),
                    tom = now().plusMonths(1),
                    begrunnelse = "Begrunnelse",
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppe(
                            type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                            mottarSykepengerForFulltidsstilling =
                                VurderingMottarSykepengerForFulltidsstilling(
                                    svar = SvarJaNei.GAMMEL_MANGLER_DATA,
                                    resultat = ResultatDelvilkårperiode.IKKE_VURDERT,
                                ),
                        ),
                )
            val behandling = lagRevurderingMedKopiertMålgruppe(originalMålgruppe)
            val målgruppeFørOppdatering = vilkårperiodeRepository.findByBehandlingId(behandling.id).single()

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    målgruppeFørOppdatering.id,
                    målgruppeFørOppdatering
                        .tilOppdatering()
                        .copy(tom = målgruppeFørOppdatering.tom.plusMonths(1)),
                )
            }.hasMessageContaining("${SvarJaNei.GAMMEL_MANGLER_DATA.name} er ugyldig for nye eller oppdaterte vurderinger")
        }
    }

    private fun lagRevurderingMedKopiertMålgruppe(
        opprinneligMålgruppe: VilkårperiodeMålgruppe = målgruppe(),
        revurderFra: LocalDate = now(),
    ): Behandling {
        val revurdering = testoppsettService.lagBehandlingOgRevurdering(revurderFra = revurderFra)

        vilkårperiodeRepository.insert(
            opprinneligMålgruppe.copy(behandlingId = revurdering.forrigeIverksatteBehandlingId!!),
        )

        vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeIverksatteBehandlingId!!, revurdering.id)

        return revurdering
    }
}
