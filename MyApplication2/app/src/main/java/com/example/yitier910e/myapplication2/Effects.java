package com.example.yitier910e.myapplication2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;


/**
 * Created by yitier910e on 11/04/18.
 */


public class Effects {
    private Bitmap bmp;
    private Bitmap bmpSave;
    private Bitmap lastBmp;
    int[] inK;
    Context theThis;

    /**
     * Constructeur d'une entité effect qui permet de gérer l'application des différents effets d'imagerie.
     * @param b
     */
    public Effects(Bitmap b,Context c){
        loadBmp(b);
        theThis=c;
    }

    /**
     * Permet de retourner la Bitmap
     * @return
     */
    public Bitmap getBmp(){
        return bmp;
    }


    /**
     * Fonction permetant d'afficher la bitmap précédant un effet afin d'annuler celui-ci.
     * Une fois cette fonction utilisé le bouton qui l'appel n'est plus selectionnable.
     * @param b
     */
    public void undo(Button b){
        b.setEnabled(false);
        bmp = lastBmp.copy(lastBmp.getConfig(), true);
    }

    /**
     * Fonction qui permet de mettre a jour la bitmap servant à faire un retour en arrière.
     * @param b contient la référence du bouton "Undo" afin de le rendre enable.
     */
    public void update(Button b) {
        lastBmp = bmp.copy(bmp.getConfig(), true);
        b.setEnabled(true);
    }

    /**
     * Permet de charger une bitmap passé en paramètre. Celle-ci devient la bitmap de référence et est stocké dans les différentes variables bmp
     * @param b
     */
    public void loadBmp(Bitmap b){
        try {
            lastBmp = bmp.copy(bmp.getConfig(),true);
        }
        catch(NullPointerException e){
        }
        bmp=b.copy(b.getConfig(), true);
        bmpSave=b.copy(b.getConfig(), true);
    }

    /**
     * Permet de recharger la dernière image ouverte sans modification
     */
    public void reset(){
        bmp= bmpSave.copy(bmpSave.getConfig(),true);
    }

    /**
     * Fonction permettant de mettre l'image en niveau de gris, celle-ci utilise la technologie Renderscript permettant une optimisation de temps.
     * @param c Le contexte de l'application
     */
    public  void  toGreyRS(android.content.Context c) {
        //1)  Creer un  contexte  RenderScript
        android.support.v8.renderscript.RenderScript rs = android.support.v8.renderscript.RenderScript.create(c);
        //2)  Creer  des  Allocations  pour  passer  les  donnees
        android.support.v8.renderscript.Allocation input = android.support.v8.renderscript.Allocation.createFromBitmap(rs , bmp);
        android.support.v8.renderscript.Allocation output = android.support.v8.renderscript.Allocation.createTyped(rs , input.getType ());
        //3)  Creer le  script
        ScriptC_grey  greyScript = new  ScriptC_grey(rs);
        //4)  Copier  les  donnees  dans  les  Allocations
        // ...
        //5)  Initialiser  les  variables  globales  potentielles
        // ...
        //6)  Lancer  le noyau
        greyScript.forEach_toGrey(input , output);
        //7)  Recuperer  les  donnees  des  Allocation(s)
        output.copyTo(bmp);
        //8)  Detruire  le context , les  Allocation(s) et le  script
        input.destroy (); output.destroy ();
        greyScript.destroy (); rs.destroy ();
    }


    /**
     * Fonction qui met en application un filtre sepia en utilisant la technologie RenderScript.
     * @param c le contexte
     */
    public  void  toSepiaRS(android.content.Context c) {

        android.support.v8.renderscript.RenderScript rs = android.support.v8.renderscript.RenderScript.create(c);
        android.support.v8.renderscript.Allocation input = android.support.v8.renderscript.Allocation.createFromBitmap(rs , bmp);
        android.support.v8.renderscript.Allocation output = android.support.v8.renderscript.Allocation.createTyped(rs , input.getType ());
        ScriptC_sepia  sepiaScript = new  ScriptC_sepia(rs);
        sepiaScript.forEach_toSepia (input , output);
        output.copyTo(bmp);
        input.destroy (); output.destroy ();
        sepiaScript.destroy (); rs.destroy ();
    }


