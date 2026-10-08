package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FaktaFlyttingMapper.tilDomain
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlytteSelvDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttebyråDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttingUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FlyttebyråTilbudDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.LagreVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.SlettVilkårFlyttingRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.VilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class FlyttingVilkårControllerTest : IntegrationTest() {
    private val fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO)
    private val behandling = behandling(fagsak = fagsak, steg = StegType.VILKÅR)

    @Autowired
    private lateinit var vilkårRepository: VilkårRepository

    private val nyttVilkårFlytteSelv =
        LagreVilkårFlyttingDto(
            fom = 1 januar 2026,
            tom = 15 januar 2026,
            svar = mapOf(RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelseDto(SvarId.FLYTTER_SELV, "Kjører selv")),
            fakta =
                FaktaFlytteSelvDto(
                    avstandEnVei = 100,
                    henger = 1500,
                    bompenger = 300,
                    ferge = 400,
                    parkering = 100,
                    adresse = "Flytteveien 1",
                ),
        )

    private val nyttVilkårFlyttebyrå =
        LagreVilkårFlyttingDto(
            fom = 1 januar 2026,
            tom = 15 januar 2026,
            svar = mapOf(RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelseDto(SvarId.FLYTTEBYRÅ, "Bruker flyttebyrå")),
            fakta =
                FaktaFlyttebyråDto(
                    tilbud1 = FlyttebyråTilbudDto("Byrå A", 10000),
                    tilbud2 = FlyttebyråTilbudDto("Byrå B", 12000),
                    adresse = "Flytteveien 1",
                    erBetalingDokumentert = true,
                ),
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(
            behandling,
            stønadstype = Stønadstype.FLYTTING_TSO,
            identer =
                setOf(PersonIdent(FnrGenerator.generer())),
        )
        opprettOgTilordneOppgaveForBehandling(behandling.id)
    }

    @Test
    fun `skal kunne lagre, endre og slette vilkår for flytting selv`() {
        val opprettet = kall.vilkårFlytting.opprettVilkår(behandling.id, nyttVilkårFlytteSelv)

        assertThat(opprettet.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(opprettet.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(nyttVilkårFlytteSelv, opprettet)
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).containsExactly(opprettet)

        val oppdatering =
            nyttVilkårFlytteSelv.copy(
                tom = 20 januar 2026,
                fakta =
                    FaktaFlytteSelvDto(
                        avstandEnVei = 200,
                        henger = 2000,
                        bompenger = 500,
                        ferge = 600,
                        parkering = 200,
                        adresse = "Flytteveien 2",
                    ),
                svar = mapOf(RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelseDto(SvarId.FLYTTER_SELV, "Ny begrunnelse")),
            )
        val oppdatert = kall.vilkårFlytting.oppdaterVilkår(oppdatering, opprettet.id, behandling.id)

        assertThat(oppdatert.id).isEqualTo(opprettet.id)
        assertThat(oppdatert.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(oppdatert.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatering, oppdatert)
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).containsExactly(oppdatert)

        val slettet = kall.vilkårFlytting.slettVilkår(behandling.id, oppdatert.id, SlettVilkårFlyttingRequestDto())

        assertThat(slettet.slettetPermanent).isTrue()
        assertThat(slettet.vilkår).isEqualTo(oppdatert)
        assertThat(vilkårRepository.findById(oppdatert.id)).isEmpty()
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).isEmpty()
    }

    @Test
    fun `skal kunne lagre, endre og slette vilkår for flyttebyrå`() {
        val opprettet = kall.vilkårFlytting.opprettVilkår(behandling.id, nyttVilkårFlyttebyrå)

        assertThat(opprettet.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(opprettet.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(nyttVilkårFlyttebyrå, opprettet)
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).containsExactly(opprettet)

        val oppdatering =
            nyttVilkårFlyttebyrå.copy(
                fakta =
                    FaktaFlyttebyråDto(
                        tilbud1 = FlyttebyråTilbudDto("Byrå C", 8000),
                        tilbud2 = FlyttebyråTilbudDto("Byrå D", 9000),
                        adresse = "Flytteveien 2",
                        erBetalingDokumentert = true,
                    ),
            )
        val oppdatert = kall.vilkårFlytting.oppdaterVilkår(oppdatering, opprettet.id, behandling.id)

        assertThat(oppdatert.id).isEqualTo(opprettet.id)
        assertThat(oppdatert.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(oppdatert.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatering, oppdatert)
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).containsExactly(oppdatert)

        val slettet = kall.vilkårFlytting.slettVilkår(behandling.id, oppdatert.id, SlettVilkårFlyttingRequestDto())

        assertThat(slettet.slettetPermanent).isTrue()
        assertThat(slettet.vilkår).isEqualTo(oppdatert)
        assertThat(vilkårRepository.findById(oppdatert.id)).isEmpty()
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).isEmpty()
    }

    @Test
    fun `skal kunne lagre og hente vilkår med ubestemte fakta`() {
        val nyttVilkår =
            nyttVilkårFlytteSelv.copy(
                svar = emptyMap(),
                fakta = FaktaFlyttingUbestemtDto(adresse = "Flytteveien 1"),
            )

        val opprettet = kall.vilkårFlytting.opprettVilkår(behandling.id, nyttVilkår)

        assertThat(opprettet.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertLagretVilkår(nyttVilkår, opprettet)
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).containsExactly(opprettet)
    }

    @Test
    fun `skal kunne lagre ufullstendige tilbud fra flyttebyrå`() {
        val nyttVilkår =
            nyttVilkårFlyttebyrå.copy(
                fakta =
                    FaktaFlyttebyråDto(
                        tilbud1 = FlyttebyråTilbudDto("Byrå A", 10000),
                        tilbud2 = FlyttebyråTilbudDto("Byrå B"),
                        adresse = "Flytteveien 1",
                        erBetalingDokumentert = false,
                    ),
            )

        val opprettet = kall.vilkårFlytting.opprettVilkår(behandling.id, nyttVilkår)

        assertThat(opprettet.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertLagretVilkår(nyttVilkår, opprettet)
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).containsExactly(opprettet)
    }

    @Test
    fun `skal slettemarkere vilkår fra forrige behandling og bevare kommentaren`() {
        val opprettet = kall.vilkårFlytting.opprettVilkår(behandling.id, nyttVilkårFlytteSelv)
        val lagret = vilkårRepository.findById(opprettet.id).orElseThrow()
        vilkårRepository.update(lagret.copy(status = VilkårStatus.UENDRET))

        val slettet =
            kall.vilkårFlytting.slettVilkår(
                behandling.id,
                opprettet.id,
                SlettVilkårFlyttingRequestDto(kommentar = "Flyttingen er ikke aktuell"),
            )

        assertThat(slettet.slettetPermanent).isFalse()
        assertThat(slettet.vilkår.id).isEqualTo(opprettet.id)
        assertThat(slettet.vilkår.status).isEqualTo(VilkårStatus.SLETTET)
        assertThat(slettet.vilkår.resultat).isEqualTo(Vilkårsresultat.SLETTET)
        assertThat(slettet.vilkår.slettetKommentar).isEqualTo("Flyttingen er ikke aktuell")
        assertLagretVilkår(nyttVilkårFlytteSelv, slettet.vilkår)
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).containsExactly(slettet.vilkår)
        assertThat(vilkårRepository.findById(opprettet.id).orElseThrow().slettetKommentar)
            .isEqualTo("Flyttingen er ikke aktuell")
    }

    @Test
    fun `skal markere uendret vilkår som endret ved oppdatering`() {
        val opprettet = kall.vilkårFlytting.opprettVilkår(behandling.id, nyttVilkårFlytteSelv)
        val lagret = vilkårRepository.findById(opprettet.id).orElseThrow()
        vilkårRepository.update(lagret.copy(status = VilkårStatus.UENDRET))
        val oppdatering = nyttVilkårFlytteSelv.copy(tom = 20 januar 2026)

        val oppdatert = kall.vilkårFlytting.oppdaterVilkår(oppdatering, opprettet.id, behandling.id)

        assertThat(oppdatert.id).isEqualTo(opprettet.id)
        assertThat(oppdatert.status).isEqualTo(VilkårStatus.ENDRET)
        assertLagretVilkår(oppdatering, oppdatert)
        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).containsExactly(oppdatert)
    }

    @Test
    fun `skal returnere brukerfeil for overlappende flyttevilkår`() {
        val opprettet = kall.vilkårFlytting.opprettVilkår(behandling.id, nyttVilkårFlytteSelv)
        val overlappende = nyttVilkårFlytteSelv.copy(fom = 15 januar 2026, tom = 20 januar 2026)

        kall.vilkårFlytting.apiRespons
            .opprettVilkår(behandling.id, overlappende)
            .expectProblemDetail(HttpStatus.BAD_REQUEST, "Flyttevilkår kan ikke ha overlappende perioder")

        assertThat(kall.vilkårFlytting.hentVilkår(behandling.id)).containsExactly(opprettet)
    }

    @Test
    fun `skal hente alle regler som tilhører flytting`() {
        val resultat = kall.vilkårFlytting.regler()

        FileUtil.assertFileJsonIsEqual("vilkår/regelstruktur/FLYTTING.json", resultat)
    }

    private fun assertLagretVilkår(
        request: LagreVilkårFlyttingDto,
        resultat: VilkårFlyttingDto,
        behandlingId: BehandlingId = behandling.id,
    ) {
        assertThat(resultat.fom).isEqualTo(request.fom)
        assertThat(resultat.tom).isEqualTo(request.tom)
        assertThat(resultat.fakta).isEqualTo(request.fakta)
        assertThat(resultat.delvilkårsett).hasSize(1)
        val vurderinger = resultat.delvilkårsett.single().vurderinger
        assertThat(vurderinger.map { it.regelId }).containsExactly(RegelId.HVORDAN_SKAL_BRUKER_FLYTTE)
        val svar = request.svar[RegelId.HVORDAN_SKAL_BRUKER_FLYTTE]
        assertThat(vurderinger.single().svar).isEqualTo(svar?.svar)
        assertThat(vurderinger.single().begrunnelse).isEqualTo(svar?.begrunnelse)

        val lagret = vilkårRepository.findById(resultat.id).orElseThrow()
        assertThat(lagret.behandlingId).isEqualTo(behandlingId)
        assertThat(lagret.type).isEqualTo(VilkårType.FLYTTING)
        assertThat(lagret.fom).isEqualTo(request.fom)
        assertThat(lagret.tom).isEqualTo(request.tom)
        assertThat(lagret.fakta).isEqualTo(request.fakta.tilDomain())
        assertThat(lagret.status).isEqualTo(resultat.status)
        assertThat(lagret.resultat).isEqualTo(resultat.resultat)
    }
}
