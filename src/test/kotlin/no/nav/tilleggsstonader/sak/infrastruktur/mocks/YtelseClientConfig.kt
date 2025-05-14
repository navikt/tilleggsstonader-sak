package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto.KildeResultatYtelse
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

            val perioder =
                request.typer
                    .map { type ->
                        val ensligForsørgerStønadstype =
                            if (type == TypeYtelsePeriode.ENSLIG_FORSØRGER) {
                                EnsligForsørgerStønadstype.OVERGANGSSTØNAD
                            } else {
                                null
                            }
                        YtelsePeriode(
                            type = type,
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            ensligForsørgerStønadstype = ensligForsørgerStønadstype,
                        )
                    }.toMutableList()
            if (request.typer.contains(TypeYtelsePeriode.AAP)) {
                perioder +=
                    YtelsePeriode(
                        type = TypeYtelsePeriode.AAP,
                        fom = LocalDate.now().plusDays(1),
                        tom = LocalDate.now().plusDays(1),
                        aapErFerdigAvklart = true,
                    )
            }
            val kildeResultat =
                request.typer.map {
                    KildeResultatYtelse(type = it, resultat = ResultatKilde.OK)
                }
            YtelsePerioderDto(perioder = perioder, kildeResultat = kildeResultat)
        }

        return client
    }
}
