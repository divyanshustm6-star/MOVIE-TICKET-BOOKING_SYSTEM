package com.movie.moviebooking.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.movie.moviebooking.entity.Booking;
import com.movie.moviebooking.entity.BookingSeat;
import com.movie.moviebooking.entity.Payment;
import com.movie.moviebooking.entity.ShowSeat;
import com.movie.moviebooking.repository.PaymentRepository;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    
    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;

    public TicketService(BookingService bookingService, PaymentRepository paymentRepository) {
        this.bookingService = bookingService;
        this.paymentRepository = paymentRepository;
    }

    public byte[] generateTicketPdf(Long bookingId) throws Exception {
        log.info("Generating ticket PDF for booking ID: {}", bookingId);
        Booking booking = bookingService.getBooking(bookingId);
        String filename = "tickets/" + booking.getBookingReference() + ".pdf";
        File dir = new File("tickets");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(com.lowagie.text.PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        // Color definitions
        Color primaryColor = new Color(15, 23, 42); // slate 900
        Color accentColor = new Color(249, 115, 22); // orange 500 (Ember)
        Color textColor = new Color(51, 65, 85); // slate 700
        Color lightGray = new Color(241, 245, 249); // slate 100
        Color borderColor = new Color(226, 232, 240); // slate 200

        // Typography
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, accentColor);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, primaryColor);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(100, 116, 139));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textColor);
        Font valueBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryColor);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(148, 163, 184));

        // Main table to serve as a container
        PdfPTable containerTable = new PdfPTable(1);
        containerTable.setWidthPercentage(100);

        // Header Cell
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(primaryColor);
        headerCell.setPadding(18);
        headerCell.setBorderWidth(1);
        headerCell.setBorderColor(primaryColor);
        
        Paragraph brandPara = new Paragraph("DIVYANSHU MOVIES", brandFont);
        brandPara.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(brandPara);
        
        Paragraph subBrandPara = new Paragraph("ELECTRONIC BOOKING CONFIRMATION TICKET", titleFont);
        subBrandPara.setAlignment(Element.ALIGN_CENTER);
        subBrandPara.setSpacingBefore(4);
        headerCell.addElement(subBrandPara);
        containerTable.addCell(headerCell);

        // Content Area
        PdfPCell contentCell = new PdfPCell();
        contentCell.setPadding(20);
        contentCell.setBorderWidth(1);
        contentCell.setBorderColor(borderColor);
        contentCell.setBackgroundColor(Color.WHITE);

        // 2-column layout inside content area (left: details, right: QR code)
        PdfPTable innerTable = new PdfPTable(new float[]{68f, 32f});
        innerTable.setWidthPercentage(100);

        // Left Side: Ticket Details
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(PdfPCell.NO_BORDER);

        Paragraph secHeading = new Paragraph("SHOW INFORMATION", sectionHeaderFont);
        secHeading.setSpacingAfter(8);
        leftCell.addElement(secHeading);

        leftCell.addElement(new Paragraph("MOVIE", labelFont));
        Paragraph movieTitle = new Paragraph(booking.getShow().getMovie().getTitle(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor));
        movieTitle.setSpacingAfter(8);
        leftCell.addElement(movieTitle);

        leftCell.addElement(new Paragraph("THEATER", labelFont));
        Paragraph theaterName = new Paragraph(booking.getShow().getScreen().getTheater().getName(), valueBoldFont);
        theaterName.setSpacingAfter(2);
        leftCell.addElement(theaterName);
        Paragraph screenName = new Paragraph(booking.getShow().getScreen().getName(), valueFont);
        screenName.setSpacingAfter(8);
        leftCell.addElement(screenName);

        leftCell.addElement(new Paragraph("DATE & TIME", labelFont));
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a");
        String dateStr = booking.getShow().getShowDate().format(dateFormat);
        String timeStr = booking.getShow().getStartsAt().format(timeFormat);
        Paragraph dateTimePara = new Paragraph(dateStr + " @ " + timeStr, valueBoldFont);
        dateTimePara.setSpacingAfter(8);
        leftCell.addElement(dateTimePara);

        leftCell.addElement(new Paragraph("SEATS", labelFont));
        StringBuilder seatLabels = new StringBuilder();
        for (int i = 0; i < booking.getSeats().size(); i++) {
            BookingSeat bs = booking.getSeats().get(i);
            ShowSeat ss = bs.getShowSeat();
            if (ss != null && ss.getSeat() != null) {
                seatLabels.append(ss.getSeat().getRowLabel()).append(ss.getSeat().getSeatNumber());
                if (i < booking.getSeats().size() - 1) {
                    seatLabels.append(", ");
                }
            }
        }
        Paragraph seatsPara = new Paragraph(seatLabels.toString().isEmpty() ? "N/A" : seatLabels.toString(), valueBoldFont);
        seatsPara.setSpacingAfter(8);
        leftCell.addElement(seatsPara);
        
        leftCell.addElement(new Paragraph("TICKETS COUNT", labelFont));
        Paragraph countPara = new Paragraph(String.valueOf(booking.getSeatsCount()), valueFont);
        leftCell.addElement(countPara);

        innerTable.addCell(leftCell);

        // Right Side: QR Code
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(PdfPCell.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // Retrieve payment ID
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);
        Payment payment = payments.stream()
                .filter(p -> p.getPaymentStatus() == com.movie.moviebooking.entity.PaymentStatus.SUCCESS)
                .findFirst()
                .orElse(null);
        String paymentId = "N/A";
        if (payment != null) {
            paymentId = payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId() :
                        (payment.getProviderTransactionId() != null ? payment.getProviderTransactionId() : payment.getPaymentReference());
        }

        try {
            JSONObject qrJson = new JSONObject();
            qrJson.put("bookingId", booking.getId());
            qrJson.put("paymentId", paymentId);
            qrJson.put("userEmail", booking.getUser() != null ? booking.getUser().getEmail() : "N/A");

            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode(qrJson.toString(), BarcodeFormat.QR_CODE, 140, 140);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", imgOut);
            Image pdfImg = Image.getInstance(imgOut.toByteArray());
            pdfImg.setAlignment(Element.ALIGN_CENTER);
            
            rightCell.addElement(pdfImg);
            
            Paragraph scanPrompt = new Paragraph("SCAN AT THEATER ENTRANCE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, accentColor));
            scanPrompt.setAlignment(Element.ALIGN_CENTER);
            scanPrompt.setSpacingBefore(5);
            rightCell.addElement(scanPrompt);
        } catch (Exception e) {
            log.error("Failed to generate QR Code inside PDF: {}", e.getMessage(), e);
        }

        innerTable.addCell(rightCell);
        contentCell.addElement(innerTable);

        // Separator
        Paragraph divider = new Paragraph("----------------------------------------------------------------------------------------------------", FontFactory.getFont(FontFactory.HELVETICA, 10, borderColor));
        divider.setSpacingBefore(12);
        divider.setSpacingAfter(12);
        contentCell.addElement(divider);

        // Booking Receipt Info (3 columns: Ref, Payment, Amount)
        PdfPTable billTable = new PdfPTable(3);
        billTable.setWidthPercentage(100);

        PdfPCell c1 = new PdfPCell();
        c1.setBorder(PdfPCell.NO_BORDER);
        c1.addElement(new Paragraph("BOOKING REFERENCE", labelFont));
        c1.addElement(new Paragraph(booking.getBookingReference(), valueBoldFont));
        billTable.addCell(c1);

        PdfPCell c2 = new PdfPCell();
        c2.setBorder(PdfPCell.NO_BORDER);
        c2.addElement(new Paragraph("PAYMENT ID", labelFont));
        c2.addElement(new Paragraph(paymentId, valueBoldFont));
        billTable.addCell(c2);

        PdfPCell c3 = new PdfPCell();
        c3.setBorder(PdfPCell.NO_BORDER);
        c3.addElement(new Paragraph("TOTAL AMOUNT PAID", labelFont));
        c3.addElement(new Paragraph("Rs. " + booking.getTotalAmount().toString(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, accentColor)));
        billTable.addCell(c3);

        contentCell.addElement(billTable);
        containerTable.addCell(contentCell);

        // Terms and conditions
        PdfPCell footerCell = new PdfPCell();
        footerCell.setBackgroundColor(lightGray);
        footerCell.setPadding(10);
        footerCell.setBorderWidth(1);
        footerCell.setBorderColor(borderColor);
        Paragraph infoPara = new Paragraph("Please present this ticket at the counter. Food and beverages are subject to availability inside the hall. Once booked, tickets cannot be cancelled or exchanged.", footerFont);
        infoPara.setAlignment(Element.ALIGN_CENTER);
        footerCell.addElement(infoPara);
        containerTable.addCell(footerCell);

        doc.add(containerTable);
        doc.close();

        byte[] pdfBytes = baos.toByteArray();
        
        // Write PDF file to folder
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(pdfBytes);
        }
        
        // Save ticket path
        booking.setTicketPath(filename);
        bookingService.updateTicketPath(bookingId, filename);
        
        log.info("Successfully generated and saved ticket PDF to {}", filename);
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
