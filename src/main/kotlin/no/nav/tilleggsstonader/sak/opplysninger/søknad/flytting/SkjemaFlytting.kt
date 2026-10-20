package no.nav.tilleggsstonader.sak.opplysninger.søknad.flytting

import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FlyttingDetaljer
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt

data class SkjemaFlytting(
    val hovedytelse: HovedytelseAvsnitt,
    val flyttingDetaljer: FlyttingDetaljer?,
    val dokumentasjon: List<Dokumentasjon>,
)
