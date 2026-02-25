package no.nav.tilleggsstonader.sak.oppfølging

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OppfølgingIntegrasjonstest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var oppfølginController: OppfølgingController

    @Autowired
    private lateinit var oppfølgingRepository: OppfølgingRepository

    @Autowired
    private lateinit var registerAktivitetClient: RegisterAktivitetClient

    @Test
    fun `henter kun oppfølginger for spesifiesert enhet`() {
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.BARNETILSYN,
        ) { defaultTilsynBarnTestdata() }

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSR,
        ) { defaultDagligReiseTsrTestdata() }

        kall.oppfølging.startJobb(Tema.TSO)
        kall.oppfølging.startJobb(Tema.TSR)

        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        assertThat(
            kall.oppfølging
                .hentAktiveOppfølginger(Tema.TSO)
                .single()
                .behandlingsdetaljer
                .stønadstype
                .tilTema(),
        ).isEqualTo(Tema.TSO)

        assertThat(
            kall.oppfølging
                .hentAktiveOppfølginger(Tema.TSR)
                .single()
                .behandlingsdetaljer
                .stønadstype
                .tilTema(),
        ).isEqualTo(Tema.TSR)
    }

    @Test
    fun `oppretter oppfølginger bare oppgaver for spesifiesert enhet`() {
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.BARNETILSYN,
        ) { defaultTilsynBarnTestdata() }

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSR,
        ) { defaultDagligReiseTsrTestdata() }

        kall.oppfølging.startJobb(Tema.TSO)

        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        assertThat(
            kall.oppfølging
                .hentAktiveOppfølginger(Tema.TSO)
                .single()
                .behandlingsdetaljer
                .stønadstype
                .tilTema(),
        ).isEqualTo(Tema.TSO)

        assertThat(
            kall.oppfølging
                .hentAktiveOppfølginger(Tema.TSR),
        ).isEmpty()
    }

    @Test
    fun `skal ikke opprette oppfølging oppgave for dagpengevedtak uten tom`() {
        every { registerAktivitetClient.hentAktiviteter(any(), any(), any()) } returns
            listOf(
                aktivitetArenaDto(
                    fom = 1 januar 2026,
                    tom = 28 februar 2026,
                    type = "TILTAK",
                    antallDagerPerUke = 3,
                ),
            )

        every { ytelseClient.hentYtelser(any()) } returns
            ytelsePerioderDto(
                perioder =
                    listOf(
                        YtelsePeriode(
                            type = TypeYtelsePeriode.DAGPENGER,
                            fom = 1 januar 2026,
                            tom = 31 januar 2026,
                            ensligForsørgerStønadstype = null,
                        ),
                        YtelsePeriode(
                            type = TypeYtelsePeriode.DAGPENGER,
                            fom = 1 februar 2026,
                            tom = null,
                            ensligForsørgerStønadstype = null,
                        ),
                    ),
            )

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSR,
        ) {
            aktivitet {
                opprett {
                    aktivitetTiltakTsr(
                        fom = 1 januar 2026,
                        tom = 28 februar 2026,
                        typeAktivitet = TypeAktivitet.GRUPPEAMO,
                        kildeId = "123",
                    )
                }
            }
            målgruppe {
                opprett {
                    målgruppeDagpenger(1 januar 2026, 28 februar 2026)
                }
            }
            vilkår {
                opprett {
                    offentligTransport(fom = 1 januar 2026, tom = 28 februar 2026)
                }
            }
        }

        kall.oppfølging.startJobb(Tema.TSR)

        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        assertThat(
            kall.oppfølging
                .hentAktiveOppfølginger(Tema.TSR),
        ).isEmpty()
    }
}
