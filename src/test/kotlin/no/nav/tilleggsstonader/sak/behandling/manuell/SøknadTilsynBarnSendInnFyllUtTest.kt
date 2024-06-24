package no.nav.tilleggsstonader.sak.behandling.manuell

import no.nav.tilleggsstonader.sak.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SøknadTilsynBarnSendInnFyllUtTest {

    @Test
    fun `skal kunne parsea informasjon om søker og barn fra søknad med 2 barn`() {
        val data = FileUtil.readFile("fyllut-sendinn/søknad-2-barn.xml").toByteArray()

        val søknadsinformasjon = SøknadTilsynBarnSendInnFyllUtUtil.parseInfoFraSøknad(data)

        assertThat(søknadsinformasjon.ident).isEqualTo("13518741815")
        assertThat(søknadsinformasjon.identerBarn)
            .containsExactlyInAnyOrder("02501961038", "27412174017")
    }

    @Test
    fun `skal kunne parsea informasjon om søker og barn fra søknad med 1 barn`() {
        val data = FileUtil.readFile("fyllut-sendinn/søknad-1-barn.xml").toByteArray()

        val søknadsinformasjon = SøknadTilsynBarnSendInnFyllUtUtil.parseInfoFraSøknad(data)

        assertThat(søknadsinformasjon.ident).isEqualTo("13518741815")
        assertThat(søknadsinformasjon.identerBarn).containsExactly("02501961038")
    }
}
