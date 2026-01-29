package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.Dokument
import no.nav.tilleggsstonader.kontrakter.søknad.DokumentasjonFelt
import no.nav.tilleggsstonader.kontrakter.søknad.Reisedag
import no.nav.tilleggsstonader.kontrakter.søknad.UkeMedReisedager
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaKjøreliste
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadKjøreliste
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object SøknadKjørelisteUtil {
    fun søknadKjøreliste(
        data: SkjemaKjøreliste = lagSkjemaKjøreliste(),
        journalpostId: String = "123321",
        språk: Språkkode = Språkkode.NB,
        mottattTidspunkt: LocalDateTime = LocalDate.of(2023, 1, 1).atStartOfDay().truncatedTo(ChronoUnit.MILLIS),
    ) = SøknadKjøreliste(
        journalpostId = journalpostId,
        språk = språk,
        mottattTidspunkt = mottattTidspunkt,
        data = data,
    )

    fun lagSkjemaKjøreliste(
        reiser: List<UkeMedReisedager> = listOf(lagUkeMedReisedager()),
        dokumentasjon: List<DokumentasjonFelt> = listOf(lagDokumentasjon()),
    ) = SkjemaKjøreliste(
        reiser = reiser,
        dokumentasjon = dokumentasjon,
    )

    private fun lagDokumentasjon(): DokumentasjonFelt =
        DokumentasjonFelt(
            type = Vedleggstype.PARKERINGSUTGIFT,
            label = "Dokumentasjon av parkeringsutgift",
            opplastedeVedlegg =
                listOf(
                    Dokument(
                        id = UUID.randomUUID(),
                        navn = "parkering.jpg",
                    ),
                ),
        )

    private fun lagUkeMedReisedager() =
        UkeMedReisedager(
            ukeLabel = "Uke 1",
            spørsmål = "Spørsmål for uke 1",
            reisedager =
                listOf(
                    Reisedag(
                        dato = DatoFelt("Fom", LocalDate.of(2026, 1, 1)),
                        harKjørt = true,
                        parkeringsutgift = VerdiFelt(50, "kr"),
                    ),
                    Reisedag(
                        dato = DatoFelt("Fom", LocalDate.of(2026, 1, 2)),
                        harKjørt = false,
                        parkeringsutgift = VerdiFelt(0, "kr"),
                    ),
                ),
        )
}
