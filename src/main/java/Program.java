import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.lang.System.err;

public class Program  implements Runnable{

    public OptionsFrame optionsFrame = new OptionsFrame();
    public ManipulatedImage original;
    public ManipulatedImage edited;
    public BufferedImage preview, miniature;
    public JPanel canvas = optionsFrame.panel;
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


        if (hasFile == fc.APPROVE_OPTION) {
            File fl = new File(fc.getSelectedFile().getPath());
            original = new ManipulatedImage(fl);
            resetImage();
            hasImage = true;
            blockInputs(optionsFrame.saveFileButton,hasImage);
            blockInputs(optionsFrame.resetFileButton,hasImage);
            render();
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

        fc.setDialogTitle("Salvar Imagem");
        fc.setApproveButtonText("Salvar");

        if(fc.showOpenDialog(optionsFrame) != JFileChooser.APPROVE_OPTION){
            return;
        }
        File choosed = fc.getSelectedFile();
        ImageIO.write(edited.image, "png", choosed);

    }
    
    /*Atualiza a Imagem na Janela de Visualização*/
    public void render() {
        Graphics2D g2d = (Graphics2D) canvas.getGraphics();
        g2d.setBackground(new Color(0x00FFFFFF, true));
        g2d.setColor(new Color(0x00FFFFFF, true));
        g2d.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        g2d.drawImage(edited.image, 25, 20, null);
    }

    /*Adiciona os Listeners aos botões e Slider */
    public void addListeners() {
        optionsFrame.panel.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (hasImage)
                    updatePreview();
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
            updatePreview();
        });

        optionsFrame.applyRefinementButton.addActionListener(e ->{
            edited.zhangSuen();
            updatePreview();
        });

        optionsFrame.invertColorButton.addActionListener(e ->{
            edited.makeNegative();
            updatePreview();
        });

        optionsFrame.resetFileButton.addActionListener(e -> {
            resetImage();
        });

    }

    /*Reseta a imagem para o estado original*/
    private void resetImage(){
        try {
        edited = new ManipulatedImage(new File(original.path));
        updatePreview();
    } catch (IOException ioException) {
        ioException.printStackTrace();
         }
    }

    public void updatePreview(){
        int ratio;
        if((edited.image.getHeight() > canvas.getHeight()) || (edited.image.getWidth() > canvas.getWidth())) {
            if (edited.image.getHeight() > edited.image.getWidth()) {
                ratio = (edited.image.getHeight()) / canvas.getHeight();
            } else {
                ratio = (edited.image.getWidth()) / canvas.getWidth();
            }

            Image temp = edited.image.getScaledInstance(edited.image.getWidth()/ratio,
                                                edited.image.getHeight()/ratio, Image.SCALE_FAST);

            preview = new BufferedImage(temp.getWidth(null), temp.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            preview.getGraphics().drawImage(temp, 0, 0, null);
        }
        render();
    }

    /*Monta as Janelas*/
    public void assembleFrames() {

        optionsFrame.setResizable(true);
        optionsFrame.setVisible(true);
        addListeners();
    }
    
    /*Bloqueia ou libera o Acesso aos Botões de Modificação*/
    public void blockInputs (JComponent component, boolean b){
        component.setEnabled(b);
    }

    /*Método Principal*/
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.printf(String.valueOf(e));
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

