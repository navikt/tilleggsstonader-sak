package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterSøknadService
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeTestUtil
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingerMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BeslutteVedtakIntegrationTest(
    @Autowired private val håndterSøknadService: HåndterSøknadService,
) : IntegrationTest() {
    @Test
    fun `skal kunne opprette en behandling fra journalpost og fullføre den steg for steg`() {
        val søkerIdent = "11112222333"
        val journalpost =
            Journalpost(
                journalpostId = "1",
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.MOTTATT,
                dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.DAGLIG_REISE.verdi)),
                bruker = Bruker(søkerIdent, BrukerIdType.FNR),
                journalforendeEnhet = "9999",
                kanal = "NAV_NO",
                tema = Tema.TSO.name,
            )
        val behandling = håndterSøknadService.håndterSøknad(journalpost)!!

        // Henter behandling, fordi det gjør at det opprettes grunnlagsdata(!) hvis ikke det eksisterer fra før
        kall.behandling.hentBehandling(behandling.id)

        // Oppretter oppgave
        kjørTasksKlareForProsessering()

        kall.vilkårperiode.opprett(
            lagreVilkårperiode =
                LagreVilkårperiode(
                    type = MålgruppeType.AAP,
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                    behandlingId = behandling.id,
                ),
        )

        kall.vilkårperiode.opprett(
            lagreVilkårperiode =
                LagreVilkårperiode(
                    type = AktivitetType.TILTAK,
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    faktaOgSvar =
                        FaktaOgSvarAktivitetDagligReiseTsoDto(
                            svarLønnet = SvarJaNei.NEI,
                            svarHarUtgifter = SvarJaNei.JA,
                        ),
                    behandlingId = behandling.id,
                ),
        )

        // INNGANGSVILKÅR -> VILKÅR
        kall.steg.ferdigstill(
            behandling.id,
            StegController.FerdigstillStegRequest(
                steg = StegType.INNGANGSVILKÅR,
            ),
        )

        // lagre vilkår (reise)
        val svarOffentligTransport =
            mapOf(
                RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.JA),
                RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
            )
        val reise =
            LagreDagligReiseDto(
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                svar = svarOffentligTransport,
                fakta = faktaOffentligTransport(),
            )
        kall.vilkår.opprettDagligReise(reise, behandling.id)

        // VILKÅR -> BEREGNE_YTELSE
        kall.steg.ferdigstill(
            behandling.id,
            StegController.FerdigstillStegRequest(
                steg = StegType.VILKÅR,
            ),
        )

        // BEREGNE_YTELSE -> SIMULERING
        kall.vedtak.dagligReise.lagreInnvilgelseResponse(
            behandlingId = behandling.id,
            innvilgelseDto =
                InnvilgelseDagligReiseRequest(
                    vedtaksperioder = listOf(VedtaksperiodeTestUtil.vedtaksperiode().tilDto()),
                ),
        )

        // SIMULERING -> SEND_TIL_BESLUTTER
        kall.steg.ferdigstill(
            behandling.id,
            StegController.FerdigstillStegRequest(
                steg = StegType.SIMULERING,
            ),
        )

        val minimaltBrev = "\"SAKSBEHANDLER_SIGNATUR - BREVDATO_PLACEHOLDER - BESLUTTER_SIGNATUR\""
        kall.brev.brev(behandling.id, GenererPdfRequest(minimaltBrev))
        // SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
        kall.totrinnskontroll.sendTilBeslutter(behandling.id)

//        BESLUTTE_VEDTAK -> JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV
        medBrukercontext(bruker = "nissemor", rolle = rolleConfig.beslutterRolle) {
            kall.totrinnskontroll.beslutteVedtak(behandling.id, BeslutteVedtakDto(godkjent = true))
        }

//        JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
//        FERDIGSTILLE_BEHANDLING -> BEHANDLING_FERDIGSTILT
    }

    private fun faktaOffentligTransport(
        reisedagerPerUke: Int = 5,
        prisEnkelbillett: Int? = 40,
        prisSyvdagersbillett: Int? = null,
        prisTrettidagersbillett: Int? = 800,
    ) = FaktaDagligReiseOffentligTransportDto(
        reisedagerPerUke = reisedagerPerUke,
        prisEnkelbillett = prisEnkelbillett,
        prisSyvdagersbillett = prisSyvdagersbillett,
        prisTrettidagersbillett = prisTrettidagersbillett,
    )
}
