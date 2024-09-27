# language: no
# encoding: UTF-8

Egenskap: Beregning med stønadsperioder for februar med 3 aktivitetsdager. Hele februar og 2 oppsplittede perioder

  Scenario: En periode for februar, 3 aktivitetsdager
    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 01.02.2024 | 29.02.2024 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.02.2024 | 29.02.2024 | TILTAK    | 3               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2024 | 02.2024 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 14           | 1000   | 413         |

  Scenario: Februar, uppsplittet med 3 stønadsperioder, 3 aktivitetsdager
    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 01.02.2024 | 14.02.2024 | TILTAK    | AAP       |
      | 15.02.2024 | 29.02.2024 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.02.2024 | 29.02.2024 | TILTAK    | 3               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2024 | 02.2024 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 14           | 1000   | 413         |
