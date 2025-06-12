package com.example.tb.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QrCodeUtil {
    public static void generateQRCodeImage(String text, int width, int height, String filePath)
            throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        Path path = new File(filePath).toPath();
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }

    public static void generateComplexQRCode(String text, int width, int height, String filePath)
            throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // Custom colors
        int qrColor = 0xFF0000FF; // Dark blue
        int backgroundColor = 0xFFFFFFFF; // White

        // Apply custom colors
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? qrColor : backgroundColor);
            }
        }

        // Add a border
        int borderSize = 10;
        BufferedImage borderedImage = new BufferedImage(width + 2 * borderSize, height + 2 * borderSize,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = borderedImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, borderedImage.getWidth(), borderedImage.getHeight());
        g.drawImage(qrImage, borderSize, borderSize, null);
        g.dispose();

        // Save to file
        ImageIO.write(borderedImage, "PNG", new File(filePath));
    }
}