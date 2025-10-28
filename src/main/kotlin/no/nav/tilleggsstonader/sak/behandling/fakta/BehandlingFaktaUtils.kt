package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Personopplysninger

fun mapPersonopplysninger(personopplysninger: Personopplysninger?): FaktaPersonopplysninger =
    FaktaPersonopplysninger(
        søknadsgrunnlag =
            personopplysninger?.adresse?.let { adresse ->
                FaktaPersonopplysningerSøknadsgrunnlag(
                    adresse =
                        listOfNotNull(adresse.adresse, adresse.postnummer, adresse.poststed)
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(", "),
                )
            },
    )
