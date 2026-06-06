package com.movie.moviebooking.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.movie.moviebooking.entity.Booking;
import com.movie.moviebooking.entity.BookingSeat;
import com.movie.moviebooking.entity.ShowSeat;
import com.movie.moviebooking.service.BookingService;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class TicketService {
    private final BookingService bookingService;

    public TicketService(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public byte[] generateTicketPdf(Long bookingId) throws Exception {
        Booking booking = bookingService.getBooking(bookingId);
        String filename = "tickets/" + booking.getBookingReference() + ".pdf";
        File dir = new File("tickets");
        if (!dir.exists()) dir.mkdirs();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, baos);
        doc.open();

        // Header
        doc.add(new Paragraph("Movie Booking - Ticket"));
        doc.add(new Paragraph("Booking ref: " + booking.getBookingReference()));
        doc.add(new Paragraph("Movie: " + booking.getShow().getMovie().getTitle()));
        doc.add(new Paragraph("Theater: " + booking.getShow().getScreen().getTheater().getName()));
        doc.add(new Paragraph("Screen: " + booking.getShow().getScreen().getName()));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
        doc.add(new Paragraph("Show: " + booking.getShow().getShowDate() + " " + booking.getShow().getStartsAt()));

        StringBuilder seats = new StringBuilder();
        for (BookingSeat bs : booking.getSeats()) {
            ShowSeat ss = bs.getShowSeat();
            seats.append(ss.getSeat().getRowLabel()).append(ss.getSeat().getSeatNumber()).append(" ");
        }
        doc.add(new Paragraph("Seats: " + seats.toString()));
        doc.add(new Paragraph("Total: " + booking.getTotalAmount()));

        // QR code
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode(booking.getBookingReference(), BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", imgOut);
            Image pdfImg = Image.getInstance(imgOut.toByteArray());
            doc.add(pdfImg);
        } catch (WriterException we) {
            // ignore QR failures
        }

        doc.close();

        byte[] pdfBytes = baos.toByteArray();
        // write file
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(pdfBytes);
        fos.close();

        // update booking ticket path
        booking.setTicketPath(filename);
        bookingService.updateTicketPath(bookingId, filename);

        return pdfBytes;
    }

    public byte[] loadTicketBytes(Long bookingId) throws Exception {
        Booking booking = bookingService.getBooking(bookingId);
        if (booking.getTicketPath() == null) {
            return generateTicketPdf(bookingId);
        }
        File f = new File(booking.getTicketPath());
        if (!f.exists()) {
            return generateTicketPdf(bookingId);
        }
        return Files.readAllBytes(f.toPath());
    }
}
