package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusHarSakerDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.arena.VedtakStatus
import no.nav.tilleggsstonader.kontrakter.arena.oppgave.ArenaOppgaveDto
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.AktivitetDto
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.ArenaSakOgVedtakDto
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.Rettighet
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.SakDto
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.VedtakDto
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.VedtakfaktaDto
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime

@Configuration
@Profile("mock-arena")
class ArenaClientConfig {

    @Bean
    @Primary
    fun arenaClient(): ArenaClient {
        val client = mockk<ArenaClient>()
        resetMock(client)
        return client
    }

    companion object {
        fun resetMock(client: ArenaClient) {
            clearMocks(client)
            every { client.hentStatus(any()) } returns ArenaStatusDto(
                SakStatus(harAktivSakUtenVedtak = false),
                VedtakStatus(
                    harVedtak = false,
                    harInnvilgetVedtak = false,
                    harAktivtVedtak = false,
                    harVedtakUtenUtfall = false,
                    vedtakTom = LocalDate.now().minusDays(10),
                ),
            )
            every { client.harSaker(any()) } returns ArenaStatusHarSakerDto(true)
            every { client.hentOppgaver(any()) } returns listOf(
                ArenaOppgaveDto(
                    id = 1,
                    tittel = "Kontroller/registrer saksopplysninger - automatisk journalført",
                    kommentar = "En kommentar\n\n med \nradbryte",
                    benk = "Inn",
                    tildelt = null,
                    opprettetTidspunkt = LocalDateTime.now(),
                ),
                ArenaOppgaveDto(
                    id = 2,
                    tittel = "Vurder dokument",
                    kommentar = "En kommentar",
                    benk = null,
                    tildelt = "ABC1234",
                    opprettetTidspunkt = LocalDateTime.now(),
                ),
            )
            every { client.hentVedtak(any()) } returns ArenaSakOgVedtakDto(
                vedtak = listOf(
                    VedtakDto(
                        sakId = 1,
                        type = "Ny rettighet",
                        status = "Opprettet",
                        rettighet = "Tilsyn av barn tilleggsstønad",
                        rettighetkode = Rettighet.TILSYN_BARN,
                        fom = LocalDate.now().minusDays(10),
                        tom = LocalDate.now().plusDays(10),
                        totalbeløp = 1000,
                        datoInnstillt = null,
                        utfall = "Ja",
                        vedtakfakta = listOf(
                            VedtakfaktaDto(
                                type = "Antall måneder",
                                verdi = "1",
                            ),
                        ),
                        vilkårsvurderinger = listOf(
                            VilkårsvurderingDto(
                                vilkår = "Aktiviteten er ikke lønnet",
                                vurdering = "Oppfylt",
                                vurdertAv = "Saksbeh1",
                            ),
                        ),
                        datoMottatt = LocalDate.now(),
                        saksbehandler = "Saksbeh 1",
                        beslutter = "Beslutter 1",
                    ),
                ),
                saker = mapOf(
                    1 to SakDto(
                        målgruppe = "Målgruppe",
                        aktivitet = AktivitetDto(
                            aktivitetId = 1,
                            type = TypeAktivitet.JOBBK.beskrivelse,
                            status = StatusAktivitet.BEHOV.beskrivelse,
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            beskrivelse = "En beskrivelse",
                            gjelderUtdanning = false,
                            typekode = TypeAktivitet.JOBBK.name,
                            statuskode = StatusAktivitet.BEHOV.name,
                        ),
                    ),
                ),
            )
        }
    }
}
