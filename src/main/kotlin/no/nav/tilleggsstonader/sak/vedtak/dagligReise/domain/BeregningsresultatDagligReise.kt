package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

data class BeregningsresultatDagligReise(
    val offentligTransport: BeregningsresultatOffentligTransport?,
    val privatBil: BeregningsresultatPrivatBil?,
){
    /**
     * TODO: Må oppdateres
     * Skal returnere false dersom det kun er rammevedtak som er beregnet
     */
    fun førerTilUtbetaling(): Boolean {
        if (offentligTransport != null) return true

        return false
    }
}
