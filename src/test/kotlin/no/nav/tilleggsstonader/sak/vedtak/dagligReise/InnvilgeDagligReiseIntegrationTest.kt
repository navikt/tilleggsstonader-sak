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
                )
        ) {
            aktivitet {
                opprett {
                    aktivitetTiltakTsr(fomTiltaksenheten, tomTiltaksenheten, typeAktivitet = TypeAktivitet.GRUPPEAMO)
                }
            }
            målgruppe {
                opprett {
                    målgruppeTiltakspenger(fomTiltaksenheten, tomTiltaksenheten)
                }
            }
            vilkår {
                opprett {
                    offentligTransport(fomTiltaksenheten, tomTiltaksenheten)
                }
            }
        }

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
                tilSteg = StegType.BEREGNE_YTELSE
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fomNay, tomNay)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fomNay, tomNay)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(fomNay, tomNay)
                    }
                }
            }

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
