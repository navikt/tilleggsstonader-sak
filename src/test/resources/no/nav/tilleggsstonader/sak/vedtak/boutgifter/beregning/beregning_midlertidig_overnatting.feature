# language: no
# encoding: UTF-8

Egenskap: Beregning av midlertidig overnatting

  Scenario: Vedtaksperiode inneholder utgift
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 07.01.2025 | 09.01.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |

    Når beregner stønad for boutgifter

    Så skal stønaden for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
      | 07.01.2025 | 09.01.2025 | 1              | 1000         | 4953      | 07.01.2025      | AAp       | TILTAK    |


#  Scenario: Ingen utgift i vedtaksperiode

#  Scenario: Utgift delvis i vedtaksperiode
#
#  Scenario: Utgift kan ikke krysse utbetalingsperiode
