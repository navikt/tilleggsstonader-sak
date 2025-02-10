# language: no
# encoding: UTF-8

Egenskap: Beregning - En vedtaksperiode med full aktivitet

  Scenario: Vedtaksperiode og utgift for full måned:
    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 31.01.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 31.01.2023 | UTDANNING | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 22           | 1000   | 650         |

  Scenario: Vedtaksperiode kun deler av måned

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 02.01.2023 | 11.01.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 11.01.2023 | UTDANNING | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedbeløp |
      | 01.2023 | 29.53   | 8            | 1000   | 236        |

    Så V2 - forvent følgende vedtaksperiodeGrunnlag for: 01.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet | Antall dager |
      | 02.01.2023 | 11.01.2023 | OVERGANGSSTØNAD | UTDANNING | 8            |

  Scenario: Vedtaksperiode og utgift over flere måneder - start og slutt midt i måned
    # Mål: Beregning skal dele opp stønadsperiode som går over flere måneder uten å endre fom og tom

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 10.04.2023 | 26.05.2023 | OVERGANGSSTØNAD | UTDANNING |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 10.04.2023 | 26.05.2023 | UTDANNING | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 06.2023 | 1000   |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 04.2023 | 29.53   | 15           | 1000   | 443         |
      | 05.2023 | 29.53   | 20           | 1000   | 591         |

    Så V2 - forvent følgende vedtaksperiodeGrunnlag for: 04.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet | Antall dager |
      | 10.04.2023 | 30.04.2023 | OVERGANGSSTØNAD | UTDANNING | 15           |

    Så V2 - forvent følgende vedtaksperiodeGrunnlag for: 05.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet | Antall dager |
      | 01.05.2023 | 26.05.2023 | OVERGANGSSTØNAD | UTDANNING | 20           |