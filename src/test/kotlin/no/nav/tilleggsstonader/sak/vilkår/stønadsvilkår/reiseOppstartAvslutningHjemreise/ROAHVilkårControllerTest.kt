package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileJsonIsEqual
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ROAHTestUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårMidlertidigBostedDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

class ROAHVilkårControllerTest : CleanDatabaseIntegrationTest() {
    val ident = "12345678910"

    @Autowired
    private lateinit var vilkårRepository: VilkårRepository

    @Test
    fun `skal lagre og oppdatere paraplyvilkår med datoer, adresser og egen vurdering`() {
        val (behandling, aktivitet) = opprettBehandlingMedAktivitet(ident)

        val paraply = vilkårKall.opprettParaplyvilkår(behandling.id, paraplyvilkår(aktivitet.globalId))

        assertThat(paraply.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(paraply.status).isEqualTo(VilkårStatus.NY)
        assertThat(paraply.fraAdresse).isEqualTo("Adresse 1")
        assertThat(paraply.tilAdresse).isEqualTo("Adresse 2")

        val oppdatert =
            vilkårKall.oppdaterParaplyvilkår(
                lagreVilkår = paraplyvilkår(aktivitet.globalId, fraAdresse = "Adresse 3"),
                vilkårId = paraply.id,
                behandlingId = behandling.id,
            )

        assertThat(oppdatert.id).isEqualTo(paraply.id)
        assertThat(oppdatert.fraAdresse).isEqualTo("Adresse 3")
        assertThat(oppdatert.tilAdresse).isEqualTo("Adresse 2")
    }

    @Test
    fun `skal hente reiser og paraplyvilkår hver for seg`() {
        val (behandling, aktivitet) = opprettBehandlingMedAktivitet(ident)
        val paraply = vilkårKall.opprettParaplyvilkår(behandling.id, paraplyvilkår(aktivitet.globalId))
        val reise = vilkårKall.opprettVilkår(behandling.id, reise(aktivitet.globalId))

        val hentet = vilkårKall.hentVilkår(behandling.id).single()

        assertThat(hentet.reiser.single().id).isEqualTo(reise.id)
        assertThat(hentet.paraplyvilkår.single().id).isEqualTo(paraply.id)
    }

    @Test
    fun `skal ikke kunne overskrive et paraplyvilkår med en reise`() {
        val (behandling, aktivitet) = opprettBehandlingMedAktivitet(ident)
        val paraply = vilkårKall.opprettParaplyvilkår(behandling.id, paraplyvilkår(aktivitet.globalId))

        vilkårKall.apiRespons
            .oppdaterVilkår(reise(aktivitet.globalId), paraply.id, behandling.id)
            .expectProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Kan ikke endre mellom reise og paraplyvilkår")
    }

    @Test
    fun `skal ikke kunne overskrive vilkår av en annen type`() {
        val (behandling, aktivitet) = opprettBehandlingMedAktivitet(ident)
        val dagligReise = vilkårRepository.insert(vilkår(behandlingId = behandling.id, type = VilkårType.DAGLIG_REISE))

        vilkårKall.apiRespons
            .oppdaterVilkår(reise(aktivitet.globalId), dagligReise.id, behandling.id)
            .expectProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Vilkåret er ikke et vilkår for reise oppstart, avslutning og hjemreise",
            )
        assertThat(vilkårRepository.findByIdOrThrow(dagligReise.id).type).isEqualTo(VilkårType.DAGLIG_REISE)
    }

    @Test
    fun `skal hente alle regler som tilhører reise oppstart avslutning og hjemreise`() {
        val regelstruktur = kall.vilkårReiseOppstartAvslutningHjemreise.regler()

        assertFileJsonIsEqual("vilkår/regelstruktur/REISE_OPPSTART_AVSLUTNING_HJEMREISE.json", regelstruktur)
    }

    @Test
    fun `skal kunne lagre, endre og slette vilkår for daglig reise - offentlig transport`() {
        val fom = 1 januar 2026
        val tom = 31 mars 2026

        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                identer = setOf(PersonIdent(ident = ident)),
            )

        opprettOgTilordneOppgaveForBehandling(behandling.id)

        gjennomførBehandlingsløp(ident = ident, behandlingId = behandling.id, tilSteg = StegType.VILKÅR) {
            aktivitet {
                opprett {
                    aktivitetTiltakTsoReiseTilOppstart(fom, tom)
                }
            }
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
        }

        val aktivitet =
            kall.vilkårperiode
                .hentForBehandling(behandling.id)
                .vilkårperioder.aktiviteter
                .single()

        val nyttVilkår =
            LagreVilkårReiseOppstartAvslutningHjemreiseDto(
                fom = fom,
                tom = null,
                reiseId = dummyReiseId,
                aktivitetId = aktivitet.globalId,
                typeReiseformål = TypeReiseformål.OPPSTART,
                svar = ROAHTestUtil.oppfylteSvarOppstartAvslutningOffentligTransportDto(),
                fakta = offentligTransport(BigDecimal.valueOf(100)),
            )

