package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

data class BarnDvh(
    val fnr: String,
) {
    data class JsonWrapper(
        val barn: List<BarnDvh>,
    )

    companion object {
        fun fraDomene(barn: List<String>) =
            JsonWrapper(
                barn = barn.map { BarnDvh(it) },
            )
    }
}
