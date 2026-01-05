# language: no
# encoding: UTF-8

Egenskap: Beregning av vedtaksperioder med type aktivitet

  Regel: Vedtaksperioder skal være snittet av målgruppe, aktivitet og vilkår

    Scenario: Enkleste case der målgruppe, aktivitet og vilkår overlapper perfekt
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   | TypeAktivitet |
        | 01.01.2023 | 31.03.2023 | TILTAK | GRUPPEAMO     |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 31.03.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe           | TypeAktivitet |
        | 01.01.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE | GRUPPEAMO     |