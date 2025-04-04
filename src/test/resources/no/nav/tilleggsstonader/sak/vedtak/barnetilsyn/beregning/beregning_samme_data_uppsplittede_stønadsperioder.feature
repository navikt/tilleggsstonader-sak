# language: no
# encoding: UTF-8

Egenskap: Beregning med vedtaksperioder for februar med 3 aktivitetsdager. Hele februar og 2 oppsplittede perioder

  Scenario: En periode for februar, 3 aktivitetsdager
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.02.2024 | 29.02.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.02.2024 | 29.02.2024 | TILTAK    | 3               |

    Gitt følgende målgrupper
      | Fom        | Tom        | Målgruppe |
      | 01.02.2024 | 29.02.2024 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2024 | 02.2024 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 14           | 1000   | 413         |

  Scenario: Februar, uppsplittet med 3 vedtaksperioder, 3 aktivitetsdager
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.02.2024 | 14.02.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 15.02.2024 | 29.02.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE  |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.02.2024 | 29.02.2024 | TILTAK    | 3               |

    Gitt følgende målgrupper
      | Fom        | Tom        | Målgruppe |
      | 01.02.2024 | 29.02.2024 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2024 | 02.2024 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 14           | 1000   | 413         |

  Scenario: Skal trekke 1 dag for første aktiviteten og 1 fra den andre då den andre perioden kun overlapper med
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 05.02.2024 | 06.02.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 07.02.2024 | 07.02.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 05.02.2024 | 07.02.2024 | TILTAK    | 1               |
      | 07.02.2024 | 08.02.2024 | TILTAK    | 3               |

    Gitt følgende målgrupper
      | Fom        | Tom        | Målgruppe |
      | 01.02.2024 | 29.02.2024 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2024 | 02.2024 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2024 | 29.53   | 2            | 1000   | 60          |
