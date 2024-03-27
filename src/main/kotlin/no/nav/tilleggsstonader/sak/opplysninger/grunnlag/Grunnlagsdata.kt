package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import java.util.UUID


// TODO tabell
data class Grunnlagsdata(
    @Id
    val behandlingId: UUID,
    val grunnlag: Grunnlag,
    val sporbar: Sporbar = Sporbar(),
)

/**
 * Info om annen forelder?
 * dødsfall
 * adressebeskyttelse
 * fullmakt
 * vergemaalEllerFremtidsfullmakt
 * folkeregisterpersonstatus
 * opphold?
 * innflyttingTilNorge?
 * utflyttingFraNorge?
 */
data class Grunnlag(
    val navn: Navn,
    val barn: List<GrunnlagBarn>,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)
