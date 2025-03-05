package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

sealed interface Vurdering {
    val svar: SvarJaNei?
    val resultat: ResultatDelvilkårperiode
}

enum class SvarJaNei {
    JA,
    JA_IMPLISITT,
    NEI,
    ;

    fun harVurdert(): Boolean = this != JA_IMPLISITT
}

enum class ResultatDelvilkårperiode {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
    IKKE_AKTUELT,
}

data class VurderingLønnet private constructor(
    override val svar: SvarJaNei?,
    override val resultat: ResultatDelvilkårperiode,
) : Vurdering {
    constructor(svar: SvarJaNei?) : this(svar, utledResultat(svar))

    companion object {
        private fun utledResultat(svar: SvarJaNei?): ResultatDelvilkårperiode =
            when (svar) {
                null -> ResultatDelvilkårperiode.IKKE_VURDERT
                SvarJaNei.JA -> ResultatDelvilkårperiode.IKKE_OPPFYLT
                SvarJaNei.NEI -> ResultatDelvilkårperiode.OPPFYLT
                SvarJaNei.JA_IMPLISITT -> error("$svar er ugyldig for ${VurderingLønnet::class.simpleName}")
            }
    }
}

data class VurderingHarUtgifter private constructor(
    override val svar: SvarJaNei?,
    override val resultat: ResultatDelvilkårperiode,
) : Vurdering {
    constructor(svar: SvarJaNei?) : this(svar, utledResultat(svar))

    companion object {
        private fun utledResultat(svar: SvarJaNei?): ResultatDelvilkårperiode =
            when (svar) {
                null -> ResultatDelvilkårperiode.IKKE_VURDERT
                SvarJaNei.JA -> ResultatDelvilkårperiode.OPPFYLT
                SvarJaNei.NEI -> ResultatDelvilkårperiode.IKKE_OPPFYLT
                SvarJaNei.JA_IMPLISITT -> error("$svar er ugyldig for ${VurderingHarUtgifter::class.simpleName}")
            }
    }
}

data class VurderingHarRettTilUtstyrsstipend private constructor(
    override val svar: SvarJaNei?,
    override val resultat: ResultatDelvilkårperiode,
) : Vurdering {
    constructor(svar: SvarJaNei?) : this(svar, utledResultat(svar))

    companion object {
        private fun utledResultat(svar: SvarJaNei?): ResultatDelvilkårperiode =
            when (svar) {
                null -> ResultatDelvilkårperiode.IKKE_VURDERT
                SvarJaNei.JA -> ResultatDelvilkårperiode.IKKE_OPPFYLT
                SvarJaNei.NEI -> ResultatDelvilkårperiode.OPPFYLT
                SvarJaNei.JA_IMPLISITT -> error("$svar er ugyldig for ${VurderingHarUtgifter::class.simpleName}")
            }
    }
}

data class VurderingMedlemskap private constructor(
    override val svar: SvarJaNei?,
    override val resultat: ResultatDelvilkårperiode,
) : Vurdering {
    constructor(svar: SvarJaNei?) : this(svar, utledResultat(svar))

    companion object {
        private fun utledResultat(svar: SvarJaNei?): ResultatDelvilkårperiode =
            when (svar) {
                null -> ResultatDelvilkårperiode.IKKE_VURDERT
                SvarJaNei.JA,
                SvarJaNei.JA_IMPLISITT,
                -> ResultatDelvilkårperiode.OPPFYLT

                SvarJaNei.NEI -> ResultatDelvilkårperiode.IKKE_OPPFYLT
            }

        val IMPLISITT = VurderingMedlemskap(SvarJaNei.JA_IMPLISITT)
    }
}

data class VurderingDekketAvAnnetRegelverk private constructor(
    override val svar: SvarJaNei?,
    override val resultat: ResultatDelvilkårperiode,
) : Vurdering {
    constructor(svar: SvarJaNei?) : this(svar, utledResultat(svar))

    companion object {
        private fun utledResultat(svar: SvarJaNei?): ResultatDelvilkårperiode =
            when (svar) {
                null -> ResultatDelvilkårperiode.IKKE_VURDERT
                SvarJaNei.JA -> ResultatDelvilkårperiode.IKKE_OPPFYLT
                SvarJaNei.NEI -> ResultatDelvilkårperiode.OPPFYLT
                SvarJaNei.JA_IMPLISITT -> error("$svar er ugyldig for ${VurderingDekketAvAnnetRegelverk::class.simpleName}")
            }
    }
}
