package no.nav.tilleggsstonader.sak.util

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.data.repository.CrudRepository
import java.util.Optional
import java.util.UUID

object RepositoryMockUtil {

    inline fun <reified REPO, reified TYPE : Any, reified ID> mockRepository(
        crossinline getId: (TYPE) -> ID,
    ): REPO where REPO : CrudRepository<TYPE, ID>, REPO : InsertUpdateRepository<TYPE> {
        val map: MutableMap<ID, TYPE> = mutableMapOf()
        val mockk = mockk<REPO>()
        every { mockk.findById(any()) } answers {
            val verdi = map[firstArg()]
            Optional.ofNullable(verdi)
        }
        every { mockk.insert(any()) } answers {
            val firstArg = firstArg<TYPE>()
            map[getId(firstArg)] = firstArg
            firstArg
        }
        every { mockk.deleteAll() } answers { map.clear() }
        every { mockk.deleteById(any()) } answers { map.remove(firstArg()) }
        every { mockk.findAll() } answers { map.values }
        return mockk
    }

    fun mockVilkårperiodeRepository() =
        mockRepository<VilkårperiodeRepository, Vilkårperiode, UUID> { it.id }.apply {
            val repo = this
            every { repo.findByBehandlingId(any()) } answers {
                findAll().filter { it.behandlingId.id == firstArg() }
            }
            every { repo.findByBehandlingIdAndResultat(any(), any()) } answers {
                findAll().filter { it.behandlingId.id == firstArg() && it.resultat == secondArg() }
            }
        }

    fun mockStønadsperiodeRepository() =
        mockRepository<StønadsperiodeRepository, Stønadsperiode, UUID> { it.id }.apply {
            val repo = this
            every { repo.findAllByBehandlingId(any()) } answers {
                findAll().filter { it.behandlingId.id == firstArg() }
            }
        }

    fun mockVedtakRepository() = mockRepository<VedtakRepository, Vedtak, BehandlingId> { it.behandlingId }.apply {
        val repo = this
        /*every { repo.findById(any()) } answers {
            findAll().singleOrNull { it.behandlingId.id == firstArg() }
        }*/
    }

    fun mockTilkjentYtelseRepository() =
        mockRepository<TilkjentYtelseRepository, TilkjentYtelse, UUID> { it.id }.apply {
            val repo = this
            every { repo.findByBehandlingId(any()) } answers {
                findAll().singleOrNull { it.behandlingId.id == firstArg() }
            }
        }
}
