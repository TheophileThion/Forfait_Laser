package com.theot.forfait_laser;

import java.math.BigInteger;

public class RSA {
    public int chiffrer (BigInteger n, BigInteger e,int message){
        BigInteger nb = BigInteger.valueOf(message);
        int retour = nb.modPow(e,n).intValue();
        return retour;
    }

    public int dechiffrer (BigInteger d, BigInteger n, int message){
        BigInteger nb  = BigInteger.valueOf(message);
        int retour = nb.modPow(d,n).intValue();
        return retour;
    }
}
