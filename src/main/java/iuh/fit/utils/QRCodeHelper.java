package iuh.fit.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.awt.image.BufferedImage;

public class QRCodeHelper {

    public static BufferedImage generateQRCode(String text, int width, int height) {
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            return MatrixToImageWriter.toBufferedImage(bitMatrix);

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
