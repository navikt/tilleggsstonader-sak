# language: no
# encoding: UTF-8

Egenskap: Beregning - Flere stønadsperioder med full aktivitet

  Scenario: Flere stønadsperioder innenfor samme måned - en aktivitet:
    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 11.01.2023 | UTDANNING | OVERGANGSSTØNAD |
      | 12.01.2023 | 31.01.2023 | UTDANNING | AAP             |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 31.01.2023 | UTDANNING | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 22           | 1000   | 649         |

    Så V2 - forvent følgende vedtaksperiodeGrunnlag for: 01.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet | Antall dager |
      | 01.01.2023 | 11.01.2023 | OVERGANGSSTØNAD | UTDANNING | 8            |
      | 12.01.2023 | 31.01.2023 | AAP             | UTDANNING | 14           |

    Så V2 - forvent følgende beløpsperioder for: 01.2023
      | Dato       | Beløp | Målgruppe       |
      # dato skal egentligen være 01.01.2023 men er en søndag
      | 02.01.2023 | 236   | OVERGANGSSTØNAD |
      | 12.01.2023 | 413   | AAP             |

  Scenario: Flere stønadsperioder innenfor samme måned - ulike aktiviteter:
    # Mål: Beregning skal kun bruke aktiviteter som matcher aktivitet i stønadsperiode selv om det er overlapp i periode

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 11.01.2023 | UTDANNING | OVERGANGSSTØNAD |
      | 12.01.2023 | 31.01.2023 | TILTAK    | AAP             |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 31.01.2023 | UTDANNING | 5               |
      | 12.01.2023 | 31.01.2023 | TILTAK    | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 22           | 1000   | 649         |

    Så V2 - forvent følgende vedtaksperiodeGrunnlag for: 01.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet | Antall dager |
      | 01.01.2023 | 11.01.2023 | OVERGANGSSTØNAD | UTDANNING | 8            |
      | 12.01.2023 | 31.01.2023 | AAP             | TILTAK    | 14           |

    Så V2 - forvent følgende beløpsperioder for: 01.2023
      | Dato       | Beløp | Målgruppe       |
      | 02.01.2023 | 236   | OVERGANGSSTØNAD |
      | 12.01.2023 | 413   | AAP             |