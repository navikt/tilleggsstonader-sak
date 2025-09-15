# language: no
# encoding: UTF-8

Egenskap: Beregning av offentlig transport for daglig reise

  Scenario: Forventer at tretti-dagersbillett lønner seg
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 01.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.01.2025 | 44                 | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 778   |

  Scenario: Skal ikke ta med 0kr som en ekte billetpris
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.06.2025 | 30.06.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.06.2025 | 30.06.2025 | 0                  | 100                    | 0                    | 3                        |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.06.2025 | 30.06.2025 |   500   |



  Scenario: Forventer at tretti-dagersbillett lønner seg og at vedtaksperioden kortes ned til reiseperioden
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 01.04.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.01.2025 | 44                 | 788                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 788   |

  Scenario: Forventer at enkeltbillett lønner seg
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 30.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 31.01.2025 | 44                 | 778                       | 1                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 440   |

  Scenario: Forventer at to mnd kort og seks enkeltbilletter lønner seg
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 08.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 08.03.2025 | 44                 | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 778   |
      | 31.01.2025 | 01.03.2025 | 778   |
      | 02.03.2025 | 08.03.2025 | 264   |


    # Reisen er lengre enn vedtaksperioden, sjekke at reisen snippes til fom/tom på vedtaksperioden
  Scenario: Forventer at fom og tom på reisen forkortes når den er lengre enn vedtaksperioden
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.03.2025 | 30.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 15.02.2025 | 15.04.2025 | 44                 | 778                       | 1                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.03.2025 | 30.03.2025 | 352   |

  Scenario: Forventer at reisen beregnes til to tretti-dagersbilletter og åtte enkeltbilletter
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 30.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.02.2025 | 12.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.04.2025 | 44                 | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 778   |
      | 31.01.2025 | 01.03.2025 | 778   |
      | 02.03.2025 | 12.03.2025 | 704   |

  Scenario: Forventer to trettidagersperioder og to perioder med 0 i beløp på grunn av opphold i vedtaksperiodene
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 30.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.04.2025 | 30.04.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 31.05.2025 | 44                 | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 778   |
      | 31.01.2025 | 01.03.2025 | 778   |
      | 02.03.2025 | 31.03.2025 | 778   |
      | 01.04.2025 | 30.04.2025 | 778   |

  # To reiser i samme periode tog (Vy) + buss (ruter), her lønner det seg med mnd kort selv om det er opphold i periodene
  Scenario: to identiske reiseperioder med ulike transportmiddel og vedtaksperioder med opphold
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 30.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.04.2025 | 30.04.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 31.05.2025 | 44                 | 778                       | 5                         |
      | 01.01.2025 | 31.05.2025 | 287                | 3369                      | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 778   |
      | 31.01.2025 | 01.03.2025 | 778     |
      | 02.03.2025 | 31.03.2025 | 778    |
      | 01.04.2025 | 30.04.2025 | 778   |

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=2
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 3369  |
      | 31.01.2025 | 01.03.2025 | 3369  |
      | 02.03.2025 | 31.03.2025 |3369   |
      | 01.04.2025 | 30.04.2025 | 3369  |

  # Lønner seg med mnd kort selv om det er opphold (f.eks tre dager på slutten første og 10 dager på starten av siste)
  Scenario: Månedskort lønner seg selv om vedtaksperiodene har opphold og mer enn rene 30 dagersperioder
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 03.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 10.02.2025 | 21.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 31.05.2025 | 44                 | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 778   |
      | 31.01.2025 | 01.03.2025 | 778   |
      | 02.03.2025 | 21.03.2025 | 778   |

  # Lønner seg IKKE mnd kort selv om det er opphold (f.eks tre dager på slutten første og to dager på starten av siste)
  Scenario: Månedskort lønner IKKE hvor vedtaksperiodene har opphold
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 03.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 18.02.2025 | 21.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 31.05.2025 | 44                 | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 30.01.2025 | 778   |
      | 31.01.2025 | 01.03.2025 | 704   |
      | 02.03.2025 | 21.03.2025 | 778   |
