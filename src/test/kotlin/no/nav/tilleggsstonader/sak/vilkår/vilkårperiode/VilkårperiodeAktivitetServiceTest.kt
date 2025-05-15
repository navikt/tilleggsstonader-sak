package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.RegisterAktivitetClientConfig.Companion.resetMock
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetClient
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.lønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfVurderinger
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.HarRettTilUtstyrsstipendVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.HarUtgifterVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBoutgifterDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetLæremidlerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.HentetInformasjon
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagDomain
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagTestUtil.periodeGrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.grunnlagYtelseOk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime

class VilkårperiodeAktivitetServiceTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    @Autowired
    lateinit var aktivitetService: VilkårperiodeService

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
            val opprettetAktivitet =
                aktivitetService.opprettVilkårperiode(
                    dummyVilkårperiodeAktivitet(
                        behandlingId = behandling.id,
                    ),
                )
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
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = true)
            val hentetInformasjon = HentetInformasjon(fom = now(), tom = now(), tidspunktHentet = LocalDateTime.now())
            val grunnlag =
                VilkårperioderGrunnlag(
                    aktivitet = GrunnlagAktivitet(aktiviteter = listOf(periodeGrunnlagAktivitet("123"))),
                    ytelse = grunnlagYtelseOk(perioder = emptyList()),
                    hentetInformasjon = hentetInformasjon,
                )
            vilkårperioderGrunnlagRepository.insert(VilkårperioderGrunnlagDomain(behandling.id, grunnlag))
            aktivitetService.opprettVilkårperiode(
                dummyVilkårperiodeAktivitet(
                    behandlingId = behandling.id,
                    kildeId = "123",
                ),
            )
            val lagretAktivitet = vilkårperiodeService.hentVilkårperioder(behandling.id).aktiviteter.single()
            assertThat(lagretAktivitet.kildeId).isEqualTo("123")
        }

        @Test
        fun `skal kaste feil hvis kildeId ikke finnes blant aktivitetIder i grunnlag`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val hentetInformasjon =
                HentetInformasjon(fom = now(), tom = now(), tidspunktHentet = LocalDateTime.now())
            val grunnlag =
                VilkårperioderGrunnlag(
                    aktivitet = GrunnlagAktivitet(emptyList()),
                    ytelse = grunnlagYtelseOk(emptyList()),
                    hentetInformasjon = hentetInformasjon,
                )
            vilkårperioderGrunnlagRepository.insert(VilkårperioderGrunnlagDomain(behandling.id, grunnlag))

            val opprettAktivitet = dummyVilkårperiodeAktivitet(behandlingId = behandling.id, kildeId = "finnesIkke")
            assertThatThrownBy {
                aktivitetService.opprettVilkårperiode(opprettAktivitet)
            }.hasMessageContaining("Aktivitet med id=finnesIkke finnes ikke i grunnlag")
        }

        @Test
        fun `skal kaste feil ved opprettelse av lønnet tiltak uten begrunnelse`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val faktaOgSvarTilsynBarnDto =
                FaktaOgSvarAktivitetBarnetilsynDto(
                    svarLønnet = SvarJaNei.JA,
                    aktivitetsdager = 5,
                )

            assertThatThrownBy {
                aktivitetService.opprettVilkårperiode(
                    dummyVilkårperiodeAktivitet(
                        type = AktivitetType.TILTAK,
                        behandlingId = behandling.id,
                        begrunnelse = null,
                        faktaOgSvar = faktaOgSvarTilsynBarnDto,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for ikke oppfylt vurdering av lønnet arbeid")
        }

        @Test
        fun `skal kaste feil ved tom og null begrunnelse på ingen aktivitet`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val faktaOgSvarTilsynBarnDto =
                FaktaOgSvarAktivitetBarnetilsynDto(
                    svarLønnet = SvarJaNei.JA,
                    aktivitetsdager = null,
                )
            listOf("", null).forEach {
                assertThatThrownBy {
                    aktivitetService.opprettVilkårperiode(
                        dummyVilkårperiodeAktivitet(
                            type = AktivitetType.INGEN_AKTIVITET,
                            begrunnelse = it,
                            behandlingId = behandling.id,
                            faktaOgSvar = faktaOgSvarTilsynBarnDto,
                        ),
                    )
                }.hasMessageContaining("Mangler begrunnelse for ingen relevant aktivitet")
            }
        }

        @Test
        fun `skal kaste feil dersom aktivitetsdager registreres på aktivitet med type ingen aktivitet`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                aktivitetService.opprettVilkårperiode(
                    dummyVilkårperiodeAktivitet(
                        behandlingId = behandling.id,
                        type = AktivitetType.INGEN_AKTIVITET,
                    ),
                )
            }.hasMessageContaining("Kan ikke registrere aktivitetsdager på ingen aktivitet")
        }

        @Test
        fun `kan ikke opprette aktivitet hvis periode begynner før revurderFra`() {
            val behandling =
                testoppsettService.oppdater(
                    testoppsettService.lagBehandlingOgRevurdering().copy(revurderFra = now()),
                )

            assertThatThrownBy {
                aktivitetService.opprettVilkårperiode(
                    dummyVilkårperiodeAktivitet(
                        behandlingId = behandling.id,
                        fom = now().plusDays(1),
                    ),
                )
            }.hasMessageContaining("Til-og-med før fra-og-med")
        }

        @Nested
        inner class Læremidler {
            @Test
            fun `skal ikke kunne lage aktivitet med type reell arbeidssøker for søknad om læremidler`() {
                val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = Stønadstype.LÆREMIDLER))
                val behandling = testoppsettService.lagre(behandling(fagsak))

                assertThatThrownBy {
                    vilkårperiodeService.opprettVilkårperiode(
                        LagreVilkårperiode(
                            type = AktivitetType.REELL_ARBEIDSSØKER,
                            behandlingId = behandling.id,
                            fom = now(),
                            tom = now(),
                            faktaOgSvar =
                                FaktaOgSvarAktivitetLæremidlerDto(
                                    prosent = 50,
                                    svarHarUtgifter = SvarJaNei.JA,
                                ),
                        ),
                    )
                }.hasMessageContaining("Reell arbeidssøker er ikke en gyldig aktivitet for læremidler")
            }

            @Test
            fun `Skal kunne sende inn og hente ut felter spesifikke for læremidler`() {
                val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = Stønadstype.LÆREMIDLER))
                val behandling = testoppsettService.lagre(behandling(fagsak))

                val persistertAktivitet =
                    vilkårperiodeService.opprettVilkårperiode(
                        LagreVilkårperiode(
                            type = AktivitetType.TILTAK,
                            behandlingId = behandling.id,
                            fom = now(),
                            tom = now(),
                            faktaOgSvar =
                                FaktaOgSvarAktivitetLæremidlerDto(
                                    prosent = 50,
                                    studienivå = Studienivå.HØYERE_UTDANNING,
                                    svarHarUtgifter = SvarJaNei.JA,
                                    svarHarRettTilUtstyrsstipend = SvarJaNei.NEI,
                                ),
                        ),
                    )

                val studienivå =
                    persistertAktivitet.faktaOgVurdering.fakta
                        .takeIfFakta<FaktaAktivitetLæremidler>()
                        ?.studienivå
                val harUtgifter =
                    persistertAktivitet.faktaOgVurdering.vurderinger
                        .takeIfVurderinger<HarUtgifterVurdering>()
                        ?.harUtgifter
                val harRettTilUtstyrsstipend =
                    persistertAktivitet.faktaOgVurdering.vurderinger
                        .takeIfVurderinger<HarRettTilUtstyrsstipendVurdering>()
                        ?.harRettTilUtstyrsstipend

                assertThat(studienivå).isEqualTo(Studienivå.HØYERE_UTDANNING)
                assertThat(harUtgifter?.svar).isEqualTo(SvarJaNei.JA)
                assertThat(harRettTilUtstyrsstipend?.svar).isEqualTo(SvarJaNei.NEI)
            }
        }
    }

    @Nested
    inner class Boutgifter {
        @Test
        fun `skal ikke kunne lage aktivitet med type reell arbeidssøker for søknad om boutgifter`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = Stønadstype.BOUTGIFTER))
            val behandling = testoppsettService.lagre(behandling(fagsak))

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    LagreVilkårperiode(
                        type = AktivitetType.REELL_ARBEIDSSØKER,
                        behandlingId = behandling.id,
                        fom = now(),
                        tom = now(),
                        faktaOgSvar =
                            FaktaOgSvarAktivitetBoutgifterDto(
                                svarLønnet = SvarJaNei.NEI,
                            ),
                    ),
                )
            }.hasMessageContaining("Reell arbeidssøker er ikke en gyldig aktivitet for boutgifter")
        }

        @Test
        fun `Skal kunne sende inn og hente ut felter for boutgfiter`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = Stønadstype.BOUTGIFTER))
            val behandling = testoppsettService.lagre(behandling(fagsak))

            val persistertAktivitet =
                vilkårperiodeService.opprettVilkårperiode(
                    LagreVilkårperiode(
                        type = AktivitetType.TILTAK,
                        behandlingId = behandling.id,
                        fom = now(),
                        tom = now(),
                        faktaOgSvar =
                            FaktaOgSvarAktivitetBoutgifterDto(
                                svarLønnet = SvarJaNei.NEI,
                            ),
                    ),
                )

            val harUtgifter =
                persistertAktivitet.faktaOgVurdering.vurderinger
                    .takeIfVurderinger<VurderingTiltakBoutgifter>()
                    ?.lønnet

            assertThat(harUtgifter).isEqualTo(VurderingLønnet(SvarJaNei.NEI))
        }
    }

    @Nested
    inner class OppdaterAktivitetFørstegangsbehandling {
        @Test
        fun `skal oppdatere alle felter på aktivitet`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val eksisterendeAktivitet =
                aktivitetService.opprettVilkårperiode(
                    dummyVilkårperiodeAktivitet(
                        behandlingId = behandling.id,
                    ),
                )

            val nyDato = LocalDate.parse("2020-01-01")
            val oppdatering =
                eksisterendeAktivitet.tilOppdatering(
                    nyFom = nyDato,
                    nyTom = nyDato,
                    nyBegrunnelse = "Oppdatert begrunnelse",
                    svarLønnet = SvarJaNei.NEI,
                )

            aktivitetService.oppdaterVilkårperiode(eksisterendeAktivitet.id, oppdatering)
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
            val aktivitet =
                aktivitetService.opprettVilkårperiode(
                    dummyVilkårperiodeAktivitet(behandlingId = behandling.id),
                )
            val oppdatering = aktivitet.tilOppdatering()
            val oppdatertAktivitet = aktivitetService.oppdaterVilkårperiode(aktivitet.id, oppdatering)
            assertThat(aktivitet.status).isEqualTo(Vilkårstatus.NY)
            assertThat(oppdatertAktivitet.status).isEqualTo(Vilkårstatus.NY)
        }

        @Test
        fun `skal feile dersom manglende begrunnelse når lønnet endres til ja`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val tiltak =
                aktivitetService.opprettVilkårperiode(
                    dummyVilkårperiodeAktivitet(
                        behandlingId = behandling.id,
                    ),
                )

            val oppdatering =
                tiltak.tilOppdatering(
                    nyBegrunnelse = "",
                    svarLønnet = SvarJaNei.JA,
                )

            assertThatThrownBy {
                aktivitetService.oppdaterVilkårperiode(tiltak.id, oppdatering)
            }.hasMessageContaining("Mangler begrunnelse for ikke oppfylt vurdering av lønnet arbeid")
        }

        @Test
        fun `skal ikke kunne oppdatere kommentar hvis behandlingen ikke er under behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    behandling(status = BehandlingStatus.FERDIGSTILT),
                )
            val aktivitet =
                vilkårperiodeRepository.insert(
                    aktivitet(behandlingId = behandling.id),
                )
            assertThatThrownBy {
                aktivitetService.oppdaterVilkårperiode(
                    id = aktivitet.id,
                    vilkårperiode = aktivitet.tilOppdatering(),
                )
            }.hasMessageContaining("Kan ikke gjøre endringer på denne behandlingen fordi den er ferdigstilt.")
        }
    }

    @Nested
    inner class OppdaterAktivitetRevurdering {
        @Test
        fun `endring av ativiteter opprettet fra tidligere behandling skal få status ENDRET`() {
            val revurdering = testoppsettService.lagBehandlingOgRevurdering()
            val opprinneligAktivitet =
                vilkårperiodeRepository.insert(
                    aktivitet(
                        behandlingId = revurdering.forrigeIverksatteBehandlingId!!,
                    ),
                )
            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeIverksatteBehandlingId!!, revurdering.id)
            val vilkårperiode = vilkårperiodeRepository.findByBehandlingId(revurdering.id).single()
            val oppdatertPeriode =
                aktivitetService.oppdaterVilkårperiode(
                    id = vilkårperiode.id,
                    vilkårperiode = vilkårperiode.tilOppdatering(),
                )
            assertThat(opprinneligAktivitet.status).isEqualTo(Vilkårstatus.NY)
            assertThat(vilkårperiode.status).isEqualTo(Vilkårstatus.UENDRET)
            assertThat(oppdatertPeriode.status).isEqualTo(Vilkårstatus.ENDRET)
        }

        @Test
        fun `skal ikke kunne oppdatere aktivitetstypen`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val aktivitet =
                vilkårperiodeRepository.insert(
                    aktivitet(behandlingId = behandling.id),
                )
            assertThat(aktivitet.type).isEqualTo(AktivitetType.TILTAK)
            assertThatThrownBy {
                aktivitetService.oppdaterVilkårperiode(
                    id = aktivitet.id,
                    vilkårperiode = aktivitet.tilOppdatering().copy(type = AktivitetType.REELL_ARBEIDSSØKER),
                )
            }.hasMessageContaining("Kan ikke endre type på en eksisterende periode.")
        }

        @Test
        fun `skal ikke kunne oppdatere behandlings-IDen`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val annenBehandling =
                testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")))).let {
                    testoppsettService.lagre(behandling(it))
                }
            val aktivitet =
                vilkårperiodeRepository.insert(
                    aktivitet(behandlingId = behandling.id),
                )
            val endretBehandlingId = aktivitet.tilOppdatering().copy(behandlingId = annenBehandling.id)
            assertThatThrownBy {
                aktivitetService.oppdaterVilkårperiode(
                    id = aktivitet.id,
                    vilkårperiode = endretBehandlingId,
                )
            }.hasMessageContaining("BehandlingId er ikke lik")
        }

        @Test
        fun `kan ikke oppdatere fakta hvis periode begynner før revurderFra`() {
            val behandling =
                testoppsettService.oppdater(
                    testoppsettService.lagBehandlingOgRevurdering().copy(revurderFra = now()),
                )
            val aktivitet =
                vilkårperiodeRepository.insert(
                    aktivitet(
                        behandlingId = behandling.id,
                        fom = now().minusMonths(1),
                        tom = now().plusMonths(1),
                    ),
                )
            assertThatThrownBy {
                aktivitetService.oppdaterVilkårperiode(
                    id = aktivitet.id,
                    vilkårperiode = aktivitet.tilOppdatering(aktivitetsdager = 3),
                )
            }.hasMessageContaining("Kan ikke endre vurderinger eller fakta på perioden")
        }
    }

    private fun Vilkårperiode.tilOppdatering(
        nyFom: LocalDate? = null,
        nyTom: LocalDate? = null,
        aktivitetsdager: Int? = null,
        svarLønnet: SvarJaNei? = null,
        nyBegrunnelse: String? = null,
    ): LagreVilkårperiode {
        val faktaOgSvarTilsynBarnDto =
            FaktaOgSvarAktivitetBarnetilsynDto(
                svarLønnet =
                    svarLønnet ?: faktaOgVurdering.vurderinger
                        .takeIfVurderinger<LønnetVurdering>()
                        ?.lønnet
                        ?.svar,
                aktivitetsdager =
                    aktivitetsdager
                        ?: faktaOgVurdering.fakta.takeIfFakta<FaktaAktivitetsdager>()?.aktivitetsdager,
            )
        return dummyVilkårperiodeAktivitet(
            behandlingId = behandlingId,
            type = type as AktivitetType,
            fom = nyFom ?: fom,
            tom = nyTom ?: tom,
            faktaOgSvar = faktaOgSvarTilsynBarnDto,
            begrunnelse = nyBegrunnelse ?: begrunnelse,
        )
    }
}
