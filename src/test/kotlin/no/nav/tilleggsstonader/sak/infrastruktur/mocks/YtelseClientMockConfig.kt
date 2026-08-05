package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.GjenståendeDagerFraTelleverk
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto.KildeResultatYtelse
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("mock-ytelse-client")
class YtelseClientMockConfig {
    @Bean
    @Primary
    fun ytelseClient() = mockk<YtelseClient>().apply { resetTilDefault(this) }

    companion object {
        fun resetTilDefault(client: YtelseClient) {
            clearMocks(client)
            every { client.hentYtelser(any<YtelsePerioderRequest>()) } answers {
                val request = firstArg<YtelsePerioderRequest>()

                val perioder =
                    request.typer
                        .map { type ->
                            when (type) {
                                TypeYtelsePeriode.DAGPENGER ->
                                    YtelsePeriode.Dagpenger(
                                        fom = LocalDate.now(),
                                        tom = null,
                                        gjenståendeDagerFraTelleverk =
                                            GjenståendeDagerFraTelleverk(
                                                dato = LocalDate.now(),
                                                antallDager = 40,
                                            ),
                                    )
                                TypeYtelsePeriode.ENSLIG_FORSØRGER ->
                                    YtelsePeriode.EnsligForsørger(
                                        fom = LocalDate.now(),
                                        tom = LocalDate.now(),
                                        ensligForsørgerStønadstype = EnsligForsørgerStønadstype.OVERGANGSSTØNAD,
                                        erNyttRegelverk2026 = false,
                                    )
                                TypeYtelsePeriode.AAP ->
                                    YtelsePeriode.AAP(
                                        fom = LocalDate.now(),
                                        tom = LocalDate.now(),
                                        aapErFerdigAvklart = false,
                                    )
                                TypeYtelsePeriode.OMSTILLINGSSTØNAD ->
                                    YtelsePeriode.Omstillingsstønad(
                                        fom = LocalDate.now(),
                                        tom = LocalDate.now(),
                                    )
                                TypeYtelsePeriode.TILTAKSPENGER_TPSAK ->
                                    YtelsePeriode.TiltakspengerTPSak(
                                        fom = LocalDate.now(),
                                        tom = LocalDate.now(),
                                    )
                                TypeYtelsePeriode.TILTAKSPENGER_ARENA ->
                                    YtelsePeriode.TiltakspengerArena(
                                        fom = LocalDate.now(),
                                        tom = LocalDate.now(),
                                    )
                            }
                        }.toMutableList()
                if (request.typer.contains(TypeYtelsePeriode.AAP)) {
                    perioder +=
                        YtelsePeriode.AAP(
                            fom = LocalDate.now().plusDays(1),
                            tom = LocalDate.now().plusDays(1),
                            aapErFerdigAvklart = true,
                        )
                }
                val kildeResultat =
                    request.typer.map {
                        KildeResultatYtelse(type = it, resultat = ResultatKilde.OK)
                    }
                ytelsePerioderDto(perioder = perioder, kildeResultat = kildeResultat)
            }
        }
    }
}
