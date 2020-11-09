package com.theot.forfait_laser;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    NfcAdapter mNfcAdapter;
    RadioGroup choix_action = null;
    int nbtiredispo = 0 ;
    byte[] response ;
    byte [] testerreur;
    int signatureforfait= 33;
    int signaturebande= 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        choix_action = findViewById(R.id.choix_action);
        TextView affichagecontenu = findViewById(R.id.affichage);
        affichagecontenu.setText("nb de tire dispo : "+nbtiredispo);

        Button boutonfeu = findViewById(R.id.boutonfeu);
        boutonfeu.setOnClickListener(listenerfeu);


    }




    @Override
    protected void onNewIntent(Intent intent) {






        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            final Tag montag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            final TextView affichagecontenu = findViewById(R.id.affichage);
            TextView affichageid = findViewById(R.id.tag_detecte);
            affichageid.setText("UID :" + inverser(toHexString(montag.getId())));
            final NfcV tagV = NfcV.get(montag);


            if (choix_action.getCheckedRadioButtonId() == R.id.set_bande){
                try {
                    tagV.connect();
                   byte[] cmd = new byte[] {
                            (byte) 0x41,  //flag
                            (byte) 0x21,  //Write single block
                            (byte) 0x02,  //Block 2 (choisis arbitrairement)
                            (byte) 0x00,  //Ecriture d'une singature arbitraire d'un tag servant de bande
                            (byte) 0x00,
                            (byte) 0x00,
                            (byte) 0x00};

                   byte[] signoct = intToByteArray(chiffrer(signaturebande));
                   System.arraycopy(signoct,0,cmd,3,4);
                    Toast.makeText(this, "bande correctement initialisée", Toast.LENGTH_SHORT).show();
                    testerreur = tagV.transceive(cmd);   //Bloc 2 initialisé


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (choix_action.getCheckedRadioButtonId()==R.id.set_forfait){
                try {
                    tagV.connect();
                    byte[] cmd = new byte[] {
                            (byte) 0x41,  //flag
                            (byte) 0x21,  //Write single block
                            (byte) 0x02,  //Block 2 (choisis arbitrairement)
                            (byte) 0x00,  //Ecriture d'une singature arbitraire d'un tag servant de forfait de recharge
                            (byte) 0x00,
                            (byte) 0x00,
                            (byte) 0x00};
                    byte[] signoct = intToByteArray(chiffrer(signatureforfait));
                    System.arraycopy(signoct,0,cmd,3,4);
                    testerreur = tagV.transceive(cmd);   //Bloc 2 initialisé
                    cmd = new byte[] {
                            (byte) 0x41,  //flag
                            (byte) 0x21,  //Write single block
                            (byte) 0x01,  //Block 1 (choisis arbitrairement)
                            (byte) 0x00,  //Ecriture du nombre de tire aloué par ce forfait (initialement 0)
                            (byte) 0x00,
                            (byte) 0x00,
                            (byte) 0x00};
                    testerreur = tagV.transceive(cmd);   //Bloc 1 initialisé
                    Toast.makeText(this, "Forfait correctement initialisé", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    if (e.getMessage().equals("Tag was lost.")) {
                        // continue, because of Tag bug;
                        byte[] cmd = new byte[] {
                                (byte) 0x41,  //flag
                                (byte) 0x21,  //Write single block
                                (byte) 0x01,  //Block 1 (choisis arbitrairement)
                                (byte) 0x00,  //Ecriture du nombre de tire aloué par ce forfait (initialement 0) (On recharge le forfait à 5 avec le mode bouton "recharger le forfait")
                                (byte) 0x00,
                                (byte) 0x00,
                                (byte) 0x00};
                        try {
                            Toast.makeText(this, "Forfait correctement initialisé", Toast.LENGTH_SHORT).show();
                            testerreur = tagV.transceive(cmd);   //Bloc 1 initialisé
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                    }
                }
            }

            if (choix_action.getCheckedRadioButtonId()==R.id.recharger_forfait){
                try {
                    tagV.connect();
                    byte[] cmd = new byte[] {
                            (byte) 0x60,  //flag
                            (byte) 0x23,  //Read multiple block
                            (byte) 0x00,(byte) 0x00,(byte) 0x00, (byte) 0x00, (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                            (byte)(2 & 0x0ff),      // premier block à lire
                            (byte)((0) & 0x0ff) //nombre de block à lire
                            };
                    System.arraycopy(montag.getId(), 0, cmd, 2, 8);

                    response=tagV.transceive(cmd); //Lecture du type de forfait détecté
                    byte [] test = {(byte) 0x00,(byte) 0x00,(byte) 0x00, (byte) 0x00};
                    System.arraycopy(response,2,test,0,4);
                    int testi = dechiffrer(fromByteArray(test));

                    if (testi!=signatureforfait) {
                        Toast.makeText(this, "Il ne s'agit pas d'un tag forfait", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        cmd = new byte[] {
                                (byte) 0x41,  //flag
                                (byte) 0x21,  //Write single block
                                (byte) 0x01,  //Block 1 (choisis arbitrairement)
                                (byte) 0x00,  //Ecriture du nombre de tire aloué par ce forfait (ici on le set à 5)
                                (byte) 0x00,
                                (byte) 0x00,
                                (byte) 0x00};
                        byte[] recharge = intToByteArray(chiffrer(5));
                        System.arraycopy(recharge,0,cmd,3,4);
                        Toast.makeText(this, "Le forfait à été correctement rechargé", Toast.LENGTH_SHORT).show();
                        testerreur = tagV.transceive(cmd);   //Bloc 1 initialisé

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (choix_action.getCheckedRadioButtonId()==R.id.reloading){
                try {
                    tagV.connect();
                    byte[] cmd = new byte[] {
                            (byte) 0x60,  //flag
                            (byte) 0x23,  //Read multiple block
                            (byte) 0x00,(byte) 0x00,(byte) 0x00, (byte) 0x00, (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                            (byte)(0x02),      // premier block à lire (block signature)
                            (byte)((0) & 0x0ff) //nombre de block à lire -1
                    };
                    System.arraycopy(montag.getId(), 0, cmd, 2, 8);

                    response=tagV.transceive(cmd); //Lecture du type de forfait détecté
                    byte [] test = {(byte) 0x00,(byte) 0x00,(byte) 0x00, (byte) 0x00};
                    System.arraycopy(response,2,test,0,4);
                    int testi = dechiffrer(fromByteArray(test));
                    if (testi!=signatureforfait) {
                        Toast.makeText(this, "Il ne s'agit pas d'un tag forfait", Toast.LENGTH_SHORT).show();
                    }
                    else {
                         cmd = new byte[] {
                                (byte) 0x60,  //flag
                                (byte) 0x23,  //Read multiple block
                                (byte) 0x00,(byte) 0x00,(byte) 0x00, (byte) 0x00, (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                                (byte)(1 & 0x0ff),      // premier block à lire (block contenant l'info du nb de tire)
                                (byte)((0) & 0x0ff) //nombre de block à lire -1
                        };
                        System.arraycopy(montag.getId(), 0, cmd, 2, 8);
                        response=tagV.transceive(cmd); //Lecture de la case comptant le nb de tire donnés par ce forfait

                        System.arraycopy(response,2,test,0,4);

                        int nbtireforfait = dechiffrer(fromByteArray(test));   //Variable contenant le nombre de tire offert par le forfait


                        if (nbtireforfait==0)
                            Toast.makeText(this, "Ce forfait a déja été utilisé", Toast.LENGTH_SHORT).show();
                        else{
                            nbtiredispo += nbtireforfait;
                            cmd = new byte[] {
                                    (byte) 0x41,  //flag
                                    (byte) 0x21,  //Write single block
                                    (byte) 0x01,  //Block 1
                                    (byte) 0x00,  //Ecriture du nombre de tire aloué par ce forfait remis à 0
                                    (byte) 0x00,
                                    (byte) 0x00,
                                    (byte) 0x00};
                            affichagecontenu.setText("nb de tire dispo : "+nbtiredispo);
                            Toast.makeText(this, "Laser correctement rechargé, vous disposez de " + nbtiredispo + " tires", Toast.LENGTH_SHORT).show();
                            testerreur = tagV.transceive(cmd);   //Bloc 1 remis à 0 (On vient d'utiliser ce tag)

                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



            Button boutonfeu = findViewById(R.id.boutonfeu);
            View.OnClickListener listenerfeu = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!(choix_action.getCheckedRadioButtonId()==R.id.fire_mode)) {
                        Toast.makeText(MainActivity.this, "Mettez vous en mode tire", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        try {
                            tagV.connect();
                            byte[] cmd = new byte[]{
                                    (byte) 0x60,  //flag
                                    (byte) 0x23,  //Read multiple block
                                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                    (byte) (2 & 0x0ff),      // premier block à lire (block signature)
                                    (byte) ((0) & 0x0ff) //nombre de block à lire -1
                            };
                            System.arraycopy(montag.getId(), 0, cmd, 2, 8);

                            response = tagV.transceive(cmd); //Lecture du type de forfait détecté
                            byte[] test = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                            System.arraycopy(response, 2, test, 0, 4);
                            int testi = dechiffrer(fromByteArray(test));
                            if (testi!=signaturebande)
                                Toast.makeText(MainActivity.this, "Placez vous sur un tag bande", Toast.LENGTH_SHORT).show();
                            else {
                                if (nbtiredispo == 0)
                                    Toast.makeText(MainActivity.this, "Vous n'avez plus de tire disponible", Toast.LENGTH_SHORT).show();
                                else {
                                    nbtiredispo--;
                                    affichagecontenu.setText("nb de tire dispo : " + nbtiredispo);
                                    Toast.makeText(MainActivity.this, "Tire correctement effectué", Toast.LENGTH_SHORT).show();
                                }
                            }
                            tagV.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }
            };

                    boutonfeu.setOnClickListener(listenerfeu);
                    if (choix_action.getCheckedRadioButtonId()==R.id.testrsa){
                        /**
                        BigInteger [] liste = clefs();
                        int nb = chiffrer(164);   //(e,n)
                        int nb2 = dechiffrer(nb); //(d,n)
                        affichagecontenu.setText("Départ : 164, chiffré : "+nb+", arrivé: "+nb2);
                        */
                        try {
                            tagV.connect();
                            byte[] cmd = new byte[]{
                                    (byte) 0x60,  //flag
                                    (byte) 0x23,  //Read multiple block
                                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                    (byte) (2 & 0x0ff),      // premier block à lire (block signature)
                                    (byte) ((0) & 0x0ff) //nombre de block à lire -1
                            };
                            System.arraycopy(montag.getId(), 0, cmd, 2, 8);


                            response = tagV.transceive(cmd); //Lecture du type de forfait détecté
                            affichagecontenu.setText(toHexString(response));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }


            }

        }



    private View.OnClickListener listenerfeu = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(MainActivity.this, "Placez vous sur un tag bande", Toast.LENGTH_SHORT).show();
        }
    };



    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    public int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }







    public static String inverser (String chaine) {     //Sert à inverser lUID du tag pour l'afficher à l'endroit (inverse 2 par 2)
        String retour = "";
        for (int i=0; i < chaine.length()/2; i++) {
            retour=chaine.substring(i*2,i*2+2) + retour ;
        }
        return  retour;
    }

    public static String toHexString(byte[] bytes) {    //Converti les valeurs hexa du tag en string affichable
        char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v / 16];
            hexChars[j * 2 + 1] = hexArray[v % 16];
        }
        return new String(hexChars);
    }


    public int chiffrer (int message){
        BigInteger nb = BigInteger.valueOf(message);
        BigInteger[] liste = clefs();
        BigInteger e = liste[0];
        BigInteger n = liste[2];
        int retour = nb.modPow(e,n).intValue();
        return retour;
    }

    public int dechiffrer (int message){
        BigInteger nb  = BigInteger.valueOf(message);
        BigInteger[] liste = clefs();
        BigInteger d = liste[1];
        BigInteger n = liste[2];
        int retour = nb.modPow(d,n).intValue();
        return retour;
    }

    public BigInteger[] clefs (){
        BigInteger p = BigInteger.valueOf(1033);
        BigInteger q = BigInteger.valueOf(1579);
        BigInteger n = p.multiply(q);
        BigInteger f = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger e = BigInteger.valueOf(1628495);
        BigInteger d = e.modInverse(f);
                BigInteger[] retour = {e,d,n} ;//  On stock e, d, n
        return retour;
    }


    @Override
    public void onResume() {
        super.onResume();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }
    @Override
    public void onPause() {
        super.onPause();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }




}
