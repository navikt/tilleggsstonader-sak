package no.nav.tilleggsstonader.sak.oppfølging.domain

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaProsent
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

data class OppfølgingInngangsvilkårAktivitet(
    val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val prosent: Int?,
    val antallDager: Int?,
    val kildeId: String?,
    val registerAktivitet: AktivitetArenaDto?,
) : Periode<LocalDate> {
    init {
        fun <T> diff(
            type: String,
            vilkårVerdi: T,
            registerVerdi: T,
        ): String? =
            if (vilkårVerdi != registerVerdi) {
                "$type=$vilkårVerdi ${type}Register=$registerVerdi"
            } else {
                null
            }
        if (registerAktivitet != null) {
            val fomDiff = diff("fom", fom, registerAktivitet.fom)
            val tomDiff = diff("tom", tom, registerAktivitet.tom)
            val prosentDiff =
                diff("prosent", prosent, registerAktivitet.prosentDeltakelse?.toInt()).takeIf { prosent != null }
            val antallDagerDiff =
                diff("dager", antallDager, registerAktivitet.antallDagerPerUke).takeIf { antallDager != null }
            val diff = listOfNotNull(fomDiff, tomDiff, prosentDiff, antallDagerDiff)
            if (diff.isNotEmpty()) {
                logger.info("Diff vilkår=$id vs register ${diff.joinToString(" ")}")
            }
        }
    }

    val datoperiodeAktivitet: Datoperiode? by lazy {
        val fom = registerAktivitet?.fom
        val tom = registerAktivitet?.tom
        if (fom != null && tom != null) {
            Datoperiode(fom = fom, tom = tom)
        } else {
            null
        }
    }

    constructor(
        vilkårperiode: GeneriskVilkårperiode<AktivitetFaktaOgVurdering>,
        registerAktivitet: AktivitetArenaDto?,
    ) : this(
        id = vilkårperiode.id,
        fom = vilkårperiode.fom,
        tom = vilkårperiode.tom,
        aktivitet = vilkårperiode.faktaOgVurdering.type.vilkårperiodeType,
        prosent =
            vilkårperiode.faktaOgVurdering.fakta
                .takeIfFakta<FaktaProsent>()
                ?.prosent,
        antallDager =
            vilkårperiode.faktaOgVurdering.fakta
                .takeIfFakta<FaktaAktivitetsdager>()
                ?.aktivitetsdager,
        kildeId = vilkårperiode.kildeId,
        registerAktivitet = registerAktivitet,
    )

    companion object {
        private val logger = LoggerFactory.getLogger(OppfølgingInngangsvilkårAktivitet::class.java)

        fun fraVilkårperioder(
            vilkårperioder: List<Vilkårperiode>,
            registerAktiviteter: OppfølgingRegisterAktiviteter,
        ): List<OppfølgingInngangsvilkårAktivitet> =
            vilkårperioder
                .ofType<AktivitetFaktaOgVurdering>()
                .map { vilkår -> OppfølgingInngangsvilkårAktivitet(vilkår, registerAktiviteter.forId(vilkår.kildeId)) }
                .sorted()
    }
}
