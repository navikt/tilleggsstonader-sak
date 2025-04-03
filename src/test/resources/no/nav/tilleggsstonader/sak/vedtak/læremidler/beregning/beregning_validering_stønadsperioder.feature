# language: no
# encoding: UTF-8

Egenskap: Validering av vedtaksperioder og stønadsperioder for læremidler


  Scenario: Vedtaksperioder er ikke innenfor en stønadsperiode - mangler dag i midten
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.04.2024 |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2024 | AAP       | UTDANNING |
      | 02.04.2024 | 31.04.2024 | AAP       | UTDANNING |

    Når beregner stønad for læremidler

    Så forvent følgende feil fra læremidlerberegning: Vedtaksperiode er ikke innenfor en periode med overlapp mellom aktivitet og målgruppe.

  Scenario: Vedtaksperioder er ikke innenfor en stønadsperiode - mangler dag i starten
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.04.2024 |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 02.01.2024 | 31.03.2024 | AAP       | UTDANNING |
      | 01.04.2024 | 31.04.2024 | AAP       | UTDANNING |

    Når beregner stønad for læremidler

    Så forvent følgende feil fra læremidlerberegning: Vedtaksperiode er ikke innenfor en periode med overlapp mellom aktivitet og målgruppe.


  Scenario: Vedtaksperioder er ikke innenfor en stønadsperiode - mangler dag i slutten
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 30.03.2024 | AAP       | UTDANNING |

    Når beregner stønad for læremidler

    Så forvent følgende feil fra læremidlerberegning: Vedtaksperiode er ikke innenfor en periode med overlapp mellom aktivitet og målgruppe.