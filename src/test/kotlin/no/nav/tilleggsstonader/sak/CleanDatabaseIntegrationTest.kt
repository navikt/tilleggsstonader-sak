package no.nav.tilleggsstonader.sak

import io.mockk.every
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import org.junit.jupiter.api.BeforeEach

abstract class CleanDatabaseIntegrationTest : IntegrationTest() {
    @BeforeEach
    fun resetDatabase() {
        jdbcTemplate.update(
            """
            TRUNCATE TABLE
                fagsak_utbetaling_id,
                hendelse,
                task_logg,
                task,
                skjema_routing,
                brevmottaker_frittstaende_brev,
                frittstaende_brev,
                oppfolging,
                fakta_grunnlag,
                vedtak,
                simuleringsresultat,
                andel_tilkjent_ytelse,
                tilkjent_ytelse,
                vilkar_periode,
                vilkar,
                behandling_barn,
                soknad_behandling,
                soknad_barn,
                soknad,
                sett_pa_vent,
                oppgave,
                totrinnskontroll,
                vedtaksbrev,
                brevmottaker,
                mellomlagret_frittstaende_brev,
                mellomlagret_brev,
                vilkarperioder_grunnlag,
                behandlingshistorikk,
                behandlingsjournalpost,
                behandling_ekstern,
                tilbakekreving_hendelse,
                avklart_kjort_dag,
                avklart_kjort_uke,
                kjoreliste,
                kjoreliste_behandling_brev,
                behandling,
                fagsak_ekstern,
                fagsak,
                person_ident,
                fagsak_person,
                iverksetting_logg,
                vedtaksstatistikk_v2
            """,
            emptyMap<String, Any>(),
        )
    }

    @BeforeEach
    fun togglePrivatBil() {
        every { unleashService.isEnabled(Toggle.KAN_AUTOMATISK_BEHANDLE_KJØRELISTE) } returns false
    }
}
