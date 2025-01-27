package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import java.util.UUID

class TilkjentYtelseRepositoryFake : TilkjentYtelseRepository, DummyRepository<TilkjentYtelse, UUID>({ it.id }) {

    override fun insert(t: TilkjentYtelse): TilkjentYtelse {
        require(findAll().none { it.behandlingId == t.behandlingId }) {
            "Finnes allerede en med behandlingId"
        }
        return super.insert(t)
    }

    override fun findByBehandlingId(behandlingId: BehandlingId): TilkjentYtelse? {
        return findAll().singleOrNull { it.behandlingId == behandlingId }
    }

    override fun findByBehandlingIdForUpdate(behandlingId: BehandlingId): TilkjentYtelse? {
        return findByBehandlingId(behandlingId)
    }
}
