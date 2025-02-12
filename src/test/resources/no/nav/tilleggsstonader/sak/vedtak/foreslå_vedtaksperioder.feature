# language: no
# encoding: UTF-8

Egenskap: Beregning av vedtaksperioder

  Regel: Vedtaksperioder skal være snittet av målgruppe, aktivitet og vilkå®

    Scenario: Enkleste case der målgruppe, aktivitet og vilkår overlapper perfekt
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 31.03.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 31.03.2023 | TILTAK    | AAP       |

    Scenario: Det finnes ikke noen vilkår
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende feil for vedtaksforsalg: Det finnes ingen vilkår som kan brukes i vedtaksperioden

    Scenario: Det finnes ikke noen vilkår oppfylte
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat     |
        | 01.01.2023 | 31.03.2023 | IKKE_OPPFYLT |

      Når forslag til vedtaksperioder lages

      Så forvent følgende feil for vedtaksforsalg: Det finnes ingen vilkår som kan brukes i vedtaksperioden

    Scenario: Vilkåret er kortere enn stønadsperioden
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 15.02.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 15.02.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 31.01.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 31.01.2023 | TILTAK    | AAP       |

    Scenario: Stønadsperioden er kortere enn vilkåret
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 15.02.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 15.02.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 31.04.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 15.02.2023 | TILTAK    | AAP       |


    Scenario: Vedtaksperioden omsluttes av stønadsperioden
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.04.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.04.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.02.2023 | 31.03.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.02.2023 | 31.03.2023 | TILTAK    | AAP       |


    Scenario: Ingen overlapp mellom stønadsperiode og vilkår
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.04.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.04.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.05.2023 | 31.05.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende feil for vedtaksforsalg: Fant ingen gyldig overlapp mellom gitte aktiviteter, målgrupper og vilkår

  Regel: Vedtaksperiode skal foreslås for flere oppfylte vilkår som er rett etter hverandre
    Scenario: To oppfylte vilkår som er rett etter hverandre
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.04.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.04.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 31.01.2023 | OPPFYLT  |
        | 01.02.2023 | 31.02.2023 | OPPFYLT  |
        | 01.03.2023 | 31.03.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 31.03.2023 | TILTAK    | AAP       |

    Scenario: To oppfylte vilkår som ikke er rett etter hverandre
      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.04.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.04.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 31.01.2023 | OPPFYLT  |
        | 01.03.2023 | 31.03.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende feil for vedtaksforsalg: Foreløpig klarer vi bare å foreslå perioder når vilkår har ett sammenhengende overlapp. Du må i stedet legge inn periodene manuelt.