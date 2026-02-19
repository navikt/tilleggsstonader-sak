package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OppfølgingIntegrasjonstest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var oppfølginController: OppfølgingController

    @Autowired
    private lateinit var oppfølgingRepository: OppfølgingRepository

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
}
