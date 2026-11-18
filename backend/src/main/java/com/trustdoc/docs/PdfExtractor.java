package com.trustdoc.docs;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Extrait le texte d'un PDF, page par page (Apache PDFBox), pour pouvoir citer la page exacte. */
@Component
public class PdfExtractor {

    public List<String> pages(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> pages = new ArrayList<>();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                // NFC : PDFBox peut renvoyer « é » en deux caractères (e + accent) ; on les recompose.
                pages.add(Normalizer.normalize(stripper.getText(doc), Normalizer.Form.NFC));
            }
            return pages;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce fichier n'est pas un PDF lisible.");
        }
    }
}
