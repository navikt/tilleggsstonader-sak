package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
data class RolleConfig(
    @Value("\${rolle.beslutter}")
    val beslutterRolle: String,
    @Value("\${rolle.saksbehandler}")
    val saksbehandlerRolle: String,
    @Value("\${rolle.veileder}")
    val veilederRolle: String,
    @Value("\${rolle.kode6}")
    val kode6: String,
    @Value("\${rolle.kode7}")
    val kode7: String,
    @Value("\${rolle.egenAnsatt}")
    val egenAnsatt: String,
    @Value("\${rolle.prosessering}")
    val prosessering: String,
) {
    val rollerMedBeskrivelse: AdRoller by lazy {
        AdRoller(
            beslutter = AdRolle(rolleId = beslutterRolle, beskrivelse = "Beslutter"),
            saksbehandler = AdRolle(rolleId = saksbehandlerRolle, beskrivelse = "Saksbehandler"),
            veileder = AdRolle(rolleId = veilederRolle, beskrivelse = "Veileder"),
            kode6 = AdRolle(rolleId = kode6, beskrivelse = "Strengt fortrolig adresse"),
            kode7 = AdRolle(rolleId = kode7, beskrivelse = "Fortrolig adresse"),
            egenAnsatt = AdRolle(rolleId = egenAnsatt, beskrivelse = "NAV-ansatt"),
        )
    }
}

data class AdRoller(
    val beslutter: AdRolle,
    val saksbehandler: AdRolle,
    val veileder: AdRolle,
    val kode6: AdRolle,
    val kode7: AdRolle,
    val egenAnsatt: AdRolle,
)

data class AdRolle(val rolleId: String, val beskrivelse: String)