    /**
     * Fonction permettant d'appliquer une convolution suivant un filtre de Sobel. Cette fonction met en avant les contours de l'image.
     */
    public void sobel(){
        int h = bmp.getHeight();
        int w = bmp.getWidth();
        int[] pixels = new  int [w*h];
        int[] pixelsf = new int [w*h];
        // On met l'image en niveau de gris
        toGreyRS(theThis);
        // On récupère les pixels de l'image dans deux tableaux différents
        bmp.getPixels(pixels, 0, w,  0, 0, w, h);
        bmp.getPixels(pixelsf, 0, w,  0, 0, w, h);
        // On définit des matrices de dérivation 3x3 en X et en Y
        int[] derX = {-1,-1,-1,0,0,0,1,1,1};
        int[] derY = {-1,0,1,-1,0,1,-1,0,1};
        int[] currentPixel = new  int [9];
        // On parcourt chaque pixel
        for (int k = 1; k < w - 1; k++) {
            for (int l = 1; l < h - 1; l++) {
                int sumX = 0;
                int sumY = 0;

                // On récupère le niveau de gris du pixel et de ses voisins dans un rayon de 1.
                for (int m = 0; m < 3; m++) {
                    currentPixel[3*m] = Color.red(pixels[k - 1 + (l+m-1)*w]);
                    currentPixel[3*m+1] = Color.red(pixels[k + (m-1+l)*w]);
                    currentPixel[3*m+2] = Color.red(pixels[k + 1 + (m+l-1)*w]);
                }
                //Puis on multiplie chacun de ces points avec les matrices de dérivation et on somme.
                for (int n = 0; n < 9; n++) {
                    sumX += currentPixel[n] * derX[n];
                    sumY += currentPixel[n] * derY[n];
                }
                // Enfin On calcule la norme composée des deux dérivés et on obtient le contouring.
                int norm = (int) Math.min(Math.sqrt(sumX*sumX + sumY*sumY), 255);
                pixelsf[k + l*w] = Color.rgb(norm, norm, norm);
            }
        }
        bmp.setPixels(pixelsf, 0, w,  0, 0, w, h);
    }


    /**
     * Fonction permettant de retourner un tableau de pixel contenant les pixels de l'image après application d'un filtre Sobel. La particularité de cette fonction
     * comparé à Sobel est qu'elle ne modifie pas la Bitmap.
     * @return le tableau de Pixel contenant la transformation de Sobel
     */
    public int[] cartoonContouring(){
        int h = bmp.getHeight();
        int w = bmp.getWidth();
        int[] pixels = new  int [w*h];
        // On récupère la Bitmap en niveau de gris
        Bitmap gray = toGray();
        int[] out = new int [w*h];
        // On stock dans pixels la matrice en niveau de gris
        gray.getPixels(pixels, 0, w,  0, 0, w, h);
        //On stock dans out la matrice de l'image et le reste est exactement la même chose que la fonction sobel sauf que ça ne remplace pas la bitmap.
        bmp.getPixels(out, 0, w,  0, 0, w, h);
        int[] derX = {-1,-1,-1,0,0,0,1,1,1};
        int[] derY = {-1,0,1,-1,0,1,-1,0,1};
        int[] currentPixel = new  int [9];
        for (int k = 1; k < w - 1; k++) {
            for (int l = 1; l < h - 1; l++) {
                int sumX = 0;
                int sumY = 0;
                for (int m = 0; m < 3; m++) {
                    currentPixel[3*m] = Color.red(pixels[k - 1 + (l+m-1)*w]);
                    currentPixel[3*m+1] = Color.red(pixels[k + (m-1+l)*w]);
                    currentPixel[3*m+2] = Color.red(pixels[k + 1 + (m+l-1)*w]);
                }
                for (int n = 0; n < 9; n++) {
                    sumX += currentPixel[n] * derX[n];
                    sumY += currentPixel[n] * derY[n];
                }
                int norm = (int) Math.min(Math.sqrt(sumX*sumX + sumY*sumY), 255);
                out[k + l*w] = Color.rgb(norm, norm, norm);
            }
        }
        return out;
    }


