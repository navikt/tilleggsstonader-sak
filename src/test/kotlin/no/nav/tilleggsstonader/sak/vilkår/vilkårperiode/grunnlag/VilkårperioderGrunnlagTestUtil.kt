package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import java.math.BigDecimal
import java.time.LocalDate

object VilkårperioderGrunnlagTestUtil {
    fun periodeGrunnlagAktivitet(
        id: String = "123",
        fom: LocalDate = osloDateNow(),
        tom: LocalDate? = osloDateNow().plusMonths(1),
        type: String = "TYPE",
        typeNavn: String = "Type navn",
        status: StatusAktivitet? = StatusAktivitet.AKTUELL,
        statusArena: String? = "AKTUL",
        antallDagerPerUke: Int? = 5,
        prosentDeltakelse: BigDecimal? = 100.toBigDecimal(),
        erStønadsberettiget: Boolean? = true,
        erUtdanning: Boolean? = false,
        arrangør: String? = "Arrangør",
        kilde: Kilde = Kilde.ARENA,
    ) = RegisterAktivitet(
        id = id,
        fom = fom,
        tom = tom,
        type = type,
        typeNavn = typeNavn,
        status = status,
        statusArena = statusArena,
        antallDagerPerUke = antallDagerPerUke,
        prosentDeltakelse = prosentDeltakelse,
        erStønadsberettiget = erStønadsberettiget,
        erUtdanning = erUtdanning,
        arrangør = arrangør,
        kilde = kilde,
    )
}
