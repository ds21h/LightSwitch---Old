/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.licht.schakel;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import jb.licht.gegevens.Gegevens;
import jb.licht.klassen.Instelling;

/**
 *
 * @author Jan
 */
public class AanSensor {

    private final GpioController mGpio;
    private final GpioPinDigitalOutput mSpanning;
    private final GpioPinDigitalMultipurpose mLDRsensor;
    private int mLichtTel = 0;
    private int mLichtMeting = 0;
    private int mBasisNiveau = 0;
    private final boolean cGpioAan;
    private final Gegevens mGegevens;

    public int xLichtMeting() {
        return mLichtMeting;
    }

    public int xLichtTel() {
        return mLichtTel;
    }

    public AanSensor(Gegevens pGegevens, boolean pGpioAan) {
        mGegevens = pGegevens;
        cGpioAan = pGpioAan;
        if (cGpioAan) {
            mGpio = GpioFactory.getInstance();
            mSpanning = mGpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "Spanning", PinState.LOW);
            mSpanning.setShutdownOptions(true, PinState.LOW);
            mLDRsensor = mGpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_04, "LDR sensor", PinMode.DIGITAL_OUTPUT);
        } else {
            mGpio = null;
            mSpanning = null;
            mLDRsensor = null;
        }
    }

    public void xInit() {
        mLichtTel = 0;
        mBasisNiveau = 0;
    }

    public boolean xTestAan() throws InterruptedException {
        boolean lAan = false;
        Instelling lInst;

        lInst = mGegevens.xInstelling();

        mLichtMeting = sMeetLicht(lInst.xMaxSensor());
        if (mBasisNiveau == 0) {
            if (mLichtMeting > lInst.xSensorGrens() + 5) {
                mBasisNiveau = lInst.xSensorGrens() + 5;
            } else {
                mBasisNiveau = mLichtMeting;
            }
        } else if (mLichtMeting < mBasisNiveau) {
            mBasisNiveau = mLichtMeting;
        }
        if (mLichtMeting > lInst.xSensorGrens()) {
            if (mLichtMeting > mBasisNiveau + lInst.xSensorDrempel()) {
                mLichtTel++;
//            mGegevens.xSchrijfLog("Donker genoeg. Teller = " + mLichtTel);
                if (mLichtTel > lInst.xPeriodeDonker()) {
                    lAan = true;
                }
            } else {
                mLichtTel = 0;
            }
        } else {
            mLichtTel = 0;
        }

        return lAan;
    }

    private int sMeetLicht(int pMaxSensor) throws InterruptedException {
        int lTel;
        boolean lStop;

        if (cGpioAan) {
            mLDRsensor.setMode(PinMode.DIGITAL_OUTPUT);
            mLDRsensor.low();
            Thread.sleep(100);
            mLDRsensor.setMode(PinMode.DIGITAL_INPUT);
            mSpanning.high();
            lTel = 0;
            lStop = false;
            while (!lStop) {
                if (mLDRsensor.isLow()) {
                    lTel++;
                    if (lTel > pMaxSensor) {
                        lStop = true;
                    }
                } else {
                    lStop = true;
                }
            }
            mSpanning.low();
//            mGegevens.xSchrijfLog(" Lichtmeting: " + lTel);
        } else {
            lTel = 0;
        }

        return lTel;
    }

    public void xAfsluit() {
        if (mGpio != null) {
            mGpio.shutdown();
        }
    }
}
