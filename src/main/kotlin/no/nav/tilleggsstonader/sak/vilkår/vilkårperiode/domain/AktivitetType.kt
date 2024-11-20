package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

enum class AktivitetType : VilkårperiodeType {
    TILTAK,
    UTDANNING,
    REELL_ARBEIDSSØKER,
    INGEN_AKTIVITET,
    ;

    override fun tilDbType(): String = this.name

    override fun girIkkeRettPåStønadsperiode() =
        this == INGEN_AKTIVITET
}
