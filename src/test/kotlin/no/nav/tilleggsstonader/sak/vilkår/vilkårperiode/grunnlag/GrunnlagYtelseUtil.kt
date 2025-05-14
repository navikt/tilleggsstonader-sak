package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserUtil.finnRelevanteYtelsesTyper

fun grunnlagYtelseOk(
    perioder: List<PeriodeGrunnlagYtelse>,
    stønad: Stønadstype = Stønadstype.BARNETILSYN,
) = GrunnlagYtelse(
    perioder = perioder,
    kildeResultat =
        finnRelevanteYtelsesTyper(stønad).map {
            GrunnlagYtelse.KildeResultatYtelse(it, ResultatKilde.OK)
        },
)
