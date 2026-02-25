package no.nav.tilleggsstonader.sak.oppfølging.domain

import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppfølgingRegisterAktiviteterTest {
    @Test
    fun `skal slå sammen to påfølgende aktiviteter hvor fom og tom eksisterer`() {
        val aktiviteter =
            OppfølgingRegisterAktiviteter(
                listOf(
                    aktivitetArenaDto(id = "1", fom = 1 januar 2025, tom = 31 januar 2025),
                    aktivitetArenaDto(id = "2", fom = 1 februar 2025, tom = 28 februar 2025),
                ),
            )

        assertThat(aktiviteter.alleAktiviteter).containsExactly(
            DatoperiodeNullableTom(1 januar 2025, 28 februar 2025),
        )
    }

    @Test
    fun `skal slå sammen to påfølgende aktiviteter hvor fom og tom eksisterer på første aktivitet, men tom ikke eksisterer på andre`() {
        val aktiviteter =
            OppfølgingRegisterAktiviteter(
                listOf(
                    aktivitetArenaDto(id = "1", fom = 1 januar 2025, tom = 31 januar 2025),
                    aktivitetArenaDto(id = "2", fom = 1 februar 2025, tom = null),
                ),
            )

        assertThat(aktiviteter.alleAktiviteter).containsExactly(
            DatoperiodeNullableTom(1 januar 2025, null),
        )
    }

    @Test
    fun `skal ikke slå sammen to påfølgende aktiviteter hvor tom ikke eksisterer på første aktivitet, men tom eksisterer på andre`() {
        val aktiviteter =
            OppfølgingRegisterAktiviteter(
                listOf(
                    aktivitetArenaDto(id = "1", fom = 1 januar 2025, tom = null),
                    aktivitetArenaDto(id = "2", fom = 1 februar 2025, tom = 28 februar 2025),
                ),
            )

        assertThat(aktiviteter.alleAktiviteter).containsExactly(
            DatoperiodeNullableTom(1 januar 2025, null),
            DatoperiodeNullableTom(1 februar 2025, 28 februar 2025),
        )
    }

    @Test
    fun `skal slå sammen tre påfølgende aktiviteter hvor fom og tom eksisterer på de to første aktivitet, men tom ikke eksisterer på tredje`() {
        val aktiviteter =
            OppfølgingRegisterAktiviteter(
                listOf(
                    aktivitetArenaDto(id = "1", fom = 1 januar 2025, tom = 31 januar 2025),
                    aktivitetArenaDto(id = "2", fom = 1 februar 2025, tom = 28 februar 2025),
                    aktivitetArenaDto(id = "3", fom = 1 mars 2025, tom = null),
                ),
            )

        assertThat(aktiviteter.alleAktiviteter).containsExactly(
            DatoperiodeNullableTom(1 januar 2025, null),
        )
    }

    @Test
    fun `skal ikke slå sammen tre påfølgende aktiviteter hvor fom og tom eksisterer på første og tredje aktivitet, men tom ikke eksisterer på andre`() {
        val aktiviteter =
            OppfølgingRegisterAktiviteter(
                listOf(
                    aktivitetArenaDto(id = "1", fom = 1 januar 2025, tom = 31 januar 2025),
                    aktivitetArenaDto(id = "2", fom = 1 februar 2025, tom = null),
                    aktivitetArenaDto(id = "3", fom = 1 mars 2025, tom = 31 mars 2025),
                ),
            )

        assertThat(aktiviteter.alleAktiviteter).containsExactly(
            DatoperiodeNullableTom(1 januar 2025, null),
            DatoperiodeNullableTom(1 mars 2025, 31 mars 2025),
        )
    }
}
