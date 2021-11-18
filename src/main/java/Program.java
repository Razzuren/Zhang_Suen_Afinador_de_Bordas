import com.formdev.flatlaf.FlatDarculaLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.lang.System.err;

public class Program  implements Runnable{

    public OptionsFrame optionsFrame = new OptionsFrame();
    public ManipulatedImage original;
    public ManipulatedImage edited;
    public ManipulatedImage miniature;
    public BufferedImage preview;
    public JPanel canvas = optionsFrame.panel, miniaturePanel=optionsFrame.bwPanel;
    public Boolean hasImage = false;

    /*Abre uma imagem a partir de um arquivo png ou jpg*/
    public void selectImage() throws IOException {
        JFileChooser fc = new JFileChooser();
        int hasFile;
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                final String[] imagesExtentions = new String[] {"jpg", "png"};
                {
                    for (String extension : imagesExtentions)
                    {
                        if (f.getName().toLowerCase().endsWith(extension))
                        {
                            return true;
                        }
                    }
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return "Imagens PNG ou JPEG";
            }
        });
        hasFile = fc.showOpenDialog(optionsFrame);


        if (hasFile == JFileChooser.APPROVE_OPTION) {
            File fl = new File(fc.getSelectedFile().getPath());
            original = new ManipulatedImage(fl);
            resetImage();
            hasImage = true;
            blockInputs(optionsFrame.saveFileButton,hasImage);
            blockInputs(optionsFrame.resetFileButton,hasImage);
        } else {
            err.print("Open command cancelled by user.");
        }
    }

    /*Salva a imagem como novo arquivo*/
    public void saveFile() throws IOException{
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(edited.path));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                final String[] imagesExtentions = new String[] {"png"};
                {
                    for (String extension : imagesExtentions)
                    {
                        if (f.getName().toLowerCase().endsWith(extension))
                        {
                            return true;
                        }
                    }
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return "Imagens PNG";
            }
        });

        fc.setDialogTitle("Salvar Imagem");
        fc.setApproveButtonText("Salvar");

        if(fc.showOpenDialog(optionsFrame) != JFileChooser.APPROVE_OPTION){
            return;
        }
        File choosed = fc.getSelectedFile();
        ImageIO.write(edited.image, "png", new File((choosed + ".png")));

    }
    
    /*Atualiza a Imagem na Janela de Visualização*/
    public void render() {
        try {
                Graphics2D g2d = (Graphics2D) canvas.getGraphics();
                g2d.setBackground(new Color(0x00FFFFFF, true));
                g2d.setColor(new Color(0x00FFFFFF, true));
                g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                if (hasImage)
                    g2d.drawImage(preview, (canvas.getWidth() / 2) - (preview.getWidth() / 2), 10, null);
        }catch (Exception ignored){}
    }

    /*Adiciona os Listeners aos botões e Slider */
    public void addListeners() {
        optionsFrame.panel.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (hasImage)
                    updatePreview(true);
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });

        optionsFrame.openFileButton.addActionListener( e ->{
            try {
                selectImage();
                hasImage = true;
                blockInputs(optionsFrame.makeBWButton,hasImage);
                blockInputs(optionsFrame.threshSlider,hasImage);
                blockInputs(optionsFrame.invertColorButton,hasImage);
            } catch (IOException ioException) {
                ioException.printStackTrace();}
        });

        optionsFrame.saveFileButton.addActionListener( e ->{
            try {
                saveFile();
                hasImage = true;
            } catch (IOException ioException) {
                ioException.printStackTrace();}
        });

        optionsFrame.makeBWButton.addActionListener(e ->{
            edited.makeBlackAndWhite(optionsFrame.threshSlider.getValue());
            blockInputs(optionsFrame.applyRefinementButton,edited.isBW);
            updatePreview(false);
        });

        optionsFrame.applyRefinementButton.addActionListener(e ->{
            edited.zhangSuen();
            updatePreview(false);
        });

        optionsFrame.invertColorButton.addActionListener(e ->{
            edited.makeNegative();
            updatePreview(false);
        });

        optionsFrame.resetFileButton.addActionListener(e -> resetImage());

        optionsFrame.threshSlider.addChangeListener(e -> updateMiniature(optionsFrame.threshSlider.getValue()));

        optionsFrame.threshSlider.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                showMiniature(true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showMiniature(false);
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

    }

    /*Reseta a imagem para o estado original*/
    private void resetImage(){
        try {
        edited = new ManipulatedImage(new File(original.path));
        blockInputs(optionsFrame.applyRefinementButton,edited.isBW);
        updatePreview(true);
    } catch (IOException ioException) {
        ioException.printStackTrace();
         }
    }

    /*Cria uma nova imagem prévia*/
    public void updatePreview(boolean sizeHasChange){
        try {
            if (sizeHasChange) {
                if ((edited.image.getHeight() > canvas.getHeight()) || (edited.image.getWidth() > canvas.getWidth())) {

                    float ratio, ratioX, ratioY;
                    ratioX = ((float) edited.image.getHeight() / (float) canvas.getHeight());
                    ratioY = ((float) edited.image.getWidth() / (float) canvas.getWidth());

                    ratio = Math.max(ratioY, ratioX);

                    System.err.println(ratio);
                    int newHeight = (int) (edited.image.getHeight() / ratio);
                    int newWidth = (int)  (edited.image.getWidth() / ratio);

                    Image temp = edited.image.getScaledInstance(newWidth,newHeight, Image.SCALE_FAST);

                    preview = new BufferedImage(temp.getWidth(null), temp.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    preview.getGraphics().drawImage(temp, 0, 0, null);

                }
            }else {
                Image temp = edited.image.getScaledInstance(preview.getWidth(),
                        preview.getHeight(), Image.SCALE_FAST);

                preview = new BufferedImage(temp.getWidth(null), temp.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                preview.getGraphics().drawImage(temp, 0, 0, null);
            }
            optionsFrame.repaint();
        }catch (Exception ignored){}
    }

    /*Atualiza a imagem da miniatura*/
    public void updateMiniature(int v){
        Image temp = preview.getScaledInstance(100,100, Image.SCALE_FAST);
        BufferedImage tempBuffered = new BufferedImage(temp.getWidth(null),
                        temp.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        tempBuffered.getGraphics().drawImage(temp, 0, 0, null);

        miniature = new ManipulatedImage(tempBuffered);
        miniature.makeBlackAndWhite(v);
        miniaturePanel.getGraphics().drawImage(miniature.image,0,0,null);

    }

    /*Mostra a imagem da miniatura*/
    public void showMiniature(boolean show){
        miniaturePanel.setVisible(show);
    }

    /*Monta as Janelas*/
    public void assembleFrames() {

        optionsFrame.setResizable(true);
        optionsFrame.setVisible(true);
        addListeners();
    }
    
    /*Bloqueia ou libera o Acesso aos Botões e Componentes da Interface Gráfica*/
    public void blockInputs (JComponent component, boolean b){
        component.setEnabled(b);
    }

    /*Método Principal*/
    public static void main(String[] args) {
        try {
            FlatDarculaLaf.setup();
            UIManager.put( "Button.arc", 999 );
            UIManager.put( "Component.arc", 999 );
        } catch (Exception e) {
            System.err.print(String.valueOf(e));
        }

        Program p = new Program();
        p.assembleFrames();
        p.optionsFrame.setVisible(true);
        p.run();
    }

    @Override
    public void run() {
        boolean running = true;

        long last = System.nanoTime();
        double when = 1000000000 / 10;
        double delta = 0;
        long now = 0;
        int framePerSecond=0;
        double timer = System.currentTimeMillis();

        /*Limita a taxa de atualização de imagem para 60 vezes
        garantindo sincronismo entre a renderização da imagem
        e as alterações feitas*/

        while(running){
            now = System.nanoTime();
            delta +=(now-last)/when;
            last = now;
            if (delta >= 1){
                if(hasImage){
                    render();
                    delta--;
                    framePerSecond++;
                }
            }
            if (System.currentTimeMillis() - timer >= 1000){
                System.out.println("Fps: "+framePerSecond);
                timer+=1000;
                framePerSecond=0;
            }
        }
    }
}

