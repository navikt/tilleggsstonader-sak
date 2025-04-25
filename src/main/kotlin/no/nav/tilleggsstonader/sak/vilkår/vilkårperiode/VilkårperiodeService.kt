package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagPersonopplysninger
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FødselFaktaGrunnlag
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.validerKanLeggeTilMålgruppeManuelt
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerAtAldersvilkårErGyldig
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerAtKunTomErEndret
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerAtVilkårperiodeKanOppdateresIRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerRevurderFraErSatt
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerSlettPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mapFaktaOgSvarDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.tilDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class VilkårperiodeService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository,
    private val vilkårperiodeGrunnlagService: VilkårperiodeGrunnlagService,
    private val faktaGrunnlagService: FaktaGrunnlagService,
) {
    fun hentVilkårperioder(behandlingId: BehandlingId): Vilkårperioder {
        val vilkårsperioder = vilkårperiodeRepository.findByBehandlingId(behandlingId).sorted()

        return Vilkårperioder(
            målgrupper = vilkårsperioder.ofType<MålgruppeFaktaOgVurdering>(),
            aktiviteter = vilkårsperioder.ofType<AktivitetFaktaOgVurdering>(),
        )
    }

    fun hentVilkårperioderResponse(behandlingId: BehandlingId): VilkårperioderResponse {
        val vilkårperioder = hentVilkårperioder(behandlingId)
        val grunnlagsdataVilkårsperioder =
            vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandlingId, vilkårperioder)

        return VilkårperioderResponse(
            vilkårperioder = vilkårperioder.tilDto(),
            grunnlag = grunnlagsdataVilkårsperioder?.tilDto(),
        )
    }

    @Transactional
    fun opprettVilkårperiode(vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val behandling = behandlingService.hentSaksbehandling(vilkårperiode.behandlingId)
        validerBehandling(behandling)
        validerNyPeriodeRevurdering(behandling, vilkårperiode.fom)

        if (vilkårperiode.type is MålgruppeType) {
            validerKanLeggeTilMålgruppeManuelt(behandling.stønadstype, vilkårperiode.type)
        }

        validerKildeIdFinnesIGrunnlaget(
            behandlingId = vilkårperiode.behandlingId,
            type = vilkårperiode.type,
            kildeId = vilkårperiode.kildeId,
        )

        val fødselFaktaGrunnlag =
            faktaGrunnlagService
                .hentEnkeltGrunnlag<FaktaGrunnlagPersonopplysninger>(behandling.id)
                .data.fødsel

        val faktaOgVurdering =
            mapFaktaOgSvarDto(
                stønadstype = behandling.stønadstype,
                vilkårperiode = vilkårperiode,
                fødselFaktaGrunnlag = fødselFaktaGrunnlag,
            )
        return vilkårperiodeRepository.insert(
            GeneriskVilkårperiode(
                behandlingId = vilkårperiode.behandlingId,
                resultat = faktaOgVurdering.utledResultat(),
                status = Vilkårstatus.NY,
                kildeId = vilkårperiode.kildeId,
                type = vilkårperiode.type,
                faktaOgVurdering = faktaOgVurdering,
                fom = vilkårperiode.fom,
                tom = vilkårperiode.tom,
                begrunnelse = vilkårperiode.begrunnelse,
                gitVersjon = Applikasjonsversjon.versjon,
            ),
        )
    }

    @Transactional
    fun oppdaterVilkårperiode(
        id: UUID,
        vilkårperiode: LagreVilkårperiode,
    ): Vilkårperiode {
        val eksisterendeVilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        val behandling = behandlingService.hentSaksbehandling(eksisterendeVilkårperiode.behandlingId)
        validerBehandlingIdErLik(vilkårperiode.behandlingId, eksisterendeVilkårperiode.behandlingId)
        validerBehandling(behandling)
        validerKildeIdOgType(vilkårperiode, eksisterendeVilkårperiode)

        val fødselFaktaGrunnlag =
            faktaGrunnlagService
                .hentEnkeltGrunnlag<FaktaGrunnlagPersonopplysninger>(behandling.id)
                .data.fødsel

        if (behandling.type != BehandlingType.REVURDERING) {
            return oppdaterVilkårperiodeHvorAltKanEndres(
                eksisterendeVilkårperiode,
                vilkårperiode,
                behandling,
                fødselFaktaGrunnlag,
            )
        }

        return oppdaterVilkårperiodeIRevurdering(
            vilkårperiode,
            eksisterendeVilkårperiode,
            behandling,
            fødselFaktaGrunnlag,
        )
    }

    private fun oppdaterVilkårperiodeIRevurdering(
        vilkårperiode: LagreVilkårperiode,
        eksisterendeVilkårperiode: Vilkårperiode,
        behandling: Saksbehandling,
        fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
    ): Vilkårperiode {
        val revurderFra = behandling.revurderFra
        validerRevurderFraErSatt(revurderFra)
        validerAtVilkårperiodeKanOppdateresIRevurdering(eksisterendeVilkårperiode, revurderFra)

        // Håndter vilkårsperioder hvor alle felter kan oppdateres
        if (eksisterendeVilkårperiode.fom >= revurderFra) {
            feilHvis(vilkårperiode.fom < revurderFra) {
                "Kan ikke sette fom før revurder-fra(${revurderFra.norskFormat()})"
            }
            return oppdaterVilkårperiodeHvorAltKanEndres(
                eksisterendeVilkårperiode,
                vilkårperiode,
                behandling,
                fødselFaktaGrunnlag,
            )
        }

        return oppdaterVilkårperiodeFørRevurderFra(
            vilkårperiode,
            eksisterendeVilkårperiode,
            revurderFra,
            fødselFaktaGrunnlag,
        )
    }

    /**
     * Håndterer alle perioder som enten er helt før eller krysser revurder fra datoen, dvs. perioder hvor kun tom kan endres.
     * Dersom perioden inneholder vurderinger med svar GAMMEL_MANGLER_DATA får man kun lov å korte ned tom.
     */
    private fun oppdaterVilkårperiodeFørRevurderFra(
        vilkårperiode: LagreVilkårperiode,
        eksisterendeVilkårperiode: Vilkårperiode,
        revurderFra: LocalDate,
        fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
    ): Vilkårperiode {
        val vurderingerMedGammelManglerData =
            eksisterendeVilkårperiode.faktaOgVurdering.vurderinger.vurderingerMedSvarGammelManglerData()
        val finnesGammelDataUtenomAldersVilkår = vurderingerMedGammelManglerData.any { it !is VurderingAldersVilkår }

        if (finnesGammelDataUtenomAldersVilkår) {
            brukerfeilHvis(vilkårperiode.tom > eksisterendeVilkårperiode.tom) {
                "Det har kommet nye vilkår som må vurderes, og denne perioden er derfor ikke mulig å forlenge. Hvis du ønsker å forlenge perioden må du legge til en ny periode."
            }
        }

        validerAtKunTomErEndret(eksisterendeVilkårperiode, vilkårperiode, revurderFra)
        validerAtAldersvilkårErGyldig(eksisterendeVilkårperiode, vilkårperiode, fødselFaktaGrunnlag)

        return vilkårperiodeRepository.update(eksisterendeVilkårperiode.medNyTom(tom = vilkårperiode.tom))
    }

    @Transactional
    fun oppdaterVilkårperiodeHvorAltKanEndres(
        eksisterendeVilkårperiode: Vilkårperiode,
        vilkårperiode: LagreVilkårperiode,
        behandling: Saksbehandling,
        fødselFaktaGrunnlag: FødselFaktaGrunnlag?,
    ): Vilkårperiode {
        val oppdatert =
            eksisterendeVilkårperiode.medVilkårOgVurdering(
                fom = vilkårperiode.fom,
                tom = vilkårperiode.tom,
                begrunnelse = vilkårperiode.begrunnelse,
                faktaOgVurdering =
                    mapFaktaOgSvarDto(
                        stønadstype = behandling.stønadstype,
                        vilkårperiode = vilkårperiode,
                        fødselFaktaGrunnlag = fødselFaktaGrunnlag,
                    ),
            )

        return vilkårperiodeRepository.update(
            oppdatert,
        )
    }

    fun slettVilkårperiode(
        id: UUID,
        slettVikårperiode: SlettVikårperiode,
    ): Vilkårperiode? {
        val vilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        validerBehandlingIdErLik(slettVikårperiode.behandlingId, vilkårperiode.behandlingId)

        val behandling = behandlingService.hentSaksbehandling(vilkårperiode.behandlingId)
        validerBehandling(behandling)
        validerSlettPeriodeRevurdering(behandling, vilkårperiode)

        if (vilkårperiode.kanSlettesPermanent()) {
            vilkårperiodeRepository.deleteById(vilkårperiode.id)
            return null
        } else {
            return vilkårperiodeRepository.update(vilkårperiode.markerSlettet(slettVikårperiode.kommentar))
        }
    }

    fun gjenbrukVilkårperioder(
        forrigeIverksatteBehandlingId: BehandlingId,
        nyBehandlingId: BehandlingId,
    ) {
        val eksisterendeVilkårperioder =
            vilkårperiodeRepository.findByBehandlingIdAndResultatNot(
                forrigeIverksatteBehandlingId,
                ResultatVilkårperiode.SLETTET,
            )

        val kopiertePerioderMedReferanse = eksisterendeVilkårperioder.map { it.kopierTilBehandling(nyBehandlingId) }
        vilkårperiodeRepository.insertAll(kopiertePerioderMedReferanse)
    }

    private fun validerKildeIdFinnesIGrunnlaget(
        behandlingId: BehandlingId,
        type: VilkårperiodeType,
        kildeId: String?,
    ) {
        val kildeId = kildeId ?: return

        feilHvis(type is MålgruppeType) {
            "Kan ikke sende inn kildeId på målgruppe, då målgruppeperioder ikke direkt har en id som aktivitet"
        }

        val grunnlag =
            vilkårperioderGrunnlagRepository.findByBehandlingId(behandlingId)
                ?: error("Finner ikke grunnlag til behandling=$behandlingId")
        val idIGrunnlag =
            grunnlag.grunnlag.aktivitet.aktiviteter
                .map { it.id }
        feilHvis(kildeId !in idIGrunnlag) {
            "Aktivitet med id=$kildeId finnes ikke i grunnlag"
        }
    }

    private fun validerBehandling(behandling: Saksbehandling) {
        behandling.status.validerKanBehandlingRedigeres()
        feilHvis(behandling.steg != StegType.INNGANGSVILKÅR) {
            "Kan ikke opprette eller endre periode når behandling ikke er på steg ${StegType.INNGANGSVILKÅR}"
        }
    }

    private fun validerKildeIdOgType(
        vilkårperiode: LagreVilkårperiode,
        eksisterendeVilkårperiode: Vilkårperiode,
    ) {
        feilHvis(eksisterendeVilkårperiode.kildeId != vilkårperiode.kildeId) {
            "Kan ikke oppdatere kildeId på en eksisterende periode. Kontakt utviklingsteamet"
        }

        feilHvis(eksisterendeVilkårperiode.type != vilkårperiode.type) {
            "Kan ikke endre type på en eksisterende periode. Kontakt utviklingsteamet"
        }
    }
}
