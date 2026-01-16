package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InnvilgeDagligReiseIntegrationTest : CleanDatabaseIntegrationTest() {
    val fomTiltaksenheten = 1 september 2025
    val tomTiltaksenheten = 30 september 2025

    val fomNay = 15 september 2025
    val tomNay = 14 oktober 2025

    @Autowired
    lateinit var ytelseClient: YtelseClient

    @Test
    fun `Skal ikke kunne innvilge daglig reise for både Nay og Tiltaksenheten samtidig`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        // Gjennomfører behandling for Tiltaksenheten
        opprettBehandlingOgGjennomførBehandlingsløp(
            fraJournalpost =
                journalpost(
                    journalpostId = "1",
                    journalstatus = Journalstatus.MOTTATT,
                    dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.DAGLIG_REISE.verdi)),
                    bruker = Bruker("12345678910", BrukerIdType.FNR),
                    tema = Tema.TSR.name,
                ),
            medAktivitet = { behandlingId ->
                lagreVilkårperiodeAktivitet(
                    behandlingId = behandlingId,
                    aktivitetType = AktivitetType.TILTAK,
                    typeAktivitet = TypeAktivitet.ENKELAMO,
                    fom = fomTiltaksenheten,
                    tom = tomTiltaksenheten,
                    faktaOgSvar =
                        FaktaOgSvarAktivitetDagligReiseTsrDto(
                            svarHarUtgifter = SvarJaNei.JA,
                        ),
                )
            },
            medMålgruppe = { behandlingId ->
                lagreVilkårperiodeMålgruppe(
                    behandlingId = behandlingId,
                    målgruppeType = MålgruppeType.TILTAKSPENGER,
                    fom = fomTiltaksenheten,
                    tom = tomTiltaksenheten,
                )
            },
            medVilkår = listOf(lagreDagligReiseDto(fom = fomTiltaksenheten, tom = tomTiltaksenheten)),
        )

        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoAAP()

        // Gjennomfører behandling for Nay
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                fraJournalpost =
                    journalpost(
                        journalpostId = "1",
                        journalstatus = Journalstatus.MOTTATT,
                        dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.DAGLIG_REISE.verdi)),
                        bruker = Bruker("12345678910", BrukerIdType.FNR),
                        tema = Tema.TSO.name,
                    ),
                medAktivitet = { behandlingId ->
                    lagreVilkårperiodeAktivitet(
                        behandlingId = behandlingId,
                        aktivitetType = AktivitetType.UTDANNING,
                        typeAktivitet = null,
                        fom = fomNay,
                        tom = tomNay,
                        faktaOgSvar = FaktaOgSvarAktivitetDagligReiseTsoDto(),
                    )
                },
                medMålgruppe = { behandlingId ->
                    lagreVilkårperiodeMålgruppe(
                        behandlingId = behandlingId,
                        målgruppeType = MålgruppeType.AAP,
                        fom = fomNay,
                        tom = tomNay,
                    )
                },
                medVilkår = listOf(lagreDagligReiseDto(fom = fomNay, tom = tomNay)),
                tilSteg = StegType.BEREGNE_YTELSE,
            )

        gjennomførBeregningSteg(behandlingId, Stønadstype.DAGLIG_REISE_TSO)
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo(
                "Kan ikke ha overlappende vedtaksperioder for Nay og Tiltaksenheten. Se oversikt øverst på siden for å finne overlappende vedtaksperiode.",
            )
    }
}
