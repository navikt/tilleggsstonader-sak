package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.AdressebeskyttelseDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingÅrsakDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.StønadstypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtakResultatDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakAvslagDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakOpphørDvh
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// TODO: Vurder om dette bør flyttes til kontrakter

/**
 * @param endretTid skal oppdateres i tilfelle man må patche data på en behandling.
 * Man skal då beholde den samme raden for å beholde opprettet_tid, men oppdatere felter og oppdatere
 */
data class Vedtaksstatistikk(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: FagsakId,
    val behandlingId: BehandlingId,
    val eksternFagsakId: Long,
    val eksternBehandlingId: Long,
    val relatertBehandlingId: Long?, // Ekstern behandlingsid på relatert behandling
    val adressebeskyttelse: AdressebeskyttelseDvh,
    val tidspunktVedtak: LocalDateTime,
    @Column("malgrupper")
    val målgrupper: MålgrupperDvh.JsonWrapper,
    val aktiviteter: AktiviteterDvh.JsonWrapper,
    @Column("vilkarsvurderinger")
    val vilkårsvurderinger: VilkårsvurderingerDvh.JsonWrapper,
    val person: String,
    val barn: BarnDvh.JsonWrapper,
    val behandlingType: BehandlingTypeDvh,
    @Column("behandling_arsak")
    val behandlingÅrsak: BehandlingÅrsakDvh,
    val vedtakResultat: VedtakResultatDvh,
    val vedtaksperioder: VedtaksperioderDvh.JsonWrapper,
    val utbetalinger: UtbetalingerDvh.JsonWrapper,
    @Column("stonadstype")
    val stønadstype: StønadstypeDvh,
    val kravMottatt: LocalDate?,
    @Column("arsaker_avslag")
    val årsakerAvslag: ÅrsakAvslagDvh.JsonWrapper?,
    @Column("arsaker_opphor")
    val årsakerOpphør: ÅrsakOpphørDvh.JsonWrapper?,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
    @LastModifiedDate
    val endretTid: LocalDateTime = opprettetTid,
    // TODO: Legg inn årsak til revurdering når revurdering kommer i løsningen
    // TODO: EØS-informasjon når det kommer støtte for det i løsningen
) {
    init {
        feilHvis(endretTid < opprettetTid) {
            "EndretTid=$endretTid kan ikke være før opprettetTid=$opprettetTid"
        }
    }
}

data class UtbetalingerDvh(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val type: AndelstypeDvh,
    val beløp: Int,
) {
    data class JsonWrapper(
        val utbetalinger: List<UtbetalingerDvh>,
    )

    companion object {
        fun fraDomene(ytelser: List<AndelTilkjentYtelse>) = JsonWrapper(
            ytelser.filterNot { it.type == TypeAndel.UGYLDIG }.map {
                UtbetalingerDvh(
                    fraOgMed = it.fom,
                    tilOgMed = it.tom,
                    type = AndelstypeDvh.fraDomene(it.type),
                    beløp = it.beløp,
                )
            },
        )
    }
}

enum class AndelstypeDvh {
    TILSYN_BARN_ENSLIG_FORSØRGER,
    TILSYN_BARN_AAP,
    TILSYN_BARN_ETTERLATTE,

    LÆREMIDLER_ENSLIG_FORSØRGER,
    LÆREMIDLER_AAP,
    LÆREMIDLER_ETTERLATTE,
    ;

    companion object {
        fun fraDomene(typeAndel: TypeAndel) = when (typeAndel) {
            TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER -> TILSYN_BARN_ENSLIG_FORSØRGER
            TypeAndel.TILSYN_BARN_AAP -> TILSYN_BARN_AAP
            TypeAndel.TILSYN_BARN_ETTERLATTE -> TILSYN_BARN_ETTERLATTE
            TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER -> LÆREMIDLER_ENSLIG_FORSØRGER
            TypeAndel.LÆREMIDLER_AAP -> LÆREMIDLER_AAP
            TypeAndel.LÆREMIDLER_ETTERLATTE -> LÆREMIDLER_ETTERLATTE
            TypeAndel.UGYLDIG -> throw Error("Trenger ikke statistikk på ugyldige betalinger")
        }
    }
}

