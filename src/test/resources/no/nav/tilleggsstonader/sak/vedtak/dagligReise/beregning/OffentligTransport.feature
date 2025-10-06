# language: no
# encoding: UTF-8

Egenskap: Beregning av offentlig transport for daglig reise

  Scenario: Forventer at tretti-dagersbillett lønner seg
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 01.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.01.2025 | 0                  | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall|
      | 01.01.2025 | 30.01.2025 | 778   | 1                         |

  Scenario: Forventer at tretti-dagersbillett lønner seg og at vedtaksperioden kortes ned til reiseperioden
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 01.04.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.01.2025 | 44                 | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                          |

  Scenario: Forventer at enkeltbillett lønner seg
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 30.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 31.01.2025 | 44                 | 778                       | 1                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp | Enkeltbillett-antall |
      | 01.01.2025 | 30.01.2025 | 440   | 10                   |

  Scenario: Forventer at to mnd kort og seks enkeltbilletter lønner seg
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 08.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 08.03.2025 | 44                 | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall| Enkeltbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                         | 0                    |
      | 31.01.2025 | 01.03.2025 | 778   | 1                         | 0                    |
      | 02.03.2025 | 08.03.2025 | 264   | 0                         | 6                    |


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
      | Fom        | Tom        | Beløp | Enkeltbillett-antall |
      | 01.03.2025 | 30.03.2025 | 352   | 8                    |

  Scenario: Forventer at reisen beregnes til to tretti-dagersbilletter og seksten enkeltbilletter
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 30.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.02.2025 | 12.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.04.2025 | 44                 | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall | Enkeltbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                          | 0                    |
      | 31.01.2025 | 01.03.2025 | 778   | 1                          | 0                    |
      | 02.03.2025 | 12.03.2025 | 704   | 0                          | 16                   |

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
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                          |
      | 31.01.2025 | 01.03.2025 | 0     | 0                          |
      | 02.03.2025 | 31.03.2025 | 0     | 0                          |
      | 01.04.2025 | 30.04.2025 | 778   | 1                          |

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
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                          |
      | 31.01.2025 | 01.03.2025 | 0     | 0                          |
      | 02.03.2025 | 31.03.2025 | 0     | 0                          |
      | 01.04.2025 | 30.04.2025 | 778   | 1                          |

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=2
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall |
      | 01.01.2025 | 30.01.2025 | 3369  | 1                          |
      | 31.01.2025 | 01.03.2025 | 0     | 0                          |
      | 02.03.2025 | 31.03.2025 | 0     | 0                          |
      | 01.04.2025 | 30.04.2025 | 3369  | 1                          |

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
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                          |
      | 31.01.2025 | 01.03.2025 | 778   | 1                          |
      | 02.03.2025 | 21.03.2025 | 778   | 1                          |

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
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall | Enkeltbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                          | 0                    |
      | 31.01.2025 | 01.03.2025 | 704   | 0                          | 16                   |
      | 02.03.2025 | 21.03.2025 | 778   | 1                          | 0                    |


  Scenario: Skal kun beregne med enkeltbillett dersom det er den eneste som er satt
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 03.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Antall reisedager per uke | Pris enkeltbillett |
      | 01.01.2025 | 03.01.2025 | 3                         | 40                 |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp | Enkeltbillett-antall |
      | 01.01.2025 | 03.01.2025 | 240   | 6                    |

  Scenario: Skal kun beregne med månedsbillett dersom det er den eneste som er satt
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 03.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Antall reisedager per uke | Pris tretti-dagersbillett |
      | 01.01.2025 | 03.01.2025 | 3                         | 800                       |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall|
      | 01.01.2025 | 03.01.2025 | 800   | 1                         |

  Scenario: Skal kun beregne med syvdagersbillett dersom det er den eneste som er satt
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 03.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Antall reisedager per uke | Pris syv-dagersbillett |
      | 01.01.2025 | 03.01.2025 | 3                         | 200                    |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp | Syvdagersbillett-antall|
      | 01.01.2025 | 03.01.2025 | 200   |1                       |


  Scenario: Skal kun beregne med trettidagersbillett dersom syvdagetbillet og enkelt billet er 0 kr
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 01.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.01.2025 |           0         |      0                |778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |Trettidagersbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                          |



  Scenario: Skal vise antall av hver billet typer av billigeste beløp
    Gitt følgende vedtaksperioder for daglig reise offentlig transport
      | Fom        | Tom        | FaktiskMålgruppe    | Aktivitet |
      | 01.01.2025 | 01.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende beregningsinput for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.01.2025 |          80        |      400               |          1800             | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr=1
      | Fom        | Tom        | Beløp |Enkeltbillett-antall|Syvdagersbillett-antall|
      | 01.01.2025 | 30.01.2025 | 1760   |2                  |4                      |




