package no.nav.tilleggsstonader.sak.vilkår.regler

import no.nav.tilleggsstonader.sak.infrastruktur.config.ObjectMapperProvider
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
internal class GrafRendererTest {

    private val objectMapper = ObjectMapperProvider.objectMapper.writerWithDefaultPrettyPrinter()

    @Test
    internal fun `print alle vilkår`() {
        val vilkårsregler = Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.map {
            val regler = it.value.regler
            mapOf(
                "name" to it.key,
                "children" to it.value.hovedregler.map { regelId -> mapSpørsmål(regler, regelId) },
            )
        }
        println(
            objectMapper.writeValueAsString(
                mapOf(
                    "name" to "vilkår",
                    "children" to vilkårsregler.toList(),
                ),
            ),
        )
    }

    /**
     * Brukes kun til å rendere grafdata for d3
     */
    data class Spørsmål(
        val name: RegelId,
        val children: List<Svar>,
    ) {

        val type = "spørsmål"
    }

    data class Svar(
        val name: SvarId,
        val begrunnelseType: BegrunnelseType,
        val children: List<Spørsmål>,
        val resultat: Vilkårsresultat? = null,
    ) {

        val type = "svar"
    }

    private fun mapSvar(regler: Map<RegelId, RegelSteg>, svarMapping: Map<SvarId, SvarRegel>): List<Svar> {
        return svarMapping.map {
            try {
                val value = it.value
                if (value is SluttSvarRegel) {
                    Svar(it.key, value.begrunnelseType, emptyList(), value.resultat.vilkårsresultat)
                } else {
                    Svar(it.key, value.begrunnelseType, listOf(mapSpørsmål(regler, value.regelId)))
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private fun mapSpørsmål(regler: Map<RegelId, RegelSteg>, regelId: RegelId): Spørsmål {
        val svarMapping = regler[regelId]!!.svarMapping
        return Spørsmål(regelId, mapSvar(regler, svarMapping))
    }
}
