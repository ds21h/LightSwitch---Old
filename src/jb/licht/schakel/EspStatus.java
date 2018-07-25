/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.licht.schakel;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jan
 */
public class EspStatus {
    public static final int cOK = 0;
    public static final int cNOK = 9;
    
    private static List<String> mSchakelaar;
    private static List<Integer> mAantalFout;
    
    static {
        mSchakelaar = new ArrayList<>();
        mAantalFout = new ArrayList<>();
    }
    
    private EspStatus(){}
    
    public static void xEspAktie(String pSchakelaar, int pResultaat){
        int lTeller;
        String lSchakelaar;
        Integer lAantalFout;
        boolean lGevonden;
        
        lGevonden = false;
        for (lTeller = 0; lTeller < mSchakelaar.size(); lTeller++){
            lSchakelaar = mSchakelaar.get(lTeller);
            if (lSchakelaar.equals(pSchakelaar)){
                lGevonden = true;
                lAantalFout = mAantalFout.get(lTeller);
                if (pResultaat == cOK){
                    lAantalFout = 0;
                } else {
                    lAantalFout++;
                }
                mAantalFout.set(lTeller, lAantalFout);
                break;
            }
        }
        if (!lGevonden){
            mSchakelaar.add(pSchakelaar);
            mAantalFout.add((pResultaat == cOK) ? 0 : 1);
        }
    }
    
    public static int xAantalFout(String pSchakelaar){
        int lTeller;
        String lSchakelaar;
        Integer lAantalFout;

        lAantalFout = 0;
        for (lTeller = 0; lTeller < mSchakelaar.size(); lTeller++){
            lSchakelaar = mSchakelaar.get(lTeller);
            if (lSchakelaar.equals(pSchakelaar)){
                lAantalFout = mAantalFout.get(lTeller);
                break;
            }
        }
        return lAantalFout;
    }
}
