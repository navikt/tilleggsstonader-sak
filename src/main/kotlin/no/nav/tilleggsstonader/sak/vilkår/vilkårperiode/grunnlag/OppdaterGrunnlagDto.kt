package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import java.time.LocalDate

/**
 * @param hentFom brukes for å overskrive dato som vi henter grunnlag fra i en førstegangsbehandling
 * I en revurdering vil vi bruke revurder-fra-datoet
 */
data class OppdaterGrunnlagDto(
    val hentFom: LocalDate? = null,
)
