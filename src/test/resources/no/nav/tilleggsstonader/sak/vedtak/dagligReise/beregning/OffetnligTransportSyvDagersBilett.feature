# language: no
# encoding: UTF-8

Egenskap: Beregning av offentlig transport med 7-dagers billett innefor en uke

  Scenario: Syvdageres billett skal lønne seg innenfor en uke pga reisedager
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 12.01.2025 | 44                 | 366                    | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 12.01.2025 | 366   |

  Scenario: Enkeltbillett skal ikke lønne seg innenfor en uke pga reisedager
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 12.01.2025 | 44                 | 366                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 12.01.2025 | 264   |


  Scenario: Syvdagers bilett skal lønne seg selv om det ikke er reise hele uken
    # Setter prisen på syv-dageresbilett til 340 for at det skal lønne seg selv om det kun er 4 reisdager i løpet av uka
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 09.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 09.01.2025 | 44                 | 340                    | 778                       | 4                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 09.01.2025 | 340   |

  Scenario: Syvdageres billett skal lønne seg med flere vedtaksperioder
    # Setter prisen på syv-dageresbilett til 340 for at det skal lønne seg selv om det kun er 4 reisdager i løpet av uka
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 07.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 09.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 12.01.2025 | 44                 | 340                    | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 12.01.2025 | 340   |

  Scenario: Syvdageres billett skal lønne seg med flere reiser
    #E.g. Ruter og Vy
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 12.01.2025 | 44                 | 366                    | 778                       | 5                         |
      | 06.01.2025 | 12.01.2025 | 145                | 767                    | 1853                      | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 12.01.2025 | 366   |

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=2
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 12.01.2025 | 767   |


  Scenario: Syvdagers bilett skal lønne seg på tvers av to uker
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 19.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 19.01.2025 | 44                 | 366                    | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 19.01.2025 | 732   |

  Scenario: Syvdagers bilett skal ikke lønne seg på tvers av to uker pga resiedager
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 19.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 19.01.2025 | 44                 | 366                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 19.01.2025 | 528   |

  Scenario: Syvdagers bilett skal ikke lønne seg pga ikke reise hele uken
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 07.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 07.01.2025 | 44                 | 366                    | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 07.01.2025 | 176   |


  Scenario: To syvdagers biletter skal lønne seg selv om ikke 14 dager
    # Setter prisen på syv-dageresbilett til 340 for at det skal lønne seg selv om det kun er 4 reisdager i løpet av uka
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 16.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 16.01.2025 | 44                 | 340                    | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 16.01.2025 | 680   |

  Scenario: Syvdagersbilett skal lønne seg på tvers av uker
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 08.01.2025 | 14.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 08.01.2025 | 14.01.2025 | 44                 | 366                    | 778                       | 4                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 08.01.2025 | 14.01.2025 | 366   |

  Scenario: Syvdagers bilett skal lønne seg i kombinasjon med enkeltbilletter
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 14.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 14.01.2025 | 44                 | 366                    | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 14.01.2025 | 542   |

  Scenario: Syvdagersbilett skal lønne seg i kombinasjon med månedskort
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 11.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 06.02.2025 | 44                 | 366                    | 778                       | 4                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 778   |
      | 31.01.2025 | 06.02.2025 | 366   |

  Scenario: Syvdagersbilett skal lønne seg i kombinasjon med månedskort og enkelbiletter
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 14.01.2025 | 24.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 14.01.2025 | 24.02.2025 | 44                 | 366                    | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 14.01.2025 | 12.02.2025 | 778   |
      | 13.02.2025 | 24.02.2025 | 630   |
