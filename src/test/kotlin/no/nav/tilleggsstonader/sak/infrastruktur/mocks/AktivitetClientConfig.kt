package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.AktivitetClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-aktivitet")
class AktivitetClientConfig {

    @Bean
    @Primary
    fun aktivitetClient(): AktivitetClient {
        val client = mockk<AktivitetClient>()

        every { client.hentAktiviteter(any(), any(), any()) } returns
            listOf(
                AktivitetArenaDto(
                    id = "123",
                    fom = osloDateNow(),
                    tom = osloDateNow().plusMonths(1),
                    type = "TYPE",
                    typeNavn = "Type navn",
                    status = StatusAktivitet.AKTUELL,
                    statusArena = "AKTUL",
                    antallDagerPerUke = 5,
                    prosentDeltakelse = 100.toBigDecimal(),
                    erStønadsberettiget = true,
                    erUtdanning = false,
                    arrangør = "Arrangør",
                    kilde = Kilde.ARENA,
                ),
            )

        return client
    }
}
