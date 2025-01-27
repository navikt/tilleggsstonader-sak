package no.nav.tilleggsstonader.sak.opplysninger.pdl

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Bostedsadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Metadata
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Navn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Oppholdsadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Vegadresse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PdlPersonUtilTest {
    @Test
    internal fun `skal formetere navn med og uten mellomnavn`() {
        val navn = Navn("fornavn", null, "etternavn", metadata = Metadata(historisk = false))
        assertThat(navn.visningsnavn())
            .isEqualTo("fornavn etternavn")

        assertThat(navn.copy(mellomnavn = "mellomnavn").visningsnavn())
            .isEqualTo("fornavn mellomnavn etternavn")
    }

    @Test
    internal fun `gjeldende navn skal hente navnet som ble sist registrert`() {
        val gjeldendeNavn = listOf(Navn("b", null, "b", metadata = Metadata(historisk = false))).gjeldende().fornavn
        assertThat(gjeldendeNavn).isEqualTo("b")
    }

    @Test
    internal fun `skal finne riktig gjeldende bostedsadresse`() {
        val gjeldendeAdresse = vegadresse("Gjeldende gate", "12", false)
        val historiskeAdresser: List<Bostedsadresse> =
            listOf(
                vegadresse("Historisk gate", "15", true),
                vegadresse("Historisk gate", "13", true),
                vegadresse("Historisk gate", "1", true),
            )
        val adresser: List<Bostedsadresse> = historiskeAdresser + gjeldendeAdresse

        assertThat(adresser.gjeldende()!!.vegadresse!!.adressenavn).isEqualTo(gjeldendeAdresse.vegadresse!!.adressenavn)
        assertThat(adresser.gjeldende()!!.vegadresse!!.husnummer).isEqualTo(gjeldendeAdresse.vegadresse!!.husnummer)

        assertThat(historiskeAdresser.gjeldende()).isNull()
    }

    @Test
    internal fun `skal finne riktig gjeldende oppholdsadresse`() {
        val gjeldendeAdresse = oppholdsadresse("Gjeldende gate", "12", false)
        val historiskeAdresser: List<Oppholdsadresse> =
            listOf(
                oppholdsadresse("Historisk gate", "15", true),
                oppholdsadresse("Historisk gate", "13", true),
                oppholdsadresse("Historisk gate", "1", true),
            )

        val adresser = historiskeAdresser + gjeldendeAdresse

        assertThat(adresser.gjeldende()!!.vegadresse!!.adressenavn).isEqualTo(gjeldendeAdresse.vegadresse!!.adressenavn)
        assertThat(adresser.gjeldende()!!.vegadresse!!.husnummer).isEqualTo(gjeldendeAdresse.vegadresse!!.husnummer)

        assertThat(historiskeAdresser.gjeldende()).isNull()
    }

    private fun vegadresse(
        gate: String,
        nr: String,
        historisk: Boolean,
    ): Bostedsadresse =
        Bostedsadresse(
            null,
            null,
            null,
            null,
            null,
            Vegadresse(nr, null, null, gate, null, null, null, null),
            null,
            null,
            Metadata(historisk),
        )

    private fun oppholdsadresse(
        gate: String,
        nr: String,
        historisk: Boolean,
    ): Oppholdsadresse =
        Oppholdsadresse(
            null,
            null,
            null,
            null,
            Vegadresse(nr, null, null, gate, null, null, null, null),
            null,
            Metadata(historisk),
        )
}
