package com.example.yitier910e.myapplication2;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yitier910e on 18/04/18.
 */

public class Save {

    private Context theThis;
    private String nameOfFolder ="/Image";

    /**
     * Fonction permettant de sauver l'image. En cas de réussite ou d'échec affiche un message informant l'utilisateur du résultat de la sauvegarde
     * @param context le contexte de l'application
     * @param ImageToSave Passage de l'image à sauvegarder en paramètre
     */
    public void SaveImage(Context context, Bitmap ImageToSave) {
        theThis = context;
        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath()+nameOfFolder;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File dir = new File(file_path);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(dir,   imageFileName + ".jpg");
        try {
            //récupération d'un flux de sorti contenant le nom unique du fichier
            FileOutputStream fout = new FileOutputStream(file);
            //Compression de ce fichier pour qu'il soit sauvegarder et exploiter convenablement
            ImageToSave.compress(Bitmap.CompressFormat.JPEG, 60, fout);
            fout.flush();
            fout.close();
            maKeSureFileWasCreatedThenMakeAvaible(file);
            ableToSave();
        } catch (FileNotFoundException e) {
            unableToSave();
        } catch (IOException e) {
            unableToSave();
        }
    }

    /**
     * Détermine si le fichier est bien créer et le rend disponible
     * @param file
     */
    private void maKeSureFileWasCreatedThenMakeAvaible(File file){
        MediaScannerConnection.scanFile(theThis, new String[]{file.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String s, Uri uri) {
                Log.e("ExternalStorage","Scanned"+s+ ":");
                Log.e("ExternalStorage", "->uri=" +uri);
            }
        });
    }

    /**
     * Affiche un message d'échec de sauvegarde lorsque ce cas se produit
     */
    private void unableToSave(){
        Toast.makeText(theThis,"Echec de la sauvegarde...",Toast.LENGTH_SHORT).show();
    }

    /**
     * Affiche un message de réussite de sauvegarde lorsque ce cas se produit
     */
    private void ableToSave(){
        Toast.makeText(theThis,"Image sauvegardée !",Toast.LENGTH_SHORT).show();
    }
}