    /**
     * Fonction permettant l'application de notre filtre supérieur Cartoon ! Il utilise la technique de seuillage des couleurs k-means et la détection des contours de Sobel.
     * En fonction de l'intensité du contour, celui-ci est amplifié.
     * Pour le k-means le seuillage se fait en fonction des paramètres n et k qui détermine le nombre de couleurs gardés et le nombre d'appel récursif fait
     * @param n détermine le nombre de couleurs choisi pour seuiller l'image
     * @param r détemine le nombre de tour de boucle effectué pour une précision des couleurs plus effective
     */
    public void mergeCatoon(int n , int r){
        int h = bmp.getHeight();
        int w = bmp.getWidth();
        int[] out= new int[w*h];
        // Contient la matrice de contour de sobel
        int[] in = cartoonContouring();
        // effectue un seuillage des couleurs k-means et modifie la bitmap
        kMeans(n,r , null, false);
        //stock la matrice dans le tableau de pixel out
        bmp.getPixels(out, 0, w,  0, 0, w, h);
        //Parcourt tous les pixels de sobel et si le seuil de contouring est assez élevé on épaissit les trais ou on fait disparaitre le bruit,
        //puis on le applique sur la matrice contenant le changement k-means.
        for(int i=4; i<w-4;i++){
            for(int j=4;j<h-4;j++) {
                if (Color.red(in[j*w+i]) > 150) {
                    for(int k=-4;k<4;k++){
                        for(int l=-4;l<4;l++){
                             out[(j+l)*w+i+k]=Color.BLACK;
                        }
                    }
                }
                else if(Color.red(in[j*w+i]) > 70) {
                    for(int k=-2;k<2;k++){
                        for(int l=-2;l<2;l++){
                            out[(j+l)*w+i+k]=Color.BLACK;
                        }
                    }
                }
            }
        }
        bmp.setPixels(out, 0, w,  0, 0, w, h);
    }

    /**
     * Permet de calculer la distance entre deux couleurs selon la norme euclidienne
     * @param a couleur a
     * @param b couleur b
     * @return la distance euclidienne des deux couleurs
     */
    private double range(int a, int b){
        int ar = Color.red(a);
        int ab= Color.blue(a);
        int ag= Color.green(a);

        int br=Color.red(b);
        int bb=Color.blue(b);
        int bg=Color.green(b);
        //Retourne la distance euclidienne des deux couleurs
        return Math.sqrt((ar - br)*(ar - br)+(ab - bb)*(ab - bb)+(ag - bg)*(ag - bg));
    }

    /**
     * Fonction déterminant la valeur moyenne d'une liste de pixel proche afin d'affiner la recherche pour le k-means
     * @param list contient une liste de pixel qui sont assez proche
     * @return retourne la couleur moyenne de ces pixels
     */
    private int average(ArrayList<Integer> list){
        int sumR = 0;
        int sumG = 0;
        int sumB =0;
        int h = bmp.getHeight();
        int w = bmp.getWidth();
        int[] in = new  int [w*h];
        bmp.getPixels(in, 0, w,  0, 0, w, h);
        int pix;
        int i =1;

        //Parcourt la liste des pixels et on somme sur chaque composante rgb
        for(Iterator it = list.iterator(); it.hasNext();) {
            pix = (int) it.next();
            sumR += Color.red(in[pix]);
            sumG += Color.green(in[pix]);
            sumB += Color.blue(in[pix]);
            i++;
        }

        // On divise par le nombre de pixels pour avoir la valeur moyenne de chaque composante
        sumR /= i;
        sumG /= i;
        sumB /= i;
        return Color.rgb((int)sumR,(int)sumG,(int)sumB);
    }

