package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

data class BeregningsresultatDagligReise(
    val offentligTransport: BeregningsresultatOffentligTransport?,
    // val privatBil: BeregningsresultatPrivatBil?,
) {
    fun sorterReiser(): BeregningsresultatDagligReise {
        val sortedOffentligTransport =
            offentligTransport?.copy(
                reiser =
                    offentligTransport.reiser
                        .sortedBy { reise -> reise.perioder.minOf { it.grunnlag.fom } }
                        .map { reise ->
                            reise.copy(perioder = reise.perioder.sortedBy { it.grunnlag.fom })
                        },
            )
        return this.copy(offentligTransport = sortedOffentligTransport)
    }
}
