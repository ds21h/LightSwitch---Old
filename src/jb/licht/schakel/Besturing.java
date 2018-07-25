package jb.licht.schakel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Random;
import jb.licht.gegevens.Aktie;
import jb.licht.gegevens.Gegevens;
import jb.licht.gegevens.Huidig;
import jb.licht.klassen.Instelling;
import jb.licht.klassen.Schakelaar;

/**
 *
 * @author Jan
 */
public class Besturing {

    private final boolean cGpioAan = true; //Uitsluitend uitzetten voor testen op PC!
//    private final boolean cGpioAan = false; //Uitsluitend uitzetten voor testen op PC!

    private LocalDate mSysoutDate;
    private boolean mStop;

    private boolean mTest = false;
    private int mOndTestUur;
    private int mOndTestMinuut;
    private int mIntTestMinuut;
    private int mIntTestSec;

    private final OpkomstOndergang mZon = new OpkomstOndergang();
    private final Gegevens mGegevens = new Gegevens();
    private final AanSensor mAanSensor = new AanSensor(mGegevens, cGpioAan);
    private final Huidig mHuidig = new Huidig();

    public void xStart() {
        sZetSysout();
        mTest = false;
        mStop = false;
        sUpdate();
        if (ZonedDateTime.now().isAfter(mHuidig.xLichtUit().minusMinutes(15))) {
            mHuidig.xStartLichtAanVerwerkt();
            mGegevens.xHuidig(mHuidig);
        }
        sVerwerk();
    }

    public void xStart(int pOndUur, int pOndMinuut, int pIntMinuut, int pIntSec) {
        sZetSysout();
        mTest = true;
        mStop = false;
        if (pOndUur < 0) {
            mOndTestUur = -1;
            mOndTestMinuut = -1;
        } else {
            mOndTestUur = pOndUur;
            mOndTestMinuut = pOndMinuut;
        }
        if (pIntMinuut < 0) {
            mIntTestMinuut = -1;
            mIntTestSec = -1;
        } else {
            mIntTestMinuut = pIntMinuut;
            mIntTestSec = pIntSec;
        }
        sUpdate();
        sVerwerk();
    }

    public void xStop() {
        mStop = true;
    }

    private void sZetSysout() {
        File lFile = null;
        File lDir;
        int lTel;
        LocalDate lDatum;
        String lBest;
        boolean lBestaat;

        lDir = new File("log");
        if (!lDir.exists()) {
            lDir.mkdir();
        }
        lTel = 0;
        lDatum = LocalDate.now();
        lBestaat = true;
        while (lBestaat) {
            lBest = "LichtSchakel_" + lDatum.format(DateTimeFormatter.ISO_DATE) + "_" + String.format("%03d", lTel);
            lFile = new File(lDir, lBest);
            lBestaat = lFile.exists();
            lTel++;
        }
        try {
            System.setOut(new PrintStream(lFile));
            mSysoutDate = lDatum;
        } catch (FileNotFoundException ex) {
            mGegevens.xSchrijfLog("Exception op sysout file: " + ex.getMessage());
        }
    }

    private void sVerwerk() {
        List<Aktie> lAkties;
        Aktie lAktie;
        ZonedDateTime lNu;
        boolean lIetsGedaan;

        mGegevens.xSchrijfLog("Start achtergrond");
        while (!mStop) {
            lIetsGedaan = false;
            lNu = ZonedDateTime.now();
            try {
                if (lNu.toLocalDate().isAfter(mSysoutDate)) {
                    sZetSysout();
                }

                lAkties = mGegevens.xAkties();
                if (lAkties.size() > 0) {
                    lAktie = lAkties.get(0);
                    if (lAktie.xUitvoeren() == null || lAktie.xUitvoeren().isBefore(lNu)) {
                        lIetsGedaan = true;
                        sVerwerkAktie(lAktie);
                    }
                }

                if (mHuidig.xStartLichtAan().isBefore(lNu)) {
                    lIetsGedaan = true;
                    sTestAan();
                }

                if (mHuidig.xLichtUit().isBefore(lNu)) {
                    lIetsGedaan = true;
                    mHuidig.xLichtUitVerwerkt();
                    mHuidig.xFase(Huidig.cFaseNacht);
                    mGegevens.xHuidig(mHuidig);
                    sSchakelAlles(false);
                }

                if (mHuidig.xBijwerken().isBefore(lNu)) {
                    lIetsGedaan = true;
                    sUpdate();
                }

                if (!lIetsGedaan) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException ex) {
                mGegevens.xSchrijfLog("InteruptException: " + ex.getLocalizedMessage());
                mAanSensor.xAfsluit();
                mGegevens.xAfsluit();
                mStop = true;
            }
        }
    }

