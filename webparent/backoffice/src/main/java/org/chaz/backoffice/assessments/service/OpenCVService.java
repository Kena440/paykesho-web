package org.chaz.backoffice.assessments.service;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;


@Service
public class OpenCVService {

    public Mat addText(Mat image, String text, Point position, double fontScale, Scalar fontColor) {
        Mat resultImage = image.clone();

        // Define the font and other text properties
        int fontFace = Imgproc.FONT_HERSHEY_SIMPLEX;
        int thickness = 2;

        // Add text to the image
        Imgproc.putText(
                resultImage,           // The image to which text is added
                text,                  // The text to be added
                position,              // Position of the text (x, y)
                fontFace,              // Font type (e.g., Core.FONT_HERSHEY_SIMPLEX)
                fontScale,             // Font scale
                fontColor,             // Font color (e.g., new Scalar(0, 0, 255) for blue)
                thickness              // Thickness of the text
        );

        return resultImage;
    }
}





