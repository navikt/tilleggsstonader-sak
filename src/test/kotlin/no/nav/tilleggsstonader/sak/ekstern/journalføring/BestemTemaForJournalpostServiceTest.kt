package no.nav.tilleggsstonader.sak.ekstern.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.tomYtelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.util.journalpostMedStrukturertSøknad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class BestemTemaForJournalpostServiceTest {
    private val ytelseService = mockk<YtelseService>()
    private val service = BestemTemaForJournalpostService(ytelseService = ytelseService)

    private val fom = 1 januar 2026
    private val tom = 31 desember 2026

    @ParameterizedTest
    @MethodSource("stønadstypepar")
    fun `skal la aktivitetspenger fra register overstyre søknad som tilsier TSR`(
        stønadstypeTso: Stønadstype,
        stønadstypeTsr: Stønadstype,
    ) {
        val aktivitetspenger = YtelsePeriode.Aktivitetspenger(fom = fom, tom = tom)
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns tomYtelsePerioderDto().copy(perioder = listOf(aktivitetspenger))

        val stønadstype =
            service.bestemStønadstype(
                journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING),
                stønadstypeTso = stønadstypeTso,
                stønadstypeTsr = stønadstypeTsr,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = setOf(MålgruppeType.TILTAKSPENGER),
            )

        assertThat(stønadstype).isEqualTo(stønadstypeTso)
        verify(exactly = 1) {
            ytelseService.hentYtelser(any(), fom, tom, match { TypeYtelsePeriode.ARBEIDSSØKER !in it })
        }
    }

    @Test
    fun `skal rute til TSR når registeret returnerer arbeidssøkerperiode`() {
        val flyttedato = 1 januar 2026
        val periode =
            YtelsePeriode.Arbeidssøker(
                fom = flyttedato,
                tom = null,
            )
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns
            tomYtelsePerioderDto().copy(
                perioder = listOf(periode),
                kildeResultat =
                    listOf(YtelsePerioderDto.KildeResultatYtelse(TypeYtelsePeriode.ARBEIDSSØKER, ResultatKilde.OK)),
            )

        assertThat(bestemFlytting(flyttedato)).isEqualTo(Stønadstype.FLYTTING_TSR)
        verify(exactly = 1) {
            ytelseService.hentYtelser(
                any(),
                flyttedato,
                flyttedato,
                match { TypeYtelsePeriode.ARBEIDSSØKER in it && TypeYtelsePeriode.AAP in it },
            )
        }
        verify(exactly = 1) { ytelseService.hentYtelser(any(), any(), any(), any()) }
    }

    @Test
    fun `skal bruke generelle rutingregler ved både AAP og arbeidssøkerperiode`() {
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns
            tomYtelsePerioderDto().copy(
                perioder = ytelsePerioderDtoAAP().perioder + YtelsePeriode.Arbeidssøker(fom, tom),
                kildeResultat =
                    listOf(YtelsePerioderDto.KildeResultatYtelse(TypeYtelsePeriode.ARBEIDSSØKER, ResultatKilde.OK)),
            )

        assertThat(bestemFlytting(fom)).isEqualTo(Stønadstype.FLYTTING_TSR)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal falle tilbake på søknad når registeret ikke gir perioder`(harKilderesultat: Boolean) {
        every { ytelseService.hentYtelser(any(), any(), any(), any()) } returns
            tomYtelsePerioderDto().copy(
                kildeResultat =
                    if (harKilderesultat) {
                        listOf(YtelsePerioderDto.KildeResultatYtelse(TypeYtelsePeriode.ARBEIDSSØKER, ResultatKilde.OK))
                    } else {
                        emptyList()
                    },
            )

        assertThat(bestemFlytting(fom)).isEqualTo(Stønadstype.FLYTTING_TSO)
    }

    @Test
    fun `skal stoppe ved feilet arbeidssøkeroppslag`() {
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns
            tomYtelsePerioderDto().copy(
                kildeResultat =
                    listOf(YtelsePerioderDto.KildeResultatYtelse(TypeYtelsePeriode.ARBEIDSSØKER, ResultatKilde.FEILET)),
            )

        assertThatThrownBy { bestemFlytting(fom) }.hasMessageContaining("Feil ved henting av ytelser")
    }

    private fun bestemFlytting(flyttedato: LocalDate) =
        service.bestemStønadstype(
            journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.FLYTTING),
            stønadstypeTso = Stønadstype.FLYTTING_TSO,
            stønadstypeTsr = Stønadstype.FLYTTING_TSR,
            fom = flyttedato,
            tom = flyttedato,
            målgrupperFraSøknad = setOf(MålgruppeType.AAP),
        )

    @Test
    fun `skal rute til TSO når målgruppe fra register kan brukes for TSO`() {
        val journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING)
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns ytelsePerioderDtoAAP()

        val stønadstype =
            service.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
                stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = emptySet(),
            )

        assertThat(stønadstype).isEqualTo(Stønadstype.REISE_TIL_SAMLING_TSO)
    }

    @Test
    fun `skal rute til TSR når målgruppe fra register ikke kan brukes for TSO`() {
        val journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING)
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns ytelsePerioderDtoTiltakspengerTpsak()

        val stønadstype =
            service.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
                stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = emptySet(),
            )

        assertThat(stønadstype).isEqualTo(Stønadstype.REISE_TIL_SAMLING_TSR)
    }

    @Test
    fun `skal falle tilbake på målgrupper fra søknad når register ikke gir treff`() {
        val journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING)
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns tomYtelsePerioderDto()

        val stønadstype =
            service.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
                stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = setOf(MålgruppeType.AAP),
            )

        assertThat(stønadstype).isEqualTo(Stønadstype.REISE_TIL_SAMLING_TSO)
    }

    @Test
    fun `skal falle tilbake på TSR når målgrupper fra søknad ikke kan brukes for TSO og register ikke gir treff`() {
        val journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING)
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns tomYtelsePerioderDto()

        val stønadstype =
            service.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
                stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = setOf(MålgruppeType.TILTAKSPENGER),
            )

        assertThat(stønadstype).isEqualTo(Stønadstype.REISE_TIL_SAMLING_TSR)
    }

    @Test
    fun `skal rute aktivitetspenger fra søknad til TSO`() {
        val journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING)
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns tomYtelsePerioderDto()

        val stønadstype =
            service.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
                stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = setOf(MålgruppeType.AKTIVITETSPENGER),
            )

        assertThat(stønadstype).isEqualTo(Stønadstype.REISE_TIL_SAMLING_TSO)
    }

    @Test
    fun `skal rute ungdomsprogrammet fra søknad til TSR`() {
        val journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING)
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns tomYtelsePerioderDto()

        val stønadstype =
            service.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
                stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = setOf(MålgruppeType.UNGDOMSPROGRAMMET),
            )

        assertThat(stønadstype).isEqualTo(Stønadstype.REISE_TIL_SAMLING_TSR)
    }

    @Test
    fun `skal kaste feil når journalpost mangler bruker`() {
        val journalpost =
            journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING)
                .copy(bruker = null)

        assertThatThrownBy {
            service.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
                stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = emptySet(),
            )
        }.hasMessageContaining("Forventer at bruker skal være satt på journalpost")
    }

    @Test
    fun `skal kaste feil når henting av ytelser feiler`() {
        val journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING)
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns
            tomYtelsePerioderDto().copy(
                kildeResultat =
                    listOf(
                        YtelsePerioderDto.KildeResultatYtelse(
                            type = TypeYtelsePeriode.AAP,
                            resultat = ResultatKilde.FEILET,
                        ),
                    ),
            )

        assertThatThrownBy {
            service.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
                stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = emptySet(),
            )
        }.hasMessageContaining("Feil ved henting av ytelser")
    }

    @Test
    fun `skal fungere likt for andre stønadstype-par, f-eks daglig reise`() {
        val journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.DAGLIG_REISE)
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns ytelsePerioderDtoAAP()

        val stønadstype =
            service.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.DAGLIG_REISE_TSO,
                stønadstypeTsr = Stønadstype.DAGLIG_REISE_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = emptySet(),
            )

        assertThat(stønadstype).isEqualTo(Stønadstype.DAGLIG_REISE_TSO)
    }

    companion object {
        @JvmStatic
        fun stønadstypepar() =
            listOf(
                Arguments.of(Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR),
                Arguments.of(Stønadstype.REISE_TIL_SAMLING_TSO, Stønadstype.REISE_TIL_SAMLING_TSR),
                Arguments.of(
                    Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                    Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
                ),
            )
    }
}
