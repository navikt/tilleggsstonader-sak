package no.nav.tilleggsstonader.sak.statistikk.vedtak

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.statistikk.vedtak.StønadstypeDvh.BARNETILSYN
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
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
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(MålgruppeDvh.JsonWrapper::class, name = "målgruppe"),
    JsonSubTypes.Type(AktivitetDvh.JsonWrapper::class, name = "aktivitet"),
    JsonSubTypes.Type(VilkårsvurderingDvh.JsonWrapper::class, name = "vilkårsvurderinger"),
    JsonSubTypes.Type(BarnDvh.JsonWrapper::class, name = "barn"),
    JsonSubTypes.Type(UtbetalingerDvh.JsonWrapper::class, name = "utbetalinger"),
    JsonSubTypes.Type(VedtaksperioderDvh.JsonWrapper::class, name = "vedtaksperioder"),
)
data class Vedtaksstatistikk(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val behandlingId: UUID,
    val eksternFagsakId: Long,
    val eksternBehandlingId: Long,
    val relatertBehandlingId: Long?, // Ekstern behandlingsid på relatert behandling
    val adressebeskyttelse: AdressebeskyttelseDvh,
    val tidspunktVedtak: LocalDateTime,

    @Column("målgruppe")
    val målgruppe: MålgruppeDvh.JsonWrapper,

    @Column("aktivitet")
    val aktivitet: AktivitetDvh.JsonWrapper,

    @Column("vilkårsvurderinger")
    val vilkårsvurderinger: VilkårsvurderingDvh.JsonWrapper,

    val person: String,

    @Column("barn")
    val barn: BarnDvh.JsonWrapper,

    val behandlingType: BehandlingTypeDvh,
    val behandlingÅrsak: BehandlingÅrsakDvh,
    val vedtakResultat: VedtakResultatDvh,

    @Column("vedtaksperioder")
    val vedtaksperioder: VedtaksperioderDvh.JsonWrapper,

    @Column("utbetalinger")
    val utbetalinger: UtbetalingerDvh.JsonWrapper,

    @Column("stønadstype")
    val stønadstype: StønadstypeDvh = BARNETILSYN,

    val kravMottatt: LocalDate?,
    val årsakRevurdering: ÅrsakRevurderingDvh? = null,
    val avslagÅrsak: String? = null,
)

// TODO: Vurder om dette bør flyttes til kontrakter

enum class StønadstypeDvh {
    BARNETILSYN,
}