    /**
     * Cette fonction avancée permet de déterminer au mieux les couleurs moyennes les plus présente dans une image. Elle prends des pixels aux hasards puis parcours
     * l'image et ajoute les pixels dans un tableau contenant la couleur choisit aléatoirement la plus proche
     *
     *
     * @param k Nombre de couleur choisi pour le seuillage
     * @param round Nombre d'itération pour appeler la fonction récursivement
     * @param aver Tableau de valeur de couleur déterminé par un tour de boucle précédent. Au premier appel, cette variable vaut null
     * @param init état permettant de déterminer s'il s'agit du premier appel de la fonction ou pas. si c'est le cas effectue l'initialisation du tableau aver
     */
    public void kMeans(int k,int round,int[] aver,boolean init){
        int h = bmp.getHeight();
        int w = bmp.getWidth();
        Random r = new Random();
        ArrayList<Integer>[] ker= new ArrayList[k]; // Tu crées un tableau d'ArrayList normaux
        // Si l'initialisation n'est pas faite on initialise le tableau
        if(init == false) {
            inK = new int[w * h];
            bmp.getPixels(inK, 0, w, 0, 0, w, h);
            aver = new int[k];
        }
        // On met des pixels aléatoire de l'image dans le tableau de liste
        for(int i = 0 ; i < k ; i++){
            ker[i] = new ArrayList();
            if(init == false){
                aver[i] = inK[r.nextInt(w*h)];
            }
        }
        // On parcourt les pixels et on les ajoute à la liste dont la distance avec le référent est la plus faible
        for (int i = 0; i< w*h;i++){
            int min = 0;
            for (int j = 1;j<k;j++){
                if (range(aver[min],inK[i]) > range(aver[j],inK[i])){
                    min = j;
                }
            }
            ker[min].add(i);
        }
        // On recalcule la valeur moyenne de chaque liste
        for (int i =0; i<k; i++){
            aver[i] = average(ker[i]);
        }
        // Si le nombre de tour demandé est réalisé on affiche le rendu
        if (round ==0 ){
            for (int i =0; i<k;i++){
                for(Iterator it = ker[i].iterator(); it.hasNext();) {
                    inK[(int) it.next()] = aver[i];
                }
            }
            bmp.setPixels(inK, 0, w, 0, 0,w , h);
        }
        //Sinon on réitère.
        else{
            kMeans(k, round-1, aver,true);
        }
    }



