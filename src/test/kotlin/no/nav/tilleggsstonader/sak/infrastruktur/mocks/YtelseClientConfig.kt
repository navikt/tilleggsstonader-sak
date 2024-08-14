package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.ytelse.HentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("mock-ytelse-client")
class YtelseClientConfig {

    @Bean
    @Primary
    fun ytelseClient(): YtelseClient {
        val client = mockk<YtelseClient>()

        every { client.hentYtelser(any()) } answers {
            val request = firstArg<YtelsePerioderRequest>()

            val perioder = request.typer.map {
                YtelsePeriode(type = it, fom = LocalDate.now(), tom = LocalDate.now())
            } + YtelsePeriode(type = TypeYtelsePeriode.AAP, fom = LocalDate.now(), tom = LocalDate.now(), aapAktivitetsfase = "Ferdig avklart")
            val hentetInformasjon = request.typer.map {
                HentetInformasjon(type = it, status = StatusHentetInformasjon.OK)
            }
            YtelsePerioderDto(perioder = perioder, hentetInformasjon = hentetInformasjon)
        }

        return client
    }
}
