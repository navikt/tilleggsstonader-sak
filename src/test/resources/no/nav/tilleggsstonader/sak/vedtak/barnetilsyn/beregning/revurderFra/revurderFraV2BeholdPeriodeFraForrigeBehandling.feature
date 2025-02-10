# language: no
# encoding: UTF-8

Egenskap: Beregning - med revurderFra - behold perioder fra forrige behandling

  Scenario: Skal gjenbruke perioder for måneder før revurderFra

    Gitt V2 - beregningsperioder fra forrige behandling
      | Måned   |
      | 01.2024 |
      | 02.2024 |
      | 03.2024 |

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 05.02.2024 | 08.02.2024 | TILTAK    | AAP       |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 05.02.2024 | 08.02.2024 | TILTAK    | 3               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2024 | 02.2024 | 1000   |

    Når V2 - beregner med revurderFra=2024-02-05

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2024 | 10      | 0            | 5000   | 3000        |
      | 02.2024 | 29.53   | 3            | 1000   | 89          |


  Scenario: Skal gjenbruke perioder for måneder før revurderFra

    Gitt V2 - beregningsperioder fra forrige behandling
      | Måned   |
      | 01.2024 |
      | 02.2024 |
      | 03.2024 |

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 05.02.2024 | 08.02.2024 | TILTAK    | AAP       |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 05.02.2024 | 08.02.2024 | TILTAK    | 3               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2024 | 02.2024 | 1000   |

    Når V2 - beregner med revurderFra=2024-02-05

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      # antall dager 0 er pga dummy-oppsett fra "beregningsperioder fra forrige behandling"
      | 01.2024 | 10      | 0            | 5000   | 3000        |
      | 02.2024 | 29.53   | 3            | 1000   | 89          |
