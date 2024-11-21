package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.RegisterAktivitetClientConfig.Companion.resetMock
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetClient
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.lønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfVurderinger
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.HentetInformasjon
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagDomain
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagTestUtil.periodeGrunnlagAktivitet
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime

class VilkårperiodeServiceTest : IntegrationTest() {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    @Autowired
    lateinit var aktivitetService: AktivitetService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository

    @Autowired
    lateinit var registerAktivitetClient: RegisterAktivitetClient

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        resetMock(registerAktivitetClient)
    }

    @Nested
    inner class OpprettAktivitet {
        @Test
        fun `skal kunne opprette aktivitet fra scratch`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val opprettetAktivitet = aktivitetService.opprettAktivitet(ulønnetTiltak.copy(behandlingId = behandling.id))

            with(opprettetAktivitet) {
                assertThat(type).isEqualTo(opprettetAktivitet.type)
                assertThat(fom).isEqualTo(opprettetAktivitet.fom)
                assertThat(tom).isEqualTo(opprettetAktivitet.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(begrunnelse).isNull()

                assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
                assertThat(lønnet.svar).isEqualTo(SvarJaNei.NEI)
                assertThat(lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            }
        }


        @Test
        fun `skal lagre kildeId på aktivitet`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val hentetInformasjon = HentetInformasjon(fom = now(), tom = now(), tidspunktHentet = LocalDateTime.now())
            val grunnlag = VilkårperioderGrunnlag(
                aktivitet = GrunnlagAktivitet(aktiviteter = listOf(periodeGrunnlagAktivitet("123"))),
                ytelse = GrunnlagYtelse(emptyList()),
                hentetInformasjon = hentetInformasjon
            )

            vilkårperioderGrunnlagRepository.insert(VilkårperioderGrunnlagDomain(behandling.id, grunnlag))

            aktivitetService.opprettAktivitet(ulønnetTiltak.copy(behandlingId = behandling.id, kildeId = "123"))

            val lagretAktivitet = vilkårperiodeService.hentVilkårperioder(behandling.id).aktiviteter.single()

            assertThat(lagretAktivitet.kildeId).isEqualTo("123")
        }

        @Test
        fun `skal kaste feil hvis kildeId ikke finnes blant aktivitetIder i grunnlag`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val hentetInformasjon =
                HentetInformasjon(fom = now(), tom = now(), tidspunktHentet = LocalDateTime.now())
            val grunnlag = VilkårperioderGrunnlag(
                aktivitet = GrunnlagAktivitet(emptyList()),
                ytelse = GrunnlagYtelse(emptyList()),
                hentetInformasjon = hentetInformasjon
            )

            vilkårperioderGrunnlagRepository.insert(VilkårperioderGrunnlagDomain(behandling.id, grunnlag))

            val opprettAktivitet = ulønnetTiltak.copy(kildeId = "finnesIkke", behandlingId = behandling.id)

            assertThatThrownBy {
                aktivitetService.opprettAktivitet(opprettAktivitet)
            }.hasMessageContaining("Aktivitet med id=finnesIkke finnes ikke i grunnlag")
        }

        @Test
        fun `skal kaste feil ved opprettelse av lønnet tiltak uten begrunnelse`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                aktivitetService.opprettAktivitet(
                    ulønnetTiltak.copy(
                        svarLønnet = SvarJaNei.JA,
                        behandlingId = behandling.id,
                        begrunnelse = null
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for ikke oppfylt vurdering av lønnet arbeid")
        }

        @Test
        fun `skal kaste feil ved tom og null begrunnelse på ingen aktivitet`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            listOf("", null).forEach {
                assertThatThrownBy {
                    aktivitetService.opprettAktivitet(
                        ulønnetTiltak.copy(
                            begrunnelse = it,
                            type = AktivitetType.INGEN_AKTIVITET,
                            behandlingId = behandling.id,
                            aktivitetsdager = null,
                        ),
                    )
                }.hasMessageContaining("Mangler begrunnelse for ingen aktivitet")
            }
        }

        @Test
        fun `skal kaste feil dersom aktivitetsdager registreres på aktivitet med type ingen aktivitet`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                aktivitetService.opprettAktivitet(
                    ingenAktivitet.copy(
                        aktivitetsdager = 5,
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Kan ikke registrere aktivitetsdager på ingen aktivitet")
        }

    }

    @Test
    fun `kan ikke opprette aktivitet hvis periode begynner før revurderFra`() {
        val behandling = testoppsettService.oppdater(
            testoppsettService.lagBehandlingOgRevurdering().copy(revurderFra = now()),
        )

        assertThatThrownBy {
            aktivitetService.opprettAktivitet(
                ingenAktivitet.copy(
                    behandlingId = behandling.id,
                    fom = ingenAktivitet.tom.plusDays(1),
                    aktivitetsdager = null
                ),
            )
        }.hasMessageContaining("Til-og-med før fra-og-med")
    }

    @Nested
    inner class OppdaterAktivitet {

        @Test
        fun `skal oppdatere alle felter hvis aktivitet`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val eksisterendeAktivitet = aktivitetService.opprettAktivitet(
                tiltak.copy(behandlingId = behandling.id)
            )

            val nyDato = LocalDate.parse("2020-01-01")
            val oppdatering = eksisterendeAktivitet.tilOppdatering().copy(
                fom = nyDato,
                tom = nyDato,
                begrunnelse = "Oppdatert begrunnelse",
                svarLønnet = SvarJaNei.NEI,
            )

            aktivitetService.oppdaterAktivitet(eksisterendeAktivitet.id, oppdatering)

            val oppdatertAktivitet = vilkårperiodeService.hentVilkårperioder(behandling.id).aktiviteter.single()

            with(oppdatertAktivitet) {
                assertThat(lønnet.svar).isEqualTo(SvarJaNei.NEI)
                assertThat(fom).isEqualTo(nyDato)
                assertThat(tom).isEqualTo(nyDato)
                assertThat(begrunnelse).isEqualTo("Oppdatert begrunnelse")
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            }
        }


        @Test
        fun `endring av aktiviteter opprettet i denne behandlingen skal beholde status NY`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val aktivitet = aktivitetService.opprettAktivitet(
                ulønnetTiltak.copy(behandlingId = behandling.id),
            )
            val oppdatering = aktivitet.tilOppdatering()
            val oppdatertAktivitet = aktivitetService.oppdaterAktivitet(aktivitet.id, oppdatering)

            assertThat(aktivitet.status).isEqualTo(Vilkårstatus.NY)
            assertThat(oppdatertAktivitet.status).isEqualTo(Vilkårstatus.NY)
        }

        @Test
        fun `endring av ativiteter opprettet fra tidligere behandling skal få status ENDRET`() {
            val revurdering = testoppsettService.lagBehandlingOgRevurdering()

            val opprinneligAktivitet = vilkårperiodeRepository.insert(
                aktivitet(
                    behandlingId = revurdering.forrigeBehandlingId!!,
                ),
            )

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeBehandlingId!!, revurdering.id)

            val vilkårperiode = vilkårperiodeRepository.findByBehandlingId(revurdering.id).single()
            val oppdatertPeriode = aktivitetService.oppdaterAktivitet(
                id = vilkårperiode.id,
                aktivitet = vilkårperiode.tilOppdatering(),
            )

            assertThat(opprinneligAktivitet.status).isEqualTo(Vilkårstatus.NY)
            assertThat(vilkårperiode.status).isEqualTo(Vilkårstatus.UENDRET)
            assertThat(oppdatertPeriode.status).isEqualTo(Vilkårstatus.ENDRET)
        }

        @Test
        fun `skal feile dersom manglende begrunnelse når lønnet endres til ja`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val tiltak = aktivitetService.opprettAktivitet(
                ulønnetTiltak.copy(behandlingId = behandling.id),
            )

            val oppdatering = tiltak.tilOppdatering().copy(
                begrunnelse = "",
                svarLønnet = SvarJaNei.JA,
            )

            assertThatThrownBy {
                aktivitetService.oppdaterAktivitet(tiltak.id, oppdatering)
            }.hasMessageContaining("Mangler begrunnelse for ikke oppfylt vurdering av lønnet arbeid")
        }

        @Test
        fun `skal ikke kunne oppdatere kommentar hvis behandlingen ikke er under behandling`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(status = BehandlingStatus.FERDIGSTILT)
            )

            val periode = vilkårperiodeRepository.insert(
                aktivitet(behandlingId = behandling.id)
            )

            assertThatThrownBy {
                aktivitetService.oppdaterAktivitet(
                    id = periode.id,
                    aktivitet = periode.tilOppdatering(),
                )
            }.hasMessageContaining("Kan ikke opprette eller endre aktivitet når behandling er låst for videre redigering")
        }


        @Test
        fun `kan ikke oppdatere periode hvis periode begynner før revurderFra`() {
            val behandling = testoppsettService.oppdater(
                testoppsettService.lagBehandlingOgRevurdering().copy(revurderFra = now()),
            )

            val aktivitet = vilkårperiodeRepository.insert(
                aktivitet(
                    behandlingId = behandling.id,
                    fom = now().minusMonths(1),
                    tom = now().plusMonths(1),
                )
            )

            assertThatThrownBy {
                aktivitetService.oppdaterAktivitet(
                    id = aktivitet.id,
                    aktivitet = aktivitet.tilOppdatering().copy(aktivitetsdager = 3),
                )
            }.hasMessageContaining("Ugyldig endring på periode")
        }

        private fun Vilkårperiode.tilOppdatering(): LagreAktivitet {
            return LagreAktivitet(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                begrunnelse = begrunnelse,
                type = type as AktivitetType,
                aktivitetsdager = faktaOgVurdering.fakta.takeIfFakta<FaktaAktivitetsdager>()?.aktivitetsdager,
                svarLønnet = faktaOgVurdering.vurderinger.takeIfVurderinger<LønnetVurdering>()?.lønnet?.svar,
            )
        }
    }
}