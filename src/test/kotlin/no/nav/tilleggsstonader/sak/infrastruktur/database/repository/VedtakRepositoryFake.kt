package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak

class VedtakRepositoryFake : VedtakRepository, DummyRepository<Vedtak, BehandlingId>({ it.behandlingId })
