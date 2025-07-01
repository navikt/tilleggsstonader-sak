# language: no
# encoding: UTF-8

Egenskap: Forslag av vedtaksperioder med behold id for å kunne bruke i revurdering

  Scenario: Beholder id for vedtaksperiode hvis den er lik som tidligere
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.03.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.03.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Gitt følgende tidligere vedtaksperioder for vedtaksforslag
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Skal beholde alle vedtaksperioder, selv om man kunde slått sammen de
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.03.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.03.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Gitt følgende tidligere vedtaksperioder for vedtaksforslag
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 2  | 01.02.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 2  | 01.02.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
    # Forslag er egentlige
    # | 1  | 01.01.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Beholder id for vedtaksperiode selv om målgruppe og aktivitet er endret
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.03.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.03.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Gitt følgende tidligere vedtaksperioder for vedtaksforslag
      | Id | Fom        | Tom        | aktivitet | målgruppe        |
      | 1  | 01.01.2023 | 31.03.2023 | UTDANNING | ENSLIG_FORSØRGER |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Beholder tidligere id men legger inn vedtaksperiode for hull som ikke tidligere fantes
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.03.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.03.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Gitt følgende tidligere vedtaksperioder for vedtaksforslag
      | Id | Fom        | Tom        | aktivitet | målgruppe        |
      | 1  | 01.01.2023 | 31.01.2023 | UTDANNING | ENSLIG_FORSØRGER |
      | 2  | 01.03.2023 | 31.03.2023 | UTDANNING | ENSLIG_FORSØRGER |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | -1 | 01.02.2023 | 28.02.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 2  | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Skal ikke gjenbruke id til vedtaksperiode som allerede er gjenbrukt
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.01.2023 | TILTAK |
      | 01.03.2023 | 31.03.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.01.2023 | AAP  |
      #| 01.03.2023 | 31.03.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.01.2023 | OPPFYLT  |
      #| 01.03.2023 | 31.03.2023 | OPPFYLT  |

    Gitt følgende tidligere vedtaksperioder for vedtaksforslag
      | Id | Fom        | Tom        | aktivitet | målgruppe        |
      | 1  | 01.01.2023 | 31.03.2023 | UTDANNING | ENSLIG_FORSØRGER |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Skal forlenge siste tidligere vedtaksperiode og ikke kun beregne snitt og legge til en ny periode
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.05.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.05.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.05.2023 | OPPFYLT  |

    Gitt følgende tidligere vedtaksperioder for vedtaksforslag
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.05.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Skal ikke forlenge siste tidligere vedtaksperiode bakover
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.05.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.05.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.05.2023 | OPPFYLT  |

    Gitt følgende tidligere vedtaksperioder for vedtaksforslag
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.03.2023 | 31.05.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | -1 | 01.01.2023 | 28.02.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 1  | 01.03.2023 | 31.05.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Skal forlenge siste tidligere vedtaksperiode og ikke kun beregne snitt og legge til en ny periode
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.05.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.05.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.05.2023 | OPPFYLT  |

    Gitt følgende tidligere vedtaksperioder for vedtaksforslag
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 2  | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | -1 | 01.02.2023 | 28.02.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 2  | 01.03.2023 | 31.05.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
