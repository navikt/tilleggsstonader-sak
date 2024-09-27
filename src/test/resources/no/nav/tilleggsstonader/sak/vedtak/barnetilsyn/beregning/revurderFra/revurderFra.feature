# language: no
# encoding: UTF-8

Egenskap: Beregning - med revurderFra

  Scenario: Skal ikke ta med perioder som slutter før måneden for revurderFra
    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 21.01.2024 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 21.01.2024 | TILTAK    | 3               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Når beregner med revurderFra=2024-02-15

    Så forvent følgende beregningsresultat
      | Måned | Dagsats | Antall dager | Utgift | Månedsbeløp |


  Scenario: Skal ta med alle perioder som starter etter revurderFra
    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 21.01.2024 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 21.01.2024 | TILTAK    | 3               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Når beregner med revurderFra=2023-12-15

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2024 | 29.53   | 9            | 1000   | 266         |

  Scenario: Skal ta med perioder fra og med den måneden man revurderer dersom de overlapper
    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 31.03.2024 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 31.03.2024 | TILTAK    | 3               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 03.2024 | 1000   |

    Når beregner med revurderFra=2024-02-15

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 14           | 1000   | 413         |
      | 03.2024 | 29.53   | 13           | 1000   | 384         |

  # Når man revurder må man ta med alle perioder i inneværende måned fordi en andel får fom=fom og fom=fom dvs med samme fom og tom
  # Det betyr at hvis man tidligere har en periode fra 1.8-31.8 så er hele beløpet lagt inn på en andel med fom/tom = 1.8, med det fulle beløpet for hele den perioden
  # Når man då revurderer må man få et nytt beløp for 1.8 for å sen få ett nytt beløp fra og med 15.8
  Scenario: Skal ta med perioder som er før revurderFra men fortsatt i den samme måneden
    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 02.01.2024 | TILTAK    | AAP       |
      | 15.01.2024 | 31.01.2024 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 02.01.2024 | TILTAK    | 1               |
      | 15.01.2024 | 31.01.2024 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Når beregner med revurderFra=2024-01-15

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 14           | 1000   | 414         |
    # Antall dager = 1 + 13
