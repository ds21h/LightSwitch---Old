/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.licht.schakel;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Zonsopkomst/ondergang berekenen
 *
 * Algoritme afkomstig van
 * http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
 *
 */
public class OpkomstOndergang {
    static final double cGradenNaarRadialen = Math.PI / 180;
    static final double cRadialenNaarGraden = 180 / Math.PI;

    /**
     * Bepaalt zonsondergang voor vandaag op een gegeven lokatie.
     *
     * @param pBreedte Breedtegraad (Noord positief, Zuid negatief)
     * @param pLengte Lengtegraad (Oost positief, West negatief)
     * @return UTC datum/tijd van zonsondergang
     */
    public OffsetDateTime xOndergang(double pBreedte, double pLengte) {
        LocalDate lDatum;
        int lMaand;
        int lDag;
        int lJaarDag;
        int lJaar;
        double lLengteUur;
        double lGlobaal;
        double lGemAnomaly;
        double lEchteLengte;
        double lRightAscension;
        double lEchteLengteKwadrant;
        double lRightAscensionKwadrant;
        double lSinDeclinatie;
        double lCosDeclinatie;
        double lCosUur;
        double lLocalUurHoek;
        double lLocalGemTijd;
        double lUtcTijd;
        int lTijdSec;
        OffsetDateTime lOffset;
        
        lDatum = LocalDate.now();
        lMaand = lDatum.getMonthValue();
        lDag = lDatum.getDayOfMonth();
        lJaarDag = lDatum.getDayOfYear();
        lJaar = lDatum.getYear();

        // Lengte omrekenen naar uurwaarde 
        lLengteUur = pLengte / 15;
        lGlobaal = lJaarDag + ((18 - lLengteUur) / 24);

        // Bereken de Gemiddelde Anomaliteit van de zon
        lGemAnomaly = (0.9856 * lGlobaal) - 3.289;

        // Bereken de echte lengtegraad van de zon
        lEchteLengte = (lGemAnomaly
                + (1.916 * Math.sin(lGemAnomaly * cGradenNaarRadialen))
                + (0.020 * Math.sin(lGemAnomaly * 2 * cGradenNaarRadialen))
                + 282.634);
        if (lEchteLengte < 0.0) {
            lEchteLengte += 360;
        }
        if (lEchteLengte > 360) {
            lEchteLengte -= 360;
        }

        // Bereken de Rechte Klimming (Right Ascension)
        lRightAscension = cRadialenNaarGraden * Math.atan(0.91764 * Math.tan(cGradenNaarRadialen * lEchteLengte));

        // Rechte Klimming moet in hetzelfde kwadrant zitten als de echte lengte
        lEchteLengteKwadrant = (Math.floor(lEchteLengte / 90) * 90);
        lRightAscensionKwadrant = (Math.floor(lRightAscension / 90) * 90);
        lRightAscension = lRightAscension + (lEchteLengteKwadrant - lRightAscensionKwadrant);

        // Vertaal Rechte Klimming naar uren
        lRightAscension = lRightAscension / 15;

        // Bereken de Declinatie van de zon
        lSinDeclinatie = (0.39782 * (Math.sin(cGradenNaarRadialen * lEchteLengte)));
        lCosDeclinatie = Math.cos(Math.asin(lSinDeclinatie));

        // Bereken de lokale uurhoek. Gebruik offial zenith (90gr 50') ==> cos(zenith) = -0.01454
        lCosUur = ((-0.01454) - (lSinDeclinatie * (Math.sin(cGradenNaarRadialen * pBreedte)))) / (lCosDeclinatie * Math.cos(cGradenNaarRadialen * pBreedte));
        if (lCosUur < -1) {
            // De zon gaat niet onder!
            return null;
        }

        // Uurhoek berekening afmaken en converteren naar uren
        lLocalUurHoek = (cRadialenNaarGraden * Math.acos(lCosUur)) / 15;

        // Lokale (geografische) tijd van zonsondergang berekenen.
        lLocalGemTijd = lLocalUurHoek + lRightAscension - (0.06571 * lGlobaal) - 6.622;

        // Omzetten naat UTC
        lUtcTijd = lLocalGemTijd - lLengteUur;
        if (lUtcTijd < 0) {
            lUtcTijd += 24;
        }

        // Omzetten naar OffsetDateTime formaat
        lTijdSec = (int) Math.floor(lUtcTijd * 60 * 60);
        lOffset = OffsetDateTime.of(lJaar, lMaand, lDag, 0, 0, 0, 0, ZoneOffset.UTC).plusSeconds(lTijdSec);

        return lOffset;
    }
}
