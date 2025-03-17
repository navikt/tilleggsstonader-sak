package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.EnumUtil.enumName
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksdataFilesUtil.tilTypeVedtaksdataSuffix
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Denne må fylles på etterhvert som man legger til flere enums for andre stønader
 */
val alleEnumTypeVedtaksdata: List<Pair<Stønadstype, TypeVedtaksdata>> =
    listOf(
        Stønadstype.BARNETILSYN to TypeVedtakTilsynBarn.entries,
        Stønadstype.LÆREMIDLER to TypeVedtakLæremidler.entries,
        Stønadstype.BOUTGIFTER to TypeVedtakBoutgifter.entries,
    ).flatMap { (stønadstype, enums) -> enums.map { stønadstype to it } }

class VedtaksdataTest {
    /**
     * Hvis [when] mangler noe, legg til der der og i [alleEnumTypeVedtaksdata]
     */
    @Test
    fun `sjekk at alle har riktig navn`() {
        alleEnumTypeVedtaksdata.forEach { (stønadstype, type) ->
            when (type) {
                is TypeVedtakTilsynBarn -> type.assertHarRiktigNavn(stønadstype)
                is TypeVedtakLæremidler -> type.assertHarRiktigNavn(stønadstype)
                is TypeVedtakBoutgifter -> type.assertHarRiktigNavn(stønadstype)
            }
        }
    }

    @Test
    fun `sjekker at det feiler hvis man bruker feil stønadstype på tilsyn barn`() {
        assertThatThrownBy {
            alleEnumTypeVedtaksdata.forEach { (_, type) ->
                if (type == TypeVedtakTilsynBarn.INNVILGELSE_TILSYN_BARN) {
                    type.assertHarRiktigNavn(Stønadstype.LÆREMIDLER)
                }
            }
        }.hasMessageContaining("but was: \"INNVILGELSE_TILSYN_BARN\"")
    }

    private fun TypeVedtaksdata.assertHarRiktigNavn(stønadstype: Stønadstype) {
        assertThat(this.enumName())
            .isEqualTo("${this.typeVedtak.enumName()}_${stønadstype.tilTypeVedtaksdataSuffix()}")
    }
}
