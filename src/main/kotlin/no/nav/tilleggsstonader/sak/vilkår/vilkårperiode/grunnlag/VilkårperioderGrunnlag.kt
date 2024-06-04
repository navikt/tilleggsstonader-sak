package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

data class VilkårperioderGrunnlag(
    val aktivitet: GrunnlagAktivitet,
)

@Table("vilkarperioder_grunnlag")
data class VilkårperioderGrunnlagDomain(
    @Id
    val behandlingId: UUID,
    val grunnlag: VilkårperioderGrunnlag,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class GrunnlagAktivitet(
    val aktiviteter: List<AktivitetArenaDto>,
    val tidspunktHentet: LocalDateTime,
)