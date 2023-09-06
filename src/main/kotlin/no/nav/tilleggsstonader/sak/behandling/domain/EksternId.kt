package no.nav.tilleggsstonader.sak.behandling.domain

import java.util.UUID

data class EksternId(val behandlingId: UUID, val eksternBehandlingId: Long, val eksternFagsakId: Long)
