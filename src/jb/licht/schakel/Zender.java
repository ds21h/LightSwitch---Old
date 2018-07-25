/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.licht.schakel;

import java.io.IOException;
import java.time.ZonedDateTime;
import jb.licht.gegevens.Aktie;
import jb.licht.gegevens.Gegevens;
import jb.licht.klassen.Schakelaar;
import org.json.JSONObject;

/**
 *
 * @author Jan
 */
public class Zender {

    private final boolean mMetPauze;
    private final boolean mIoAan;
    private final Gegevens mGegevens;

    public Zender(Gegevens pGegevens, boolean pIoAan, boolean pMetPauze) {
        mGegevens = pGegevens;
        mIoAan = pIoAan;
        mMetPauze = pMetPauze;
    }

    public void xSchakel(Schakelaar pSchakelaar, boolean pAan) {
        if (pSchakelaar.xAktief()) {
            if (pSchakelaar.xType().equals("esp")) {
                sSchakelIot(pSchakelaar, pAan);
            } else {
                if (mIoAan) {
                    sSchakelFM(pSchakelaar, pAan);
                }
            }
        }
    }

    private void sSchakelFM(Schakelaar pSchakelaar, boolean pAan) {
        Runtime lRun;
        Process lProces;
        String lCommand;
        String lAktie;

        if (mMetPauze) {
            try {
                Thread.sleep(pSchakelaar.xPauze());
            } catch (InterruptedException ex) {
            }
        }

        lRun = Runtime.getRuntime();
        if (pAan) {
            lAktie = "on";
        } else {
            lAktie = "off";
        }

        lCommand = "/usr/local/licht/schakelen/" + pSchakelaar.xType() + " " + pSchakelaar.xGroep() + " " + pSchakelaar.xPunt() + " " + lAktie;

        try {
            lProces = lRun.exec(lCommand);
            lProces.waitFor();
            Thread.sleep(1000);
            lProces = lRun.exec(lCommand);
            lProces.waitFor();
        } catch (IOException | InterruptedException ex) {
        }
    }

    private void sSchakelIot(Schakelaar pSchakelaar, boolean pAan) {
        RestAPI lRestAPI;
        JSONObject lVraag;
        String lAktie;
        String lUrl;
        RestAPI.RestResult lResult;
        boolean lFout;
        JSONObject lAntwoord;
        String lResultaat;
        String lStatus;
        Aktie lCorrAktie;
        String lAktieType;
        ZonedDateTime lAktieMoment;
        int lAantFout;
        int lInterval;

        if (mMetPauze) {
            try {
                Thread.sleep(pSchakelaar.xPauze());
            } catch (InterruptedException ex) {
            }
        }

//        lUrl = "http://" + pSchakelaar.xIP() + "/Schakelaar/" + pSchakelaar.xNaam();
        lUrl = "http://" + pSchakelaar.xIP() + "/Schakelaar";
        if (pAan) {
            lAktie = "aan";
        } else {
            lAktie = "uit";
        }
        lVraag = new JSONObject();
        lVraag.put("status", lAktie);
        lRestAPI = new RestAPI();
        lRestAPI.xUrl(lUrl);
        lRestAPI.xMethod(RestAPI.cMethodPut);
        lRestAPI.xMediaVraag(RestAPI.cMediaJSON);
        lRestAPI.xMediaAntwoord(RestAPI.cMediaJSON);
        lRestAPI.xAktie(lVraag.toString());

        lResult = lRestAPI.xRoepApi();

        if (lResult.xResult() == Resultaat.cResultOK) {
            if (lResult.xAntwoordJ() == null) {
                lFout = true;
            } else {
                lAntwoord = lResult.xAntwoordJ();
                lResultaat = lAntwoord.optString("resultaat", "");
                lStatus = lAntwoord.optString("status", "");
                if (lResultaat.equals("OK")) {
                    if (lStatus.equals(lAktie)) {
                        lFout = false;
                    } else {
                        lFout = true;
                    }
                } else {
                    lFout = true;
                }
            }
        } else {
            lFout = true;
        }

        EspStatus.xEspAktie(pSchakelaar.xNaam(), (lFout) ? EspStatus.cNOK : EspStatus.cOK);
        if (lFout) {
            lAantFout = EspStatus.xAantalFout(pSchakelaar.xNaam());
            if (lAantFout > 10) {
                if (lAantFout > 15) {
                    lInterval = 60;
                } else {
                    lInterval = 10;
                }
            } else {
                lInterval = 1;
            }
            if (pAan) {
                lAktieType = Aktie.cAktieZetAan;
            } else {
                lAktieType = Aktie.cAktieZetUit;
            }
            lAktieMoment = ZonedDateTime.now().plusMinutes(lInterval);
            lCorrAktie = new Aktie(lAktieMoment, lAktieType, pSchakelaar.xNaam());
            mGegevens.xNieuweAktie(lCorrAktie);
        }
    }
}