        val opprettetVilkår = kall.vilkårReiseOppstartAvslutningHjemreise.opprettVilkår(behandling.id, nyttVilkår)

        assertThat(opprettetVilkår.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(opprettetVilkår.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(lagreVilkårRequest = nyttVilkår, resultat = opprettetVilkår, forventetTom = nyttVilkår.fom)

        val oppdatertVilkår =
            nyttVilkår.copy(
                fakta =
                    offentligTransport(BigDecimal.valueOf(150)),
            )

        val resultatOppdatert =
            kall.vilkårReiseOppstartAvslutningHjemreise.oppdaterVilkår(
                lagreVilkår = oppdatertVilkår,
                vilkårId = opprettetVilkår.id,
                behandlingId = behandling.id,
            )

        assertThat(resultatOppdatert.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultatOppdatert.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatertVilkår, resultatOppdatert, forventetTom = oppdatertVilkår.fom)

        val resultatSlettet =
            kall.vilkårReiseOppstartAvslutningHjemreise.slettVilkår(
                behandlingId = behandling.id,
                vilkårId = resultatOppdatert.id,
                dto = SlettVilkårRequestDto(),
            )

        assertThat(resultatSlettet.slettetPermanent).isTrue
        assertThat((resultatSlettet.vilkår as VilkårReiseOppstartAvslutningHjemreiseDto).slettetKommentar).isNull()

        val hentedeVilkår = kall.vilkårDagligReise.hentVilkår(behandling.id)
        assertThat(hentedeVilkår).isEmpty()
    }

    private val vilkårKall get() = kall.vilkårReiseOppstartAvslutningHjemreise

    private fun opprettBehandlingMedAktivitet(ident: String): Pair<Behandling, VilkårperiodeDto> {
        val fom = 1 januar 2026
        val tom = 31 mars 2026
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                identer = setOf(PersonIdent(ident = ident)),
            )
        opprettOgTilordneOppgaveForBehandling(behandling.id)
        gjennomførBehandlingsløp(ident = ident, behandlingId = behandling.id, tilSteg = StegType.VILKÅR) {
            aktivitet { opprett { aktivitetTiltakTsoReiseTilOppstart(fom, tom) } }
            målgruppe { opprett { målgruppeAAP(fom, tom) } }
        }
        val aktivitet =
            kall.vilkårperiode
                .hentForBehandling(behandling.id)
                .vilkårperioder.aktiviteter
                .single()
        return behandling to aktivitet
    }

    private fun reise(aktivitetId: VilkårperiodeGlobalId) =
        LagreVilkårReiseOppstartAvslutningHjemreiseDto(
            fom = 1 januar 2026,
            tom = null,
            reiseId = ReiseId.random(),
            aktivitetId = aktivitetId,
            typeReiseformål = TypeReiseformål.OPPSTART,
            svar = ROAHTestUtil.oppfylteSvarOppstartAvslutningOffentligTransportDto(),
            fakta = offentligTransport(BigDecimal.valueOf(100)),
        )

    private fun paraplyvilkår(
        aktivitetId: VilkårperiodeGlobalId,
        fraAdresse: String = "Adresse 1",
        svar: Map<RegelId, SvarOgBegrunnelseDto> = ROAHTestUtil.oppfylteSvarMidlertidigBostedDto(),
    ) = LagreVilkårMidlertidigBostedDto(
        fom = 1 januar 2026,
        tom = 31 mars 2026,
        aktivitetId = aktivitetId,
        svar = svar,
        fraAdresse = fraAdresse,
        tilAdresse = "Adresse 2",
    )

    private fun assertLagretVilkår(
        lagreVilkårRequest: LagreVilkårReiseOppstartAvslutningHjemreiseDto,
        resultat: VilkårReiseOppstartAvslutningHjemreiseDto,
        forventetTom: LocalDate? = lagreVilkårRequest.tom,
    ) {
        assertThat(resultat.fom).isEqualTo(lagreVilkårRequest.fom)
        assertThat(resultat.tom).isEqualTo(forventetTom)
        assertThat(resultat.fakta).isEqualTo(lagreVilkårRequest.fakta)
        assertThat(resultat.delvilkårsett).hasSize(1)

        assertAlleSvarHarFåttVurdering(delvilkår = resultat.delvilkårsett, svar = lagreVilkårRequest.svar)
    }

    private fun assertAlleSvarHarFåttVurdering(
        delvilkår: List<DelvilkårDto>,
        svar: Map<RegelId, SvarOgBegrunnelseDto>,
    ) {
        val brukteRegelIder = delvilkår.flatMap { it.vurderinger.map { vurdering -> vurdering.regelId } }.toSet()

        assertThat(brukteRegelIder).hasSize(svar.size)
    }

    private fun offentligTransport(utgift: BigDecimal) =
        FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
            begrunnelse = "buss",
            nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.JA, begrunnelse = null),
            utgifterOffentligTransport = utgift,
        )
}