    private void sTestAan() throws InterruptedException {
        boolean lAan;
        String lMelding;
        Instelling lInst;
        ZonedDateTime lAanTest;

        lInst = mGegevens.xInstelling();

        if (ZonedDateTime.now().isAfter(mHuidig.xZonsOndergang())) {
            mGegevens.xSchrijfLog("Na zonsondergang. Licht aan.");
            lAan = true;
        } else {
            lAan = mAanSensor.xTestAan();
            lMelding = "Lichtmeting: " + mAanSensor.xLichtMeting();
            if (mAanSensor.xLichtTel() > 0) {
                lMelding = lMelding + ". Donker genoeg. Teller: " + mAanSensor.xLichtTel();
            }
            if (!lAan) {
                lAanTest = ZonedDateTime.now();
                if (mTest) {
                    if (mIntTestMinuut < 0) {
                        lAanTest = lAanTest.plusMinutes(lInst.xPeriodeMinuut()).plusSeconds(lInst.xPeriodeSec());
                    } else {
                        lAanTest = lAanTest.plusMinutes(mIntTestMinuut).plusSeconds(mIntTestSec);
                    }
                } else {
                    lAanTest = lAanTest.plusMinutes(lInst.xPeriodeMinuut()).plusSeconds(lInst.xPeriodeSec());
                }
                lMelding = lMelding + ". Volgende test om " + lAanTest;
                mHuidig.xStartLichtAan(lAanTest);
                mHuidig.xFase(Huidig.cFaseSchemer);
                mHuidig.xLichtMeting(mAanSensor.xLichtMeting());
                mGegevens.xHuidig(mHuidig);
            }
            mGegevens.xSchrijfLog(lMelding);
        }

        if (lAan) {
            mHuidig.xStartLichtAanVerwerkt();
            mHuidig.xFase(Huidig.cFaseAvond);
            mGegevens.xHuidig(mHuidig);
            sSchakelAlles(true);
        }
    }