data class VedtaksperioderDvh(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
) {
    data class JsonWrapper(
        val vedtaksperioder: List<VedtaksperioderDvh>,
    )

    companion object {
        // TODO: Map fra faktiske vedtaksperioder når vi har det (også relatert til revurdering)

        fun fraDomene(ytelser: List<StønadsperiodeDto>) = JsonWrapper(
            ytelser.map {
                VedtaksperioderDvh(fraOgMed = it.fom, tilOgMed = it.tom)
            },
        )
    }
}


data class BarnDvh(
    val fnr: String,
) {
    data class JsonWrapper(
        val barn: List<BarnDvh>,
    )

    companion object {
        fun fraDomene(behandlingBarn: List<BehandlingBarn>) = JsonWrapper(
            behandlingBarn.map {
                BarnDvh(fnr = it.ident)
            },
        )
    }
}

data class AktiviteterDvh(
    val type: AktivitetTypeDvh,
    val resultat: ResultatVilkårperiodeDvh,
) {
    data class JsonWrapper(
        val aktivitet: List<AktiviteterDvh>,
    )

    companion object {
        fun fraDomene(vilkårsperioder: List<Vilkårperiode>): JsonWrapper {
            return JsonWrapper(
                vilkårsperioder
                    .filterNot { ResultatVilkårperiode.SLETTET == it.resultat }
                    .map {
                        AktiviteterDvh(
                            type = AktivitetTypeDvh.fraDomene(it.type),
                            resultat = ResultatVilkårperiodeDvh.fraDomene(it.resultat),
                        )
                    },
            )
        }
    }
}

data class MålgrupperDvh(
    val type: MålgruppeTypeDvh,
    val resultat: ResultatVilkårperiodeDvh,
) {
    data class JsonWrapper(
        val målgrupper: List<MålgrupperDvh>,
    )

    companion object {
        fun fraDomene(vilkårsperioder: List<Vilkårperiode>): JsonWrapper {
            return JsonWrapper(
                vilkårsperioder
                    .filterNot { ResultatVilkårperiode.SLETTET == it.resultat }
                    .map {
                        MålgrupperDvh(
                            type = MålgruppeTypeDvh.fraDomene(it.type),
                            resultat = ResultatVilkårperiodeDvh.fraDomene(it.resultat),
                        )
                    },
            )
        }
    }
}

enum class ResultatVilkårperiodeDvh {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_TATT_STILLING_TIL,
    ;

    companion object {
        fun fraDomene(resultat: ResultatVilkårperiode) = when (resultat) {
            ResultatVilkårperiode.OPPFYLT -> OPPFYLT
            ResultatVilkårperiode.IKKE_OPPFYLT -> IKKE_OPPFYLT
            ResultatVilkårperiode.IKKE_VURDERT -> IKKE_TATT_STILLING_TIL
            ResultatVilkårperiode.SLETTET ->
                throw IllegalArgumentException("Slettede vedtak er ikke relevant, og skal ha blitt filtrert bort.")
        }
    }
}

enum class VilkårsresultatDvh {
    OPPFYLT,
    AUTOMATISK_OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_AKTUELL,
    IKKE_TATT_STILLING_TIL,
    SKAL_IKKE_VURDERES,
    ;

    companion object {
        fun fraDomene(resultat: Vilkårsresultat) = when (resultat) {
            Vilkårsresultat.OPPFYLT -> OPPFYLT
            Vilkårsresultat.AUTOMATISK_OPPFYLT -> AUTOMATISK_OPPFYLT
            Vilkårsresultat.IKKE_OPPFYLT -> IKKE_OPPFYLT
            Vilkårsresultat.IKKE_AKTUELL -> IKKE_AKTUELL
            Vilkårsresultat.IKKE_TATT_STILLING_TIL -> IKKE_TATT_STILLING_TIL
            Vilkårsresultat.SKAL_IKKE_VURDERES -> SKAL_IKKE_VURDERES
            // TODO: Vurder om vi trenger alle disse
        }
    }
}

