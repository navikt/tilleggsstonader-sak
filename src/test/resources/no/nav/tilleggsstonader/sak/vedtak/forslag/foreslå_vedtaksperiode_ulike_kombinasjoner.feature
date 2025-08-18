# language: no
# encoding: UTF-8

Egenskap: Foreslå vedtaksperioder v2

  Scenario: Skal kunne foreslå vedtaksperioder med ulike typer kombinasjoner av målgrupper, aktiviteter og vilkår
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type               |
      | 01.01.2023 | 31.01.2023 | TILTAK             |
      | 01.02.2023 | 28.02.2023 | UTDANNING          |
      | 01.03.2023 | 31.03.2023 | REELL_ARBEIDSSØKER |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type              |
      | 01.01.2023 | 31.01.2023 | AAP               |
      | 01.02.2023 | 28.02.2023 | OVERGANGSSTØNAD   |
      | 01.03.2023 | 31.03.2023 | OMSTILLINGSSTØNAD |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet          | målgruppe           |
      | -1 | 01.01.2023 | 31.01.2023 | TILTAK             | NEDSATT_ARBEIDSEVNE |
      | -1 | 01.02.2023 | 28.02.2023 | UTDANNING          | ENSLIG_FORSØRGER    |
      | -1 | 01.03.2023 | 31.03.2023 | REELL_ARBEIDSSØKER | GJENLEVENDE         |

  Scenario: Skal kunne foreslå vedtaksperioder der aktiviteter ikke er sammenhengende
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.01.2023 | TILTAK |
      | 01.03.2023 | 31.03.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.03.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | -1 | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | -1 | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Skal kunne foreslå vedtaksperioder der stønadsvilkår ikke er sammenhengende
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.03.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.03.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.01.2023 | OPPFYLT  |
      | 01.03.2023 | 31.03.2023 | OPPFYLT  |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | -1 | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | -1 | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Skal kunne foreslå vedtaksperioder der med flere aktiviteter men kun en som er gyldig sammen med målgruppe
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type               |
      | 01.01.2023 | 31.03.2023 | TILTAK             |
      | 01.01.2023 | 31.03.2023 | REELL_ARBEIDSSØKER |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.03.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | -1 | 01.01.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |


  Scenario: Skal feile hvis man har flere kombinasjoner av aktiviteter som overlapper
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type      |
      | 01.01.2023 | 31.01.2023 | TILTAK    |
      | 01.01.2023 | 31.01.2023 | UTDANNING |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 28.02.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende feil for vedtaksforsalg: Foreløpig klarer vi bare å foreslå perioder når målgruppe og aktivitet har ett sammenhengende overlapp. Her må du i stedet legge inn periodene manuelt.

  Scenario: Skal feile hvis man har flere kombinasjoner av målgrupper som overlapper
    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type      |
      | 01.01.2023 | 28.02.2023 | UTDANNING |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type            |
      | 01.01.2023 | 31.01.2023 | AAP             |
      | 01.01.2023 | 31.01.2023 | OVERGANGSSTØNAD |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Når forslag til vedtaksperioder behold id lages

    Så forvent følgende feil for vedtaksforsalg: Foreløpig klarer vi bare å foreslå perioder når målgruppe og aktivitet har ett sammenhengende overlapp. Her må du i stedet legge inn periodene manuelt.
