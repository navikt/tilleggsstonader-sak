package no.nav.tilleggsstonader.sak.googlemaps

fun RuteResponse.finnDefaultRute() = this.routes.find { it.routeLabels.contains(RouteLabel.DEFAULT_ROUTE) }

fun RuteResponse.finnKortesteRute() = this.routes.minBy { it.distanceMeters }
