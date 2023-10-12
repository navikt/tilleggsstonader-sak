package no.nav.tilleggsstonader.sak.cucumber

import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Så
import org.assertj.core.api.Assertions.assertThat

class StepDefinitions {
    var faktiskTall: Int? = null

    @Gitt("tallet {int}")
    fun noe(tall: Int) {
        faktiskTall = tall
    }

    @Så("forvent er lik {int}")
    fun `så skal dette virke`(forventet: Int) {
        assertThat(faktiskTall).isEqualTo(forventet)
    }
}
