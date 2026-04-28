import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class CoverManager {

    // Sucht nach einem existierenden Cover für eine bestimmte ID
    public static File getCoverFile(int appId) {
        File localJpg = new File(ProfileManager.SAVE_DIR + "/covers/" + appId + ".jpg");
        File localPng = new File(ProfileManager.SAVE_DIR + "/covers/" + appId + ".png");
        File localJpeg = new File(ProfileManager.SAVE_DIR + "/covers/" + appId + ".jpeg");

        if (localJpg.exists()) return localJpg;
        if (localPng.exists()) return localPng;
        if (localJpeg.exists()) return localJpeg;
        
        return null; // Kein eigenes Cover gefunden
    }

    // Kopiert das ausgewählte Bild in den Covers-Ordner
    public static void saveCover(File sourceFile, int appId) throws Exception {
        Path coversPath = Paths.get(ProfileManager.SAVE_DIR, "covers");
        if (!Files.exists(coversPath)) {
            Files.createDirectories(coversPath);
        }

        String fileName = sourceFile.getName();
        String extension = fileName.substring(fileName.lastIndexOf("."));
        Path targetPath = coversPath.resolve(appId + extension);

        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    // Schneidet das Bild proportional zu
    public static void applyProportionalSize(ImageView iv, Image img, double targetW, double targetH) {
        double imgW = img.getWidth();
        double imgH = img.getHeight();

        double targetAspect = targetW / targetH;
        double imgAspect = imgW / imgH;

        double subW, subH, subX, subY;

        if (imgAspect > targetAspect) {
            subH = imgH;
            subW = imgH * targetAspect;
            subX = (imgW - subW) / 2;
            subY = 0;
        } else {
            subW = imgW;
            subH = imgW / targetAspect;
            subX = 0;
            subY = (imgH - subH) / 2;
        }
        iv.setViewport(new Rectangle2D(subX, subY, subW, subH));
        iv.setFitWidth(targetW);
        iv.setFitHeight(targetH);
    }
}