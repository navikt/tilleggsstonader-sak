package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

@JsonDeserialize(using = TilsynBarnBeregningObjektDeserializer::class)
interface TilsynBarnBeregningObjekt :
    Periode<LocalDate>,
    KopierPeriode<TilsynBarnBeregningObjekt> {
    val målgruppe: MålgruppeType
    val aktivitet: AktivitetType
}

// TODO denne funker kun i tester ikke på ordentlig
class TilsynBarnBeregningObjektDeserializer : JsonDeserializer<StønadsperiodeBeregningsgrunnlag>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): StønadsperiodeBeregningsgrunnlag {
        val node: JsonNode = p.codec.readTree(p)

        val fom = LocalDate.parse(node["fom"].asText())
        val tom = LocalDate.parse(node["tom"].asText())
        val målgruppe = MålgruppeType.valueOf(node["målgruppe"].asText()) // Assuming enum
        val aktivitet = AktivitetType.valueOf(node["aktivitet"].asText()) // Assuming enum

        return StønadsperiodeBeregningsgrunnlag(fom, tom, målgruppe, aktivitet)
    }
}
