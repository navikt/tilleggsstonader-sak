package no.nav.tilleggsstonader.sak.ekstern.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.tomYtelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.journalpostMedStrukturertSøknad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BestemTemaForJournalpostServiceTest {
    private val ytelseService = mockk<YtelseService>()
    private val unleashService =
        mockk<UnleashService> {
            every { isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER) } returns false
        }
    private val service = BestemTemaForJournalpostService(ytelseService = ytelseService, unleashService = unleashService)

    private val fom = 1 januar 2026
    private val tom = 31 desember 2026

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal bare hente aktivitetspenger fra register når togglen er på`(aktivert: Boolean) {
        val journalpost = journalpostMedStrukturertSøknad(dokumentBrevkode = DokumentBrevkode.REISE_TIL_SAMLING)
        val typer = slot<List<TypeYtelsePeriode>>()
        every { unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER) } returns aktivert
        every {
            ytelseService.hentYtelser(journalpost.bruker!!.id, fom, tom, capture(typer))
        } returns tomYtelsePerioderDto()

        service.bestemStønadstype(
            journalpost = journalpost,
            stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
            stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
            fom = fom,
            tom = tom,
            målgrupperFraSøknad = setOf(MålgruppeType.AAP),
        )

        val forventedeTyper = TypeYtelsePeriode.entries.filter { aktivert || it != TypeYtelsePeriode.AKTIVITETSPENGER }
        assertThat(typer.captured).containsExactlyElementsOf(forventedeTyper)
    }

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
}
