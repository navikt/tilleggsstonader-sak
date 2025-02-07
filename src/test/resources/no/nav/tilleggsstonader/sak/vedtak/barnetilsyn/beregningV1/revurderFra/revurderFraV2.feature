# language: no
# encoding: UTF-8

Egenskap: Beregning - med revurderFra

  Scenario: Skal ikke ta med perioder som slutter før måneden for revurderFra
    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 21.02.2024 | TILTAK    | AAP       |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 21.02.2024 | TILTAK    | 3               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 02.2024 | 1000   |

    Når V2 - beregner med revurderFra=2024-02-15

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 11           | 1000   | 325         |

  Scenario: Skal ikke kunne innvilge når det ikke finnes noen overlapp mellom målgruppe, aktivitet og utgifter etter revurder fra dato
    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 21.01.2024 | TILTAK    | AAP       |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 21.01.2024 | TILTAK    | 3               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Når V2 - beregner med revurderFra=2024-02-15

    Så V2 - forvent følgende feil: Kan ikke innvilge når det ikke finnes noen overlapp mellom målgruppe, aktivitet og utgifter etter revurder fra dato


  Scenario: Skal ta med alle perioder som starter etter revurderFra
    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 21.01.2024 | TILTAK    | AAP       |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 21.01.2024 | TILTAK    | 3               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Når V2 - beregner med revurderFra=2023-12-15

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2024 | 29.53   | 9            | 1000   | 266         |

  Scenario: Skal ta med perioder fra og med den måneden man revurderer dersom de overlapper
    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 31.03.2024 | TILTAK    | AAP       |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 31.03.2024 | TILTAK    | 3               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 03.2024 | 1000   |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2024 | 29.53   | 15           | 1000   | 443         |
      | 02.2024 | 29.53   | 14           | 1000   | 413         |
      | 03.2024 | 29.53   | 13           | 1000   | 384         |

    Så V2 - forvent følgende beløpsperioder for: 02.2024
      | Dato       | Beløp | Målgruppe |
      | 01.02.2024 | 413   | AAP       |

    Når V2 - beregner med revurderFra=2024-02-15

    # Disse tallene skal være de samme som når man ikke bruker revurderFra,
    # då beregningsresultatet skal gi de nye tallene for selve måneden, sånn at ev. andeler blir oppdaterte for den måneden
    # Eks hvis man tidligere hadde en periode som var 01.01 - 31.01 så har hele beløpet blitt utbetalt på 01.01
    # Når vi då revurderer fra 15.01 så skal beløpet splittes opp på 01.01 og 15.01
    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 14           | 1000   | 413         |
      | 03.2024 | 29.53   | 13           | 1000   | 384         |

    Så V2 - forvent følgende beløpsperioder for: 02.2024
      | Dato       | Beløp | Målgruppe |
      | 01.02.2024 | 236   | AAP       |
      | 15.02.2024 | 177   | AAP       |

  # Når man revurder må man ta med alle perioder i inneværende måned fordi en andel får fom=fom og fom=fom dvs med samme fom og tom
  # Det betyr at hvis man tidligere har en periode fra 1.8-31.8 så er hele beløpet lagt inn på en andel med fom/tom = 1.8, med det fulle beløpet for hele den perioden
  # Når man då revurderer må man få et nytt beløp for 1.8 for å sen få ett nytt beløp fra og med 15.8
  Scenario: Skal ta med perioder som er før revurderFra men fortsatt i den samme måneden
    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 02.01.2024 | TILTAK    | AAP       |
      | 15.01.2024 | 31.01.2024 | TILTAK    | AAP       |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 02.01.2024 | TILTAK    | 1               |
      | 15.01.2024 | 31.01.2024 | TILTAK    | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Når V2 - beregner med revurderFra=2024-01-15

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2024 | 29.53   | 14           | 1000   | 414         |
    # Antall dager = 1 + 13

  Scenario: Skal splitte grunnlaget for stønadsperioder sånn at man kan filtrere ut de som endret seg til frontend
    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 31.01.2024 | TILTAK    | AAP       |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 14.01.2024 | TILTAK    | 1               |
      | 15.01.2024 | 31.01.2024 | TILTAK    | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Når V2 - beregner med revurderFra=2024-01-15

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2024 | 29.53   | 15           | 1000   | 443         |
    # Antall dager = 1 + 13

    Så V2 - forvent følgende vedtaksperiodeGrunnlag for: 01.2024
      | Fom        | Tom        | Målgruppe | Aktivitet | Antall dager |
      | 02.01.2024 | 14.01.2024 | AAP       | TILTAK    | 2            |
      | 15.01.2024 | 31.01.2024 | AAP       | TILTAK    | 13           |

  Scenario: Skal splitte stønadsperioder 2
    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2024 | 31.01.2024 | TILTAK    | AAP       |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2024 | 31.01.2024 | TILTAK    | 1               |
      | 17.01.2024 | 31.01.2024 | TILTAK    | 2               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Når V2 - beregner med revurderFra=2024-01-15

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2024 | 29.53   | 11           | 1000   | 325         |
    # Antall dager = 1 + 13

    Så V2 - forvent følgende vedtaksperiodeGrunnlag for: 01.2024
      | Fom        | Tom        | Målgruppe | Aktivitet | Antall dager |
      | 02.01.2024 | 14.01.2024 | AAP       | TILTAK    | 2            |
      | 15.01.2024 | 31.01.2024 | AAP       | TILTAK    | 9            |