data class UtbetalingerDvh(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val beløp: Int,
) {
    data class JsonWrapper(
        val utbetalinger: List<UtbetalingerDvh>,
    )

    companion object {
        fun fraDomene(ytelser: List<AndelTilkjentYtelse>) = JsonWrapper(
            ytelser.map {
                UtbetalingerDvh(fraOgMed = it.fom, tilOgMed = it.tom, beløp = it.beløp)
            },
        )
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

enum class VedtakResultatDvh {
    INNVILGET,
    AVSLÅTT,
    OPPHØRT,
    ;

    companion object {
        fun fraDomene(behandlingResultat: BehandlingResultat): VedtakResultatDvh {
            return when (behandlingResultat) {
                BehandlingResultat.INNVILGET -> INNVILGET
                BehandlingResultat.OPPHØRT -> OPPHØRT
                BehandlingResultat.AVSLÅTT -> AVSLÅTT
                BehandlingResultat.IKKE_SATT, BehandlingResultat.HENLAGT ->
                    throw IllegalStateException("Skal ikke sende vedtaksstatistikk når resultat=$behandlingResultat.")
            }
        }
    }
}

enum class BehandlingÅrsakDvh {
    KLAGE,
    NYE_OPPLYSNINGER,
    SØKNAD,
    PAPIRSØKNAD,
    MANUELT_OPPRETTET,
    KORRIGERING_UTEN_BREV,
    SATSENDRING,
    ;

    companion object {
        fun fraDomene(årsak: BehandlingÅrsak) = when (årsak) {
            BehandlingÅrsak.KLAGE -> KLAGE
            BehandlingÅrsak.NYE_OPPLYSNINGER -> NYE_OPPLYSNINGER
            BehandlingÅrsak.SØKNAD -> SØKNAD
            BehandlingÅrsak.PAPIRSØKNAD -> PAPIRSØKNAD
            BehandlingÅrsak.MANUELT_OPPRETTET -> MANUELT_OPPRETTET
            BehandlingÅrsak.KORRIGERING_UTEN_BREV -> KORRIGERING_UTEN_BREV
            BehandlingÅrsak.SATSENDRING -> SATSENDRING
        }
    }
}

enum class BehandlingTypeDvh {
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
    ;

    companion object {
        fun fraDomene(type: BehandlingType) = when (type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> FØRSTEGANGSBEHANDLING
            BehandlingType.REVURDERING -> REVURDERING
        }
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

data class AktivitetDvh(
    val type: AktivitetTypeDvh,
    val resultat: ResultatVilkårperiodeDvh,
) {
    data class JsonWrapper(
        val aktivitet: List<AktivitetDvh>,
    )

    companion object {
        fun fraDomene(vilkårsperioder: List<Vilkårperiode>): JsonWrapper {
            return JsonWrapper(
                vilkårsperioder
                    .filterNot { ResultatVilkårperiode.SLETTET == it.resultat }
                    .map {
                        AktivitetDvh(
                            type = AktivitetTypeDvh.fraDomene(it.type),
                            resultat = ResultatVilkårperiodeDvh.fraDomene(it.resultat),
                        )
                    },
            )
        }
    }
}

data class MålgruppeDvh(
    val type: MålgruppeTypeDvh,
    val resultat: ResultatVilkårperiodeDvh,
) {
    data class JsonWrapper(
        val målgruppe: List<MålgruppeDvh>,
    )

    companion object {
        fun fraDomene(vilkårsperioder: List<Vilkårperiode>): JsonWrapper {
            return JsonWrapper(
                vilkårsperioder
                    .filterNot { ResultatVilkårperiode.SLETTET == it.resultat }
                    .map {
                        MålgruppeDvh(
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
    ;

    companion object {
        fun fraDomene(vilkårsperiodeType: VilkårperiodeType) = when (vilkårsperiodeType) {
            AktivitetType.TILTAK -> TILTAK
            AktivitetType.UTDANNING -> UTDANNING
            AktivitetType.REELL_ARBEIDSSØKER -> REELL_ARBEIDSSØKER
            else -> {
                throw IllegalArgumentException("$vilkårsperiodeType er ikke en gyldig type aktivitet.")
            }
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
    ;

    companion object {
        fun fraDomene(vilkårsperiodeType: VilkårperiodeType) = when (vilkårsperiodeType) {
            MålgruppeType.AAP -> AAP
            MålgruppeType.DAGPENGER -> DAGPENGER
            MålgruppeType.OMSTILLINGSSTØNAD -> OMSTILLINGSSTØNAD
            MålgruppeType.OVERGANGSSTØNAD -> OVERGANGSSTØNAD
            MålgruppeType.NEDSATT_ARBEIDSEVNE -> NEDSATT_ARBEIDSEVNE
            MålgruppeType.UFØRETRYGD -> UFØRETRYGD
            else -> {
                throw IllegalArgumentException("$vilkårsperiodeType er ikke en gyldig type målgruppe.")
            }
        }
    }
}

data class VilkårsvurderingDvh(
    val resultat: VilkårsresultatDvh,
    val vilkår: List<DelvilkårDvh>,
) {
    data class JsonWrapper(
        val vilkårsvurderinger: List<VilkårsvurderingDvh>,
    )

    companion object {
        fun fraDomene(vilkår: List<VilkårDto>) = JsonWrapper(
            vilkår.map {
                VilkårsvurderingDvh(
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

enum class AdressebeskyttelseDvh {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
    ;

    companion object {
        fun fraDomene(adressebeskyttelse: AdressebeskyttelseGradering) =
            when (adressebeskyttelse) {
                AdressebeskyttelseGradering.STRENGT_FORTROLIG -> STRENGT_FORTROLIG
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> STRENGT_FORTROLIG_UTLAND
                AdressebeskyttelseGradering.FORTROLIG -> FORTROLIG
                AdressebeskyttelseGradering.UGRADERT -> UGRADERT
            }
    }
}

enum class ÅrsakRevurderingDvh {
    // TODO når vi får revurdering i løsningen
}
