# language: no
# encoding: UTF-8

Egenskap: Beregning - Komplisert scenario

  Scenario: Flere stønadsperioder og aktiviteter over lenger periode
    Gitt følgende støndsperioder
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 01.01.2023 | 15.02.2023 | AAP             | TILTAK    |
      | 16.02.2023 | 28.02.2023 | AAP             | UTDANNING |
      | 01.03.2023 | 30.04.2023 | OVERGANGSSTØNAD | UTDANNING |
      | 15.05.2023 | 04.06.2023 | AAP             | TILTAK    |
      | 12.06.2023 | 30.06.2023 | AAP             | TILTAK    |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 30.06.2023 | UTDANNING | 5               |
      | 01.01.2023 | 15.02.2023 | TILTAK    | 3               |
      | 15.05.2023 | 04.06.2023 | TILTAK    | 2               |
      | 12.06.2023 | 30.06.2023 | TILTAK    | 3               |
      | 19.06.2023 | 30.06.2023 | TILTAK    | 2               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 06.2023 | 1000   |

    Gitt følgende utgifter for barn med id: 2
      | Fom     | Tom     | Utgift |
      | 04.2023 | 06.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 14           | 1000   | 413         |
      | 02.2023 | 29.53   | 18           | 1000   | 532         |
      | 03.2023 | 29.53   | 23           | 1000   | 679         |
      | 04.2023 | 59.07   | 20           | 2000   | 1181        |
      | 05.2023 | 59.07   | 6            | 2000   | 354         |
      | 06.2023 | 59.07   | 15           | 2000   | 886         |

    Så forvent følgende stønadsperiodeGrunnlag for: 01.2023
      | Fom        | Tom        | Målgruppe | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.01.2023 | 31.01.2023 | AAP       | TILTAK    | 1                  | 14           |

    Så forvent følgende beløpsperioder for: 01.2023
      | Dato       | Beløp | Målgruppe |
      | 01.01.2023 | 413   | AAP       |

    Så forvent følgende stønadsperiodeGrunnlag for: 02.2023
      | Fom        | Tom        | Målgruppe | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.02.2023 | 15.02.2023 | AAP       | TILTAK    | 1                  | 9            |
      | 16.02.2023 | 28.02.2023 | AAP       | UTDANNING | 1                  | 9            |

    Så forvent følgende beløpsperioder for: 02.2023
      | Dato       | Beløp | Målgruppe |
      | 01.02.2023 | 266   | AAP       |
      | 16.02.2023 | 266   | AAP       |

    Så forvent følgende stønadsperiodeGrunnlag for: 03.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.03.2023 | 31.03.2023 | OVERGANGSSTØNAD | UTDANNING | 1                  | 23           |

    Så forvent følgende beløpsperioder for: 03.2023
      | Dato       | Beløp | Målgruppe       |
      | 01.03.2023 | 679   | OVERGANGSSTØNAD |

    Så forvent følgende stønadsperiodeGrunnlag for: 04.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.04.2023 | 30.04.2023 | OVERGANGSSTØNAD | UTDANNING | 1                  | 20           |

    Så forvent følgende beløpsperioder for: 04.2023
      | Dato       | Beløp | Målgruppe       |
      | 01.04.2023 | 1181  | OVERGANGSSTØNAD |

    Så forvent følgende stønadsperiodeGrunnlag for: 05.2023
      | Fom        | Tom        | Målgruppe | Aktivitet | Antall aktiviteter | Antall dager |
      | 15.05.2023 | 31.05.2023 | AAP       | TILTAK    | 1                  | 6            |

    Så forvent følgende beløpsperioder for: 05.2023
      | Dato       | Beløp | Målgruppe |
      | 15.05.2023 | 354   | AAP       |

    Så forvent følgende stønadsperiodeGrunnlag for: 06.2023
      | Fom        | Tom        | Målgruppe | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.06.2023 | 04.06.2023 | AAP       | TILTAK    | 1                  | 2            |
      | 12.06.2023 | 30.06.2023 | AAP       | TILTAK    | 2                  | 13           |

    Så forvent følgende beløpsperioder for: 06.2023
      | Dato       | Beløp | Målgruppe |
      | 01.06.2023 | 118   | AAP       |
      | 12.06.2023 | 768   | AAP       |
