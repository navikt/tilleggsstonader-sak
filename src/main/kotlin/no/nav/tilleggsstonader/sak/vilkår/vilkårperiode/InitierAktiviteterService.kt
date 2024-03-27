package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.AktivitetClient
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeUtil.finnPerioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeData
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class InitierAktiviteterService(
    private val behandlingService: BehandlingService,
    private val aktivitetClient: AktivitetClient,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
) {

    @Transactional
    fun initierAktiviteter(behandlingId: UUID) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        initierAktiviteter(behandling)
    }

    private fun initierAktiviteter(
        behandling: Saksbehandling,
    ) {
        val behandlingId = behandling.id
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke inititere aktiviteter når behandling=$behandlingId er låst"
        }
        validerIkkeFinnesAktiviteterFraFør(behandlingId)
        val aktiviteter = hentOgMapAktiviteter(behandling, behandlingId)
        vilkårperiodeRepository.insertAll(aktiviteter)
    }

    private fun hentOgMapAktiviteter(
        behandling: Saksbehandling,
        behandlingId: UUID,
    ): List<Vilkårperiode> {
        val aktiviteter = aktivitetClient.hentAKtiviteter(
            ident = behandling.ident,
            fom = LocalDate.now().minusYears(1),
            tom = LocalDate.now().plusYears(1),
        )

        return aktiviteter.mapNotNull { tilVilkårperiode(behandlingId, it) }
    }

    private fun tilVilkårperiode(
        behandlingId: UUID,
        dto: AktivitetArenaDto,
    ): Vilkårperiode? {
        val fom = dto.fom ?: return null
        return Vilkårperiode(
            behandlingId = behandlingId,
            kilde = kilde(dto),
            fom = fom,
            tom = dto.tom ?: fom.plusMonths(2),
            type = type(dto),
            delvilkår = ikkeVurdertDelvilkår(),
            begrunnelse = null,
            resultat = ResultatVilkårperiode.IKKE_VURDERT,
            aktivitetsdager = dto.antallDagerPerUke,
            kildeData = kildeData(dto),
        )
    }

    private fun kildeData(dto: AktivitetArenaDto) = KildeData(
        id = dto.id,
        fom = dto.fom,
        tom = dto.tom,
        type = dto.type,
        typeNavn = dto.typeNavn,
        status = dto.status?.name,
        statusArena = dto.statusArena,
        erStønadsberettiget = dto.erStønadsberettiget,
        gjelderUtdanning = dto.erUtdanning,
        arrangør = dto.arrangør,
        antallDagerPerUke = dto.antallDagerPerUke,
    )

    private fun type(it: AktivitetArenaDto) =
        if (it.erUtdanning == true) {
            AktivitetType.UTDANNING
        } else {
            AktivitetType.TILTAK
        }

    private fun kilde(it: AktivitetArenaDto) = when (it.kilde) {
        Kilde.ARENA -> KildeVilkårsperiode.SYSTEM
    }

    // TODO burde DelvilkårAktivitet være nullable?
    private fun ikkeVurdertDelvilkår() = DelvilkårAktivitet(
        lønnet = DelvilkårVilkårperiode.Vurdering(
            svar = null,
            begrunnelse = null,
            resultat = ResultatDelvilkårperiode.IKKE_VURDERT,
        ),
        mottarSykepenger = DelvilkårVilkårperiode.Vurdering(
            svar = null,
            begrunnelse = null,
            resultat = ResultatDelvilkårperiode.IKKE_VURDERT,
        ),
    )

    private fun validerIkkeFinnesAktiviteterFraFør(behandlingId: UUID) {
        val vilkårperioder = vilkårperiodeRepository.findByBehandlingId(behandlingId)
        val aktiviteter = finnPerioder<AktivitetType>(vilkårperioder)
        feilHvis(aktiviteter.isNotEmpty()) {
            "Kan ikke initiere aktiviteter for behandling=$behandlingId når det allerede finnes aktiviteter"
        }
    }
}
