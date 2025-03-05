# language: no
# encoding: UTF-8

Egenskap: Beregning - Flere aktiviteter med delvis aktivitet

  Scenario: Full uker med flere aktiviteter hvor summen av antall dager er mindre enn 5
    # Mål: Summen av antall dager skal inkludere alle aktiviteter

    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2023 | 08.01.2023 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 08.01.2023 | TILTAK    | 3               |
      | 02.01.2023 | 08.01.2023 | TILTAK    | 1               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 4            | 1000   | 118         |

  Scenario: Full uker med flere aktiviteter hvor summen av antall dager er mer enn 5
    # Mål: Summen av antall dager skal maks bli 5

    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2023 | 08.01.2023 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 08.01.2023 | TILTAK    | 3               |
      | 02.01.2023 | 08.01.2023 | TILTAK    | 3               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 5            | 1000   | 148         |

  Scenario: Full uker med flere aktiviteter hvor summen av antall dager er mer enn 5
    # Mål: Summen av antall dager skal maks bli 5

    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2023 | 08.01.2023 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 08.01.2023 | TILTAK    | 3               |
      | 02.01.2023 | 08.01.2023 | TILTAK    | 3               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 5            | 1000   | 148         |

  Scenario: Flere aktiviteter i månedsskifte
    # Mål: Summen av antall dager skal ikke bli mer enn antall dager i uken som er innenfor måneden

    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 30.01.2023 | 05.02.2023 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 30.01.2023 | 05.02.2023 | TILTAK    | 1               |
      | 30.01.2023 | 05.02.2023 | TILTAK    | 3               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 02.2023 | 1000   |

    Når beregner

    # Beregningen fordeler ikke aktivitetsdager på tvers av måneder
    # I månedskifte fylles uken med så mange dager man får plass til
    # Resultat: 2 dager i januar + 3 dager i februar = 5 dager (selv om sum av aktivitetsdager er 4)
    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 2            | 1000   | 59          |
      | 02.2023 | 29.53   | 3            | 1000   | 89          |

  Scenario: Vedtaksperiode skal bruke 1 dag fra aktivitet 1 og 1 fra aktivitet 2
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 05.02.2024 | 09.02.2024 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 05.02.2024 | 08.02.2024 | TILTAK    | 1               |
      | 09.02.2024 | 09.02.2024 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2024 | 02.2024 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 2            | 1000   | 59          |

  Scenario: Skal bruke 1 dag fra første aktivitet og 1 dag fra den andre aktiviteten
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 05.02.2024 | 05.02.2024 | TILTAK    | AAP       |
      | 09.02.2024 | 09.02.2024 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 05.02.2024 | 09.02.2024 | TILTAK    | 1               |
      | 05.02.2024 | 05.02.2024 | TILTAK    | 1               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2024 | 02.2024 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 2            | 1000   | 60          |