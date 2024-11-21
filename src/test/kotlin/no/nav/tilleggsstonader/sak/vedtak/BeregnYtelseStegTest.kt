package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BeregnYtelseStegTest: IntegrationTest() {

    @Autowired
    lateinit var beregnYtelseSteg: List<BeregnYtelseSteg<*>>

    @Test
    fun `kan maks ha et BeregnYtelseSteg per stønadstype`() {
        val stønaderMedFlereSteg = beregnYtelseSteg.groupBy { it.stønadstype }
            .filter { it.value.size > 1 }
        assertThat(stønaderMedFlereSteg).isEmpty()
    }
}