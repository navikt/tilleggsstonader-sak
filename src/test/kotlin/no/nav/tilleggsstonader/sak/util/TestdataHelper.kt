package no.nav.tilleggsstonader.sak.util

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.skjema.SkjemaBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.skjema.SøknadsskjemaBarnetilsyn
import java.time.LocalDateTime
import java.util.UUID

fun søknad(
    datoMottatt: LocalDateTime = LocalDateTime.now(),
    barn: List<SkjemaBarn> = mockk(),
) =
    SøknadsskjemaBarnetilsyn(
        datoMottatt = datoMottatt,
        barn = barn,
    )

fun søknadBarnTilBehandlingBarn(barn: Set<SøknadBarn>, behandlingId: UUID = UUID.randomUUID()): List<BehandlingBarn> =
    barn.map {
        it.tilBehandlingBarn(behandlingId)
    }

fun SøknadBarn.tilBehandlingBarn(behandlingId: UUID) = BehandlingBarn(
    behandlingId = behandlingId,
    søknadBarnId = this.id,
    personIdent = this.fødselsnummer,
    navn = this.navn,
)
/*
fun BarnMedIdent.tilBehandlingBarn(behandlingId: UUID) = BehandlingBarn(
    behandlingId = behandlingId,
    søknadBarnId = null,
    personIdent = this.personIdent,
    navn = this.navn.visningsnavn(),
    fødselTermindato = null,
)
*/
