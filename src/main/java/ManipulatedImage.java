import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManipulatedImage  {
    public BufferedImage image;
    public Color[][] matrix;
    public String path;
    public boolean isBW;

    //Contrutor, recebe um arquivo de imagem
    public ManipulatedImage (File f) throws IOException {
        image = ImageIO.read(f);
        path = f.getPath();
        createMatrixByImage();
        isBW = false;
    }

    //Transforma a imagem em escala de cinza
    public void makeGrayscale () {
        Color helper;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < matrix[0].length; y++) {
                int r = matrix[x][y].getRed();
                int g = matrix[x][y].getGreen();
                int b = matrix[x][y].getBlue();

                int media = (r+g+b)/3;

                helper = new Color(media,media,media);
                matrix[x][y] = helper;
            }
        }

        createImageByMatrix(matrix);
        createMatrixByImage();
    }

    //Transforma a imagem em escala de cinza
    public void makeBlackAndWhite (int threshold) {
        makeGrayscale();

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < matrix[0].length; y++) {
                int current = matrix[x][y].getRed();

                if(current < threshold)
                    matrix[x][y] = new Color(0,0,0);
                else
                     matrix[x][y] = new Color(255,255,255);
            }
        }

        createImageByMatrix(matrix);
        createMatrixByImage();
        isBW = true;
    }


    //aplica o afinamento de zhangSuen
    public void zhangSuen(){
        int connected, neighbor;
        Color bk = new Color(0,0,0);
        Color wh = new Color(255,255,255);
        
        List<Point> pointsToChange = new ArrayList< >();

        boolean hasChange = true;

        while (hasChange) {

            hasChange = false;

            for (int x = 0; x  < matrix.length; x++) {
                for (int y = 0; y < matrix[x].length; y++) {

                    try {
                        connected = numberOfConnections(x,y);
                        neighbor = blackNeighborns(x,y);

                        if ((matrix [x][y].equals(bk))
                                && (neighbor >= 2)
                                && (neighbor < 7)
                                && (connected == 1)

                                && (condition(matrix[x][y-1].equals(wh)
                                , matrix[x+1][y].equals(wh)
                                , matrix[x][y+1].equals(wh)))

                                && (condition(matrix[x+1][y].equals(wh)
                                , matrix[x][y+1].equals(wh)
                                , matrix[x-1][y].equals(wh)))) {

                            pointsToChange.add(new Point(x, y));
                            hasChange = true;
                        }
                    }catch (Exception e){
                    }
                }
            }
            for (Point point : pointsToChange)
                matrix[point.x][point.y] = wh;

            pointsToChange.clear();

            for (int x = 0; x  < matrix.length; x++) {
                for (int y = 0; y  < matrix[x].length; y++) {

                    try {
                        connected = numberOfConnections(x,y);
                        neighbor = blackNeighborns(x,y);

                        if ((matrix [x][y].equals(bk))
                                && (neighbor >= 2)
                                && (neighbor < 7)
                                && (connected == 1)

                                && (condition(matrix[x][y-1].equals(wh)
                                , matrix[x+1][y].equals(wh)
                                , matrix[x-1][y].equals(wh)))

                                && (condition(matrix[x][y-1].equals(wh)
                                , matrix[x][y+1].equals(wh)
                                , matrix[x-1][y].equals(wh)))) {

                            pointsToChange.add(new Point(x, y));
                            hasChange = true;
                        }
                    }catch (Exception e){}
                }
            }
            for (Point point : pointsToChange)
                matrix[point.x][point.y] = wh;

            pointsToChange.clear();
        }

        createImageByMatrix(matrix);
        createMatrixByImage();
        System.out.println("Applied");

    }

    //descobre a quantos pixels pretos
    // um pixel está conectado
    private int numberOfConnections(int x, int y) {
        int count = 0;
        Color bk = new Color(0,0,0);
        Color wh = new Color(255,255,255);

        //p2 p3
        if ( matrix[x][y-1].equals(wh) && matrix[x+1][y-1].equals(bk))
             count++;

        //p3 p4
        if ( matrix[x+1][y-1].equals(wh) && matrix[x+1][y].equals(bk))
            count++;

        //p4 p5
        if ( matrix[x+1][y].equals(wh) && matrix[x+1][y+1].equals(bk) )
            count++;

        //p5 p6
        if ( matrix[x+1][y+1].equals(wh) && matrix[x][y+1].equals(bk))
            count++;

        //p6 p7
        if (matrix[x][y+1].equals(wh) && matrix[x-1][y+1].equals(bk))
            count++;

        //p7 p8
        if ( matrix[x-1][y+1].equals(wh) && matrix[x-1][y].equals(bk))
            count++;

        //p8 p9
        if (matrix[x-1][y].equals(wh) && matrix[x-1][y-1].equals(bk))
            count++;

        //p9 p2
        if (matrix[x-1][y-1].equals(wh) && matrix[x][y-1].equals(bk))
            count++;

        return count;
    }

    private boolean condition(boolean c1, boolean c2, boolean c3){
        int a,b,c;

        a = (c1 == true) ?  0 : 1;
        b = (c2 == true) ?  0 : 1;
        c = (c3 == true) ?  0 : 1;

        return (a*b*c) == 0;

    }

    //descobre os pixels vizinhos pretos de um pixel
    private int blackNeighborns(int x, int y) {
        return (255-matrix[x][y-1].getBlue()
                + 255-matrix[x+1][y-1].getBlue()
                + 255-matrix[x+1][y].getBlue()
                + 255-matrix[x+1][y+1].getBlue()
                + 255-matrix[x][y+1].getBlue()
                + 255-matrix[x-1][y+1].getBlue()
                + 255-matrix[x-1][y].getBlue()
                + 255-matrix[x-1][y-1].getBlue())/255;
    }

    //Transforma para o negativo da imagem
    public void makeNegative () {
        Color helper;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < matrix[0].length; y++) {
                int r = matrix[x][y].getRed();
                int g = matrix[x][y].getGreen();
                int b = matrix[x][y].getBlue();

                helper = new Color(255-r,255-g,255-b);
                matrix[x][y] = helper;
            }
        }

        createImageByMatrix(matrix);
        createMatrixByImage();
    }

    //Cria a matrix de pixel a partir da imagem
    private void createMatrixByImage() {
        matrix = new Color[image.getWidth()][image.getHeight()];

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                matrix[x][y] = new Color (image.getRGB(x,y),true);
            }
        }
    }

    //Cria a imagem a partir da matrix de pixel
    private void createImageByMatrix(Color[][] edited) {
        BufferedImage newImage = new BufferedImage(edited.length, edited[0].length,
                                        BufferedImage.TYPE_INT_ARGB);

        //System.err.printf("%d %d \n", edited.length, edited[0].length);

        for (int x = 0; x < edited.length; x++) {
            for (int y = 0; y < edited[0].length; y++) {
                //System.err.println(edited[x][y]);
                if (edited[x][y] != null)
                    newImage.setRGB(x,y,edited[x][y].getRGB());
            }
        }
        this.image=newImage;
    }

}
