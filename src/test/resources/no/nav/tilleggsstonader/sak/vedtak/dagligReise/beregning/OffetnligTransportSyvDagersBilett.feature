# language: no
# encoding: UTF-8

Egenskap: Beregning av offentlig transport med 7-dagers bilett

  Scenario: Syvdageres billett skal lønne seg innenfor en uke
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 12.01.2025 | 44                 | 100                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 12.01.2025 | 100   |

  Scenario: Syvdageres billett skal ikke lønne seg innenfor en uke
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 12.01.2025 | 44                 | 300                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 12.01.2025 | 264   |

  Scenario: Syvdagers bilett skal lønne seg på tvers av to uker pga pris
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 19.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 19.01.2025 | 44                 | 100                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 19.01.2025 | 200   |

  Scenario: Syvdagers bilett skal ikke lønne seg på tvers av to uker pga pris
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 19.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 19.01.2025 | 44                 | 300                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 19.01.2025 | 528   |

  Scenario: Syvdagers bilett skal lønne seg på tvers av to uker pga antall reisedager
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 19.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 19.01.2025 | 44                 | 300                    | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 19.01.2025 | 600   |

  Scenario: Syvdagers bilett skal ikke lønne seg på tvers av to uker pga antall reisedager
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 19.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 19.01.2025 | 44                 | 300                    | 778                       | 1                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 19.01.2025 | 176   |


  Scenario: Syvdageres billett skal lønne seg med flere vedtaksperioder
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 07.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 09.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 12.01.2025 | 44                 | 100                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 12.01.2025 | 100   |

  Scenario: Syvdagers bilett skal lønne seg selv om det ikke er reise hele uken
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 08.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 08.01.2025 | 44                 | 200                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 08.01.2025 | 200   |

  Scenario: Syvdagers bilett skal ikke lønne seg pga ikke reise hele uken
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 06.01.2025 | 07.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 07.01.2025 | 44                 | 200                    | 778                       | 2                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 07.01.2025 | 176   |

#    Kombinasjon av enkelbiletter og syvdagersbilletter