    private void sUpdate() {
        Random lRnd;
        int lRndCorr;
        ZonedDateTime lOndergang;
        Instelling lInst;
        int lDag;

        lInst = mGegevens.xInstelling();

        mGegevens.xSchrijfLog("Start update");

        lOndergang = mZon.xOndergang(lInst.xBreedte(), lInst.xLengte()).atZoneSameInstant(ZoneId.systemDefault());
        if (mTest) {
            if (mOndTestUur < 0) {
                mHuidig.xZonsOndergang(lOndergang);
            } else {
                mHuidig.xZonsOndergang(ZonedDateTime.now().withHour(mOndTestUur).withMinute(mOndTestMinuut));
            }
        } else {
            mHuidig.xZonsOndergang(lOndergang);
        }
        mHuidig.xZetStartLichtaan();
        mAanSensor.xInit();
        mHuidig.xBijwerken(ZonedDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        mGegevens.xSchrijfLog("Zonsondergang om " + mHuidig.xZonsOndergang().toString());

        lRnd = new Random();
        lRndCorr = lRnd.nextInt(lInst.xUitTijd());
        if (lInst.xLichtUitUur() < 10) {
            lDag = 1;
        } else {
            lDag = 0;
        }
        mHuidig.xLichtUit(ZonedDateTime.now().plusDays(lDag).withHour(lInst.xLichtUitUur()).withMinute(lInst.xLichtUitMin()).withSecond(0).withNano(0).plusMinutes(lRndCorr));
        mHuidig.xFase(Huidig.cFaseDag);
        mGegevens.xHuidig(mHuidig);
        mGegevens.xSchrijfLog("Licht uit om " + mHuidig.xLichtUit().toString());
        mGegevens.xSchrijfLog("Volgende update " + mHuidig.xBijwerken().toString());
    }

    private void sSchakelAlles(boolean pAan) {
        List<Schakelaar> lSchakelaars;
        Schakelaar lSchakelaar;
        Zender lZend;
        String lMelding;
        int lTel;

        if (pAan) {
            lMelding = "Licht aan!";
        } else {
            lMelding = "Licht uit!";
        }
        mGegevens.xSchrijfLog(lMelding);
        lZend = new Zender(mGegevens, cGpioAan, true);
        lSchakelaars = mGegevens.xSchakelaars();
        for (lTel = 0; lTel < lSchakelaars.size(); lTel++) {
            lSchakelaar = lSchakelaars.get(lTel);
            mGegevens.xAktieSchakelaarUitgevoerd(lSchakelaar);
            lZend.xSchakel(lSchakelaar, pAan);
        }
    }

    private void sVerwerkAktie(Aktie pAktie) {
        ZonedDateTime lDatumTijd;
        Schakelaar lSchakel;
        Zender lZend;

        if (!pAktie.xKlaar()) {
            switch (pAktie.xType()) {
                case Aktie.cAktieRefresh:
                    sUpdate();
                    break;
                case Aktie.cAktieZetLichtUit: {
                    try {
                        lDatumTijd = ZonedDateTime.parse(pAktie.xPar(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
                        mHuidig.xLichtUit(lDatumTijd);
                        if (mHuidig.xFase() == Huidig.cFaseNacht) {
                            mHuidig.xFase(Huidig.cFaseAvond);
                        }
                        mGegevens.xHuidig(mHuidig);
                        mGegevens.xSchrijfLog("Licht uit gezet op " + lDatumTijd.toString());
                    } catch (DateTimeParseException ex) {
                        mGegevens.xSchrijfLog("Ongeldige tijd " + pAktie.xPar() + ", Aktie niet uitgevoerd");
                    }
                    break;
                }
                case Aktie.cAktieZetAan: {
                    lZend = new Zender(mGegevens, cGpioAan, false);
                    lSchakel = mGegevens.xSchakelaar(pAktie.xPar());
                    mGegevens.xAktieSchakelaarUitgevoerd(lSchakel);
                    lZend.xSchakel(lSchakel, true);
                    mGegevens.xSchrijfLog("Inschakelen " + pAktie.xPar() + "!");
                    break;
                }
                case Aktie.cAktieZetUit: {
                    lZend = new Zender(mGegevens, cGpioAan, false);
                    lSchakel = mGegevens.xSchakelaar(pAktie.xPar());
                    mGegevens.xAktieSchakelaarUitgevoerd(lSchakel);
                    lZend.xSchakel(lSchakel, false);
                    mGegevens.xSchrijfLog("Uitschakelen " + pAktie.xPar() + "!");
                    break;
                }
                case Aktie.cAktieZetAllesAan: {
                    sSchakelAlles(true);
                    break;
                }
                case Aktie.cAktieZetAllesUit: {
                    sSchakelAlles(false);
                    break;
                }
                case Aktie.cAktieGeen: {
                    mGegevens.xSchrijfLog("Dummy aktie. Niets gedaan!");
                    break;
                }
                default: {
                    mGegevens.xSchrijfLog("Onbekende aktie " + pAktie.xType() + ". Niets gedaan!");
                    break;
                }
            }
            mGegevens.xAktieUitgevoerd(pAktie);
        }
    }
}
