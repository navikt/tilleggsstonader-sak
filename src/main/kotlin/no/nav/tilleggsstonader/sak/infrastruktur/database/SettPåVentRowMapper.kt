package no.nav.tilleggsstonader.sak.infrastruktur.database

import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVent
import no.nav.tilleggsstonader.sak.behandling.vent.ÅrsakSettPåVent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.postgresql.jdbc.PgArray
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.util.UUID

/**
 * Etter oppgradering til spring boot 4, klarer ikke spring å tolke SettPåVent.årsaker som en List<ÅrsakSettPåVent>,
 * og klarer istedenfor å sette verdien til en List<String>(!), selv om det er feil type.
 * Definerer derfor en egen RowMapper for å mappe riktig.
 */
class SettPåVentRowMapper : RowMapper<SettPåVent> {
    override fun mapRow(rs: ResultSet, rowNum: Int): SettPåVent {
        return SettPåVent(
            id = UUID.fromString(rs.getString("id")),
            behandlingId = BehandlingId.fromString(rs.getString("behandling_id")),
            årsaker = pgArrayTilÅrsakSettPåVent(rs.getObject("arsaker") as PgArray),
            kommentar = rs.getString("kommentar"),
            aktiv = rs.getBoolean(("aktiv")),
            taAvVentKommentar = rs.getString("ta_av_vent_kommentar"),
            sporbar = Sporbar(
                opprettetAv = rs.getString("opprettet_av"),
                opprettetTid = rs.getTimestamp("opprettet_tid").toLocalDateTime(),
                endret = Endret(
                    endretAv = rs.getString("endret_av"),
                    endretTid = rs.getTimestamp("endret_tid").toLocalDateTime()
                ),
            )
        )
    }

    private fun pgArrayTilÅrsakSettPåVent(pgArray: PgArray): List<ÅrsakSettPåVent> {
        return (pgArray.array as Array<*>).map { ÅrsakSettPåVent.valueOf(it.toString()) }
    }
}