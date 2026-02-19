package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.behandlendeEnhet
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

        kall.oppfølging.startJobb(Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)
        kall.oppfølging.startJobb(Enhet.NAV_TILTAK_OSLO)

        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        assertThat(
            kall.oppfølging
                .hentAktiveOppfølginger(Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)
                .single()
                .behandlingsdetaljer
                .stønadstype
                .behandlendeEnhet(),
        ).isEqualTo(Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)

        assertThat(
            kall.oppfølging
                .hentAktiveOppfølginger(Enhet.NAV_TILTAK_OSLO)
                .single()
                .behandlingsdetaljer
                .stønadstype
                .behandlendeEnhet(),
        ).isEqualTo(Enhet.NAV_TILTAK_OSLO)
    }

    @Test
    fun `oppretter oppfølginger bare oppgaver for spesifiesert enhet`() {
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.BARNETILSYN,
        ) { defaultTilsynBarnTestdata() }

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSR,
        ) { defaultDagligReiseTsrTestdata() }

        kall.oppfølging.startJobb(Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)

        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        assertThat(
            kall.oppfølging
                .hentAktiveOppfølginger(Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)
                .single()
                .behandlingsdetaljer
                .stønadstype
                .behandlendeEnhet(),
        ).isEqualTo(Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD)

        assertThat(
            kall.oppfølging
                .hentAktiveOppfølginger(Enhet.NAV_TILTAK_OSLO),
        ).isEmpty()
    }
}
