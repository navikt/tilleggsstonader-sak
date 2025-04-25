package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.Fødsel
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataDomain
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerAtAldersvilkårErGyldig
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerAtKunTomErEndret
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerSlettPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.medAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.tilOppdatering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilFaktaOgSvarDto
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class VilkårperiodeRevurderFraValideringTest {
    val revurderFra = LocalDate.of(2024, 1, 1)
    val behandlingUtenRevurderFra = saksbehandling(revurderFra = null, type = BehandlingType.REVURDERING)
    val behandlingMedRevurderFra = saksbehandling(revurderFra = revurderFra, type = BehandlingType.REVURDERING)

    @Nested
    inner class NyPeriode {
        @Test
        fun `kan ikke gjøre endringer på periode dato dersom revurder-fra ikke er satt`() {
            assertThatThrownBy {
                validerNyPeriodeRevurdering(
                    behandling = behandlingUtenRevurderFra,
                    fom = revurderFra.minusDays(1),
                )
            }.hasMessageContaining("Revurder fra-dato må settes før du kan gjøre endringer på inngangvilkårene")
        }

        @Test
        fun `kan legge inn periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerNyPeriodeRevurdering(
                    behandling = behandlingMedRevurderFra,
                    fom = revurderFra,
                )
                validerNyPeriodeRevurdering(
                    behandling = behandlingMedRevurderFra,
                    fom = revurderFra.plusDays(1),
                )
            }
        }

        @Test
        fun `kan ikke legge inn ny periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                validerNyPeriodeRevurdering(behandling = behandlingMedRevurderFra, fom = revurderFra.minusDays(1))
            }.hasMessageContaining("Kan ikke opprette periode")
        }
    }

    @Nested
    inner class SlettPeriode {
        @Test
        fun `kan ikke slette periode med valgfritt dato dersom revurder-fra ikke er satt`() {
            assertThatThrownBy {
                validerSlettPeriodeRevurdering(
                    behandlingUtenRevurderFra,
                    målgruppe(fom = revurderFra.plusDays(1)),
                )
            }.hasMessageContaining("Revurder fra-dato må settes før du kan gjøre endringer på inngangvilkårene")
        }

        @Test
        fun `kan slette periode som begynner samme eller etter revurderingsdato`() {
            assertDoesNotThrow {
                validerSlettPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    målgruppe(fom = revurderFra),
                )
                validerSlettPeriodeRevurdering(
                    behandlingMedRevurderFra,
                    målgruppe(fom = revurderFra.plusDays(1)),
                )
            }
        }

        @Test
        fun `kan ikke slette periode som begynner før revurder-fra`() {
            assertThatThrownBy {
                val vilkårperiode = målgruppe(fom = revurderFra.minusDays(1))
                validerSlettPeriodeRevurdering(behandlingMedRevurderFra, vilkårperiode)
            }.hasMessageContaining("Kan ikke slette periode")
        }
    }

    @Nested
    inner class OppdateringAvPeriodeHvorKunTomKanEndres {
        @Test
        fun `kan oppdatere periodens tom-dato til og med dagen før revurder fra`() {
            val eksisterendeVilkårperiode =
                aktivitet(
                    fom = revurderFra.minusMonths(1),
                    tom = revurderFra.plusMonths(1),
                )
            assertDoesNotThrow {
                listOf(revurderFra.minusDays(1), revurderFra, revurderFra.plusDays(1)).forEach { nyttTom ->
                    validerEndringKunTomKanOppdates(
                        eksisterendeVilkårperiode,
                        eksisterendeVilkårperiode.copy(tom = nyttTom),
                    )
                }
            }
        }

        @Test
        fun `kan ikke oppdatere tom til 2 dager før revurder fra, då fjerner man data som gjelder dagen før revurderingsdatoet`() {
            val eksisterendeVilkårperiode =
                aktivitet(
                    fom = revurderFra.minusMonths(1),
                    tom = revurderFra.plusMonths(1),
                )
            assertThatThrownBy {
                validerEndringKunTomKanOppdates(
                    eksisterendeVilkårperiode,
                    eksisterendeVilkårperiode.copy(tom = revurderFra.minusDays(2)),
                )
            }.hasMessageContaining("Kan ikke sette tom tidligere enn dagen før revurder-fra")
        }

        @Test
        fun `kan ikke endre fom til å begynne før revurderFra`() {
            val eksisterendeVilkårperiode =
                aktivitet(
                    fom = revurderFra,
                    tom = revurderFra.plusMonths(1),
                )
            assertThatThrownBy {
                validerEndringKunTomKanOppdates(
                    eksisterendeVilkårperiode,
                    eksisterendeVilkårperiode.copy(fom = revurderFra.minusDays(2)),
                )
            }.message().contains("Kan ikke endre fom til", "fordi revurder fra", "er før")
        }

        @Test
        fun `kan kunne oppdatere begrunnelse på periode hvis det begynner før revurder-fra`() {
            val eksisterendeVilkårperiode =
                aktivitet(
                    fom = revurderFra.minusMonths(1),
                    tom = revurderFra.plusMonths(1),
                )

            assertDoesNotThrow {
                validerEndringKunTomKanOppdates(
                    eksisterendeVilkårperiode,
                    eksisterendeVilkårperiode.copy(begrunnelse = "en begrunnelse"),
                )
            }
        }

        @Test
        fun `skal ikke kunne oppdatere fakta`() {
            val eksisterendeVilkårperiode =
                aktivitet(
                    fom = revurderFra.minusMonths(1),
                    tom = revurderFra.plusMonths(1),
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetTilsynBarn(
                            aktivitetsdager = 3,
                            lønnet = vurderingLønnet(SvarJaNei.NEI),
                        ),
                    resultat = ResultatVilkårperiode.OPPFYLT,
                )
            assertThatThrownBy {
                validerEndringKunTomKanOppdates(
                    eksisterendeVilkårperiode,
                    eksisterendeVilkårperiode.medAktivitetsdager(5),
                )
            }.hasMessageContaining("Kan ikke endre vurderinger eller fakta på perioden")
        }

        @Test
        fun `skal ikke kunne oppdatere vurderinger`() {
            val eksisterendeVilkårperiode =
                målgruppe(
                    fom = revurderFra.minusMonths(1),
                    tom = revurderFra.plusMonths(1),
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppe(
                            dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(SvarJaNei.JA),
                        ),
                    begrunnelse = "en begrunnelse",
                )
            val oppdatert =
                eksisterendeVilkårperiode.let {
                    @Suppress("UNCHECKED_CAST")
                    it as GeneriskVilkårperiode<AAPTilsynBarn>
                    val dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(SvarJaNei.NEI)
                    val oppdaterteVurderinger =
                        it.faktaOgVurdering.vurderinger.copy(dekketAvAnnetRegelverk = dekketAvAnnetRegelverk)
                    it.copy(faktaOgVurdering = it.faktaOgVurdering.copy(vurderinger = oppdaterteVurderinger))
                }
            assertThatThrownBy {
                validerEndringKunTomKanOppdates(
                    eksisterendeVilkårperiode,
                    oppdatert,
                )
            }.hasMessageContaining("Kan ikke endre vurderinger eller fakta på perioden")
        }

        @Test
        fun `skal kunne oppdatere aldersvilkår uten at det kastes feil då det er en automatisk vurdering`() {
            val vurderingAldersVilkår =
                VurderingAldersVilkår(
                    svar = SvarJaNei.GAMMEL_MANGLER_DATA,
                    vurderingFaktaEtterlevelse = null,
                    resultat = ResultatDelvilkårperiode.IKKE_VURDERT,
                )
            val eksisterendeVilkårperiode =
                målgruppe(
                    fom = revurderFra.minusMonths(1),
                    tom = revurderFra.plusMonths(1),
                    faktaOgVurdering = faktaOgVurderingMålgruppe(aldersvilkår = vurderingAldersVilkår),
                )
            val oppdatert =
                eksisterendeVilkårperiode.let {
                    @Suppress("UNCHECKED_CAST")
                    it as GeneriskVilkårperiode<AAPTilsynBarn>
                    val oppdaterteVurderinger =
                        it.faktaOgVurdering.vurderinger.copy(aldersvilkår = vurderingAldersVilkår(SvarJaNei.JA))
                    it.copy(faktaOgVurdering = it.faktaOgVurdering.copy(vurderinger = oppdaterteVurderinger))
                }
            assertDoesNotThrow {
                validerEndringKunTomKanOppdates(
                    eksisterendeVilkårperiode,
                    oppdatert,
                )
            }
        }

        private fun validerEndringKunTomKanOppdates(
            eksisterendeVilkårperiode: Vilkårperiode,
            oppdatertVilkårperiode: Vilkårperiode,
        ) {
            validerAtKunTomErEndret(
                eksisterendeVilkårperiode,
                LagreVilkårperiode(
                    behandlingId = oppdatertVilkårperiode.behandlingId,
                    type = oppdatertVilkårperiode.type,
                    fom = oppdatertVilkårperiode.fom,
                    tom = oppdatertVilkårperiode.tom,
                    faktaOgSvar = oppdatertVilkårperiode.faktaOgVurdering.tilFaktaOgSvarDto(),
                ),
                behandlingMedRevurderFra.revurderFra!!,
            )
        }
    }

    @Nested
    inner class ValiderGyldigAldersvilkår {
        @Test
        fun `Skal kaste feil dersom vilkårperiode utvides utover aldersbegrensning`() {
            val fødselsdato = osloDateNow().minusYears(67)
            val grunnlagdata =
                grunnlagsdataDomain(
                    grunnlag =
                        lagGrunnlagsdata(
                            fødsel =
                                Fødsel(
                                    fødselsdato = fødselsdato,
                                    fødselsår = fødselsdato.year,
                                ),
                        ),
                )
            val eksisterendeVilkårperiode =
                målgruppe(fom = osloDateNow().minusYears(2), tom = osloDateNow().minusYears(1))

            assertThatThrownBy {
                validerAtAldersvilkårErGyldig(
                    eksisterendePeriode = eksisterendeVilkårperiode,
                    oppdatertPeriode = eksisterendeVilkårperiode.tilOppdatering().copy(tom = osloDateNow().plusYears(2)),
                    grunnlagsdata = grunnlagdata,
                )
            }.hasMessageContaining("Brukeren fyller 67 år i løpet av vilkårsperioden")
        }

        @Test
        fun `Skal ikke kaste feil dersom hele vilkårperioden er innenfor 18 og 67 år`() {
            val fødselsdato = osloDateNow().minusYears(30)
            val grunnlagdata =
                grunnlagsdataDomain(
                    grunnlag =
                        lagGrunnlagsdata(
                            fødsel =
                                Fødsel(
                                    fødselsdato = fødselsdato,
                                    fødselsår = fødselsdato.year,
                                ),
                        ),
                )
            val eksisterendeVilkårperiode =
                målgruppe(fom = osloDateNow().minusYears(2), tom = osloDateNow().minusYears(1))

            assertDoesNotThrow {
                validerAtAldersvilkårErGyldig(
                    eksisterendePeriode = eksisterendeVilkårperiode,
                    oppdatertPeriode = eksisterendeVilkårperiode.tilOppdatering().copy(tom = osloDateNow().plusYears(2)),
                    grunnlagsdata = grunnlagdata,
                )
            }
        }

        @Test
        fun `Skal ikke kaste feil dersom hele vilkårperioden er før bruker fyller 18 år`() {
            val fødselsdato = osloDateNow().minusYears(10)
            val grunnlagdata =
                grunnlagsdataDomain(
                    grunnlag =
                        lagGrunnlagsdata(
                            fødsel =
                                Fødsel(
                                    fødselsdato = fødselsdato,
                                    fødselsår = fødselsdato.year,
                                ),
                        ),
                )
            val eksisterendeVilkårperiode =
                målgruppe(fom = osloDateNow(), tom = osloDateNow().plusYears(1))

            assertDoesNotThrow {
                validerAtAldersvilkårErGyldig(
                    eksisterendePeriode = eksisterendeVilkårperiode,
                    oppdatertPeriode = eksisterendeVilkårperiode.tilOppdatering().copy(tom = osloDateNow().plusYears(2)),
                    grunnlagsdata = grunnlagdata,
                )
            }
        }
    }
}
