package no.nav.tilleggsstonader.sak.opplysninger.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PdlDtoTest {

    @Test
    fun `pdlSøkerData inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/søker.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlSøkerData)!!

        assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }

    @Test
    fun `pdlBarnData inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/barn.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlBarnData)!!

        assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }

    @Test
    fun `pdlAnnenForelderData inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/andreForeldre.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlAnnenForelderData)!!

        assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }

    @Test
    fun `pdlPersonKortBolk inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/person_kort_bolk.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlPersonKortBolk)!!

        assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }

    @Test
    fun `pdlPersonSok inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/søk_person.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlPersonSøk)!!

        assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }
}
