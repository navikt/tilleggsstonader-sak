# language: no
# encoding: UTF-8

Egenskap: Verifisering av makssats

  Scenario: Makssats for 1 barn

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 31.01.2023 | UTDANNING | OVERGANGSSTØNAD |
      | 03.07.2023 | 03.07.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 30.07.2023 | UTDANNING | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Antall barn | Utgift | Makssats |
      | 01.2023 | 201.62  | 22           | 1           | 10000  | 4369     |
      | 07.2023 | 206.74  | 1            | 1           | 10000  | 4480     |

  Scenario: Makssats for 2 barn

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 31.01.2023 | UTDANNING | OVERGANGSSTØNAD |
      | 03.07.2023 | 03.07.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 30.07.2023 | UTDANNING | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Gitt V2 - følgende utgifter for barn med id: 2
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Antall barn | Utgift | Makssats |
      | 01.2023 | 263.04  | 22           | 2           | 20000  | 5700     |
      | 07.2023 | 269.68  | 1            | 2           | 20000  | 5844     |

  Scenario: Makssats for 3 barn

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 31.01.2023 | UTDANNING | OVERGANGSSTØNAD |
      | 03.07.2023 | 03.07.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 30.07.2023 | UTDANNING | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Gitt V2 - følgende utgifter for barn med id: 2
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Gitt V2 - følgende utgifter for barn med id: 3
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Antall barn | Utgift | Makssats |
      | 01.2023 | 298.11  | 22           | 3           | 30000  | 6460     |
      | 07.2023 | 305.63  | 1            | 3           | 30000  | 6623     |

  Scenario: Makssats for 4 barn er lik makssats for 3 barn

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 31.01.2023 | UTDANNING | OVERGANGSSTØNAD |
      | 03.07.2023 | 03.07.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 30.07.2023 | UTDANNING | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Gitt V2 - følgende utgifter for barn med id: 2
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Gitt V2 - følgende utgifter for barn med id: 3
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Gitt V2 - følgende utgifter for barn med id: 4
      | Fom     | Tom     | Utgift |
      | 01.2023 | 12.2023 | 10000  |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Antall barn | Utgift | Makssats |
      | 01.2023 | 298.11  | 22           | 4           | 40000  | 6460     |
      | 07.2023 | 305.63  | 1            | 4           | 40000  | 6623     |