    /**
     * Fonction qui permet d'appliquer une convolution à l'image. En fonction de la matrice passée en paramètre les effets peuvent être différent.
     * Cela peut être représenté par un flou (via calcule d'une moyenne)
     * Cela peut être représenté par un filtre laplacien (vla une matrice laplacienne)
     * @param conv
     */
    public void convol(int[][] conv) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();

        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);

        int[] newPixels = new int[w * h];
        bmp.getPixels(newPixels, 0, w, 0, 0, w, h);

        int length = conv.length;
        int l2 = (length - 1) / 2;
        int div=0;
        for(int i=0; i<length;i++){
            for(int j=0; j<length;j++){
                div+= conv[i][j];
            }
        }
        if(div==0)
            div=1;

        for (int i = l2; i < w - l2; i++) {
            for (int j = l2; j < h - l2; j++) {
                int summR=0;
                int summG=0;
                int summB=0;
                for (int k = 0; k < length; k++) {
                    for (int l = 0; l < length; l++) {
                        int c = pixels[i + k - l2 + (j + l - l2) * w];
                        summR += Color.red(c) * conv[k][l];
                        summG += Color.green(c) * conv[k][l];
                        summB += Color.blue(c) * conv[k][l];
                    }

                }
                summR/=div;
                summG/=div;
                summB/=div;
                newPixels[i+(j*w)] = Color.rgb(summR,summG,summB);
            }
        }
        bmp.setPixels(newPixels, 0, w, 0, 0,w , h);
    }

    /**
     *Fonction permettant de calculer les coefficients de la gaussienne
     *
     * @param x position du pixel
     * @param y position du pixel
     * @param sigma le coefficient de courbure de la gaussienne
     * @param mu position du centre
     * @param a la valeur centrale
     * @return
     */
    public double gauss(int x, double y, double sigma,int mu, double a){
        return a*Math.exp(-((Math.pow(x-mu,2))+Math.pow(y-mu,2))/(2*sigma*sigma));
    }



    /**
     * Fonction permettant de retourner la matrice de la Gaussienne en parcourant seuelement 1/8 des valeurs de la matrice
     * @param size représente la taille de la matrice
     * @param a représente la valeur du pic de la gaussienne
     * @return
     */
     public int[][] ArrayGauss(int size,int a){

        int [][] tab= new int[size][size];
        int mu= (size-1)/2;
        double  sigma = mu/Math.sqrt((Math.log(a)*2));
        for(int i= 0; i<=mu;i++){
            for(int j=0; j<=i;j++){
                int g= (int)gauss(i,j,sigma,mu,a);
                tab[i][j]=g;
                tab[j][i]=g;
                tab[2*mu-i][j]=g;
                tab[2*mu-j][i]=g;
                tab[i][2*mu-j]=g;
                tab[j][2*mu-i]=g;
                tab[2*mu-i][2*mu-j]=g;
                tab[2*mu-j][2*mu-i]=g;
            }
        }
        return tab;
    }


    /**
     * Fonction qui retourne l'histograme de la luminance d'une image
     * @return
     */
    public int[] histoHSV(){
        int[] hist=new int[256];
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = new int[w*h];
        bmp.getPixels(pixels, 0, w , 0,0,w,h);
        for(int i=0;i< w*h;i++){
            int r = Color.red(pixels[i]);
            int g = Color.green(pixels[i]);
            int b = Color.blue(pixels[i]);
            float[] hsv = new float[3];
            Color.RGBToHSV(r, g, b, hsv);
            hist[(int)(hsv[2]*255)]+=1;
        }
        return hist;
    }



    /**
     * Met l'image en gris sauf la couleur passée dans le paramètre c
     * Dans le cas de l'application le choix de la couleur se fera par les SeekBars qui apparaitrons après avoir choisi l'option Monocolor
     * @param c
     */
    public void toGrayExcept(int c){
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = new int[w*h];
        bmp.getPixels(pixels, 0, w , 0,0,w,h);
        float[] hsvC = new float[3];
        Color.colorToHSV(c, hsvC);
        for (int i = 0; i<w*h;i++){
            float[] hsv = new float[3];
            int r = Color.red(pixels[i]);
            int g = Color.green(pixels[i]);
            int b = Color.blue(pixels[i]);
            Color.RGBToHSV(r, g, b, hsv);
            if (hsv[0]>hsvC[0]+25 || hsv[0]<hsvC[0]-25) {
                int gl = (int) (r * 0.3 + g * 0.59 + b * 0.11);
                pixels[i] = Color.rgb(gl, gl, gl);
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0,w , h);
    }

    /**
     * Fonction qui retourne une bitmap en noir et blanc sans modifié la bitmap d'origine
     * @return
     */
    public Bitmap toGray(){
        Bitmap bit = bmp.copy(bmp.getConfig(),true);
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = new int[w*h];
        bmp.getPixels(pixels, 0, w , 0,0,w,h);
        for (int i = 0; i<w*h;i++){
            int r = Color.red(pixels[i]);
            int g = Color.green(pixels[i]);
            int b = Color.blue(pixels[i]);
            int gl = (int) (r*0.3+g*0.59+b*0.11);
            pixels[i] = Color.rgb(gl,gl,gl);
        }
        bit.setPixels(pixels, 0, w, 0, 0,w , h);
        return bit;
    }

    /**
     * Fonction permettant de retourner une table d'association grâce à un histogramme. Cette table d'association sera dispersé
     * sur les valeurs 0-255 permettant d'éclairer l'image
     * @param hist
     * @return
     */
    public int[] egalisator(int[] hist) {
        int nbPix = bmp.getWidth()*bmp.getHeight();
        int[] tabAss = new int[256];
        int cpt = 0;
        for (int i = 0; i < 256; i++) {
            cpt += hist[i];
            tabAss[i] = cpt * 255 / nbPix;
        }
        return tabAss;
    }


    /**
     * Fonction qui remonte la luminosité de l'image en ajoutant alpha selectionné via les SeekBars lors de la selection de l'option du menu Luminosity
     *
     * @param alpha
     */
    public void lumin(int alpha){
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = new int[w*h];
        bmp.getPixels(pixels, 0, w , 0,0,w,h);
        for (int i = 0; i<w*h;i++){
            pixels[i] = Color.rgb(Math.max(0,Math.min(255,Color.red(pixels[i])+alpha)),Math.max(0,Math.min(255,Color.green(pixels[i])+alpha)),Math.max(0,Math.min(255,Color.blue(pixels[i])+alpha)));
        }
        bmp.setPixels(pixels, 0, w, 0, 0,w , h);
    }
    //

    /**
     * Fonction qui applique un coefficient alpha à la luminosité aillant pour effet de la rendre plus claire ou sombre en fonction de celui-ci
     * Si l'alpha est compris entre 0 et 1 alors l'image sera plus sombre.
     * Si l'alpha est ocmpris entre 1 et +oo alors l'image sera plus clair
     * @param alpha
     */
    public void expo(double alpha){
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = new int[w*h];
        bmp.getPixels(pixels, 0, w , 0,0,w,h);
        for (int i = 0; i<w*h;i++){
            int red = (int)(Color.red(pixels[i])*alpha);
            red = Math.min(255,red);
            int green = (int)(Color.green(pixels[i])*alpha);
            green = Math.min(255,green);
            int blue = (int)(Color.blue(pixels[i])*alpha);
            blue = Math.min(255,blue);
            pixels[i] = Color.rgb(red,green,blue);
        }
        bmp.setPixels(pixels, 0, w, 0, 0,w , h);
    }


    /**
     *    Fonction qui transforme un pixel rgb en pixel hsv
     *
     * @param pix
     * @return
     */
    public float[] RGBToHSV(int pix){
        int r = Color.red(pix);
        int g = Color.green(pix);
        int b = Color.blue(pix);
        float[] hsv = new float[3];
        Color.RGBToHSV(r, g, b, hsv);
        return hsv;
    }





    /**
     * permet d'appliquer une table d'association sur la luminosité à une image.
     * @param table table d'association des pixels
     */
    public void applyTableHSV(int[] table){
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = new int[w*h];
        bmp.getPixels(pixels, 0, w , 0,0,w,h);
        for (int i = 0; i<w*h;i++){
            float[] hsv = RGBToHSV(pixels[i]);
            hsv[2]=(float)table[(int)(hsv[2]*255)]/255;
            pixels[i]= Color.HSVToColor(hsv);
        }
        bmp.setPixels(pixels, 0, w, 0, 0,w , h);
    }

    /**
     * Fonction permettant de retourner une table d'association de la fonction contraste. Cette table d'association associera les luminosités inferieures
     * au minimum a une luminosité nul, celle superieurs au max a une luminausité maximal et celle entre ces deux valeurs se veront
     * repartis lineairement entre 0 et 255 par raport a leur ancienne repartition.
     * @param min seuil minimum
     * @param max seuil maximum
     * @return table d'association des luminosités
     */
    public int[] contrast(int min, int max) {
        int[] table = new int[256];
        for (int i = 0; i < 256; i++) {
            if (i < min){
                table[i] = 0;
            }
            else if (i > max) {
                table[i] = 255;
            }
            else{
                table[i] = (i-min)*255/(max -min);
            }
        }
        return table;
    }


}
