package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileJsonIsEqual
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ROAHTestUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ROAHVilkårControllerTest : CleanDatabaseIntegrationTest() {
    val ident = "12345678910"

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
        assertThat(resultatSlettet.vilkår.slettetKommentar).isNull()

        val hentedeVilkår = kall.vilkårDagligReise.hentVilkår(behandling.id)
        assertThat(hentedeVilkår).isEmpty()
    }

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
