package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import java.time.LocalDate
import java.util.UUID

data class BehandlingFaktaDto(
    val hovedytelse: FaktaHovedytelse,
    val aktivitet: FaktaAktivtet,
    val barn: List<FaktaBarn>,
)

data class FaktaHovedytelse(
    val søknadsgrunnlag: SøknadsgrunnlagHovedytelse?,
)

data class SøknadsgrunnlagHovedytelse(
    val hovedytelse: List<Hovedytelse>,
    val boddSammenhengende: JaNei?,
    val planleggerBoINorgeNeste12mnd: JaNei?
)

data class FaktaAktivtet(
    val søknadsgrunnlag: SøknadsgrunnlagAktivitet?,
)

data class SøknadsgrunnlagAktivitet(
    val utdanning: JaNei,
)

data class FaktaBarn(
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
