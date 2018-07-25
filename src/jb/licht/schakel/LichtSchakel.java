/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.licht.schakel;

/**
 * Dient uitsluitend om deze functie als deamon te kunnen starten.
 * 
 * Zie voor de echte functionaliteit de klasse Besturing
 */
public class LichtSchakel {

    private static final Besturing mBesturing = new Besturing();

    public static void main(String[] args) {
        Thread lRunHook;
        String lArg;
        String lParam;
        String lUurS;
        String lScheiding;
        String lMinuutS;
        int lUur = 0;
        int lMinuut = 0;
        boolean lArgGoed = false;

        if (args.length > 0) {
            lArg = args[0].trim();
            System.out.println("Ontvangen parameter: " + lArg);
            if (lArg.length() == 15) {
                lParam = lArg.substring(0, 10);
                lUurS = lArg.substring(10, 12);
                lScheiding = lArg.substring(12, 13);
                lMinuutS = lArg.substring(13);

                if (lParam.equals("Ondergang=")) {
                    if (lScheiding.equals(":")) {
                        try {
                            lUur = Integer.parseInt(lUurS);
                            lMinuut = Integer.parseInt(lMinuutS);
                            lArgGoed = true;
                        } catch (NumberFormatException nfe) {

                        }
                    }
                }
            }
            if (lArgGoed) {
                mBesturing.xStart(lUur, lMinuut, 0, 15);
            } else {
                System.out.println("Juiste formaat: Ondergang=hh:mm");
            }
        } else {

            lRunHook = new Thread() {
                @Override
                public void run() {
                    sShutDown();
                }
            };
            Runtime.getRuntime().addShutdownHook(lRunHook);

            mBesturing.xStart();
        }
    }

    private static void sShutDown() {
        mBesturing.xStop();
    }
}
