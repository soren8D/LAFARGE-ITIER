package com.example.yitier910e.myapplication2;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;



public class MainActivity extends AppCompatActivity {

    // Variable permettant d'acceder aux différents boutons.
    //----------------------------------------
    private Button resetButton;
    private Button undoButton;
    private Button applyButton;
    // Variable permettant d'acceder aux différentes TextView
    //----------------------------------------
    private TextView effectText;
    private TextView valueText;
    //Variable permettant d'acceder aux différentes SeekBar
    //----------------------------------------
    private SeekBar sb1;
    private SeekBar sb2;
    private SeekBar sb3;
    //Variable permettant de gérer l'affichage et les effets appliqué à l'image
    //----------------------------------------
    private PhotoView imView;
    private Effects effect;
    PhotoViewAttacher mAttacher;
    private Item currentEffect=Item.NULL;

    //Variable permettant de gérer la sauvegarde/le chargement et la prise de photo
    //----------------------------------------
    private BitmapFactory.Options o;
    public Uri file;
    public static final int PHOTO = 1;
    public static final int GALLERY = 2;
    private String mCurrentPhotoPath;

    /**
     * Fonction qui permet de détecter quand on clique sur le bouton "apply" et appel la fonction apply.
     */
    private View.OnClickListener applyButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            applyButton.refreshDrawableState();
            apply();
        }

    };
    /**
     * Fonction qui permet de détecter quand on clique sur le bouton "undo" et appel la fonction undo.
     */
    private View.OnClickListener undoButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            effect.undo(undoButton);
            imView.setImageBitmap(effect.getBmp());

        }

    };
    /**
     * Fonction qui permet de détecter quand on clique sur le bouton "reset" et appel la fonction reset
     */
    private View.OnClickListener resetButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            effect.reset();
            imView.setImageBitmap(effect.getBmp());
        }

    };

    /**
     * Permet de créer un ficher image dans le répertoire courant.Le nom du fichier sera généré en fonction de l'heure courante afin d'éviter
     * qu'il y ait des doublons.
     * @return retourne un fichier contenant une image
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        // Le chemin est stocké dans une variable globale
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Fonction qui permet d'ouvrir la galerie afin de sélectionner la photo à afficher dans l'appli
     */
    private void dispatchOpenGalleryIntent() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, GALLERY);

    }

    /**
     * Permet de créer un fichier lors de la prise d'une photo
     */
    private void dispatchTakePictureIntent() {
        //Initialisation de l'Intent permettant de prendre une photo
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
            }
            if (photoFile != null) {
                //Permet d'associer un chemin d'accès à l'URI
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.yitier910e.myapplication2.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, PHOTO);
            }
        }
    }



    /**
     * Fonction qui permet d'ajouter une photo à la galerie
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     *Fonction permettant de calculer un coefficient pour réduire la photo.
     *
     * @param options contient les options permetant de récup
     * @param reqWidth définit la largeur de l'image que l'on souhaite à l'arrivé
     * @param reqHeight définit la longueur de l'image que l'on souahite à l'arrivé
     * @return retourne le coefficient de rétrécissement à appliquer à la photo.
     */
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // récupération de la taille de l'image de la photo qu'on souhaite réduire
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // on cherche la plus grande puissance de 2 qui divise la largeur ou la longueur à la taille souhaiter
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     *
     * @param path chemin absolue du fichier à transformer en bitmap
     * @param reqWidth largeur de la bitmap souhaité
     * @param reqHeight longueur de la bitmap souhaité
     * @param op options à passer en paramètre
     * @return retourne la bitmap contenant l'image souhaité
     */
    public static Bitmap decodeSampledBitmapFromFile(String path,
                                                         int reqWidth, int reqHeight, BitmapFactory.Options op) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // calcul du coefficient réducteur
        op.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // réduction de l'image grace à l'option "op" contenant le fameux coefficient
        Bitmap temp = BitmapFactory.decodeFile(path, op);
        return temp;
    }


    /**
     * Fonction permettant de sauver une bitmap en fichier
     */
    private void letsSave(){
        Save savefile = new Save();
        savefile.SaveImage(this,effect.getBmp());
    }



    /**
     *Fonction qui permet de mettre la bitmap dans l'imageView en prennant en compte les options afin de pouvoir la modifier
     */
    private void setPic() {

        effect.loadBmp(decodeSampledBitmapFromFile(mCurrentPhotoPath,377,358, o));
        imView.setImageBitmap(effect.getBmp());

    }

    /**
     * Fonction principale des Intent qui permet de traiter les différents appel d'intent
     * Ceux ci sont gérés par la valeur du requestCode et du resultCode.
     * Cette fonction est appelée après l'utilisation de l'intent
     *
     * Si elle est appelée par une prise de photo elle ajoute la photo à la galerie et l'affiche dans la PhotoView.
     * Si elle est appelée par l'ouverture d'une image, elle affiche la photo dans la PhotoView
     *
     * @param requestCode Code permettant de déterminer par quelle fonction celle-ci est appelé. les valeurs valable de requestCode sont 0 ou 1
     * @param resultCode Code permettant de déterminer si l'intent s'est terminé normalement ou pas.
     * @param data Contient les informations stocké dans l'intent
     *
     */
    @Override

    /**
     *
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case (PHOTO):
                    galleryAddPic();
                    setPic();
                    letsSave();
                    break;

                case (GALLERY):
                    Uri photoUri = data.getData();
                    if (photoUri != null) {
                        try {

                            effect.loadBmp(MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri));
                            imView.setImageBitmap(effect.getBmp());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                default:
                    break;
            }
        }
        else {

        }

    }





    //

    /**
     * Fonction extrêmement important qui permet de demander les permissions nécessaires afin d'enregistrer les images et de les ouvrir.
     *
     */
    private void checkPermissions() {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] permissions;
        if (apiLevel < 16) {
            permissions = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
        } else {
            permissions = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        ActivityCompat.requestPermissions(this,permissions, 0);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    /**
     * Fonction appelé à l'ouverture de l'application afin d'initialisé toutes les variables qui le nécessite ainsi que les divers boutons, seekbar, PhotoView et TextView
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imView = (PhotoView) findViewById(R.id.imgView);
        mAttacher=new PhotoViewAttacher(imView);
        resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(resetButtonListener);
        applyButton = (Button) findViewById(R.id.applyButton);
        applyButton.setOnClickListener(applyButtonListener);
        applyButton.setEnabled(false);
        effectText=(TextView) findViewById(R.id.effectId);
        valueText=(TextView)  findViewById(R.id.valueId);
        undoButton = (Button) findViewById(R.id.undoButton);
        undoButton.setOnClickListener(undoButtonListener);
        undoButton.setEnabled(false);

        sb1= (SeekBar)findViewById(R.id.seekBar);

        sb1.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        int valueInt;
                        float valueFloat;
                        switch (currentEffect){
                            case MONOCOLOR:
                                valueText.setBackgroundColor(Color.rgb(sb1.getProgress(),sb2.getProgress(),sb3.getProgress()));

                                break;
                            case LUMINOSITY:
                                 valueFloat =(float)(sb1.getProgress()-255)/255*100;
                                valueText.setText(valueFloat+ " %" );
                                break;
                            case MOYENNE:
                                 valueInt = sb1.getProgress()*2+1;
                                valueText.setText(valueInt+ "X"+valueInt );
                                break;
                            case GAUSSIENNE:
                                valueInt= sb1.getProgress()*2+1;
                                valueText.setText(valueInt+"X"+valueInt );
                                break;
                            case EXPOSITION:
                                 valueFloat = (float)sb1.getProgress()/100;
                                if (valueFloat > 1) {
                                    valueFloat = 5 * (valueFloat - 1) + 1;
                                }
                                valueText.setText("Coeff : "+valueFloat );


                                break;
                            case CARTOON:
                                valueText.setText("nombre de tour "+sb1.getProgress() );
                                break;

                            case CONTRAST:

                                valueText.setText("Min : "+sb1.getProgress()+ "\nMax : "+sb2.getProgress() );
                                break;
                        }


                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

        sb2= (SeekBar)findViewById(R.id.seekBar2);
        sb2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(currentEffect == Item.MONOCOLOR){
                    valueText.setBackgroundColor(Color.rgb(sb1.getProgress(),sb2.getProgress(),sb3.getProgress()));
                }
                else if  (currentEffect == Item.CONTRAST){
                    valueText.setText("Min : "+sb1.getProgress()+ "\nMax : "+sb2.getProgress() );
                }

            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb3= (SeekBar)findViewById(R.id.seekBar3);
        sb3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(currentEffect == Item.MONOCOLOR){
                    valueText.setBackgroundColor(Color.rgb(sb1.getProgress(),sb2.getProgress(),sb3.getProgress()));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });




        o = new BitmapFactory.Options();
        o.inMutable = true;
        o.inScaled=true;

        effect=new Effects(BitmapFactory.decodeResource(getResources(),R.drawable.lena,o),this);
         imView.setImageBitmap(effect.getBmp());
         mAttacher.update();
        checkPermissions();
    }

    /**
     * Permet de modifier et de changer le menu.
     * @param menu
     * @return
     */
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_styley,menu);
        return true;
    }

    /**
     *Permet de selectionner les effets ou les fonctions de prise de photo/charger une image/sauvegarder une image.
     * Cette fonction gère l'affichage des SeekBars qui peuvent être utilisés pour les divers effets, elle gère aussi l'activation des boutons undo et apply suivant s'ils peuvent être utilisé ou pas
     *
     * @param item
     * @return
     */
    public boolean onOptionsItemSelected(MenuItem item){
        sb1.setVisibility(View.INVISIBLE);
        sb2.setVisibility(View.INVISIBLE);
        sb3.setVisibility(View.INVISIBLE);
        sb1.setBackgroundColor(Color.WHITE);
        sb2.setBackgroundColor(Color.WHITE);
        sb3.setBackgroundColor(Color.WHITE);
        valueText.setText("");
        switch(item.getItemId()){
            case R.id.grayId:
                currentEffect=Item.GRAY;

                break;

            case R.id.sepiaId :
                currentEffect=Item.SEPIA;

                break;

            case R.id.expoId:
                sb1.setVisibility(View.VISIBLE);
                sb1.setMax(200);
                sb1.setProgress(100);
                valueText.setText("Coeff: 1");
                currentEffect=Item.EXPOSITION;

                break;
            case R.id.contourId:
                currentEffect=Item.SOBEL;

                break;

            case R.id.laplaceId:
                currentEffect=Item.LAPLACE;

                break;

            case R.id.luminId:
                sb1.setVisibility(View.VISIBLE);
                sb1.setMax(510);
                sb1.setProgress(255);
                valueText.setText("0 %");
                currentEffect=Item.LUMINOSITY;

                break;
                case R.id.contrastId:
                currentEffect=Item.CONTRAST;

                sb1.setVisibility(View.VISIBLE);
                sb2.setVisibility(View.VISIBLE);
                sb1.setMax(255);
                sb2.setMax(255);
                sb1.setProgress(0);
                sb2.setProgress(255);

                break;
            case R.id.monoId:// on peut changer la couleur en replaçant Color.RED par celle que l'on souhaite
                currentEffect=Item.MONOCOLOR;

                sb1.setVisibility(View.VISIBLE);
                sb2.setVisibility(View.VISIBLE);
                sb3.setVisibility(View.VISIBLE);
                sb1.setMax(255);
                sb2.setMax(255);
                sb3.setMax(255);
                sb1.setProgress(128);
                sb2.setProgress(128);
                sb3.setProgress(128);
                sb1.setBackgroundColor(Color.RED);
                sb2.setBackgroundColor(Color.GREEN);
                sb3.setBackgroundColor(Color.BLUE);

                break;
            case R.id.moyenneId: // a tester que sur la première image parce qu'elle est suffisament petit pour voir l'effet (image urbex)

                currentEffect=Item.MOYENNE;
                sb1.setVisibility(View.VISIBLE);
                sb1.setMax(8);
                sb1.setProgress(0);
                valueText.setText("1X1");


                break;
            case R.id.equalId:
                currentEffect=Item.EQUALISATOR;
                break;

            case R.id.gaussId:
                currentEffect=Item.GAUSSIENNE;
                sb1.setVisibility(View.VISIBLE);
                sb1.setProgress(0);
                sb1.setMax(8);
                valueText.setText("1X1");


                break;
            case R.id.photoId:
                dispatchTakePictureIntent();
                break;
            case R.id.loadId:
                dispatchOpenGalleryIntent();
                break;
            case R.id.saveId:
                letsSave();
                break;
            case R.id.cartoonId:
                currentEffect=Item.CARTOON;
                sb1.setVisibility(View.VISIBLE);
                sb1.setMax(7);
                sb1.setProgress(2);

                break;
            default:
                return false;

        }
        if(currentEffect!=Item.NULL) {
            applyButton.setEnabled(true);
            effectText.setText(currentEffect.name());
        }
        valueText.setBackgroundColor(Color.WHITE);

        return true;
    }

    /**
     *Fonction mère du programme qui permet de lancer les divers algorithmes de traitement d'image. Ceux-ci sont selectionner en fonction de la valeur de l'enum currentEffect.
     * Celui-ci est déterminé suivant l'item selectionné dans le menu.
     */
    public void apply(){
        effect.update(undoButton);
        switch (currentEffect){
            case CONTRAST:
                if (sb1.getProgress() < sb2.getProgress()) {
                    effect.applyTableHSV(effect.contrast(sb1.getProgress(), sb2.getProgress()));
                }
                else{ Toast.makeText(this,"Il faut un minimum plus petit que le maximum",Toast.LENGTH_SHORT).show();}
                break;
            case GRAY:
                effect.toGreyRS(this);
                break;
            case SEPIA:
                effect.toSepiaRS(this);
                break;
            case CARTOON:
                effect.mergeCatoon(10,sb1.getProgress());
                break;
            case SOBEL:
                effect.sobel();
                break;
            case LAPLACE:
                effect.toGreyRS(this);
                int[][] matrix = {{0,1,0},{1,-4,1},{0,1,0}};
                effect.convol(matrix);
                break;
            case MOYENNE:
            matrix = new int[sb1.getProgress()*2+1][sb1.getProgress()*2+1];
                for (int i =0; i< sb1.getProgress()*2+1;i++){
                    for (int j =0;j< sb1.getProgress()*2+1;j++){
                        matrix[i][j]=1;
                    }
                }
                effect.convol(matrix);
                break;
            case MONOCOLOR:
                effect.toGrayExcept(Color.rgb(sb1.getProgress(),sb2.getProgress(),sb3.getProgress()));

                break;
            case EXPOSITION:
                float temp = (float)sb1.getProgress()/100;
                if (temp > 1){
                    temp = 5*(temp-1)+1;
                }
                effect.expo(temp);

                break;
            case GAUSSIENNE:
                effect.convol(effect.ArrayGauss(sb1.getProgress()*2+1,sb1.getProgress()*sb1.getProgress()+1));

                break;
            case LUMINOSITY:

                effect.lumin(sb1.getProgress()-255);

                break;
            case EQUALISATOR:
                effect.applyTableHSV(effect.egalisator(effect.histoHSV()));

                break;
            default :

                break;
        }

    }

}
