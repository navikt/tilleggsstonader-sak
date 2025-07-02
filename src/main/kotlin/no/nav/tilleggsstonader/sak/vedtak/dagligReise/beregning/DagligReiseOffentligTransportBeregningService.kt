package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate

@Service
class DagligReiseOffentligTransportBeregningService {
    fun beregn(beregningsInputOffentligTransport: BeregningsInputOffentligTransport): Int {
        val fom = beregningsInputOffentligTransport.fom
        val tom = beregningsInputOffentligTransport.tom
        val prisEnkelBilett = beregningsInputOffentligTransport.prisEnkelBilett

        val antallDagerMellomFomOgTom = Duration.between(fom.atStartOfDay(), tom.atStartOfDay()).toDays().toInt()
        // Antar alltid reiser 7 dager i uka

        // Antar kun enkelbilett er tilgjenglig
        return prisKunEnkelbilett(antallDagerMellomFomOgTom, prisEnkelBilett)
    }

    fun prisKunEnkelbilett(
        antallDager: Int,
        prisEnkelBilett: Int,
    ): Int = antallDager * prisEnkelBilett
}

data class BeregningsInputOffentligTransport(
    val fom: LocalDate,
    val tom: LocalDate,
//    val antallReisedagerPerUke: Int,
    val prisEnkelBilett: Int,
//    val pris30dagersBilett: Int,
//    val pris7dagersBilett: Int,
)