enum class AktivitetTypeDvh {
    TILTAK,
    UTDANNING,
    REELL_ARBEIDSSØKER,
    INGEN_AKTIVITET,
    ;

    companion object {
        fun fraDomene(vilkårsperiodeType: VilkårperiodeType) = when (vilkårsperiodeType) {
            is AktivitetType -> fraDomene(aktivitetType = vilkårsperiodeType)
            is MålgruppeType -> throw IllegalArgumentException("$vilkårsperiodeType er ikke en gyldig type aktivitet.")
        }

        fun fraDomene(aktivitetType: AktivitetType) = when (aktivitetType) {
            AktivitetType.TILTAK -> TILTAK
            AktivitetType.UTDANNING -> UTDANNING
            AktivitetType.REELL_ARBEIDSSØKER -> REELL_ARBEIDSSØKER
            AktivitetType.INGEN_AKTIVITET -> INGEN_AKTIVITET
        }
    }
}

enum class MålgruppeTypeDvh {
    AAP,
    DAGPENGER,
    OMSTILLINGSSTØNAD,
    OVERGANGSSTØNAD,
    NEDSATT_ARBEIDSEVNE,
    UFØRETRYGD,
    INGEN_MÅLGRUPPE,
    SYKEPENGER_100_PROSENT,
    ;

    companion object {
        fun fraDomene(vilkårsperiodeType: VilkårperiodeType) = when (vilkårsperiodeType) {
            is MålgruppeType -> fraDomene(målgruppeType = vilkårsperiodeType)
            is AktivitetType -> throw IllegalArgumentException("$vilkårsperiodeType er ikke en gyldig type målgruppe.")
        }

        fun fraDomene(målgruppeType: MålgruppeType) = when (målgruppeType) {
            MålgruppeType.AAP -> AAP
            MålgruppeType.DAGPENGER -> DAGPENGER
            MålgruppeType.OMSTILLINGSSTØNAD -> OMSTILLINGSSTØNAD
            MålgruppeType.OVERGANGSSTØNAD -> OVERGANGSSTØNAD
            MålgruppeType.NEDSATT_ARBEIDSEVNE -> NEDSATT_ARBEIDSEVNE
            MålgruppeType.UFØRETRYGD -> UFØRETRYGD
            MålgruppeType.INGEN_MÅLGRUPPE -> INGEN_MÅLGRUPPE
            MålgruppeType.SYKEPENGER_100_PROSENT -> SYKEPENGER_100_PROSENT
        }
    }
}

data class VilkårsvurderingerDvh(
    val resultat: VilkårsresultatDvh,
    val vilkår: List<DelvilkårDvh>,
) {
    data class JsonWrapper(
        val vilkårsvurderinger: List<VilkårsvurderingerDvh>,
    )

    companion object {
        fun fraDomene(vilkår: List<VilkårDto>) = JsonWrapper(
            vilkår.map {
                VilkårsvurderingerDvh(
                    resultat = VilkårsresultatDvh.fraDomene(it.resultat),
                    vilkår = DelvilkårDvh.fraDomene(it.delvilkårsett),
                )
            },
        )
    }
}

data class DelvilkårDvh(
    val resultat: Vilkårsresultat,
    val vurderinger: List<RegelId>, // Anti corruption layer
) {
    companion object {
        fun fraDomene(delvilkår: List<DelvilkårDto>): List<DelvilkårDvh> {
            return delvilkår.map {
                DelvilkårDvh(
                    resultat = it.resultat,
                    vurderinger = it.vurderinger.map { vurdering -> vurdering.regelId },
                )
            }
        }
    }
}

