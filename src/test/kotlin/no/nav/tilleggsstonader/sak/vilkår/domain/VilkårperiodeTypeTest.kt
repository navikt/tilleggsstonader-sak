package no.nav.tilleggsstonader.sak.vilkår.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VilkårperiodeTypeTest {

    @Test
    fun `vilkårperiodeType må inneholde unike navn`() {
        assertThat(vilkårperiodetyper.keys).hasSize(MålgruppeType.entries.size + AktivitetType.entries.size)
    }
}
