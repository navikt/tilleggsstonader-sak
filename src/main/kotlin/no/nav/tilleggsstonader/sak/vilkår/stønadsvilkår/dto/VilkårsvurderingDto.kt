package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import java.time.LocalDate
import java.util.UUID

data class VilkårsvurderingDto(
    val vilkårsett: List<VilkårDto>,
    val grunnlag: VilkårGrunnlagDto,
)

data class VilkårGrunnlagDto(
    val hovedytelse: GrunnlagHovedytelse,
    val aktivitet: GrunnlagAktivitet,
    val barn: List<GrunnlagBarn>,
)

data class GrunnlagHovedytelse(
    val søknadsgrunnlag: SøknadsgrunnlagHovedytelse?,
)

data class SøknadsgrunnlagHovedytelse(
    val hovedytelse: Hovedytelse,
)

data class GrunnlagAktivitet(
    val søknadsgrunnlag: SøknadsgrunnlagAktivitet?,
)

data class SøknadsgrunnlagAktivitet(
    val utdanning: JaNei,
)

data class GrunnlagBarn(
    val ident: String,
    val barnId: UUID,
    val registergrunnlag: RegistergrunnlagBarn,
    val søknadgrunnlag: SøknadsgrunnlagBarn?,
)

data class RegistergrunnlagBarn(
    val navn: String,
    val dødsdato: LocalDate?,
)

data class SøknadsgrunnlagBarn(
    val type: TypeBarnepass,
    val startetIFemte: JaNei?,
    val årsak: ÅrsakBarnepass?,
)
